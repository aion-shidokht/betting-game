package worker;

import internal.CriticalException;
import main.SignedTransactionBuilder;
import org.aion.harness.kernel.Address;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.result.RpcResult;
import org.aion.util.bytes.ByteUtil;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.UserState;
import util.NodeConnection;
import util.Pair;
import types.TransactionDetails;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * This thread keeps polling the rawTransaction queue, and transmits the transactions to the connected node
 * Note that if the transactionHashes queue is full, this thread will sleep until there is one available slot
 */

public class TransactionSender implements Runnable {

    private LinkedBlockingDeque<byte[]> rawTransactions;
    private LinkedBlockingDeque<Pair<ReceiptHash, Long>> transactionHashes;
    private BlockNumberCollector blockNumberCollector;
    private NodeConnection nodeConnection;
    private UserState userState;
    private final long pollIntervalMilliSeconds;
    private final long queueQueryIntervalMillis;
    private volatile boolean shutdown = false;
    private final Logger logger = LoggerFactory.getLogger("TransactionSender");

    public TransactionSender(BlockNumberCollector blockNumberCollector,
                             LinkedBlockingDeque<byte[]> rawTransactions,
                             LinkedBlockingDeque<Pair<ReceiptHash, Long>> transactionHashes,
                             NodeConnection nodeConnection,
                             UserState userState,
                             long pollIntervalMilliSeconds,
                             long queueQueryIntervalMillis) {
        this.blockNumberCollector = blockNumberCollector;
        this.rawTransactions = rawTransactions;
        this.transactionHashes = transactionHashes;
        this.nodeConnection = nodeConnection;
        this.userState = userState;
        this.pollIntervalMilliSeconds = pollIntervalMilliSeconds;
        this.queueQueryIntervalMillis = queueQueryIntervalMillis;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                if (transactionHashes.remainingCapacity() > 0) {
                    byte[] toSend = rawTransactions.poll();
                    if (toSend != null) {
                        Long blockNumber = blockNumberCollector.getCurrentBlockNumber();
                        // this should only be null during startup
                        while (blockNumber == null) {
                            logger.error("Could not retrieve the block number.");
                            Thread.sleep(pollIntervalMilliSeconds);
                            blockNumber = blockNumberCollector.getCurrentBlockNumber();
                        }
                        RpcResult<ReceiptHash> sendResult = nodeConnection.sendSignedTransaction(toSend);
                        logger.debug("Blk: " + blockNumber + ", result: " + sendResult.toString());
                        if (sendResult.isSuccess()) {
                            ReceiptHash hash = sendResult.getResult();
                            transactionHashes.offer(Pair.of(hash, blockNumber));
                        } else {
                            //retry later?
                            logger.debug("Blk: " + blockNumber + ", Could not send " + Hex.encodeHexString(toSend));
                            // todo decode sender address from the bytes
                            userState.putTransaction(new Address(ByteUtil.hexStringToBytes("0x0000000000000000000000000000000000000000000000000000000000000000")),
                                    TransactionDetails.fromFailedTransaction(SignedTransactionBuilder.getTransactionHashOfSignedTransaction(toSend)));
                        }

                    } else {
                        if (!shutdown) {
                            Thread.sleep(pollIntervalMilliSeconds);
                        }
                    }
                } else {
                    if (!shutdown) {
                        Thread.sleep(queueQueryIntervalMillis);
                    }
                }

                // for now shutdown if an exception happens
            } catch (Throwable e) {
                throw new CriticalException(e.getMessage());
            }
        }
    }

    public void shutdown() {
        this.shutdown = true;
        logger.info("Shutdown");
    }
}
