package state;

import org.aion.harness.result.RpcResult;
import types.*;
import types.TransactionDetails;
import org.aion.harness.kernel.Address;
import util.Helper;
import util.NodeConnection;

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

    public UserState(ProjectedState projectedState,
                     NodeConnection nodeConnection) {
        this.userTransactions = new ConcurrentHashMap<>();
        this.projectedState = projectedState;
        this.nodeConnection = nodeConnection;
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
        Map<Integer, Statement> statements = projectedState.getStatements();
        Map<Integer, Answer> answers = projectedState.getAnswers();
        List<AggregatedStatement> response = new ArrayList<>();

        for (Statement s : statements.values()) {
            if (s.getAnswerEventId() > 0 && answers.containsKey(s.getAnswerEventId())) {
                response.add(new AggregatedStatement(s, answers.get(s.getAnswerEventId())));
            } else {
                response.add(new AggregatedStatement(s, null));
            }
        }

        return response;
    }

    public Map<Integer, Player> getPlayers() {
        return projectedState.getPlayers();
    }

    public Map<Integer, Answer> getAnswers() {
        return projectedState.getAnswers();
    }

    public Map<Integer, Vote> getVotes() {
        return projectedState.getVotes();
    }

    public Game getGameStatus() {
        return projectedState.getGameStatus();
    }
}
