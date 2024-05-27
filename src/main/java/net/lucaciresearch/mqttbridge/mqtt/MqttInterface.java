package net.lucaciresearch.mqttbridge.mqtt;

import com.hivemq.client.internal.mqtt.message.auth.MqttSimpleAuth;
import com.hivemq.client.internal.mqtt.message.auth.MqttSimpleAuthBuilder;
import com.hivemq.client.internal.util.AsyncRuntimeException;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuthBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.client.rx.FlowableWithSingle;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.exceptions.ConnectionFailedException;
import net.lucaciresearch.mqttbridge.util.ConnectionManager;
import net.lucaciresearch.mqttbridge.util.Util;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;


@Slf4j
public class MqttInterface {

    @Inject
    @Getter
    private MqttConfig config;

    @Getter
    private volatile boolean isOpen = false;

    private final PublishSubject<Boolean> obsIsOpenStream = PublishSubject.create();

    private final ConnectionManager connectionManager = new ConnectionManager();

    private Mqtt5RxClient client;


    public MqttInterface() {
        connectionManager.creator(() -> {
            log.info("Connecting to MQTT");
            client = Mqtt5Client.builder()
                    .identifier(config.publisherId())
                    .serverHost(config.host())
                    .serverPort(config.port())
                    .willPublish(Mqtt5Publish.builder()
                            .topic(config.baseTopic())
                            .qos(MqttQos.EXACTLY_ONCE)
                            .payload("\"{ \"system\": false }\"".getBytes())
                            .retain(true)
                            .build()
                    )
                    .automaticReconnectWithDefaultConfig()
                    .buildRx();

            Single<Mqtt5ConnAck> ack = client.connectWith()
                    .simpleAuth()
                    .username(config.username())
                    .password(config.password().getBytes())
                    .applySimpleAuth()
                    .applyConnect();

            try {
                ack.toFlowable().blockingSubscribe(); // how to get errors?
            } catch (RuntimeException e) {
                log.warn("Failed to connect to mqtt because {}", Util.unwrap(e).getMessage());
                return false;
            }
            log.info("Connected to MQTT");

            try {
                client.publish(Single.just(Mqtt5Publish.builder()
                        .topic(config.baseTopic())
                        .qos(MqttQos.EXACTLY_ONCE)
                        .payload("\"{ \"system\": true }\"".getBytes())
                        .retain(true)
                        .build()
                ).toFlowable()).blockingSubscribe();
            } catch (RuntimeException e) {
                log.warn("Failed to connect to mqtt because {}", Util.unwrap(e).getMessage());
                return false;
            }
            log.info("Published online state");

            isOpen = true;
            obsIsOpenStream.onNext(true);
            return true;
        });
        connectionManager.destroyer(() -> {
            log.info("Disconnecting from MQTT");
            isOpen = false;
            obsIsOpenStream.onNext(false);
            if (client == null)
                return;
            try {
                client.disconnect().blockingAwait();
            } catch (RuntimeException e) {
                log.warn("Failed to disconnect from mqtt because {}", Util.unwrap(e).getMessage());
            }
            client = null;
        });
        connectionManager.baseDelay(2000);
    }

    public void startConnection() {
        connectionManager.start();
    }

    public Single<Mqtt5PublishResult> publishTopic(String topic, String message, boolean retained) /* throws ConnectionFailedException, AsyncRuntimeException */ {

        if (!isOpen) {
            return Single.<Mqtt5PublishResult>error(new ConnectionFailedException("Not connected"));
        }

        return client.publish(Single.just(Mqtt5Publish.builder()
                .topic(topic)
                .qos(MqttQos.EXACTLY_ONCE)
                .payload(message.getBytes())
                .retain(retained)
                .build()
        ).toFlowable()).doOnNext((pr) -> {
            if (pr.getError().isPresent()) {
                log.error("Failed to publish to MQTT topic {} because {}", topic, pr.getError().get().getMessage());
                throw (AsyncRuntimeException) pr.getError().get();
            }
        }).singleOrError();
    }

    public Flowable<TopicMessage> subscribeTopic(String topic) /* throws ConnectionFailedException, AsyncRuntimeException */ {

        if (!isOpen) {
            return Single.<TopicMessage>error(new ConnectionFailedException("Not connected")).toFlowable();
        }

        FlowableWithSingle<Mqtt5Publish, Mqtt5SubAck> res = client.subscribePublishesWith()
                .topicFilter(topic)
                .qos(MqttQos.EXACTLY_ONCE)
                .applySubscribe();

        res.doOnError(t -> {
            log.error("Failed to subscribe to MQTT topic {} because {}", topic, t.getMessage());
            connectionManager.markFailed();
        });

        res.doOnSingle(mqtt5SubAck -> {
            log.debug("Success subscribing to MQTT topic {}", topic);
        });

        return res.map(mqtt5Publish -> {
            return new TopicMessage(
                    StandardCharsets.UTF_8.decode(mqtt5Publish.getTopic().toByteBuffer()).toString(),
                    mqtt5Publish.getPayload().map(bb -> StandardCharsets.UTF_8.decode(bb).toString()).orElse("")
            );
        });
    }

    public Observable<Boolean> isOpenStream() {
        return obsIsOpenStream;
    }

    public void closeConnection() {
        connectionManager.stop();
    }

}
