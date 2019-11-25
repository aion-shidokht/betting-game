package types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import internal.Assertion;
import org.aion.harness.kernel.Address;
import util.Pair;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Game {
    private Pair<Integer, Boolean> isStopped;
    private Pair<Integer, Boolean> prizeDistributed;
    private Pair<Integer, Address[]> winners;
    private Map<Integer, BigInteger> transferredValues;
    private Map<Integer, byte[]> transactionHashes;

    public Game() {
        isStopped = Pair.of(-1, false);
        prizeDistributed = Pair.of(-1, false);
        transferredValues = new HashMap<>();
        winners = Pair.of(-1, null);
        transactionHashes = new HashMap<>();
    }

    private Game(Pair<Integer, Boolean> isStopped, Pair<Integer, Boolean> prizeDistributed, Pair<Integer, Address[]> winners, Map<Integer, BigInteger> transferredValues, Map<Integer, byte[]> transactionHashes) {
        this.isStopped = isStopped;
        this.prizeDistributed = prizeDistributed;
        this.winners = winners;
        this.transferredValues = transferredValues;
        this.transactionHashes = transactionHashes;
    }

    public void setAsStopped(Integer id, byte[] transactionHash) {
        this.isStopped = Pair.of(id, true);
        Assertion.assertTrue(!transactionHashes.containsKey(id));
        transactionHashes.put(id, transactionHash);
    }

    public void setPrizeDistributed(Integer id, byte[] transactionHash) {
        this.prizeDistributed = Pair.of(id, true);
        Assertion.assertTrue(!transactionHashes.containsKey(id));
        transactionHashes.put(id, transactionHash);
    }

    public void setWinners(Integer id, Address[] winners, byte[] transactionHash) {
        this.winners = Pair.of(id, winners);
        Assertion.assertTrue(!transactionHashes.containsKey(id));
        transactionHashes.put(id, transactionHash);
    }

    public void addValueTransfer(Integer id, BigInteger value, byte[] transactionHash) {
        Assertion.assertTrue(!transferredValues.containsKey(id));
        transferredValues.put(id, value);
        Assertion.assertTrue(!transactionHashes.containsKey(id));
        transactionHashes.put(id, transactionHash);
    }

    public boolean isStopped() {
        return isStopped.value;
    }

    public boolean isPrizeDistributed() {
        return prizeDistributed.value;
    }

    public Address[] getWinners() {
        return winners.value;
    }

    public BigInteger getTotalPrizeAmount() {
        return transferredValues.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
    }

    @JsonIgnore
    public Game getCopy(){
        return new Game(isStopped, prizeDistributed, winners, transferredValues, transactionHashes);
    }

    public Map<Integer, byte[]> getTransactionHashes() {
        return transactionHashes;
    }

    public byte[] getTransactionHash(int id) {
        return transactionHashes.get(id);
    }

    public void clear() {
        resetStopped();
        resetDistributedPrize();
        resetWinnersList();
        transferredValues.clear();
        transactionHashes.clear();
    }

    private void resetStopped() {
        isStopped = Pair.of(-1, false);
    }

    private void resetDistributedPrize() {
        prizeDistributed = Pair.of(-1, false);
    }

    private void resetWinnersList() {
        winners = Pair.of(-1, null);
    }

    public void revert(Set<Integer> logIds) {
        transferredValues.keySet().removeAll(logIds);
        transactionHashes.keySet().removeAll(logIds);
        if (logIds.contains(isStopped.key)) {
            resetStopped();
        }
        if (logIds.contains(prizeDistributed.key)) {
            resetDistributedPrize();
        }
        if (logIds.contains(winners.key)) {
            resetWinnersList();
        }
    }

    @Override
    public String toString() {
        return "Game{" +
                "isStopped=" + isStopped +
                ", prizeDistributed=" + prizeDistributed +
                ", winners=" + winners +
                ", transferredValues=" + transferredValues +
                ", transactionHashes=" + transactionHashes +
                '}';
    }
}
