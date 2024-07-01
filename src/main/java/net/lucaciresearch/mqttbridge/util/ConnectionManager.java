package net.lucaciresearch.mqttbridge.util;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Accessors(fluent = true, chain = true)
public class ConnectionManager {

    @Getter @Setter
    private Supplier<Boolean> creator;

    @Getter @Setter
    private Runnable destroyer;

    @Getter @Setter
    private long baseDelay = 1000;

    @Getter @Setter
    private long incrementDelay = 500;

    private DesiredState state = DesiredState.OFF;

    private SystemState systemState = SystemState.STOPPED;

    private final Semaphore semaphore = new Semaphore(1);

    @Getter
    private final ExecutorService service = Executors.newSingleThreadExecutor();


    // TODO: this one to be able to markFailed and set state (start/stop) from the exector
    public void start() {
        semaphore.acquireUninterruptibly();
        state = DesiredState.ON;
        if (systemState == SystemState.STOPPED || systemState == SystemState.STOPPING) {
            CompletableFuture.runAsync(() -> {
                systemState = SystemState.STARTING;
                if (loopToCreate()) systemState = SystemState.RUNNING;
            }, service);
        }
        semaphore.release();
    }

    public void stop() {
        semaphore.acquireUninterruptibly();
        state = DesiredState.OFF;
        if (systemState == SystemState.RUNNING || systemState == SystemState.STARTING) {
            CompletableFuture.runAsync(() -> {
                systemState = SystemState.STOPPING;
                destroyer.run();
                systemState = SystemState.STOPPED;
            }, service);
        }
        semaphore.release();
    }

    public void markFailed() {
        semaphore.acquireUninterruptibly();
        if (state != DesiredState.ON) {
            semaphore.release();
            return;
        }
        if (systemState == SystemState.STARTING || systemState == SystemState.RUNNING) {
            CompletableFuture.runAsync(() -> {
                systemState = SystemState.STARTING;
                destroyer.run();
                if (loopToCreate()) systemState = SystemState.RUNNING;
            }, service);
        }
        semaphore.release();
    }

    private boolean loopToCreate() {
        Boolean result = creator.get();
        while (!result && state == DesiredState.ON) {
            try {
                Thread.sleep(baseDelay);
            } catch (InterruptedException e) {
                // nothing
            }
            result = creator.get();
        }
        return result;
    }


    private enum DesiredState {
        ON, OFF;
    }

    private enum SystemState {
        STARTING, RUNNING, STOPPING, STOPPED;
    }

}
