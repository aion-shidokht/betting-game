package org.aion;

import org.aion.harness.kernel.Address;
import org.aion.util.conversions.Hex;
import org.apache.commons.codec.DecoderException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import state.ProjectedState;
import state.StatePopulator;
import util.Log;
import util.NodeConnection;
import worker.EventListener;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventListenerTest {
    NodeConnection nodeConnection = mock(NodeConnection.class);
    private EventListener eventListener;
    private static long pollingIntervalMillis = 50;
    private static Log deployLog;
    private static byte[] Hash = new byte[32];
    private static Address Address = new Address(new byte[32]);
    private Thread eventListenerThread;
    private ProjectedState projectedState;

    @Before
    public void setup() {
        deployLog = new Log(Address,
                "BettingContractDeployed".getBytes(),
                new ArrayList<>(),
                BigInteger.TEN,
                0,
                0,
                Hash);

        projectedState = new ProjectedState();

        StatePopulator statePopulator = new StatePopulator(projectedState);

        eventListener = new EventListener(nodeConnection,
                statePopulator,
                deployLog,
                pollingIntervalMillis,
                BigInteger.valueOf(5));

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
    public void testSuccessfulFirstLogMatchSameBlock() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        BigInteger blockNumber = BigInteger.valueOf(100);
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)));

        when(nodeConnection.getLogs(blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)));

        when(nodeConnection.getLogs(BigInteger.valueOf(102), "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(4, projectedState.getBlocks().size());
        Assert.assertEquals(BigInteger.valueOf(102), projectedState.getBlocks().getLast().getBlockNumber());

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertEquals(player1, projectedState.getPlayers().get(expectedId));
        Assert.assertEquals(player2, projectedState.getPlayers().get(expectedId + 2));

        Assert.assertEquals(1, projectedState.getStatements().size());
    }

    @Test
    public void testSuccessfulUnorderedBlockEvents() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        BigInteger blockNumber = BigInteger.valueOf(100);
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)));

        when(nodeConnection.getLogs(blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, registerLog, submitLog)));

        when(nodeConnection.getLogs(BigInteger.valueOf(102), "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(4, projectedState.getBlocks().size());
        Assert.assertEquals(BigInteger.valueOf(102), projectedState.getBlocks().getLast().getBlockNumber());

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertEquals(player1, projectedState.getPlayers().get(expectedId));
        Assert.assertEquals(player2, projectedState.getPlayers().get(expectedId + 2));

        Assert.assertEquals(1, projectedState.getStatements().size());
    }

    @Test
    public void testReorgRemoveEvent() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(100), player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(110), player2, 1, "A".getBytes(), 0, null);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2)));

        // chain reorgs
        when(nodeConnection.getLogs(BigInteger.valueOf(110), "latest", null))
                .thenReturn(new ArrayList<>());

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(4, projectedState.getBlocks().size());
        Assert.assertEquals(BigInteger.valueOf(102), projectedState.getBlocks().getLast().getBlockNumber());

        Assert.assertEquals(1, projectedState.getStatements().size());
        Assert.assertEquals(0, projectedState.getVotes().size());

    }

    @Test
    public void testReorgNewEvent() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(100), player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(110), player2, 1, "A".getBytes(), 0, null);
        Log newVoteLog = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(109), player2, 1, "A".getBytes(), 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(110), player1, 1, "A".getBytes(), 0, null);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, newVoteLog, voteLogReplacement)));

        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(voteLogReplacement)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(6, projectedState.getBlocks().size());
        Assert.assertEquals(BigInteger.valueOf(110), projectedState.getBlocks().getLast().getBlockNumber());

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertEquals(1, projectedState.getStatements().size());
        Assert.assertEquals(2, projectedState.getVotes().size());
    }

    @Test
    public void testReorgUntilDeployEvent() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2Replacement = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(100), player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(110), player1, 1, "A".getBytes(), 0, null);

        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(80), player1, 0, null);
        Log submitLogReplacement = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(91), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2Replacement = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2Replacement, 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(102), player2Replacement, 1, "A".getBytes(), 0, registerLog2Replacement.blockHash);

        Log submitLog2 = TestingHelper.getSubmittedStatementLog(player1, BigInteger.valueOf(123), player2Replacement, 2, "Q".getBytes(), "H".getBytes(), 0, null);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        when(nodeConnection.getLogs(submitLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(5, projectedState.getBlocks().size());
        Assert.assertEquals(BigInteger.valueOf(123), projectedState.getBlocks().getLast().getBlockNumber());

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertTrue(projectedState.getPlayers().containsValue(player1));
        Assert.assertTrue(projectedState.getPlayers().containsValue(player2Replacement));

        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertEquals(1, projectedState.getVotes().size());
    }

    @Test
    public void testReorgNewDeployEventLater() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2Replacement = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(100), player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(110), player1, 1, "A".getBytes(), 0, null);

        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(80), player1, 0, null);
        Log submitLogReplacement = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(91), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2Replacement = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2Replacement, 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(102), player2Replacement, 1, "A".getBytes(), 0, registerLog2Replacement.blockHash);

        Log submitLog2 = TestingHelper.getSubmittedStatementLog(player1, BigInteger.valueOf(123), player2Replacement, 2, "Q".getBytes(), "H".getBytes(), 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getNoTopicEvent(deployLog.address, BigInteger.valueOf(20), "BettingContractDeployed", 0, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>());

        when(nodeConnection.getLogs(submitLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        when(nodeConnection.getLogs(eq(BigInteger.valueOf(5)), eq("latest"), any(HashSet.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(5, projectedState.getBlocks().size());
        Assert.assertEquals(BigInteger.valueOf(123), projectedState.getBlocks().getLast().getBlockNumber());
        Assert.assertEquals(newDeployLog.blockNumber, projectedState.getBlocks().getFirst().getBlockNumber());

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertTrue(projectedState.getPlayers().containsValue(player1));
        Assert.assertTrue(projectedState.getPlayers().containsValue(player2Replacement));

        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertEquals(1, projectedState.getVotes().size());
    }

    @Test
    public void testReorgNewDeployEventEarlier() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2Replacement = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(100), player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(110), player1, 1, "A".getBytes(), 0, null);

        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(80), player1, 0, null);
        Log submitLogReplacement = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(91), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2Replacement = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2Replacement, 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(102), player2Replacement, 1, "A".getBytes(), 0, registerLog2Replacement.blockHash);

        Log submitLog2 = TestingHelper.getSubmittedStatementLog(player1, BigInteger.valueOf(123), player2Replacement, 2, "Q".getBytes(), "H".getBytes(), 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getNoTopicEvent(deployLog.address, BigInteger.valueOf(5), "BettingContractDeployed", 0, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>());

        when(nodeConnection.getLogs(submitLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        when(nodeConnection.getLogs(eq(BigInteger.valueOf(5)), eq("latest"), any(HashSet.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(5, projectedState.getBlocks().size());
        Assert.assertEquals(BigInteger.valueOf(123), projectedState.getBlocks().getLast().getBlockNumber());
        Assert.assertEquals(newDeployLog.blockNumber, projectedState.getBlocks().getFirst().getBlockNumber());

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertTrue(projectedState.getPlayers().containsValue(player1));
        Assert.assertTrue(projectedState.getPlayers().containsValue(player2Replacement));

        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertEquals(1, projectedState.getVotes().size());
    }

    @Test
    public void testReorgOnlyDeployEvent() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(100), player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(110), player1, 1, "A".getBytes(), 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getNoTopicEvent(deployLog.address, BigInteger.valueOf(15), "BettingContractDeployed", 0, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>());
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>());
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>());

        when(nodeConnection.getLogs(eq(BigInteger.valueOf(5)), eq("latest"), any(HashSet.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(1, projectedState.getBlocks().size());
        Assert.assertEquals(newDeployLog.blockNumber, projectedState.getBlocks().getLast().getBlockNumber());

        Assert.assertEquals(0, projectedState.getPlayers().size());
        Assert.assertEquals(0, projectedState.getStatements().size());
        Assert.assertEquals(0, projectedState.getVotes().size());
    }

    @Test
    public void testReorgOnlyDeployEventBefore() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(100), player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, BigInteger.valueOf(110), player1, 1, "A".getBytes(), 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getNoTopicEvent(deployLog.address, BigInteger.valueOf(5), "BettingContractDeployed", 0, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>());
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>());
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>());
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>());

        when(nodeConnection.getLogs(eq(BigInteger.valueOf(5)), eq("latest"), any(HashSet.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(1, projectedState.getBlocks().size());
        Assert.assertEquals(newDeployLog.blockNumber, projectedState.getBlocks().getLast().getBlockNumber());

        Assert.assertEquals(0, projectedState.getPlayers().size());
        Assert.assertEquals(0, projectedState.getStatements().size());
        Assert.assertEquals(0, projectedState.getVotes().size());
    }

    @Test
    public void testReorgUntilDeployEventUnorderedReturn() throws DecoderException, InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(100), player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, BigInteger.valueOf(101), player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(102), player2, 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getNoTopicEvent(deployLog.address, BigInteger.valueOf(5), "BettingContractDeployed", 0, newDeployBlockHash);
        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, BigInteger.valueOf(5), player1, 1, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>());
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>());
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", null))
                .thenReturn(new ArrayList<>());
        when(nodeConnection.getLogs(eq(BigInteger.valueOf(5)), eq("latest"), any(HashSet.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));
        // chain reorgs
        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", null))
                .thenReturn(Arrays.asList(newDeployLog, registerLogReplacement));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(1, projectedState.getBlocks().size());
        Assert.assertEquals(BigInteger.valueOf(5), projectedState.getBlocks().getLast().getBlockNumber());
        Assert.assertEquals(1, projectedState.getPlayers().size());
        Assert.assertEquals(0, projectedState.getStatements().size());
        Assert.assertEquals(0, projectedState.getVotes().size());
    }

}
