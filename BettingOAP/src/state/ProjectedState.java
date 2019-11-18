package state;

import types.Answer;
import types.Game;
import types.Statement;
import types.Vote;
import internal.Assertion;
import org.aion.harness.kernel.Address;
import types.BlockTuple;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class to hold all the data related to application state
 */
public class ProjectedState {

    private LinkedList<BlockTuple> blocks;
    // mappings of logId to the actual log
    private Map<Integer, Address> players;
    private Map<Integer, Statement> statements;
    private Map<Integer, Vote> votes;
    private Map<Integer, Answer> answers;
    private Game currentGame;
    // id to associate with new logs, starting from zero
    // Note that if the log is removed, the associated id is not reused
    private int currentEventId;

    private ReadWriteLock gameLock = new ReentrantReadWriteLock();

    public ProjectedState() {
        blocks = new LinkedList<>();
        players = new ConcurrentHashMap<>();
        statements = new ConcurrentHashMap<>();
        votes = new ConcurrentHashMap<>();
        answers = new ConcurrentHashMap<>();
        currentGame = new Game();
    }

    public void addBlockTuple(BigInteger blockNumber, byte[] blockHash, List<Integer> logIds) {
        if (blocks.size() > 0) {
            Assertion.assertTrue(blocks.getLast().getBlockNumber().compareTo(blockNumber) < 0);
        }
        blocks.add(BlockTuple.of(blockNumber, blockHash, logIds));
    }

    public int addPlayer(Address player) {
        currentEventId++;
        Assertion.assertTrue(!players.containsValue(player));
        players.put(currentEventId, player);
        return currentEventId;
    }

    public int addStatement(Statement statement) {
        currentEventId++;
        // imposing contract restrictions again as a sanity check for the projected state
        Assertion.assertTrue(players.containsValue(statement.getPlayer()));
        Assertion.assertTrue(!containsStatementId(statement.getStatementId()));
        statements.put(currentEventId, statement);
        return currentEventId;
    }

    public int addVote(Vote vote) {
        currentEventId++;
        // imposing contract restrictions again as a sanity check for the projected state
        Assertion.assertTrue(players.containsValue(vote.getPlayer()));
        Assertion.assertTrue(containsStatementId(vote.getStatementId()));
        votes.put(currentEventId, vote);
        return currentEventId;
    }

    public int addAnswer(Answer answer) {
        currentEventId++;
        // imposing contract restrictions again as a sanity check for the projected state
        Assertion.assertTrue(containsStatementId(answer.getStatementId()));
        answers.put(currentEventId, answer);
        return currentEventId;
    }

    /*
     * Game methods
     */

    public int stopGame() {
        gameLock.writeLock().lock();
        try {
            currentEventId++;
            currentGame.setAsStopped(currentEventId);
            return currentEventId;
        } finally {
            gameLock.writeLock().unlock();
        }
    }

    public int distributedPrize() {
        gameLock.writeLock().lock();
        try {
            currentEventId++;
            currentGame.setPrizeDistributed(currentEventId);
            return currentEventId;
        } finally {
            gameLock.writeLock().unlock();
        }
    }

    public int addTransferValue(BigInteger value) {
        gameLock.writeLock().lock();
        try {
            currentEventId++;
            currentGame.addValueTransfer(currentEventId, value);
            return currentEventId;
        } finally {
            gameLock.writeLock().unlock();
        }
    }

    public int deployedContract() {
        // start from current event Id 0
        return currentEventId;
    }

    // This method is mainly used for testing
    public LinkedList<BlockTuple> getBlocks() {
        return new LinkedList<>(blocks);
    }

    public Map<Integer, Address> getPlayers() {
        return Collections.unmodifiableMap(players);
    }

    public Map<Integer, Statement> getStatements() {
        Map<Integer, Statement> statementsCopy = new HashMap<>();
        for(int i: statements.keySet()){
            statementsCopy.put(i, new Statement(statements.get(i)));
        }
        return Collections.unmodifiableMap(statementsCopy);
    }

    public Map<Integer, Vote> getVotes() {
        Map<Integer, Vote> votesCopy = new HashMap<>();
        for(int i: votes.keySet()){
            votesCopy.put(i, new Vote(votes.get(i)));
        }
        return Collections.unmodifiableMap(votesCopy);
    }

    public Map<Integer, Answer> getAnswers() {
        Map<Integer, Answer> answersCopy = new HashMap<>();
        for(int i: answers.keySet()){
            answersCopy.put(i, new Answer(answers.get(i)));
        }
        return Collections.unmodifiableMap(answersCopy);
    }

    public Game getGameStatus() {
        gameLock.readLock().lock();
        try {
            return currentGame.getCopy();
        } finally {
            gameLock.readLock().unlock();
        }
    }

    private boolean containsStatementId(Integer id) {
        return statements.values().stream().anyMatch(s -> s.getStatementId() == id);
    }

    public void clear() {
        blocks.clear();
        players.clear();
        statements.clear();
        votes.clear();
        answers.clear();
        currentGame.clear();
    }

    public void revertBlocks(int count) {
        Set<Integer> logIds = new HashSet<>();
        for (int i = 0; i < count; i++) {
            BlockTuple block = blocks.removeLast();
            logIds.addAll(block.getIncluededLogIds());
        }
        revertLogs(logIds);
    }

    private void revertLogs(Set<Integer> logIds) {
        players.keySet().removeAll(logIds);
        statements.keySet().removeAll(logIds);
        votes.keySet().removeAll(logIds);
        answers.keySet().removeAll(logIds);
        gameLock.writeLock().lock();
        try {
            currentGame.revert(logIds);
        } finally {
            gameLock.writeLock().unlock();
        }
    }

}