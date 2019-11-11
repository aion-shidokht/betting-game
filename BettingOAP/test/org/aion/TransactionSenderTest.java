package org.aion;

import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.SignedTransaction;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.RpcResult;
import org.aion.util.bytes.ByteUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import util.NodeConnection;
import util.Pair;
import worker.BlockNumberCollector;
import worker.ReceiptCollector;
import util.TransactionCreator;
import worker.TransactionSender;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionSenderTest {

    NodeConnection nodeConnection = mock(NodeConnection.class);

    private LinkedBlockingDeque<SignedTransaction> rawTransactions;
    private LinkedBlockingDeque<Pair<ReceiptHash, Long>> transactionHashes;
    private LinkedBlockingDeque<TransactionReceipt> transactionReceipts;
    private TransactionSender transactionSender;
    private ReceiptCollector receiptCollector;
    private BlockNumberCollector blockNumberCollector;

    Thread transactionSenderThread;
    Thread receiptCollectorThread;
    Thread blockNumberCollectorThread;

    private static long currentBlockNumber = 10;
    private static long pollingIntervalMillis = 50;

    @Before
    public void setup() throws InterruptedException {
        when(nodeConnection.blockNumber()).thenAnswer(getNextBlock);
        blockNumberCollector = new BlockNumberCollector(nodeConnection, pollingIntervalMillis, 3);

        rawTransactions = new LinkedBlockingDeque<>(100);
        transactionHashes = new LinkedBlockingDeque<>(100);
        transactionReceipts = new LinkedBlockingDeque<>(100);

        transactionSender = new TransactionSender(blockNumberCollector,
                rawTransactions,
                transactionHashes,
                nodeConnection,
                pollingIntervalMillis,
                1);

        receiptCollector = new ReceiptCollector(blockNumberCollector,
                transactionHashes,
                transactionReceipts,
                nodeConnection,
                2,
                pollingIntervalMillis,
                3);

        transactionSenderThread = new Thread(transactionSender);
        receiptCollectorThread = new Thread(receiptCollector);
        blockNumberCollectorThread = new Thread(blockNumberCollector);
    }

    private void startThreads() {
        transactionSenderThread.start();
        receiptCollectorThread.start();
        blockNumberCollectorThread.start();
    }

    private void shutdownThreads() throws InterruptedException {
        blockNumberCollector.shutdown();
        transactionSender.shutdown();
        receiptCollector.shutdown();

        transactionSenderThread.join();
        receiptCollectorThread.join();
        blockNumberCollectorThread.join();
    }

    @Test
    public void testSendTransactionSuccess() throws InterruptedException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String receiptHash = "15c6fce4f6d59f5207ac26bdd0190713b1fdb207411301a1eaaf4b1875aecaa1";
        when(nodeConnection.sendSignedTransaction(any(SignedTransaction.class))).thenReturn(
                RpcResult.successful(
                        new ReceiptHash(ByteUtil.hexStringToBytes(receiptHash)),
                        System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS));

        when(nodeConnection.getTransactionReceipt(any(ReceiptHash.class))).thenReturn(
                RpcResult.successful(
                        receipt,
                        System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS));

        PrivateKey privateKey = PrivateKey.fromBytes(
                ByteUtil.hexStringToBytes("0x15c6fce4f6d59f5207ac26bdd0190713b1fdb207411301a1eaaf4b1875aecaa1"));
        SignedTransaction rawTransaction = TransactionCreator.buildRawTransaction(
                privateKey,
                BigInteger.ONE,
                new Address(ByteUtil.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf")),
                new byte[0],
                BigInteger.ONE);

        rawTransactions.add(rawTransaction);

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        Assert.assertEquals(1, transactionReceipts.size());
        Assert.assertEquals(receipt, transactionReceipts.poll());

        shutdownThreads();
    }

    @Test
    public void testSendTransactionFail() throws InterruptedException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String receiptHash = "15c6fce4f6d59f5207ac26bdd0190713b1fdb207411301a1eaaf4b1875aecaa1";
        when(nodeConnection.sendSignedTransaction(any(SignedTransaction.class))).thenReturn(
                RpcResult.successful(
                        new ReceiptHash(ByteUtil.hexStringToBytes(receiptHash)),
                        System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS));

        when(nodeConnection.getTransactionReceipt(any(ReceiptHash.class))).thenReturn(
                RpcResult.unsuccessful("could not retrieve receipt"));

        PrivateKey privateKey = PrivateKey.fromBytes(
                ByteUtil.hexStringToBytes("0x15c6fce4f6d59f5207ac26bdd0190713b1fdb207411301a1eaaf4b1875aecaa1"));
        SignedTransaction rawTransaction = TransactionCreator.buildRawTransaction(
                privateKey,
                BigInteger.ONE,
                new Address(ByteUtil.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf")),
                new byte[0],
                BigInteger.ONE);

        rawTransactions.add(rawTransaction);

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        Assert.assertEquals(0, transactionReceipts.size());

        shutdownThreads();
    }

    Answer getNextBlock = invocation -> {
        currentBlockNumber++;
        return RpcResult.successful(
                currentBlockNumber,
                System.currentTimeMillis(),
                TimeUnit.MILLISECONDS);
    };

    private TransactionReceipt receipt = new TransactionReceipt(10_000_000_000L,
            2_000_000L, 1_000_000L, 1_000_000,
            0, new byte[0], new byte[0], new byte[0], new byte[0],
            BigInteger.valueOf(currentBlockNumber),
            new Address(ByteUtil.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf")),
            new Address(ByteUtil.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf")),
            new Address(ByteUtil.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf")),
            new ArrayList<>(), 1);
}
