package state;

import types.Player;
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
    private Map<Integer, Player> players;
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

    public void addBlockTuple(BlockTuple blockTuple) {
        if (blocks.size() > 0) {
            Assertion.assertTrue(blocks.getLast().getBlockNumber().compareTo(blockTuple.getBlockNumber()) < 0);
        }
        blocks.add(blockTuple);
    }

    public int addPlayer(Player player) {
        currentEventId++;
        Assertion.assertTrue(!containsPlayer(player.getPlayerAddress()));
        players.put(currentEventId, player);
        return currentEventId;
    }

    public int addStatement(Statement statement) {
        currentEventId++;
        // imposing contract restrictions again as a sanity check for the projected state
        Assertion.assertTrue(containsPlayer(statement.getPlayer()));
        Assertion.assertTrue(!findStatementId(statement.getStatementId()).isPresent());
        statements.put(currentEventId, statement);
        return currentEventId;
    }

    public int addVote(Vote vote) {
        currentEventId++;
        // imposing contract restrictions again as a sanity check for the projected state
        Assertion.assertTrue(containsPlayer(vote.getPlayer()));
        Optional<Statement> s = findStatementId(vote.getStatementId());
        Assertion.assertTrue(s.isPresent());
        s.get().addVoteId(currentEventId);
        votes.put(currentEventId, vote);
        return currentEventId;
    }

    public int addAnswer(Answer answer) {
        currentEventId++;
        // imposing contract restrictions again as a sanity check for the projected state
        Optional<Statement> s = findStatementId(answer.getStatementId());
        Assertion.assertTrue(s.isPresent());
        s.get().setAnswerEventId(currentEventId);
        answers.put(currentEventId, answer);
        return currentEventId;
    }

    /*
     * Game methods
     */

    public int stopGame(byte[] transactionHash) {
        gameLock.writeLock().lock();
        try {
            currentEventId++;
            currentGame.setAsStopped(currentEventId, transactionHash);
            return currentEventId;
        } finally {
            gameLock.writeLock().unlock();
        }
    }

    public int distributedPrize(byte[] transactionHash) {
        gameLock.writeLock().lock();
        try {
            currentEventId++;
            currentGame.setPrizeDistributed(currentEventId, transactionHash);
            return currentEventId;
        } finally {
            gameLock.writeLock().unlock();
        }
    }

    public int addTransferValue(BigInteger value, byte[] transactionHash) {
        gameLock.writeLock().lock();
        try {
            currentEventId++;
            currentGame.addValueTransfer(currentEventId, value, transactionHash);
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

    public Map<Integer, Player> getPlayers() {
        Map<Integer, Player> playersCopy = new HashMap<>();
        for(int i: players.keySet()){
            playersCopy.put(i, new Player(players.get(i)));
        }
        return Collections.unmodifiableMap(playersCopy);
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

    private Optional<Statement> findStatementId(Integer id) {
        return statements.values().stream().filter(s -> s.getStatementId() == id).findAny();
    }

    public void clear() {
        blocks.clear();
        players.clear();
        statements.clear();
        votes.clear();
        answers.clear();
        gameLock.writeLock().lock();
        try {
            currentGame.clear();
        } finally {
            gameLock.writeLock().unlock();
        }
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
        for (int id : logIds) {
            Vote v = votes.remove(id);
            Answer a = answers.remove(id);
            Optional<Statement> s = Optional.empty();
            if (v != null) {
                s = findStatementId(v.getStatementId());
                s.ifPresent(statement -> statement.removeVoteId(id));
            }

            if (a != null) {
                if (!s.isPresent()) {
                    s = findStatementId(a.getStatementId());
                }
                s.ifPresent(Statement::resetAnswerId);
            }
            gameLock.writeLock().lock();
            try {
                currentGame.revert(logIds);
            } finally {
                gameLock.writeLock().unlock();
            }
        }
    }

    private boolean containsPlayer(Address player) {
        return players.values().stream().anyMatch(p -> p.getPlayerAddress().equals(player));
    }
}
