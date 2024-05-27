package net.lucaciresearch.mqttbridge.util;

import java.util.concurrent.locks.ReentrantLock;

public class BetterReentrantLock extends ReentrantLock {
    public BetterReentrantLock() {
    }

    public BetterReentrantLock(boolean fair) {
        super(fair);
    }

    public Thread getOwnerThread() {
        return getOwner();
    }

}
