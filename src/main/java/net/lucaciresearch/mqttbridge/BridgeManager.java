package net.lucaciresearch.mqttbridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.client.internal.util.AsyncRuntimeException;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.publish.puback.Mqtt5PubAck;
import io.reactivex.Flowable;
import io.reactivex.Single;
import jakarta.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.lucaciresearch.mqttbridge.data.Availability;
import net.lucaciresearch.mqttbridge.data.VariableNode;
import net.lucaciresearch.mqttbridge.data.InfoState;
import net.lucaciresearch.mqttbridge.device.DevicePropertiesInterface;
import net.lucaciresearch.mqttbridge.exceptions.CallFailException;
import net.lucaciresearch.mqttbridge.exceptions.VariableUnavailableException;
import net.lucaciresearch.mqttbridge.mqtt.*;
import net.lucaciresearch.mqttbridge.util.*;
import net.lucaciresearch.mqttbridge.device.DeviceCallInterface;
import net.lucaciresearch.mqttbridge.exceptions.ConnectionFailedException;


import javax.naming.OperationNotSupportedException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class BridgeManager<DTy> {

    @Inject
    private MqttInterface mqttInterface;

    @Inject
    private DiscoveryConfig discoveryConfig;

    @Inject
    private ObjectMapper mapper;

    @Setter
    private DevicePropertiesInterface<DTy, ?> devicePropertiesInterface;

    @Setter
    private DeviceCallInterface<DTy> dci;

    private final int pollingRetryCount = 3;

    public boolean isDoingDiscovery = false;

    private final Map<PollSpeed, ReentrantLock> pollLocks = new HashMap<>();

    private final ScheduledExecutorService pollingExecutor = Executors.newScheduledThreadPool(2 * PollSpeed.values().length + 2);

    public BridgeManager() {

    }

    public void start() {
        dci.getNodes().forEach(n -> n.infoState(InfoState.UNINITIALIZED));
        dci.getNodes().forEach(n -> n.availability(Availability.UNAVAILABLE));

        initializeCallbacks();

        initializePolling();
        mqttInterface.startConnection();
        dci.initializeConnection();
    }

    void initializeCallbacks() {

        dci.isOpenStream().subscribe(open -> {
            CompletableFuture.runAsync(() -> {
                if (!open)
                    return;
                try {
                    discoverInitialValues();
                } catch (ConnectionFailedException e) {
                    log.error("Error occurred during device setup: {}", e.getMessage());
                }
            }, pollingExecutor);
        });

        mqttInterface.isOpenStream().subscribe(open -> {
            CompletableFuture.runAsync(() -> {
                if (!open)
                    return;
                try {
                    // specific order - values, discovery, availability and then subscribe
                    publishValues();
                    publishDiscovery();
                    publishAvailability();
                    subscribeTopics();
                } catch (AsyncRuntimeException | ConnectionFailedException e) {
                    log.error("Error occurred during MQTT topics setup: {}", e.getMessage());
                }
            }, pollingExecutor);
        });

        dci.notifyValue().subscribe(dTyKeyValue -> {
            VariableNode<?, DTy> variable = dci.getNodes().stream().filter(n -> n.deviceKey().equals(dTyKeyValue.key())).findAny().orElse(null);
            if (variable == null) {
                log.warn("Device sent update for variable {} that is unimplemented", dTyKeyValue.key());
                return;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    lockVariable(variable);

                    if (variable.availability() == Availability.UNAVAILABLE) {
                        if (!isDoingDiscovery)
                            log.error("DEV ERROR: Device sent update for variable {} that is unavailable", dTyKeyValue.key());
                        variable.lock().unlock();
                        return;
                    }

                    variable.parseDevice(dTyKeyValue.value());
                } catch (VariableUnavailableException ve) {
                    log.error("Device sent update for variable {} that is unavailable", dTyKeyValue.key());
                }catch (Exception e) {
                    // should not throw any exception here!!!
                    e.printStackTrace();
                } finally {
                    variable.lock().unlock();
                }



                try {
                    variable.parseDevice(dTyKeyValue.value());
                } catch (VariableUnavailableException ve) {
                    log.error("Device sent update for variable {} that is unavailable", dTyKeyValue.key());
                }


            }, pollingExecutor);

        });

        dci.getNodes().forEach(vb -> {
            vb.availabilityStream().subscribe(availability -> {
                CompletableFuture.runAsync(() -> {
                    try {
                        if (availability == Availability.AVAILABLE)
                            publishVariableNow(vb);
                        publishAvailability();
                        if (availability == Availability.UNAVAILABLE)
                            publishVariableNow(vb);
                    } catch (AsyncRuntimeException e) {
                        log.error("Error occurred during re-publishing availability: {}", e.getMessage());
                    } catch (ConnectionFailedException e) {
                        log.info("Variable {} changed availability but MQTT is not connected", vb.deviceKey());
                    }
                });
            });
            vb.infoStateStream().subscribe(infoState -> {

                if (!vb.availability().bool())
                    return;

                if (infoState == InfoState.DIRTY_MQTT && mqttInterface.isOpen()) {
                    try {
                        publishVariableNow(vb);
                        vb.infoState(InfoState.OKAY);
                    } catch (AsyncRuntimeException e) {
                        log.warn("Failed to publish variable {} value to MQTT: {}", vb.deviceKey(), e.getMessage());
                    }
                }
                if (infoState == InfoState.DIRTY_DEVICE && dci.isOpen()) {
                    if (flushDeviceVariableNow(vb)) {
                        vb.infoState(InfoState.OKAY);
                    }
                }
            });
        });

    }

    private void discoverInitialValues() throws ConnectionFailedException {
        List<VariableNode<?, DTy>> concluded = new ArrayList<>();
        isDoingDiscovery = true;
        for (int i = 0; i < pollingRetryCount; i++) {
            for (VariableNode<?, DTy> varb : dci.getNodes()) {
                if (concluded.contains(varb))
                    continue;
                log.info("Polling variable {} attempt {}", varb.deviceKey(), i+1);
                lockVariable(varb);
                try {
                    DTy dTy = dci.readValue(varb.deviceKey(), false);
                    varb.parseDevice(dTy);
                    varb.infoState(InfoState.DIRTY_MQTT);
                    varb.availability(Availability.AVAILABLE);
                    log.info("Polling variable {} success", varb.deviceKey());
                    concluded.add(varb);
                } catch (CallFailException e) {
                    log.info("Initial polling failure: {}", e.getMessage());
                } catch (VariableUnavailableException e) {
                    log.warn("Variable {} not available: {}", varb.deviceKey(), e.getMessage());
                    varb.infoState(InfoState.UNINITIALIZED);
                    varb.availability(Availability.UNAVAILABLE);
                    concluded.add(varb);
                } catch (Exception e) {
                    log.error("Unexpected ",  e);
                } finally {
                    varb.lock().unlock();
                }
            }
        }
        for (VariableNode<?, DTy> varb : dci.getNodes()) {
            if (!concluded.contains(varb)) {
                log.warn("Variable {} not available due to repeated failures", varb.deviceKey());
                varb.infoState(InfoState.UNINITIALIZED);
                varb.availability(Availability.UNAVAILABLE);
                concluded.add(varb);
            }
        }
        List<String> unavailable = dci.getNodes().stream().filter(n -> n.availability() == Availability.UNAVAILABLE).map(VariableNode::deviceKey).toList();
        log.info("Finished complete poll with {} available variables out of total {}", dci.getNodes().size() - unavailable.size(), dci.getNodes().size());
        log.info("Unavailable variables are: {}", String.join(" ", unavailable));
        isDoingDiscovery = false;
    }

    private void publishAvailability() throws AsyncRuntimeException {
        // publish to a topic a json with many fields, like this:
        // { system: true, variables: { volume: false } }
        // last will message is { system: false }
        // the jinja template for HA will look like this: {{ value.system and value.variables.volume }}
        HashMap<String, Boolean> availabilityMap = new HashMap<>();
        for (VariableNode<?, DTy> variable : dci.getNodes()) {
            availabilityMap.put(variable.getSimpleName(), variable.availability().bool());
        }
        HAAvailability haAvailability = new HAAvailability(
                dci.isOpen(),
                availabilityMap
        );
        try {
            mqttInterface.publishTopic(mqttInterface.getConfig().baseTopic(), mapper.writeValueAsString(haAvailability), true).blockingGet();
        } catch (JsonProcessingException e) {
            log.error("Failed to publish availability: JSON serialization threw exception", e);
        } catch (AsyncRuntimeException e) {
            log.error("Publish availability failed: {}", e.getMessage());
            throw e;
        } catch (ConnectionFailedException e) {
            // not connected yet - do nothing!
        }
    }

    private void publishValues() throws AsyncRuntimeException, ConnectionFailedException {
        List<Single<?>> futures = new ArrayList<>();
        for (VariableNode<?, DTy> varb : dci.getNodes()) {
            if (varb.infoState() == InfoState.UNINITIALIZED)
                continue;

            String message = varb.toMqtt();
            Single<?> fut = mqttInterface.publishTopic(mqttInterface.getConfig().baseTopic() + "/" + varb.mqttSubtopic(), message, true)
                .doOnError(throwable -> {
                log.error("Failed to publish state to MQTT: {}", throwable.getMessage());
            });;
            futures.add(fut);
        }
        Flowable<?> all = Single.merge(futures);
        try {
            all.blockingSubscribe();
        } catch (AsyncRuntimeException | ConnectionFailedException e) {
            log.error("Publish all values failed: {}", e.getMessage());
            throw e;
        }
    }

    private void publishVariableNow(VariableNode<?, DTy> variable) throws AsyncRuntimeException, ConnectionFailedException {
        // if variable is unavailable or unitialized, publish empty string
        String message = variable.infoState() == InfoState.UNINITIALIZED ? "" : variable.toMqtt();
        log.info("Publishing variable {} message {}", variable.deviceKey(), message);
        RuntimeException exception = null;
        for (int i = 0; i < 3; i++) {
            Single<Mqtt5PublishResult> fut = mqttInterface.publishTopic(mqttInterface.getConfig().baseTopic() + "/" + variable.mqttSubtopic(), message, true);
            try {
                long nano = System.nanoTime();
                fut.timeout(6, TimeUnit.SECONDS).blockingGet();
                exception = null;
                break;
            } catch (AsyncRuntimeException | ConnectionFailedException e) {
                log.warn("Publish variable {} failed: {}", variable.deviceKey(), e.getMessage());
                exception = e;
            } catch (RuntimeException te) {
                if (te.getCause() != null && te.getCause() instanceof TimeoutException) {
                    log.error("MQTT client got stuck publishing AGAIN. PLEASE INVESTIGATE");
                }
            }
        }
        if (exception != null)
            throw exception;


    }

    private boolean flushDeviceVariableNow(VariableNode<?, DTy> variable) {
        try {
            DTy rsp = dci.writeValue(variable.deviceKey(), variable.toDevice(), false);
            variable.parseDevice(rsp);
            return true;
        } catch (CallFailException e) {
            log.error("Failed to write variable {} because {}", variable.deviceKey(), e.getMessage());
            return false;
        } catch (ConnectionFailedException e) {
            log.error("Connection to device lost while trying to write variable {}", variable.deviceKey());
            return false;
        } catch (VariableUnavailableException e) {
            log.error("DEV ERROR: Variable was tried to be flushed to device but is unavailable");
            return false;
        }
    }


    private void publishDiscovery() throws AsyncRuntimeException, ConnectionFailedException {
        String deviceShort = discoveryConfig.entityName().toLowerCase().replace(" ", "_");
        for (VariableNode<?, DTy> variable : dci.getNodes()) {
            try {
                String nodeShort = variable.getSimpleName();
                String topic = discoveryConfig.discoveryBaseTopic() + "/" + variable.mqttAdapter().getHAClass().haName() + "/" + deviceShort + "/" + nodeShort + "/config";
                HADiscovery discovery = new HADiscovery(
                        new HADiscovery.Availability(
                                mqttInterface.getConfig().baseTopic(),
                                "{{ 'online' if (value | from_json).system and 'variables' in (value | from_json) and '" + nodeShort + "' in (value | from_json).variables and (value | from_json).variables." + nodeShort + " else 'offline' }}"
                        ),
                        mqttInterface.getConfig().baseTopic() + "/" + variable.mqttSubtopic(),
                        new HADiscovery.Device(
                                List.of(deviceShort),
                                devicePropertiesInterface.getManufacturer(),
                                devicePropertiesInterface.getModel(),
                                discoveryConfig.entityName(),
                                "1.0"
                        ),
                        variable.mqttAdapter().getHANiceName(),
                        new HADiscovery.Origin(
                                "MqttBridge for " + devicePropertiesInterface.getManufacturer() + " " + devicePropertiesInterface.getModel(),
                                "1.0",
                                "https://lucaciresearch.net"
                        ),
                        mqttInterface.getConfig().baseTopic() + "/" + variable.mqttSubtopic(),
                        deviceShort + "_" + nodeShort,
                        "{{ value }}"
                );
                JsonNode root = mapper.valueToTree(discovery);
                variable.mqttAdapter().addMqttDiscovery((ObjectNode) root);
                String json = mapper.writeValueAsString(root);
                mqttInterface.publishTopic(topic, json, false).blockingGet();
            } catch (OperationNotSupportedException operationNotSupportedException) {
                log.warn("Data {} does not support HA Discovery", variable.deviceKey());
            } catch (AsyncRuntimeException | ConnectionFailedException e) {
                log.error("Failed to publish HA Discovery: {}", e.getMessage());
                throw e;
            } catch (JsonProcessingException e) {
                log.error("DEV ERROR: Failed to publush HA discovery because json processing exception", e);
            }
        }
    }

    private void subscribeTopics() throws AsyncRuntimeException {
        try {
            Flowable<TopicMessage> t = mqttInterface.subscribeTopic(mqttInterface.getConfig().baseTopic() + "/#");
            t.subscribe(tm -> {
                CompletableFuture.runAsync(() -> {
                    log.debug("Received MQTT message on topic {}", tm.topic());
                    if (!tm.topic().startsWith(mqttInterface.getConfig().baseTopic()))
                        return;
                    if (tm.topic().length() <= mqttInterface.getConfig().baseTopic().length())
                        return;

                    String subtopic = tm.topic().substring(mqttInterface.getConfig().baseTopic().length() + 1);
                    VariableNode<?, DTy> node = dci.getNodes().stream().filter(v -> v.mqttSubtopic().equals(subtopic)).findAny().orElse(null);
                    if (node == null) {
                        log.warn("Received rogue mqtt message on topic {}", tm.topic());
                        return;
                    }
                    try {
                        lockVariable(node);
                        if (node.availability().bool()) {
                            boolean okay = node.parseMqtt(tm.message());
                            if (!okay) {
                                node.infoState(InfoState.DIRTY_MQTT); // This will cause a message to be sent back
                            }
                        } else {
                            log.warn("Received mqtt command message for variable {} that is unavailable", node.deviceKey());
                        }
                    } catch (Exception e) {
                        // no exception should be thrown here
                        e.printStackTrace();
                    } finally {
                        node.lock().unlock();
                    }
                }, pollingExecutor);

            }, throwable -> {
                log.error("Subscribe to topic failed because: ", throwable);
            });
        } catch (ConnectionFailedException e) {
            log.warn("Lost connection to MQTT while subscribing to main topic: {}", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void initializePolling() {
        for (PollSpeed spd : PollSpeed.values()) {
            if (spd.getMilliseconds() > 0) {
                pollingExecutor.scheduleAtFixedRate(() -> handlePollSpeed(spd), spd.getMilliseconds(), spd.getMilliseconds() * 2L, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void handlePollSpeed(PollSpeed pollSpeed) {
        if (!dci.isOpen())
            return;
        if (!pollLocks.computeIfAbsent(pollSpeed, pollSpeed1 -> new ReentrantLock(true)).tryLock()) {
            // lock pressure -- do something -- or nothing
            log.warn("Pressure during {} speed periodic polling", pollSpeed);
            return;
        }
        for (VariableNode<?, DTy> node : dci.getNodes()) {
            // Don't poll uninitialized variables
            if (node.infoState() == InfoState.UNINITIALIZED || node.availability() == Availability.UNAVAILABLE)
                continue;

            // Don't poll dirty_device variables because the data in the device is outdated
            if (node.infoState() == InfoState.DIRTY_DEVICE)
                continue;

            if (node.pollSpeed() != pollSpeed)
                continue;

            if (!node.lock().tryLock())
                continue;

            // actual polling
            try {
                DTy dTy = dci.readValue(node.deviceKey(), false);
                node.parseDevice(dTy);
            } catch (ConnectionFailedException e) {
                log.warn("Connection to device failed while periodic polling: {}", e.getMessage());
                return;
            } catch (CallFailException e) {
                log.warn("Periodic polling failure for variable {}: {}", node.deviceKey(), e.getMessage());
            } catch (VariableUnavailableException e) {
                log.error("Variable declared itself as unavailable during polling");
                node.infoState(InfoState.UNINITIALIZED);
                node.availability(Availability.UNAVAILABLE);
            } finally {
                node.lock().unlock();
            }
        }
        pollLocks.get(pollSpeed).unlock();
    }

    private static void lockVariable(VariableNode<?, ?> variable) {
        while (true) {
            try {
                boolean lock = variable.lock().tryLock(50, TimeUnit.SECONDS);
                if (lock)
                    break;
                log.error("DEV ERROR: Locking variable {} takes too long", variable.deviceKey());
                log.error("Thread locking the variable is {}", variable.lock().getOwnerThread().getName());
                log.error("Dumping threads\n{}", threadDump());
            } catch (InterruptedException e) {
                log.error("DEV ERROR: Locking variable {} thread was interrupted", variable.deviceKey());
            }
        }
    }

    private static String threadDump() {
        StringBuffer threadDump = new StringBuffer(System.lineSeparator());
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for(ThreadInfo threadInfo : threadMXBean.dumpAllThreads(false, false)) {
            threadDump.append(threadInfo.toString()).append("\n");
        }
        return threadDump.toString();
    }

    public void shutdown() {
        pollingExecutor.shutdown();
        dci.closeConnection();
        mqttInterface.closeConnection();
    }

}
