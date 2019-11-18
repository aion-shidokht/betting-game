package types;

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

    public Game() {
        isStopped = Pair.of(-1, false);
        prizeDistributed = Pair.of(-1, false);
        transferredValues = new HashMap<>();
        winners = Pair.of(-1, null);
    }

    private Game(Pair<Integer, Boolean> isStopped, Pair<Integer, Boolean> prizeDistributed, Pair<Integer, Address[]> winners, Map<Integer, BigInteger> transferredValues) {
        this.isStopped = isStopped;
        this.prizeDistributed = prizeDistributed;
        this.winners = winners;
        this.transferredValues = transferredValues;
    }

    public void setAsStopped(Integer id) {
        this.isStopped = Pair.of(id, true);
    }

    public void setPrizeDistributed(Integer id) {
        this.prizeDistributed = Pair.of(id, true);
    }

    public void setWinners(Integer id, Address[] winners) {
        this.winners = Pair.of(id, winners);
    }

    public void addValueTransfer(Integer id, BigInteger value){
        Assertion.assertTrue(!transferredValues.containsKey(id));
        transferredValues.put(id, value);
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

    public BigInteger getTotalPrizeAmount(){
        return transferredValues.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
    }

    public Game getCopy(){
        return new Game(isStopped, prizeDistributed, winners, transferredValues);
    }

    public void clear() {
        resetStopped();
        resetDistributedPrize();
        resetWinnersList();
        transferredValues.clear();
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
                '}';
    }
}
