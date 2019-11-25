package util;

import internal.CriticalException;

import java.util.concurrent.LinkedBlockingDeque;

public class QueuePopulator {
    private LinkedBlockingDeque<byte[]> rawTransactions;

    public QueuePopulator(LinkedBlockingDeque<byte[]> rawTransactions) {
        this.rawTransactions = rawTransactions;
    }

    public void putRawTransaction(byte[] transaction) {
        try {
            rawTransactions.put(transaction);
        } catch (InterruptedException e) {
            throw new CriticalException(e.getMessage());
        }
    }
}
