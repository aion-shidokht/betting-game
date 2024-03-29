package org.aion;

import org.aion.harness.kernel.Address;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import state.ProjectedState;
import state.StatePopulator;
import types.BlockTuple;
import types.Player;
import util.Helper;
import util.Log;
import util.NodeConnection;
import worker.EventListener;

import java.util.*;

import static org.aion.TestingHelper.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatePopulatorTest {
    private NodeConnection nodeConnection = mock(NodeConnection.class);
    private EventListener eventListener;
    private static long pollingIntervalMillis = 50;
    private static Log deployLog;
    private static byte[] sampleHash = new byte[32];
    private static Address sampleAddress = new Address(new byte[32]);
    private Thread eventListenerThread;
    private long blockNumber = 100;
    private ProjectedState projectedState;
    Set<byte[]> topics = TestingHelper.getContractTopics();

    @Before
    public void setup() {
        deployLog = TestingHelper.getOneTopicEvent(sampleAddress,
                10,
                "BettingContractDeployed",
                0,
                sampleHash);

        projectedState = new ProjectedState();
        StatePopulator statePopulator = new StatePopulator(projectedState);

        eventListener = new EventListener(nodeConnection,
                statePopulator,
                deployLog.blockNumber,
                pollingIntervalMillis,
                1,
                topics,
                sampleAddress);

        eventListenerThread = new Thread(eventListener);
    }

    private void startThreads() {
        eventListenerThread.start();
    }

    private void shutdownThreads() throws InterruptedException {
        eventListener.shutdown();
        eventListenerThread.join();
    }

    @Test
    public void testDeployEvent() throws InterruptedException {
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(deployLog)));
        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        Assert.assertEquals(1, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(deployLog.blockNumber, deployLog.blockHash, Arrays.asList(0)), projectedState.getBlocks().getFirst());
    }

    @Test
    public void testRegisterEvent() throws InterruptedException {
        Address player = new Address(getRandomAddressBytes());
        Log log = getRegisteredLog(sampleAddress, blockNumber, player, 0, null);
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log)));
        when(nodeConnection.getLogs(log.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(log)));
        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log.blockNumber, log.blockHash, Arrays.asList(expectedId)), projectedState.getBlocks().getLast());

        Assert.assertEquals(1, projectedState.getPlayers().size());
        Assert.assertArrayEquals(player.getAddressBytes(), projectedState.getPlayers().get(expectedId).getPlayerAddress().toBytes());
    }

    @Test
    public void testSubmitAnswerEvent() throws InterruptedException {
        String statement = "Q";
        int statementId = 1;
        byte[] answerHash = getRandomAddressBytes();
        // need to register first
        Address player = new Address(getRandomAddressBytes());
        Log log1 = getRegisteredLog(sampleAddress, blockNumber, player, 0, null);

        Log log2 = getSubmittedStatementLog(sampleAddress, blockNumber + 1, player, statementId, statement.getBytes(), answerHash, 0, null);
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log1, log2)));
        when(nodeConnection.getLogs(log2.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(log2)));

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        int lastExpectedId = 2;
        Assert.assertEquals(3, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log2.blockNumber, log2.blockHash, Arrays.asList(lastExpectedId)), projectedState.getBlocks().getLast());

        Assert.assertEquals(1, projectedState.getStatements().size());
        Assert.assertEquals(Helper.bytesToHexStringWith0x(answerHash), projectedState.getStatements().get(lastExpectedId).getAnswerHash());
        Assert.assertArrayEquals(player.getAddressBytes(), projectedState.getStatements().get(lastExpectedId).getPlayerAddress().toBytes());

        Assert.assertEquals(statementId, projectedState.getStatements().get(lastExpectedId).getStatementId());
        Assert.assertEquals(new String(statement.getBytes()), projectedState.getStatements().get(lastExpectedId).getStatementString());
    }

    @Test
    public void testVoteEvent() throws InterruptedException {
        String answer = "A";
        int statementId = 1;
        // need to register first
        Address player = new Address(getRandomAddressBytes());
        Log log1 = getRegisteredLog(sampleAddress, blockNumber, player, 0, null);

        Log log2 = getSubmittedStatementLog(sampleAddress, blockNumber + 1, player, statementId, "Q".getBytes(), getRandomAddressBytes(), 0, log1.blockHash);

        Log log3 = getVotedLog(sampleAddress, blockNumber+ 1, player, statementId, answer.getBytes(), 0, log1.blockHash);

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log1, log2, log3)));
        when(nodeConnection.getLogs(log1.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(log1, log2, log3)));
        when(nodeConnection.getLogs(log2.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(log2, log3)));

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        Assert.assertEquals(3, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log2.blockNumber, log2.blockHash, Arrays.asList(2, 3)), projectedState.getBlocks().getLast());

        int expectedId = 3;
        Assert.assertEquals(1, projectedState.getVotes().size());
        Assert.assertEquals(answer, projectedState.getVotes().get(expectedId).getGuessedAnswer());
        Assert.assertArrayEquals(player.getAddressBytes(), projectedState.getVotes().get(expectedId).getPlayerAddress().toBytes());
        Assert.assertEquals(statementId, projectedState.getVotes().get(expectedId).getStatementId());

        Assert.assertEquals(1, projectedState.getStatements().size());
        Assert.assertEquals(-1, (int) projectedState.getStatements().get(expectedId -1).getAnswerEventId());
        Assert.assertEquals(1, projectedState.getStatements().get(expectedId -1).getVoteEventIds().size());
    }

    @Test
    public void testRevealAnswerEvent() throws InterruptedException {
        String answer = "A";
        int statementId = 1;
        // need to register first
        Address player = new Address(getRandomAddressBytes());
        Log log1 = getRegisteredLog(deployLog.address, blockNumber, player, 0, null);

        Log log2 = getSubmittedStatementLog(sampleAddress, blockNumber, player, statementId, "S0".getBytes(), getRandomAddressBytes(), 0, log1.blockHash);

        Log log3 = getRevealedAnswerLog(deployLog.address, blockNumber, statementId, answer.getBytes(), 0, log1.blockHash);

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log1, log2, log3)));
        when(nodeConnection.getLogs(log1.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(log1, log2, log3)));

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        Assert.assertEquals(2, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log2.blockNumber, log2.blockHash, Arrays.asList(1, 2, 3)), projectedState.getBlocks().getLast());

        int expectedId = 3;
        Assert.assertEquals(1, projectedState.getAnswers().size());
        Assert.assertEquals(answer, projectedState.getAnswers().get(expectedId).getAnswer());
        Assert.assertEquals(statementId, projectedState.getAnswers().get(expectedId).getStatementId());

        Assert.assertEquals(1, projectedState.getStatements().size());
        Assert.assertEquals(expectedId, (int) projectedState.getStatements().get(expectedId -1).getAnswerEventId());
        Assert.assertEquals(0, projectedState.getStatements().get(expectedId -1).getVoteEventIds().size());
    }

    @Test
    public void testScoreCalculation() throws InterruptedException {
        String answer = "A";
        int statementId = 1;

        Address[] player = new Address[5];
        Log[] registerLogs = new Log[player.length];
        Log[] voteLogs = new Log[player.length];
        byte[] sampleHash = getRandomAddressBytes();

        for (int i = 0; i < player.length; i++) {
            player[i] = new Address(getRandomAddressBytes());
            registerLogs[i] = getRegisteredLog(deployLog.address, blockNumber, player[i], i + 1, deployLog.blockHash);
            voteLogs[i] = getVotedLog(deployLog.address, blockNumber + 1, player[i], statementId, answer.getBytes(), i + 1, sampleHash);
        }

        Log submittedLog = getSubmittedStatementLog(sampleAddress, blockNumber + 1, player[0], statementId, "S0".getBytes(), getRandomAddressBytes(), 0, sampleHash);
        Log revealedAnswerLog = getRevealedAnswerLog(deployLog.address, blockNumber + 1, statementId, answer.getBytes(), 10, sampleHash);

        List<Log> logs1 = new ArrayList<>(Arrays.asList(deployLog));
        logs1.addAll(Arrays.asList(registerLogs));
        logs1.addAll(Arrays.asList(submittedLog));
        logs1.addAll(Arrays.asList(voteLogs));
        logs1.addAll(Arrays.asList(revealedAnswerLog));

        List<Log> logs2 = new ArrayList<>();
        logs2.addAll(Arrays.asList(submittedLog));
        logs2.addAll(Arrays.asList(voteLogs));
        logs2.addAll(Arrays.asList(revealedAnswerLog));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, sampleAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(blockNumber + 1, "latest", topics, sampleAddress)).thenReturn(logs2);

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        Map<Integer, Player> players = projectedState.getPlayers();
        for (int i = 0; i < player.length; i++) {
            Assert.assertEquals(1, players.get(i + 1).getScore());
        }

        shutdownThreads();
    }

    @Test
    public void testGameStoppedEvent() throws InterruptedException {
        Address player = new Address(getRandomAddressBytes());

        Log log = getOneTopicEvent(player, blockNumber, "GameStopped", 0, null);
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log)));
        when(nodeConnection.getLogs(log.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(log)));
        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log.blockNumber, log.blockHash, Arrays.asList(expectedId)), projectedState.getBlocks().getLast());

        Assert.assertTrue(projectedState.getGameStatus().getStopped().value);
    }

    @Test
    public void testDistributedPrizeEvent() throws InterruptedException {
        Address player = new Address(getRandomAddressBytes());

        Log log = getDistributedPrizeLog(player, blockNumber, 5, 0, null);
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log)));
        when(nodeConnection.getLogs(log.blockNumber, "latest", topics, sampleAddress)).thenReturn(new ArrayList<>(Arrays.asList(log)));
        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log.blockNumber, log.blockHash, Arrays.asList(expectedId)), projectedState.getBlocks().getLast());

        Assert.assertEquals(5, (long) projectedState.getGameStatus().getPrizeDistributed().value);
    }
}
