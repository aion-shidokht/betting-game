package state;

import org.aion.harness.result.RpcResult;
import types.*;
import types.TransactionDetails;
import org.aion.harness.kernel.Address;
import util.Helper;
import util.NodeConnection;
import worker.BlockNumberCollector;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the current user transaction info
 * This is mainly used to provide feedback about the result of a transaction sent through the app, and does queries on the projected state based on the UI needs.
 */
public class UserState {

    // todo can be optimized depending on the user interface queries
    private Map<String, List<TransactionDetails>> userTransactions;

    private ProjectedState projectedState;
    private NodeConnection nodeConnection;
    private BlockNumberCollector blockNumberCollector;

    public UserState(ProjectedState projectedState,
                     NodeConnection nodeConnection,
                     BlockNumberCollector blockNumberCollector) {
        this.userTransactions = new ConcurrentHashMap<>();
        this.projectedState = projectedState;
        this.nodeConnection = nodeConnection;
        this.blockNumberCollector = blockNumberCollector;
    }

    public void putTransaction(Address sender, TransactionDetails transactionDetails) {
        String addressString = Helper.bytesToHexString(sender.getAddressBytes());
        if (userTransactions.containsKey(addressString)) {
            userTransactions.get(addressString).add(transactionDetails);
        } else {
            userTransactions.put(addressString, new ArrayList<>(Arrays.asList(transactionDetails)));
        }
    }

    // queried from front end

    public List<TransactionDetails> getTransactions(String user) {
        return userTransactions.get(user);
    }

    public TransactionDetails getTransactionInfoForHash(String user, String transactionHash) {
        if (userTransactions.containsKey(user)) {
            Optional<TransactionDetails> transaction = userTransactions.get(user).stream().filter(info -> info.getTransactionHash().equals(transactionHash)).findAny();
            if (transaction.isPresent()) {
                return transaction.get();
            }
        }
        return null;
    }

    public BigInteger getNonce(Address address) {
        try {
            RpcResult<BigInteger> nonceResult = nodeConnection.getNonce(address);
            if (nonceResult.isSuccess()) {
                return nonceResult.getResult();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Note that following methods do not always return the most current state of the projected state.
    // Once the maps are copied from the projected state, they are iterated over here. the state might have changed
    // between 2 consecutive ConcurrentHashMap copies and the retrieval methods in this class should be able to handle that.
    public List<AggregatedStatement> getStatements() {
        long blockNumber = blockNumberCollector.getCurrentBlockNumber();

        Map<Integer, Statement> statements = projectedState.getStatements();
        Map<Integer, Answer> answers = projectedState.getAnswers();
        List<AggregatedStatement> response = new ArrayList<>();

        for (Statement s : statements.values()) {
            if (s.getAnswerEventId() > 0 && answers.containsKey(s.getAnswerEventId())) {
                response.add(new AggregatedStatement(s, answers.get(s.getAnswerEventId()), blockNumber));
            } else {
                response.add(new AggregatedStatement(s, null, blockNumber));
            }
        }

        return response;
    }

    public Collection<AggregatedPlayer> getPlayers() {
        long blockNumber = blockNumberCollector.getCurrentBlockNumber();

        boolean prizeDistributed = projectedState.getGameStatus().isPrizeDistributed();

        List<Player> players = new ArrayList<>(projectedState.getPlayers().values());
        List<AggregatedPlayer> aggregatedPlayers = new ArrayList<>();
        for(Player p: players){
            aggregatedPlayers.add(new AggregatedPlayer(p, blockNumber, prizeDistributed));
        }
        return aggregatedPlayers;
    }

    public Map<Integer, Answer> getAnswers() {
        return projectedState.getAnswers();
    }

    public List<AggregatedVote> getVotes() {
        long blockNumber = blockNumberCollector.getCurrentBlockNumber();

        List<Answer> answers = new ArrayList<>(projectedState.getAnswers().values());
        List<Vote> votes = new ArrayList<>(projectedState.getVotes().values());
        List<AggregatedVote> aggregatedVotes = new ArrayList<>();

        for (Vote v : votes) {
            boolean found = false;
            for (Answer a : answers) {
                if (v.getStatementId() == a.getStatementId() && v.getGuessedAnswer().equals(a.getAnswer())) {
                    found = true;
                    break;
                }
            }
            aggregatedVotes.add(new AggregatedVote(v, blockNumber, found));
        }
        return aggregatedVotes;
    }

    public Game getGameStatus() {
        return projectedState.getGameStatus();
    }
}
