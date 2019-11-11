package worker;

import internal.CriticalException;
import org.aion.harness.result.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.NodeConnection;

/**
 * Retrieves the latest block number. If the block number cannot be retrieved after maxConsecutiveErrors tries, application is shutdown.
 */
public class BlockNumberCollector implements Runnable {

    private NodeConnection nodeConnection;
    private volatile Long currentBlockNumber;
    private int failedCallCount = 0;
    private final long pollIntervalMillis;
    private final int maxConsecutiveErrors;
    private volatile boolean shutdown = false;
    private final Logger logger = LoggerFactory.getLogger("BlockNumberCollector");

    public BlockNumberCollector(NodeConnection nodeConnection,
                                long pollIntervalMillis,
                                int maxConsecutiveErrors) {
        this.nodeConnection = nodeConnection;
        this.pollIntervalMillis = pollIntervalMillis;
        this.maxConsecutiveErrors = maxConsecutiveErrors;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                RpcResult<Long> blockNumber = nodeConnection.blockNumber();
                if (blockNumber.isSuccess()) {
                    this.currentBlockNumber = blockNumber.getResult();
                    logger.info("Current block number " + this.currentBlockNumber);
                    failedCallCount = 0;
                } else {
                    failedCallCount++;
                    if (failedCallCount > maxConsecutiveErrors) {
                        throw new RuntimeException("Error retrieving the block number");
                    }
                }
                if (!shutdown) {
                    Thread.sleep(pollIntervalMillis);
                }
            } catch (Throwable e) {
                throw new CriticalException(e.getMessage());
            }
        }
    }

    public Long getCurrentBlockNumber() {
        return this.currentBlockNumber;
    }

    public void shutdown() {
        this.shutdown = true;
        logger.info("Shutdown");
    }
}
