package org.aion;

import org.aion.harness.kernel.Address;
import org.apache.commons.codec.DecoderException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import state.ProjectedState;
import state.StatePopulator;
import types.BlockTuple;
import util.Log;
import util.NodeConnection;
import worker.EventListener;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

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
    private BigInteger blockNumber = BigInteger.valueOf(100);
    private ProjectedState projectedState;

    @Before
    public void setup() {
        deployLog = TestingHelper.getNoTopicEvent(sampleAddress,
                BigInteger.TEN,
                "BettingContractDeployed",
                0,
                sampleHash);

        projectedState = new ProjectedState();
        StatePopulator statePopulator = new StatePopulator(projectedState);

        eventListener = new EventListener(nodeConnection,
                statePopulator,
                deployLog,
                pollingIntervalMillis,
                BigInteger.ONE);

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
    public void testDeployEvent() throws DecoderException, InterruptedException {
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(deployLog)));
        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        Assert.assertEquals(1, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(deployLog.blockNumber, deployLog.blockHash, Arrays.asList(0)), projectedState.getBlocks().getFirst());
    }

    @Test
    public void testRegisterEvent() throws DecoderException, InterruptedException {
        Address player = new Address(getRandomAddressBytes());
        Log log = getRegisteredLog(sampleAddress, blockNumber, player, 0, null);
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log)));
        when(nodeConnection.getLogs(log.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(log)));
        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log.blockNumber, log.blockHash, Arrays.asList(expectedId)), projectedState.getBlocks().getLast());

        Assert.assertEquals(1, projectedState.getPlayers().size());
        Assert.assertEquals(player, projectedState.getPlayers().get(expectedId).getPlayerAddress());
    }

    @Test
    public void testSubmitAnswerEvent() throws DecoderException, InterruptedException {
        String statement = "Q";
        int statementId = 1;
        byte[] answerHash = getRandomAddressBytes();
        // need to register first
        Address player = new Address(getRandomAddressBytes());
        Log log1 = getRegisteredLog(sampleAddress, blockNumber, player, 0, null);

        Log log2 = getSubmittedStatementLog(sampleAddress, blockNumber.add(BigInteger.ONE), player, statementId, statement.getBytes(), answerHash, 0, null);
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log1, log2)));
        when(nodeConnection.getLogs(log2.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(log2)));

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        int lastExpectedId = 2;
        Assert.assertEquals(3, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log2.blockNumber, log2.blockHash, Arrays.asList(lastExpectedId)), projectedState.getBlocks().getLast());

        Assert.assertEquals(1, projectedState.getStatements().size());
        Assert.assertArrayEquals(answerHash, projectedState.getStatements().get(lastExpectedId).getAnswerHash());
        Assert.assertEquals(player, projectedState.getStatements().get(lastExpectedId).getPlayer());
        Assert.assertEquals(statementId, projectedState.getStatements().get(lastExpectedId).getStatementId());
        Assert.assertEquals(new String(statement.getBytes()), projectedState.getStatements().get(lastExpectedId).getStatementString());
    }

    @Test
    public void testVoteEvent() throws DecoderException, InterruptedException {
        String answer = "A";
        int statementId = 1;
        // need to register first
        Address player = new Address(getRandomAddressBytes());
        Log log1 = getRegisteredLog(sampleAddress, blockNumber, player, 0, null);

        Log log2 = getSubmittedStatementLog(sampleAddress, blockNumber.add(BigInteger.ONE), player, statementId, "Q".getBytes(), getRandomAddressBytes(), 0, log1.blockHash);

        Log log3 = getVotedLog(sampleAddress, blockNumber.add(BigInteger.ONE), player, statementId, answer.getBytes(), 0, log1.blockHash);

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log1, log2, log3)));
        when(nodeConnection.getLogs(log1.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(log1, log2, log3)));
        when(nodeConnection.getLogs(log2.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(log2, log3)));

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        Assert.assertEquals(3, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log2.blockNumber, log2.blockHash, Arrays.asList(2, 3)), projectedState.getBlocks().getLast());

        int expectedId = 3;
        Assert.assertEquals(1, projectedState.getVotes().size());
        Assert.assertArrayEquals(answer.getBytes(), projectedState.getVotes().get(expectedId).getGuessedAnswer());
        Assert.assertEquals(player, projectedState.getVotes().get(expectedId).getPlayer());
        Assert.assertEquals(statementId, projectedState.getVotes().get(expectedId).getStatementId());
    }

    @Test
    public void testRevealAnswerEvent() throws DecoderException, InterruptedException {
        String answer = "A";
        int statementId = 1;
        // need to register first
        Address player = new Address(getRandomAddressBytes());
        Log log1 = getRegisteredLog(deployLog.address, blockNumber, player, 0, null);

        Log log2 = getSubmittedStatementLog(sampleAddress, blockNumber, player, statementId, "S0".getBytes(), getRandomAddressBytes(), 0, log1.blockHash);

        Log log3 = getRevealedAnswerLog(deployLog.address, blockNumber, statementId, answer.getBytes(), 0, log1.blockHash);

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log1, log2, log3)));
        when(nodeConnection.getLogs(log1.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(log1, log2, log3)));

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        Assert.assertEquals(2, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log2.blockNumber, log2.blockHash, Arrays.asList(1, 2, 3)), projectedState.getBlocks().getLast());

        int expectedId = 3;
        Assert.assertEquals(1, projectedState.getAnswers().size());
        Assert.assertArrayEquals(answer.getBytes(), projectedState.getAnswers().get(expectedId).getAnswer());
        Assert.assertEquals(statementId, projectedState.getAnswers().get(expectedId).getStatementId());
    }

    @Test
    public void testGameStoppedEvent() throws DecoderException, InterruptedException {
        Address player = new Address(getRandomAddressBytes());

        Log log = getNoTopicEvent(player, blockNumber, "GameStopped", 0, null);
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log)));
        when(nodeConnection.getLogs(log.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(log)));
        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log.blockNumber, log.blockHash, Arrays.asList(expectedId)), projectedState.getBlocks().getLast());

        Assert.assertTrue(projectedState.getGameStatus().isStopped());
    }

    @Test
    public void testDistributedPrizeEvent() throws DecoderException, InterruptedException {
        Address player = new Address(getRandomAddressBytes());

        Log log = getNoTopicEvent(player, blockNumber, "DistributedPrize", 0, null);
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(deployLog, log)));
        when(nodeConnection.getLogs(log.blockNumber, "latest", null)).thenReturn(new ArrayList<>(Arrays.asList(log)));
        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        shutdownThreads();

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getBlocks().size());
        Assert.assertEquals(BlockTuple.of(log.blockNumber, log.blockHash, Arrays.asList(expectedId)), projectedState.getBlocks().getLast());

        Assert.assertTrue(projectedState.getGameStatus().isPrizeDistributed());
    }
}
