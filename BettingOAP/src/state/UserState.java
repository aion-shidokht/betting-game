package state;

import org.aion.harness.result.RpcResult;
import org.aion.util.bytes.ByteUtil;
import types.*;
import types.TransactionDetails;
import org.aion.harness.kernel.Address;
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
        String addressString = ByteUtil.toHexString(sender.getAddressBytes());
        if (userTransactions.containsKey(addressString)) {
            userTransactions.get(addressString).add(transactionDetails);
        } else {
            userTransactions.put(addressString, Arrays.asList(transactionDetails));
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

    public Map<Integer, Statement> getStatements() {
        return projectedState.getStatements();
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
