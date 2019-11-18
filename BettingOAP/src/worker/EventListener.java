package worker;

import internal.Assertion;
import internal.CriticalException;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.StatePopulator;
import types.BlockTuple;
import util.Log;
import util.NodeConnection;

import java.math.BigInteger;
import java.util.*;

public class EventListener implements Runnable {

    private NodeConnection nodeConnection;
    private final long pollIntervalMilliSeconds;
    private final Logger logger = LoggerFactory.getLogger("EventListener");
    private BigInteger lastRetrievedBlockNumber;
    private byte[] lastRetrievedBlockHash;
    private StatePopulator statePopulator;
    private Log deployedLog;
    private volatile boolean shutdown = false;
    private final BigInteger deploymentLogRangeCheck;

    public EventListener(NodeConnection nodeConnection,
                         StatePopulator statePopulator,
                         Log deployedLog,
                         long pollIntervalMilliSeconds,
                         BigInteger deploymentLogRangeCheck) {
        this.nodeConnection = nodeConnection;
        this.deployedLog = deployedLog;
        this.statePopulator = statePopulator;
        statePopulator.populate(Arrays.asList(deployedLog));
        this.lastRetrievedBlockHash = deployedLog.blockHash;
        this.lastRetrievedBlockNumber = deployedLog.blockNumber;
        this.pollIntervalMilliSeconds = pollIntervalMilliSeconds;
        this.deploymentLogRangeCheck = deploymentLogRangeCheck;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                logger.debug("Polling from " + lastRetrievedBlockNumber);
                List<Log> logs = nodeConnection.getLogs(lastRetrievedBlockNumber, "latest", null);

                List<Log> sortedLogs = sortLogs(logs);

                // there must be at least one log present, otherwise the chain has re-orged
                // validate the first log is what we expect

                if (sortedLogs.size() == 0 || !Arrays.equals(sortedLogs.get(0).blockHash, lastRetrievedBlockHash)) {
                    logger.debug("Chain has reorganized. Finding the last common log..");
                    sortedLogs = findCommonBlock();
                }

                Log lastEvent = sortedLogs.get(sortedLogs.size() - 1);

                // only remove the logs if they have been seen before, i.e the deployment event has not reorganized
                if (this.lastRetrievedBlockHash != null) {
                    BigInteger alreadySubmittedBlockNumber = sortedLogs.get(0).blockNumber;
                    sortedLogs.removeIf(l -> (l.blockNumber.equals(alreadySubmittedBlockNumber)));
                }

                lastRetrievedBlockHash = lastEvent.blockHash;
                lastRetrievedBlockNumber = lastEvent.blockNumber;

                if (sortedLogs.size() > 0) {
                    logger.debug("Found " + sortedLogs.size() + " new logs.");
                    statePopulator.populate(sortedLogs);
                }
                Thread.sleep(pollIntervalMilliSeconds);

            } catch (Throwable e) {
                throw new CriticalException(e.getMessage());
            }
        }
    }

    private static List<Log> sortLogs(List<Log> logs) {
        List<Log> sortedLog = new ArrayList<>(logs);
        sortedLog.sort(Comparator.comparing((Log l) -> l.blockNumber).thenComparing(l -> l.transactionIndex).thenComparing(l -> l.logIndex));
        return sortedLog;
    }

    private List<Log> findCommonBlock() throws DecoderException, InterruptedException {
        boolean foundCommon = false;
        BlockTuple blockTuple;
        List<Log> sortedLogs = null;
        ListIterator<BlockTuple> itr = statePopulator.getBlocksIterator();
        int count = 0;
        do {
            // always remove the last block
            count++;

            // validate the previous block (from the one that was marked to be removed)
            blockTuple = itr.previous();

            List<Log> logs = nodeConnection.getLogs(blockTuple.getBlockNumber(), "latest", null);
            // if no logs are present, revert to previous
            if (logs.size() > 0) {
                sortedLogs = sortLogs(logs);
                if (Arrays.equals(sortedLogs.get(0).blockHash, blockTuple.getBlockHash())) {
                    logger.debug("Found common log at block number " + sortedLogs.get(0).blockNumber);
                    foundCommon = true;
                }
            }
            // if all logs have changed, will break out of the while loop when it reaches the first block
        } while (!foundCommon && itr.hasPrevious());

        // Could not find a common log. Find the original deployment log and start from there
        if (!foundCommon) {
            statePopulator.clear();

            logger.debug("Fetching the deployment log..");
            List<Log> newDeploymentLog = nodeConnection.getLogs(deployedLog.blockNumber.subtract(deploymentLogRangeCheck),
                    "latest",
                    new HashSet<>(Collections.singleton("BettingContractDeployed".getBytes())));
            Assertion.assertTrue(newDeploymentLog.size() == 1);

            deployedLog = newDeploymentLog.get(0);
            this.lastRetrievedBlockHash = null;
            this.lastRetrievedBlockNumber = null;

            // get logs from deployment log block number
            sortedLogs = sortLogs(nodeConnection.getLogs(deployedLog.blockNumber, "latest", null));
        } else {
            // a common event was found
            statePopulator.revertBlocks(count);
        }

        return sortedLogs;
    }

    public void shutdown() {
        this.shutdown = true;
        logger.info("Shutdown");
    }
}
