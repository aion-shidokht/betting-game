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
    private BigInteger startingBlockNumber;
    private byte[] lastRetrievedBlockHash;
    private StatePopulator statePopulator;
    private volatile boolean shutdown = false;
    private final BigInteger deploymentLogRangeCheck;
    private final Set<byte[]> topics;

    public EventListener(NodeConnection nodeConnection,
                         StatePopulator statePopulator,
                         BigInteger startingBlockNumber,
                         long pollIntervalMilliSeconds,
                         BigInteger deploymentLogRangeCheck,
                         Set<byte[]> topics) {
        this.nodeConnection = nodeConnection;
        this.statePopulator = statePopulator;
        this.lastRetrievedBlockNumber = startingBlockNumber;
        this.startingBlockNumber = startingBlockNumber;
        this.pollIntervalMilliSeconds = pollIntervalMilliSeconds;
        this.deploymentLogRangeCheck = deploymentLogRangeCheck;
        this.topics = Collections.unmodifiableSet(topics);
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                logger.info("Polling from " + lastRetrievedBlockNumber);
                List<Log> logs = nodeConnection.getLogs(lastRetrievedBlockNumber, "latest", topics);

                List<Log> sortedLogs = sortLogs(logs);

                if (sortedLogs.size() > 0) {
                    if (this.lastRetrievedBlockHash != null) {
                        if (!Arrays.equals(sortedLogs.get(0).blockHash, lastRetrievedBlockHash)) {
                            logger.info("BlockHash is not equal to the last retrieved log's block hash. Finding the last common log..");
                            findCommonBlock();
                        } else {
                            BigInteger alreadySubmittedBlockNumber = sortedLogs.get(0).blockNumber;
                            sortedLogs.removeIf(l -> (l.blockNumber.equals(alreadySubmittedBlockNumber)));
                            if (sortedLogs.size() > 0) {
                                setStateBasedOnLogs(sortedLogs);
                            }
                        }
                    } else {
                        setStateBasedOnLogs(sortedLogs);
                    }
                } else {
                    logger.info("Could not find any contract logs. Finding the last common log..");
                    findCommonBlock();
                }

                if (!shutdown) {
                    Thread.sleep(pollIntervalMilliSeconds);
                }

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

    private void findCommonBlock() throws InterruptedException {
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

            List<Log> logs = nodeConnection.getLogs(blockTuple.getBlockNumber(), "latest", topics);
            // if no logs are present, revert to previous
            if (logs.size() > 0) {
                sortedLogs = sortLogs(logs);
                if (Arrays.equals(sortedLogs.get(0).blockHash, blockTuple.getBlockHash())) {
                    this.lastRetrievedBlockNumber = sortedLogs.get(0).blockNumber;
                    this.lastRetrievedBlockHash = sortedLogs.get(0).blockHash;

                    statePopulator.revertBlocks(count);

                    foundCommon = true;
                    logger.info("Found common log at block number " + this.lastRetrievedBlockNumber);
                }
            }
            // if all logs have changed, will break out of the while loop when it reaches the first block
        } while (!foundCommon && itr.hasPrevious());

        // Could not find a common log. Find the original deployment log and start from there
        if (!foundCommon) {
            statePopulator.clear();

            logger.info("Fetching the deployment log..");
            List<Log> newDeploymentLog = nodeConnection.getLogs(startingBlockNumber.subtract(deploymentLogRangeCheck),
                    "latest",
                    new HashSet<>(Collections.singleton("BettingContractDeployed".getBytes())));
            Assertion.assertTrue(newDeploymentLog.size() == 1);

            this.lastRetrievedBlockHash = null;
            this.lastRetrievedBlockNumber = newDeploymentLog.get(0).blockNumber;
        }
    }

    private void setStateBasedOnLogs(List<Log> sortedLogs) {
        logger.info("Found " + sortedLogs.size() + " new logs.");
        Log lastEvent = sortedLogs.get(sortedLogs.size() - 1);
        lastRetrievedBlockHash = lastEvent.blockHash;
        lastRetrievedBlockNumber = lastEvent.blockNumber;
        statePopulator.populate(sortedLogs);
    }

    public void shutdown() {
        this.shutdown = true;
        logger.info("Shutdown");
    }
}
