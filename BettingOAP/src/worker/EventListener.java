package worker;

import internal.CriticalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Log;
import util.NodeConnection;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class EventListener implements Runnable {

    private NodeConnection nodeConnection;
    private final long pollIntervalMilliSeconds;
    private final Logger logger = LoggerFactory.getLogger("EventListener");
    private BigInteger lastRetrievedBlockNumber;
    private byte[] lastRetrievedBlockHash;
    private Log deployedLog;
    private volatile boolean shutdown = false;

    public EventListener(NodeConnection nodeConnection,
                         Log deployedLog,
                         long pollIntervalMilliSeconds) {
        this.nodeConnection = nodeConnection;
        this.deployedLog = deployedLog;
        this.lastRetrievedBlockHash = deployedLog.blockHash;
        this.lastRetrievedBlockNumber = deployedLog.blockNumber;
        this.pollIntervalMilliSeconds = pollIntervalMilliSeconds;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                logger.debug("Polling from " + lastRetrievedBlockNumber);
                List<Log> logs = nodeConnection.getLogs(lastRetrievedBlockNumber, "latest", null);

                List<Log> sortedLogs = sortLogs(logs);

                BigInteger alreadySubmittedBlockNumber = sortedLogs.get(0).blockNumber;
                sortedLogs.removeIf(l -> (l.blockNumber.equals(alreadySubmittedBlockNumber)));

                if (sortedLogs.size() > 0) {
                    logger.debug("Found " + sortedLogs.size() + " new logs.");
                    // todo populate the state
                    Log lastEvent = sortedLogs.get(sortedLogs.size() - 1);
                    lastRetrievedBlockHash = lastEvent.blockHash;
                    lastRetrievedBlockNumber = lastEvent.blockNumber;
                }
                Thread.sleep(pollIntervalMilliSeconds);

            } catch (Throwable e) {
                throw new CriticalException(e.getMessage());
            }
        }
    }

    private static List<Log> sortLogs(List<Log> logs) {
        //todo sort based on block number, transaction index, log index
        return new ArrayList<>(logs);
    }

    public void shutdown() {
        this.shutdown = true;
        logger.info("Shutdown");
    }
}
