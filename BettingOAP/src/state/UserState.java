package state;

import org.aion.util.bytes.ByteUtil;
import org.apache.commons.codec.binary.Hex;
import types.TransactionDetails;
import org.aion.harness.kernel.Address;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the current user transaction info
 * This is mainly used to provide feedback about the result of a transaction sent through the app
 */
public class UserState {

    // todo can be optimized depending on the user interface queries
    private Map<String, List<TransactionDetails>> userTransactions = new ConcurrentHashMap<>();

    public void putTransaction(Address sender, TransactionDetails transactionDetails) {
        String addressString = ByteUtil.toHexString(sender.getAddressBytes());
        if (userTransactions.containsKey(addressString)) {
            userTransactions.get(addressString).add(transactionDetails);
        } else {
            userTransactions.put(addressString, Arrays.asList(transactionDetails));
        }
    }

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

}
