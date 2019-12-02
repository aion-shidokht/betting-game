package org.aion;

import types.Player;
import org.aion.harness.kernel.Address;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import state.ProjectedState;
import state.StatePopulator;
import util.Log;
import util.NodeConnection;
import worker.EventListener;

import java.util.*;

import static org.aion.TestingHelper.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventListenerTest {
    NodeConnection nodeConnection = mock(NodeConnection.class);
    private EventListener eventListener;
    private static long pollingIntervalMillis = 50;
    private static Log deployLog;
    private static byte[] Hash = new byte[32];
    private static Address contractAddress = new Address(new byte[32]);
    private Thread eventListenerThread;
    private ProjectedState projectedState;

    Set<byte[]> topics = TestingHelper.getContractTopics();
    @Before
    public void setup() {
        deployLog = TestingHelper.getOneTopicEvent(contractAddress,
                10,
                "BettingContractDeployed",
                0,
                Hash);

        projectedState = new ProjectedState();

        StatePopulator statePopulator = new StatePopulator(projectedState);

        eventListener = new EventListener(nodeConnection,
                statePopulator,
                deployLog.blockNumber,
                pollingIntervalMillis,
                5,
                topics,
                contractAddress);

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
    public void testSuccessfulFirstLogMatchSameBlock() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = 100;;
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)));

        when(nodeConnection.getLogs(blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)));

        when(nodeConnection.getLogs(102, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(4, projectedState.getBlocks().size());
        Assert.assertEquals(102, projectedState.getBlocks().getLast().getBlockNumber());

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertArrayEquals(player1.getAddressBytes(), projectedState.getPlayers().get(expectedId).getPlayerAddress().toBytes());
        Assert.assertArrayEquals(player2.getAddressBytes(), projectedState.getPlayers().get(expectedId + 2).getPlayerAddress().toBytes());

        Assert.assertEquals(1, projectedState.getStatements().size());
    }

    @Test
    public void testSuccessfulUnorderedBlockEvents() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        long blockNumber = 100;;
        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, blockNumber, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)));

        when(nodeConnection.getLogs(blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, registerLog, submitLog)));

        when(nodeConnection.getLogs(102, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(4, projectedState.getBlocks().size());
        Assert.assertEquals(102, projectedState.getBlocks().getLast().getBlockNumber());

        int expectedId = 1;
        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertArrayEquals(player1.getAddressBytes(), projectedState.getPlayers().get(expectedId).getPlayerAddress().toBytes());
        Assert.assertArrayEquals(player2.getAddressBytes(), projectedState.getPlayers().get(expectedId + 2).getPlayerAddress().toBytes());

        Assert.assertEquals(1, projectedState.getStatements().size());
    }

    @Test
    public void testReorgRemoveEvent() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, 110, player2, 1, "A".getBytes(), 0, null);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2)));

        // chain reorgs
        when(nodeConnection.getLogs(110, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>());

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(4, projectedState.getBlocks().size());
        Assert.assertEquals(102, projectedState.getBlocks().getLast().getBlockNumber());

        Assert.assertEquals(1, projectedState.getStatements().size());
        Assert.assertEquals(0, projectedState.getVotes().size());

    }

    @Test
    public void testReorgNewEvent() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, 110, player2, 1, "A".getBytes(), 0, null);
        Log newVoteLog = TestingHelper.getVotedLog(deployLog.address, 109, player2, 1, "A".getBytes(), 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, 110, player1, 1, "A".getBytes(), 0, null);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, newVoteLog, voteLogReplacement)));

        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(voteLogReplacement)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(6, projectedState.getBlocks().size());
        Assert.assertEquals(110, projectedState.getBlocks().getLast().getBlockNumber());

        int expectedId = 2;
        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertEquals(1, projectedState.getStatements().size());
        Assert.assertEquals(2, projectedState.getStatements().get(expectedId).getVoteEventIds().size());
        Assert.assertTrue(projectedState.getStatements().get(expectedId).getVoteEventIds().contains(expectedId + 3));
        Assert.assertTrue(projectedState.getStatements().get(expectedId).getVoteEventIds().contains(expectedId + 4));
        Assert.assertEquals(2, projectedState.getVotes().size());
    }

    @Test
    public void testReorgUntilDeployEvent() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2Replacement = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, 110, player1, 1, "A".getBytes(), 0, null);

        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, 80, player1, 0, null);
        Log submitLogReplacement = TestingHelper.getSubmittedStatementLog(deployLog.address, 91, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2Replacement = TestingHelper.getRegisteredLog(deployLog.address, 102, player2Replacement, 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, 102, player2Replacement, 1, "A".getBytes(), 0, registerLog2Replacement.blockHash);

        Log submitLog2 = TestingHelper.getSubmittedStatementLog(player1, 123, player2Replacement, 2, "Q".getBytes(), "H".getBytes(), 0, null);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        when(nodeConnection.getLogs(submitLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(5, projectedState.getBlocks().size());
        Assert.assertEquals(123, projectedState.getBlocks().getLast().getBlockNumber());

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertTrue(containsPlayer(projectedState.getPlayers(), player1));
        Assert.assertTrue(containsPlayer(projectedState.getPlayers(), player2Replacement));

        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertEquals(1, projectedState.getVotes().size());
    }

    @Test
    public void testReorgNewDeployEventLater() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2Replacement = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, 110, player1, 1, "A".getBytes(), 0, null);

        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, 80, player1, 0, null);
        Log submitLogReplacement = TestingHelper.getSubmittedStatementLog(deployLog.address, 91, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2Replacement = TestingHelper.getRegisteredLog(deployLog.address, 102, player2Replacement, 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, 102, player2Replacement, 1, "A".getBytes(), 0, registerLog2Replacement.blockHash);

        Log submitLog2 = TestingHelper.getSubmittedStatementLog(player1, 123, player2Replacement, 2, "Q".getBytes(), "H".getBytes(), 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getOneTopicEvent(deployLog.address, 20, "BettingContractDeployed", 0, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>());

        when(nodeConnection.getLogs(submitLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        when(nodeConnection.getLogs(eq(Long.valueOf(5)), eq("latest"), any(HashSet.class), eq(contractAddress)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(5, projectedState.getBlocks().size());
        Assert.assertEquals(123, projectedState.getBlocks().getLast().getBlockNumber());
        Assert.assertEquals(newDeployLog.blockNumber, projectedState.getBlocks().getFirst().getBlockNumber());

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertTrue(containsPlayer(projectedState.getPlayers(), player1));
        Assert.assertTrue(containsPlayer(projectedState.getPlayers(), player2Replacement));

        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertEquals(1, projectedState.getVotes().size());
    }

    @Test
    public void testReorgNewDeployEventEarlier() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2Replacement = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, 110, player1, 1, "A".getBytes(), 0, null);
        Log answerLog = TestingHelper.getRevealedAnswerLog(deployLog.address, 110, 1, "A".getBytes(), 1, voteLog.blockHash);

        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, 80, player1, 0, null);
        Log submitLogReplacement = TestingHelper.getSubmittedStatementLog(deployLog.address, 91, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2Replacement = TestingHelper.getRegisteredLog(deployLog.address, 102, player2Replacement, 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, 102, player2Replacement, 1, "A".getBytes(), 0, registerLog2Replacement.blockHash);
        Log answerLogReplacement = TestingHelper.getRevealedAnswerLog(deployLog.address, 102, 1, "A".getBytes(), 1, voteLog.blockHash);

        Log submitLog2 = TestingHelper.getSubmittedStatementLog(player1, 123, player2Replacement, 2, "Q".getBytes(), "H".getBytes(), 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getOneTopicEvent(deployLog.address, 5, "BettingContractDeployed", 0, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2, answerLogReplacement)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2, answerLogReplacement)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog, answerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2, answerLogReplacement)));
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>());

        when(nodeConnection.getLogs(submitLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        when(nodeConnection.getLogs(eq(Long.valueOf(5)), eq("latest"), any(HashSet.class), eq(contractAddress)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2, answerLogReplacement)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(5, projectedState.getBlocks().size());
        Assert.assertEquals(123, projectedState.getBlocks().getLast().getBlockNumber());
        Assert.assertEquals(newDeployLog.blockNumber, projectedState.getBlocks().getFirst().getBlockNumber());

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertTrue(containsPlayer(projectedState.getPlayers(), player1));
        Assert.assertTrue(containsPlayer(projectedState.getPlayers(), player2Replacement));

        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertEquals(1, projectedState.getVotes().size());

        int expectedId = 7;
        Assert.assertEquals(1, projectedState.getStatements().get(expectedId).getVoteEventIds().size());
        Assert.assertTrue(projectedState.getStatements().get(expectedId).getVoteEventIds().contains(expectedId + 2));
        Assert.assertEquals(10, (int) projectedState.getStatements().get(expectedId).getAnswerEventId());
    }

    @Test
    public void testReorgOnlyDeployEvent() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, 110, player1, 1, "A".getBytes(), 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getOneTopicEvent(deployLog.address, 15, "BettingContractDeployed", 0, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>());
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>());
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>());

        when(nodeConnection.getLogs(eq(Long.valueOf(5)), eq("latest"), any(HashSet.class), eq(contractAddress)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", topics, contractAddress))
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
    public void testReorgOnlyDeployEventBefore() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, 110, player1, 1, "A".getBytes(), 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getOneTopicEvent(deployLog.address, 5, "BettingContractDeployed", 0, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>());
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>());
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>());
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>());

        when(nodeConnection.getLogs(eq(Long.valueOf(5)), eq("latest"), any(HashSet.class), eq(contractAddress)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));

        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", topics, contractAddress))
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
    public void testReorgUntilDeployEventUnorderedReturn() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2Replacement = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);
        Log voteLog = TestingHelper.getVotedLog(deployLog.address, 110, player1, 1, "A".getBytes(), 0, null);

        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, 91, player1, 0, null);
        Log submitLogReplacement = TestingHelper.getSubmittedStatementLog(deployLog.address, 91, player1, 1, "Q".getBytes(), "H".getBytes(), 1, null);
        Log registerLog2Replacement = TestingHelper.getRegisteredLog(deployLog.address, 102, player2Replacement, 0, null);
        Log voteLogReplacement = TestingHelper.getVotedLog(deployLog.address, 102, player2Replacement, 1, "A".getBytes(), 2, registerLog2Replacement.blockHash);

        Log submitLog2 = TestingHelper.getSubmittedStatementLog(player1, 123, player2Replacement, 2, "Q".getBytes(), "H".getBytes(), 0, null);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLogReplacement, submitLogReplacement, registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2Replacement, voteLogReplacement, submitLog2)));
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog2, voteLog)))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2, voteLogReplacement, registerLog2Replacement)));
        // chain reorgs
        when(nodeConnection.getLogs(voteLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        when(nodeConnection.getLogs(submitLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(submitLog2)));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(4, projectedState.getBlocks().size());
        Assert.assertEquals(123, projectedState.getBlocks().getLast().getBlockNumber());

        Assert.assertEquals(2, projectedState.getPlayers().size());
        Assert.assertTrue(containsPlayer(projectedState.getPlayers(), player1));
        Assert.assertTrue(containsPlayer(projectedState.getPlayers(), player2Replacement));

        Assert.assertEquals(2, projectedState.getStatements().size());
        Assert.assertEquals(1, projectedState.getVotes().size());
    }

    @Test
    public void testReorgDeployEvent() throws InterruptedException {
        Address player1 = new Address(TestingHelper.getRandomAddressBytes());
        Address player2 = new Address(TestingHelper.getRandomAddressBytes());

        Log registerLog = TestingHelper.getRegisteredLog(deployLog.address, 100, player1, 0, null);
        Log submitLog = TestingHelper.getSubmittedStatementLog(deployLog.address, 101, player1, 1, "Q".getBytes(), "H".getBytes(), 0, null);
        Log registerLog2 = TestingHelper.getRegisteredLog(deployLog.address, 102, player2, 0, null);

        byte[] newDeployBlockHash = TestingHelper.getRandomAddressBytes();
        Log newDeployLog = TestingHelper.getOneTopicEvent(deployLog.address, 5, "BettingContractDeployed", 0, newDeployBlockHash);
        Log registerLogReplacement = TestingHelper.getRegisteredLog(deployLog.address, 5, player1, 1, newDeployBlockHash);

        // 10 -> 100
        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(deployLog, registerLog)))
                .thenReturn(new ArrayList<>());
        // 100 -> 102
        when(nodeConnection.getLogs(registerLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>(Arrays.asList(registerLog, submitLog, registerLog2)))
                .thenReturn(new ArrayList<>());
        // 102 -> 110
        when(nodeConnection.getLogs(registerLog2.blockNumber, "latest", topics, contractAddress))
                .thenReturn(new ArrayList<>());
        when(nodeConnection.getLogs(eq(Long.valueOf(5)), eq("latest"), any(HashSet.class), eq(contractAddress)))
                .thenReturn(new ArrayList<>(Arrays.asList(newDeployLog)));
        // chain reorgs
        when(nodeConnection.getLogs(newDeployLog.blockNumber, "latest", topics, contractAddress))
                .thenReturn(Arrays.asList(newDeployLog, registerLogReplacement));

        startThreads();
        Thread.sleep(pollingIntervalMillis * 10);
        shutdownThreads();

        Assert.assertEquals(1, projectedState.getBlocks().size());
        Assert.assertEquals(5, projectedState.getBlocks().getLast().getBlockNumber());
        Assert.assertEquals(1, projectedState.getPlayers().size());
        Assert.assertEquals(0, projectedState.getStatements().size());
        Assert.assertEquals(0, projectedState.getVotes().size());
    }

    @Test
    public void testReorgScoreCalculation() throws InterruptedException {
        String answer = "A";
        int statementId = 1;

        Address[] player = new Address[5];
        Log[] registerLogs = new Log[player.length];
        Log[] voteLogs = new Log[player.length * 2];
        byte[] sampleHash = getRandomAddressBytes();

        for (int i = 0; i < player.length; i++) {
            player[i] = new Address(getRandomAddressBytes());
            registerLogs[i] = getRegisteredLog(deployLog.address, deployLog.blockNumber + 1, player[i], i, sampleHash);
            voteLogs[i] = getVotedLog(deployLog.address, deployLog.blockNumber + 1, player[i], statementId, answer.getBytes(), i + player.length * 2, sampleHash);
            voteLogs[i + player.length] = getVotedLog(deployLog.address, deployLog.blockNumber + 1, player[i], statementId + 1, answer.getBytes(), i + player.length * 3, sampleHash);
        }

        Log submittedLog = getSubmittedStatementLog(deployLog.address, deployLog.blockNumber + 1, player[0], statementId, "S0".getBytes(), getRandomAddressBytes(), player.length + 1, sampleHash);
        Log submittedLog2 = getSubmittedStatementLog(deployLog.address, deployLog.blockNumber + 1, player[0], statementId + 1, "S0".getBytes(), getRandomAddressBytes(), player.length + 2, sampleHash);

        byte[] sampleHash2 = getRandomAddressBytes();
        Log revealedAnswerLog = getRevealedAnswerLog(deployLog.address, deployLog.blockNumber + 2, statementId, answer.getBytes(), player.length * 5, sampleHash2);
        Log revealedAnswerLog2 = getRevealedAnswerLog(deployLog.address, deployLog.blockNumber + 2, statementId + 1, answer.getBytes(), player.length * 5 + 10, sampleHash2);
        Log revealedAnswerLogReplacement = getRevealedAnswerLog(deployLog.address, deployLog.blockNumber + 2, statementId + 1, answer.getBytes(), player.length * 5 + 10, getRandomAddressBytes());

        List<Log> logs1 = new ArrayList<>(Arrays.asList(deployLog));
        logs1.addAll(Arrays.asList(registerLogs));
        logs1.addAll(Arrays.asList(submittedLog, submittedLog2));
        logs1.addAll(Arrays.asList(voteLogs));
        logs1.addAll(Arrays.asList(revealedAnswerLog, revealedAnswerLog2));

        List<Log> logs2 = new ArrayList<>();
        logs2.addAll(Arrays.asList(registerLogs));
        logs2.addAll(Arrays.asList(submittedLog, submittedLog2));
        logs2.addAll(Arrays.asList(voteLogs));
        logs2.addAll(Arrays.asList(revealedAnswerLogReplacement));

        List<Log> logs3 = new ArrayList<>();
        logs3.addAll(Arrays.asList(registerLogs));
        logs3.addAll(Arrays.asList(submittedLog, submittedLog2));
        logs3.addAll(Arrays.asList(voteLogs));
        logs3.addAll(Arrays.asList(revealedAnswerLogReplacement));

        when(nodeConnection.getLogs(deployLog.blockNumber, "latest", topics, contractAddress)).thenReturn(logs1);
        when(nodeConnection.getLogs(deployLog.blockNumber + 1, "latest", topics, contractAddress)).thenReturn(logs2);
        when(nodeConnection.getLogs(deployLog.blockNumber + 2, "latest", topics, contractAddress))
                .thenReturn(Arrays.asList(revealedAnswerLog, revealedAnswerLog2))
                .thenReturn(Arrays.asList(revealedAnswerLogReplacement));

        startThreads();

        Thread.sleep(pollingIntervalMillis * 10);

        Map<Integer, Player> players = projectedState.getPlayers();
        for (int i = 0; i < player.length; i++) {
            Assert.assertEquals(1, players.get(i + 1).getScore());
        }

        shutdownThreads();
    }

    private boolean containsPlayer(Map<Integer, Player> playerMap, Address player) {
        types.Address internalAddress = new types.Address(player.getAddressBytes());
        for (Player p : playerMap.values()) {
            if (p.getPlayerAddress().equals(internalAddress)) {
                return true;
            }
        }
        return false;
    }
}
