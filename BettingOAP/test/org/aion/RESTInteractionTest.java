package org.aion;

import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.SignedTransaction;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionLog;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.RpcResult;
import org.glassfish.grizzly.http.server.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import server.SimpleHttpServer;
import state.ProjectedState;
import state.StatePopulator;
import state.UserState;
import util.*;
import worker.BlockNumberCollector;
import worker.EventListener;
import worker.ReceiptCollector;
import worker.TransactionSender;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.*;

import static org.aion.TestingHelper.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RESTInteractionTest {
    private NodeConnection nodeConnection = mock(NodeConnection.class);
    private EventListener eventListener;
    private Log deployLog;
    private byte[] hash = new byte[32];
    private org.aion.harness.kernel.Address contractAddress = new Address(new byte[32]);
    private Thread eventListenerThread;
    private HttpServer server;
    private String URI;
    private UserState userState;

    private BlockNumberCollector blockNumberCollector;
    private TransactionSender transactionSender;
    private ReceiptCollector receiptCollector;

    private Thread transactionSenderThread;
    private Thread receiptCollectorThread;
    private Thread blockNumberCollectorThread;

    private long currentBlockNumber = 10;
    private QueuePopulator queuePopulator;

    Set<byte[]> topics = TestingHelper.getContractTopics();

    @Before
    public void setup() throws InterruptedException {
        URI = SimpleHttpServer.getBaseUri("localhost", "8025");
        when(nodeConnection.blockNumber()).thenAnswer(getNextBlock);

        deployLog = TestingHelper.getOneTopicEvent(contractAddress,
                10,
                "BettingContractDeployed",
                0,
                hash);

        ProjectedState projectedState = new ProjectedState();

        long pollingIntervalMillis = 50;

        StatePopulator statePopulator = new StatePopulator(projectedState);

        eventListener = new EventListener(nodeConnection,
                statePopulator,
                deployLog.blockNumber,
                pollingIntervalMillis,
                5,
                topics,
                contractAddress);


        blockNumberCollector = new BlockNumberCollector(nodeConnection, pollingIntervalMillis, 3);
        userState = new UserState(projectedState, nodeConnection, blockNumberCollector);

        LinkedBlockingDeque<byte[]> rawTransactions = new LinkedBlockingDeque<>(100);
        LinkedBlockingDeque<Pair<ReceiptHash, Long>> transactionHashes = new LinkedBlockingDeque<>(100);

        transactionSender = new TransactionSender(blockNumberCollector,
                rawTransactions,
                transactionHashes,
                nodeConnection,
                userState,
                pollingIntervalMillis,
                1);

        receiptCollector = new ReceiptCollector(blockNumberCollector,
                transactionHashes,
                nodeConnection,
                userState,
                2,
                pollingIntervalMillis,
                3);

        transactionSenderThread = new Thread(transactionSender);
        receiptCollectorThread = new Thread(receiptCollector);
        blockNumberCollectorThread = new Thread(blockNumberCollector);
        eventListenerThread = new Thread(eventListener);

        queuePopulator = new QueuePopulator(rawTransactions);
    }

    private void startThreads() throws IOException {
        server = startServer();
        eventListenerThread.start();
        transactionSenderThread.start();
        receiptCollectorThread.start();
        blockNumberCollectorThread.start();
    }

    @After
    public void shutdownThreads() throws InterruptedException {
        server.shutdown();
        eventListener.shutdown();
        blockNumberCollector.shutdown();
        transactionSender.shutdown();
        receiptCollector.shutdown();
        eventListenerThread.join();
        transactionSenderThread.join();
        receiptCollectorThread.join();
        blockNumberCollectorThread.join();
    }

    @Test
    public void testGetAllStatements() throws InterruptedException, IOException {
        Client c1 = getNewClient();
        WebTarget target1 = c1.target(URI);

        Address player1 = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = 100;
        int size = 5;
        Log[] submitLogs = new Log[size];
        for (int i = 0; i < size; i++) {
            submitLogs[i] = TestingHelper.getSubmittedStatementLog(deployLog.address, blockNumber, player1, i + 1, "Q".getBytes(), "H".getBytes(), i, hash);
        }
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, hash);
        Log answerLog1 = getRevealedAnswerLog(deployLog.address, blockNumber, 1, "A".getBytes(), size + 1, hash);
        Log gameStoppedLog = getOneTopicEvent(player1, blockNumber, "GameStopped", 0, registerLog.blockHash);

        List<Log> logs1 = new ArrayList<>();
        logs1.add(deployLog);
        logs1.add(registerLog);
        logs1.addAll(Arrays.asList(submitLogs));
        logs1.addAll(Set.of(gameStoppedLog, answerLog1));

        List<Log> logs2 = new ArrayList<>();
        logs2.add(registerLog);
        logs2.addAll(Arrays.asList(submitLogs));
        logs2.addAll(Set.of(gameStoppedLog, answerLog1));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(blockNumber, "latest", topics, contractAddress)).thenReturn(logs2);

        startThreads();

        String response = getStatements(target1);
        JSONArray obj = new JSONArray(response);

        Assert.assertEquals(size, obj.length());
        Iterator itr = obj.iterator();
        int i = 1;
        while(itr.hasNext()){
            JSONObject next = (JSONObject) itr.next();
            Assert.assertEquals(i, next.getInt("statementId"));
            Assert.assertEquals("Q", next.getString("statementString"));
            Assert.assertEquals("0x48", next.getString("answerHash"));
            Assert.assertEquals(Helper.bytesToHexStringWith0x(player1.getAddressBytes()),
                    next.getJSONObject("playerAddress").getString("addressString"));
            if(i == 1){
                Assert.assertEquals("A", next.getString("answerString"));
            } else {

                Assert.assertEquals(JSONObject.NULL , next.get("answerString"));
            }
            i++;
        }

        c1.close();
    }

    @Test
    public void testGetVotes() throws InterruptedException, IOException {
        Client c1 = getNewClient();
        WebTarget target1 = c1.target(URI);

        Address player1 = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = 100;
        int size = 5;
        Log[] submitLogs = new Log[size];
        Log[] voteLogs = new Log[size];

        for (int i = 0; i < size; i++) {
            submitLogs[i] = TestingHelper.getSubmittedStatementLog(deployLog.address, blockNumber, player1, i + 1, "Q".getBytes(), "H".getBytes(), i, hash);
            voteLogs[i] = TestingHelper.getVotedLog(deployLog.address, blockNumber, player1, 1 + i, "A".getBytes(), i + size, hash);
        }
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, hash);

        List<Log> logs1 = new ArrayList<>();
        logs1.add(deployLog);
        logs1.add(registerLog);
        logs1.addAll(Arrays.asList(submitLogs));
        logs1.addAll(Arrays.asList(voteLogs));

        List<Log> logs2 = new ArrayList<>();
        logs2.add(registerLog);
        logs2.addAll(Arrays.asList(submitLogs));
        logs2.addAll(Arrays.asList(voteLogs));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(blockNumber, "latest", topics, contractAddress)).thenReturn(logs2);

        startThreads();

        String response = getVotes(target1);

        Assert.assertEquals(1271, response.length());
        JSONArray array = new JSONArray(response);

        Assert.assertEquals(1, array.getJSONObject(0).getInt("statementId"));
        Assert.assertEquals(3, array.getJSONObject(2).getInt("statementId"));

        c1.close();
    }

    @Test
    public void testGetVotesCorrectAnswer() throws InterruptedException, IOException {
        Client c1 = getNewClient();
        WebTarget target1 = c1.target(URI);

        Address player1 = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = 100;
        int size = 5;
        Log[] submitLogs = new Log[size];
        Log[] voteLogs = new Log[size];

        for (int i = 0; i < size; i++) {
            submitLogs[i] = TestingHelper.getSubmittedStatementLog(deployLog.address, blockNumber, player1, i + 1, "Q".getBytes(), "H".getBytes(), i, hash);
            voteLogs[i] = TestingHelper.getVotedLog(deployLog.address, blockNumber, player1, 1 + i, "A".getBytes(), i + size, hash);
        }
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, hash);
        Log revealAnswer = getRevealedAnswerLog(deployLog.address, blockNumber, 1, "A".getBytes(), size + 1, hash);

        List<Log> logs1 = new ArrayList<>();
        logs1.addAll(List.of(deployLog, registerLog, revealAnswer));
        logs1.addAll(Arrays.asList(submitLogs));
        logs1.addAll(Arrays.asList(voteLogs));

        List<Log> logs2 = new ArrayList<>();
        logs2.addAll(List.of(registerLog, revealAnswer));
        logs2.addAll(Arrays.asList(submitLogs));
        logs2.addAll(Arrays.asList(voteLogs));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(blockNumber, "latest", topics, contractAddress)).thenReturn(logs2);

        startThreads();

        String response = getVotes(target1);

        Assert.assertEquals(1270, response.length());
        JSONArray array = new JSONArray(response);

        Assert.assertEquals(1, array.getJSONObject(0).getInt("statementId"));
        Assert.assertEquals(true, array.getJSONObject(0).getBoolean("correct"));

        c1.close();
    }

    @Test
    public void sendTransaction() throws InterruptedException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
        PrivateKey privateKey = PrivateKey.fromBytes(
                Helper.hexStringToBytes("0x15c6fce4f6d59f5207ac26bdd0190713b1fdb207411301a1eaaf4b1875aecaa1"));
        org.aion.harness.kernel.Address sender = new org.aion.harness.kernel.Address(Helper.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf"));
        SignedTransaction rawTransaction = TransactionCreator.buildRawTransaction(
                privateKey,
                BigInteger.ONE,
                sender,
                new byte[0],
                BigInteger.ONE);

        TransactionReceipt receipt = new TransactionReceipt(10_000_000_000L,
                2_000_000L, 1_000_000L, 1_000_000,
                0, new byte[0], new byte[0], rawTransaction.getTransactionHash(), new byte[0],
                BigInteger.valueOf(currentBlockNumber),
                sender,
                new org.aion.harness.kernel.Address(Helper.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf")),
                new org.aion.harness.kernel.Address(Helper.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf")),
                new ArrayList<>(Arrays.asList(new TransactionLog(
                        new org.aion.harness.kernel.Address(Helper.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf")),
                        Helper.hexStringToBytes("0xa0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf"),
                        Arrays.asList("Registered".getBytes()),
                        BigInteger.ONE,
                        0,
                        0))), 1);

        when(nodeConnection.sendSignedTransaction(any(byte[].class))).thenReturn(
                RpcResult.successful(
                        new ReceiptHash(receipt.getTransactionHash()),
                        System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS));

        when(nodeConnection.getTransactionReceipt(any(ReceiptHash.class))).thenReturn(
                RpcResult.successful(
                        receipt,
                        System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(Arrays.asList(deployLog));

        Client c1 = getNewClient();
        WebTarget target = c1.target(URI);
        startThreads();
        Response response = makePOSTCall(target, Helper.bytesToHexString(rawTransaction.getSignedTransactionBytes()));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(Helper.bytesToHexString(rawTransaction.getTransactionHash()), response.readEntity(String.class));
        Thread.sleep(1000);
        Assert.assertEquals(1, userState.getTransactions("a0c7ef65be0ea76f0a6691e1b7a78e8b09c7e31a23964cc81d74f56a47c2f4bf").size());
        c1.close();
    }

    @Test
    public void testGetVotesMulti() throws InterruptedException, IOException {
        int clientSize = 100;
        WebTarget[] webTargets = new WebTarget[clientSize];

        for (int i = 0; i < clientSize; i++) {
            Client c = getNewClient();
            webTargets[i] = c.target(URI);
        }

        Address player1 = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = 100;
        int size = 5;
        Log[] submitLogs = new Log[size];
        Log[] voteLogs = new Log[size];

        for (int i = 0; i < size; i++) {
            submitLogs[i] = TestingHelper.getSubmittedStatementLog(deployLog.address, blockNumber, player1, i + 1, "Q".getBytes(), "H".getBytes(), i, hash);
            voteLogs[i] = TestingHelper.getVotedLog(deployLog.address, blockNumber, player1, 1 + i, "A".getBytes(), i + size, hash);
        }
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, hash);

        List<Log> logs1 = new ArrayList<>();
        logs1.add(deployLog);
        logs1.add(registerLog);
        logs1.addAll(Arrays.asList(submitLogs));
        logs1.addAll(Arrays.asList(voteLogs));

        List<Log> logs2 = new ArrayList<>();
        logs2.add(registerLog);
        logs2.addAll(Arrays.asList(submitLogs));
        logs2.addAll(Arrays.asList(voteLogs));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(blockNumber, "latest", topics, contractAddress)).thenReturn(logs2);

        startThreads();

        String[] response = new String[clientSize];

        for (int i = 0; i < clientSize; i++) {
            response[i] = getVotes(webTargets[i]);
        }

        for (int i = 0; i < clientSize; i++) {
            JSONArray array = new JSONArray(response[i]);
            Assert.assertEquals(1, array.getJSONObject(0).getInt("statementId"));
        }
    }

    @Test
    public void testGetAllPlayers() throws InterruptedException, IOException {
        Client c1 = getNewClient();
        WebTarget target1 = c1.target(URI);

        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = 100;
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, hash);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player2, 0, hash);

        List<Log> logs1 = new ArrayList<>(Arrays.asList(deployLog, registerLog, registerLog2));

        List<Log> logs2 = new ArrayList<>(Arrays.asList(registerLog, registerLog2));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(blockNumber, "latest", topics, contractAddress)).thenReturn(logs2);

        startThreads();

        String response = getPlayers(target1);
        Assert.assertEquals(425, response.length());
        JSONArray array = new JSONArray(response);

        Assert.assertNotNull(array);
        Assert.assertEquals(2, array.length());
        Assert.assertEquals(0, array.getJSONObject(0).getInt("score"));
        Assert.assertEquals(Helper.bytesToHexStringWith0x(player1.getAddressBytes()),
                array.getJSONObject(0).getJSONObject("playerAddress").getString("addressString"));

        Assert.assertEquals(Helper.bytesToHexStringWith0x(player2.getAddressBytes()),
                array.getJSONObject(1).getJSONObject("playerAddress").getString("addressString"));

        c1.close();
    }

    @Test
    public void testGetAnswer() throws IOException, InterruptedException {
        Client c1 = getNewClient();
        WebTarget target1 = c1.target(URI);

        Address player1 = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = 100;
        int size = 5;
        Log[] logs = new Log[size * 2];

        for (int i = 0; i < size; i++) {
            logs[i] = TestingHelper.getSubmittedStatementLog(deployLog.address, blockNumber, player1, i + 1, "Q".getBytes(), "H".getBytes(), i, hash);
            logs[i + size] = getRevealedAnswerLog(deployLog.address, blockNumber, i + 1, "A".getBytes(), size + i, hash);
        }

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, hash);

        List<Log> logs1 = new ArrayList<>();
        logs1.add(deployLog);
        logs1.add(registerLog);
        logs1.addAll(Arrays.asList(logs));

        List<Log> logs2 = new ArrayList<>();
        logs2.add(registerLog);
        logs2.addAll(Arrays.asList(logs));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(blockNumber, "latest", topics, contractAddress)).thenReturn(logs2);

        startThreads();

        String allAnswers = getAnswers(target1);
        JSONArray allAnswerArray = new JSONArray(allAnswers);
        Assert.assertEquals(size, allAnswerArray.length());

        for (int i = 0; i < size; i++) {
            Assert.assertEquals(i + 1, allAnswerArray.getJSONObject(i).getInt("statementId"));
            Assert.assertEquals("A", allAnswerArray.getJSONObject(i).getString("answer"));

        }

        c1.close();
    }

    @Test
    public void testGetGame() throws IOException, InterruptedException {
        Client c1 = getNewClient();
        WebTarget target1 = c1.target(URI);
        Address player = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = deployLog.blockNumber + 1;
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player, 0, null);
        Log gameStoppedLog = getOneTopicEvent(player, blockNumber, "GameStopped", 0, registerLog.blockHash);
        Log distributedPrizeLog = TestingHelper.getDistributedPrizeLog(player, blockNumber, 5, 0,  registerLog.blockHash);

        List<Log> logs1 = new ArrayList<>(Arrays.asList(deployLog, registerLog, gameStoppedLog, distributedPrizeLog));
        List<Log> logs2 = new ArrayList<>(Arrays.asList(registerLog, gameStoppedLog, distributedPrizeLog));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs2);

        startThreads();

        String game = getGameStatus(target1);
        JSONObject gameObj = new JSONObject(game);
        Assert.assertTrue((Boolean) gameObj.get("stopped"));
        Assert.assertEquals(1, gameObj.getJSONArray("winners").length());

        c1.close();
    }

    @Test
    public void testGetNonce() throws IOException, InterruptedException {
        Client c1 = getNewClient();
        WebTarget target1 = c1.target(URI);
        org.aion.harness.kernel.Address player = new org.aion.harness.kernel.Address(TestingHelper.getRandomAddressBytes());

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(Arrays.asList(deployLog));
        when(nodeConnection.getNonce(player)).thenReturn(RpcResult.successful(new BigInteger("10", 16), System.currentTimeMillis(), TimeUnit.MILLISECONDS));

        startThreads();

        String nonce = getNonce(target1, Helper.bytesToHexString(player.getAddressBytes()));
        Assert.assertEquals("16", nonce);

        c1.close();
    }

    private static String getStatements(WebTarget target) {
        return target.path("state/allStatements").request().get(String.class);
    }

    private static Response makePOSTCall(WebTarget target, String data) {
        return target.path("state/sendTransaction").request().post(Entity.entity(data, MediaType.TEXT_PLAIN_TYPE));
    }

    private static String getVotes(WebTarget target) {
        return target.path("state/allVotes").request().get(String.class);
    }

    private static String getPlayers(WebTarget target) {
        return target.path("state/allPlayers").request().get(String.class);
    }

    private static String getAnswers(WebTarget target) {
        return target.path("state/allAnswers").request().get(String.class);
    }

    private static String getAnswer(WebTarget target, int eventId) {
        return target.queryParam("eventId", eventId).path("state/answer").request().get(String.class);
    }

    private static String getGameStatus(WebTarget target) {
        return target.path("state/gameStatus").request().get(String.class);
    }

    private static String getNonce(WebTarget target, String address) {
        return target.queryParam("address", address).path("state/getNonce").request().get(String.class);
    }

    private Client getNewClient() {
        return ClientBuilder.newClient();
    }

    private HttpServer startServer() throws IOException {
        HttpServer server = SimpleHttpServer.startServer(userState, queuePopulator, "localhost", "8025");
        server.start();
        return server;
    }

    Answer getNextBlock = invocation -> {
        currentBlockNumber++;
        return RpcResult.successful(
                currentBlockNumber,
                System.currentTimeMillis(),
                TimeUnit.MILLISECONDS);
    };
}
