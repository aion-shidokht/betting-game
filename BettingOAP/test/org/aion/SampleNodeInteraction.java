package org.aion;

import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.SignedTransaction;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.RpcResult;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import state.ProjectedState;
import state.StatePopulator;
import state.UserState;
import util.*;
import worker.BlockNumberCollector;
import worker.EventListener;
import worker.ReceiptCollector;
import worker.TransactionSender;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.LinkedBlockingDeque;

public class SampleNodeInteraction {
    // owner = "a0127f71dd45a9a0bb0775cfd6d242c75a57e6e598d31ee4555d36a9d97734f0"
    // player = "a03c81c97b0dfaa35b8d74f09690b23c61b6330eee2d1325e191426bea0d04a5"

    private static final String ownerSK =
            "".substring(0, 66);

    private static final String playerSK =
            "".substring(0, 66);

    private static PrivateKey playerPrivateKey;
    private static PrivateKey ownerPrivateKey;
    private static Address contract;

    private static BlockNumberCollector blockNumberCollector;
    private static LinkedBlockingDeque<byte[]> rawTransactions;
    private static LinkedBlockingDeque<Pair<ReceiptHash, Long>> transactionHashes;
    private static TransactionSender transactionSender;
    private static ReceiptCollector receiptCollector;
    private static EventListener eventListener;
    private static BigInteger ownerNonce;
    private static BigInteger playerNonce;
    private static long startingBlock;

    private Thread transactionSenderThread;
    private Thread receiptCollectorThread;
    private Thread blockNumberCollectorThread;
    private Thread eventListenerThread;

    private ProjectedState projectedState = new ProjectedState();
    private static NodeConnection nodeConnection;

    @Before
    public void setup() throws InvalidKeySpecException {
        ownerPrivateKey = PrivateKey.fromBytes(Helper.hexStringToBytes(ownerSK));
        playerPrivateKey = PrivateKey.fromBytes(Helper.hexStringToBytes(playerSK));

        nodeConnection = new NodeConnection("127.0.0.1", "8545");



        ownerNonce = getNonce(ownerPrivateKey.getAddress());
        playerNonce = getNonce(playerPrivateKey.getAddress());

        deployContract(ownerPrivateKey, ownerNonce);
        ownerNonce = ownerNonce.add(BigInteger.ONE);

        long pollingIntervalMilliSeconds = 5000;
        blockNumberCollector = new BlockNumberCollector(nodeConnection, pollingIntervalMilliSeconds, 3);

        rawTransactions = new LinkedBlockingDeque<>(100);
        transactionHashes = new LinkedBlockingDeque<>(100);

        StatePopulator statePopulator = new StatePopulator(projectedState);
        UserState userState = new UserState(projectedState, nodeConnection, blockNumberCollector);

        transactionSender = new TransactionSender(blockNumberCollector,
                rawTransactions,
                transactionHashes,
                nodeConnection,
                userState,
                pollingIntervalMilliSeconds,
                5);

        receiptCollector = new ReceiptCollector(blockNumberCollector,
                transactionHashes,
                nodeConnection,
                userState,
                2,
                5000,
                3);

        eventListener = new EventListener(nodeConnection,
                statePopulator,
                startingBlock,
                pollingIntervalMilliSeconds,
                10,
                TestingHelper.getContractTopics(),
                contract);

        transactionSenderThread = new Thread(transactionSender);
        receiptCollectorThread = new Thread(receiptCollector);
        blockNumberCollectorThread = new Thread(blockNumberCollector);
        eventListenerThread = new Thread(eventListener);

        blockNumberCollectorThread.start();
        transactionSenderThread.start();
        receiptCollectorThread.start();
        eventListenerThread.start();
    }

    @After
    public void shutdown() throws InterruptedException {
        blockNumberCollector.shutdown();
        transactionSender.shutdown();
        receiptCollector.shutdown();
        eventListener.shutdown();

        blockNumberCollectorThread.join();
        transactionSenderThread.join();
        receiptCollectorThread.join();
        eventListenerThread.join();
    }

    @Test
    public void testFullPath() throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, InterruptedException {
        rawTransactions.add(register(playerPrivateKey.getAddress()).getSignedTransactionBytes());
        ownerNonce = ownerNonce.add(BigInteger.ONE);

        Thread.sleep(10000);

        rawTransactions.add(register(ownerPrivateKey.getAddress()).getSignedTransactionBytes());
        ownerNonce = ownerNonce.add(BigInteger.ONE);

        Thread.sleep(10000);

        rawTransactions.add(submitStatement(playerPrivateKey, "Q0".getBytes(), "A0".getBytes(), "S0".getBytes(), playerNonce).getSignedTransactionBytes());
        playerNonce = playerNonce.add(BigInteger.ONE);

        Thread.sleep(20000);

        rawTransactions.add(submitStatement(ownerPrivateKey, "Q1".getBytes(), "A1".getBytes(), "S1".getBytes(), ownerNonce).getSignedTransactionBytes());
        ownerNonce = ownerNonce.add(BigInteger.ONE);

        Thread.sleep(10000);

        rawTransactions.add(vote(ownerPrivateKey, 1, "A0".getBytes(), ownerNonce).getSignedTransactionBytes());
        ownerNonce = ownerNonce.add(BigInteger.ONE);

        Thread.sleep(10000);

        rawTransactions.add(vote(playerPrivateKey, 2, "A0".getBytes(), playerNonce).getSignedTransactionBytes());
        playerNonce = playerNonce.add(BigInteger.ONE);

        Thread.sleep(10000);

        rawTransactions.add(endGame().getSignedTransactionBytes());
        ownerNonce = ownerNonce.add(BigInteger.ONE);

        Thread.sleep(10000);

        rawTransactions.add(revealAnswer(ownerPrivateKey, 1, "A0".getBytes(), "S0".getBytes(), ownerNonce).getSignedTransactionBytes());
        ownerNonce = ownerNonce.add(BigInteger.ONE);

        Thread.sleep(10000);

        rawTransactions.add(revealAnswer(ownerPrivateKey, 2, "A1".getBytes(), "S1".getBytes(), ownerNonce).getSignedTransactionBytes());

        Thread.sleep(50000);

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertEquals(2, projectedState.getVotes().size());
        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertTrue(projectedState.getGameStatus().getStopped().value);
    }

    private static SignedTransaction register(Address player) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] data = TransactionCreator.getRegisterTxData(new avm.Address(player.getAddressBytes()));
        return TransactionCreator.buildRawTransaction(ownerPrivateKey, ownerNonce, contract, data, BigInteger.ZERO);
    }

    private static SignedTransaction submitStatement(PrivateKey privateKey, byte[] statement, byte[] answer, byte[] salt, BigInteger nonce) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] data = TransactionCreator.getSubmitStatementTxData(statement, answer, salt);
        return TransactionCreator.buildRawTransaction(privateKey, nonce, contract, data, BigInteger.ZERO);
    }

    private static SignedTransaction vote(PrivateKey privateKey, int statementId, byte[] answer, BigInteger nonce) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] data = TransactionCreator.getVoteTxData(statementId, answer);
        return TransactionCreator.buildRawTransaction(privateKey, nonce, contract, data, BigInteger.ZERO);
    }

    private static SignedTransaction revealAnswer(PrivateKey privateKey, int statementId, byte[] answer, byte[] salt, BigInteger nonce) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] data = TransactionCreator.getRevealAnswerTxData(statementId, answer, salt);
        return TransactionCreator.buildRawTransaction(privateKey, nonce, contract, data, BigInteger.ZERO);
    }

    private static SignedTransaction endGame() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] data = TransactionCreator.getMethodWithNoArgumentTxData("stopGame");
        return TransactionCreator.buildRawTransaction(ownerPrivateKey, ownerNonce, contract, data, BigInteger.ZERO);
    }

    private static BigInteger getNonce(Address address) {
        BigInteger nonce = null;
        try {
            RpcResult<BigInteger> result = nodeConnection.getNonce(address);
            if (result.isSuccess()) {
                nonce = result.getResult();
            } else {
                System.err.println("Could not retrieve the nonce.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("Could not retrieve the nonce.");
        }
        return nonce;
    }

    // deployment

    public static void deployContract(PrivateKey privateKey, BigInteger nonce) {
        try {
            byte[] contractBytes = getContractBytes();
            SignedTransaction tx = getDeploySignedTx(contractBytes, privateKey, nonce);
            TransactionReceipt receipt = sendTransaction(tx);
            contract = receipt.getAddressOfDeployedContract().orElseThrow();
            System.out.println("Contract deployed to " + contract);
            startingBlock = receipt.getBlockNumber().longValue();

        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static SignedTransaction getDeploySignedTx(byte[] contractBytes, PrivateKey privateKey, BigInteger nonce) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
        return SignedTransaction.newAvmCreateTransaction(
                privateKey,
                nonce,
                contractBytes,
                2_000_000,
                10_010_020_345L,
                BigInteger.ZERO, null);
    }

    private static byte[] getContractBytes() throws IOException {
        return Files.readAllBytes(Paths.get("test/resources/contract"));
    }

    private static TransactionReceipt sendTransaction(SignedTransaction transaction)
            throws InterruptedException {

        System.out.println("Sending the deployment transaction...");
        RpcResult<ReceiptHash> sendResult = nodeConnection.sendSignedTransaction(transaction.getSignedTransactionBytes());
        Assert.assertTrue(sendResult.isSuccess());

        ReceiptHash txHash = sendResult.getResult();

        System.out.println("Waiting for the transaction to be processed...");

        RpcResult<TransactionReceipt> receiptResult;
        do {
            receiptResult = nodeConnection.getTransactionReceipt(txHash);
            Thread.sleep(1000);
        } while (!receiptResult.isSuccess());

        return receiptResult.getResult();
    }

}
