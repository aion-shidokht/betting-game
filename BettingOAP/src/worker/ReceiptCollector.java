package worker;

import internal.CriticalException;
import internal.RetryExecutor;
import org.aion.harness.kernel.Address;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.RpcResult;
import org.aion.util.bytes.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.UserState;
import util.NodeConnection;
import util.Pair;
import types.TransactionDetails;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * This thread retrieves the receipt for the transactions in the transactionHashes queue.
 * getTransactionReceipt call is executed when at least minimumDepth block have been generated since the block number at
 * which the transaction was broadcast to the blockchain. If the RpcResult is unsuccessful, getTransactionReceipt call
 * is retried for maxGetReceiptAttempt times.
 */
public class ReceiptCollector implements Runnable {

    private LinkedBlockingDeque<Pair<ReceiptHash, Long>> transactionHashes;
    private BlockNumberCollector blockNumberCollector;
    private NodeConnection nodeConnection;
    private UserState userState;
    private RetryExecutor<RpcResult<TransactionReceipt>> getReceiptRetryExecutor;
    private final long pollIntervalMillis;
    private long minimumDepth;
    private volatile boolean shutdown = false;
    private final Logger logger = LoggerFactory.getLogger("ReceiptCollector");

    public ReceiptCollector(BlockNumberCollector blockNumberCollector,
                            LinkedBlockingDeque<Pair<ReceiptHash, Long>> transactionHashes,
                            NodeConnection nodeConnection,
                            UserState userState,
                            long minimumDepth,
                            long pollIntervalMillis,
                            int maxGetReceiptAttempt) {
        this.blockNumberCollector = blockNumberCollector;
        this.transactionHashes = transactionHashes;
        this.nodeConnection = nodeConnection;
        this.userState = userState;
        this.minimumDepth = minimumDepth;
        // used for queue, receipt and block number polling intervals. Can be separated in the future.
        this.pollIntervalMillis = pollIntervalMillis;
        this.getReceiptRetryExecutor = new RetryExecutor<>(r -> !r.isSuccess(), maxGetReceiptAttempt, pollIntervalMillis);
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                Pair<ReceiptHash, Long> transactionInfo = transactionHashes.peek();
                if (transactionInfo != null) {
                    Long blockNumber = blockNumberCollector.getCurrentBlockNumber();

                    while (blockNumber == null) {
                        logger.error("Could not retrieve the block number.");
                        Thread.sleep(pollIntervalMillis);
                        blockNumber = blockNumberCollector.getCurrentBlockNumber();
                    }
                    // wait for minimumDepth blocks before querying for the receipt
                    long expectedBlockNumber = transactionInfo.value + minimumDepth;
                    while (blockNumber < expectedBlockNumber) {
                        logger.debug("current block number: " + blockNumber + ", waiting for " + expectedBlockNumber);
                        Thread.sleep(pollIntervalMillis);
                        blockNumber = blockNumberCollector.getCurrentBlockNumber();
                    }

                    logger.info("Calling getTransactionReceipt for " + transactionInfo.key);

                    transactionHashes.poll();

                    RpcResult<TransactionReceipt> getReceiptRpcResult = getReceipt(transactionInfo.key);
                    TransactionReceipt receipt = getReceiptRpcResult.getResult();

                    if (getReceiptRpcResult.isSuccess()) {
                        userState.putTransaction(receipt.getTransactionSender(), TransactionDetails.fromReceipt(receipt));
                        logger.debug("Blk: " + blockNumber + ", Successfully received the receipt for " + transactionInfo.key);
                    } else {
                        if(receipt != null) {
                            userState.putTransaction(receipt.getTransactionSender(), TransactionDetails.fromReceipt(receipt));
                            logger.debug("Blk: " + blockNumber + ", Failed to retrieve receipt for " + transactionInfo.key);
                        } else {
                            // todo decode sender address from the bytes
                            userState.putTransaction(new Address(ByteUtil.hexStringToBytes("0x0000000000000000000000000000000000000000000000000000000000000000")),
                                    TransactionDetails.fromFailedTransaction(transactionInfo.key.getHash()));
                        }
                    }

                } else {
                    if (!shutdown) {
                        Thread.sleep(pollIntervalMillis);
                    }
                }
            } catch (Throwable e) {
                throw new CriticalException(e.getMessage());
            }
        }
    }

    public void shutdown() {
        this.shutdown = true;
        logger.info("Shutdown");
    }

    private RpcResult<TransactionReceipt> getReceipt(ReceiptHash txnHash) {
        Callable<RpcResult<TransactionReceipt>> getReceipt = () -> nodeConnection.getTransactionReceipt(txnHash);
        return getReceiptRetryExecutor.execute(getReceipt);
    }
}
