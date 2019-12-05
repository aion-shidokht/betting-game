package types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import internal.Assertion;
import util.Helper;
import util.Pair;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Game {
    private Pair<Integer, Boolean> isStopped;
    private Pair<Integer, Integer> prizeDistributed;
    private Pair<Integer, Address[]> winners;
    private Map<Integer, BigInteger> transferredValues;
    private Map<Integer, String> transactionHashes;
    private Map<Integer, Long> blockNumbers;

    public Game() {
        isStopped = Pair.of(-1, false);
        prizeDistributed = Pair.of(-1, -1);
        transferredValues = new HashMap<>();
        blockNumbers = new HashMap<>();
        winners = Pair.of(-1, null);
        transactionHashes = new HashMap<>();
        blockNumbers = new HashMap<>();
    }

    private Game(Pair<Integer, Boolean> isStopped, Pair<Integer, Integer> prizeDistributed, Pair<Integer, Address[]> winners,
                 Map<Integer, BigInteger> transferredValues, Map<Integer, String> transactionHashes, Map<Integer, Long> blockNumbers) {
        this.isStopped = isStopped;
        this.prizeDistributed = prizeDistributed;
        this.winners = winners;
        this.transferredValues = transferredValues;
        this.transactionHashes = transactionHashes;
        this.blockNumbers = blockNumbers;
    }

    public void setAsStopped(Integer id, byte[] transactionHash, long blockNumber) {
        this.isStopped = Pair.of(id, true);
        Assertion.assertTrue(!transactionHashes.containsKey(id));
        transactionHashes.put(id, Helper.bytesToHexStringWith0x(transactionHash));
        blockNumbers.put(id, blockNumber);
    }

    public void setPrizeDistributed(Integer id, byte[] data, byte[] transactionHash, long blockNumber) {
        int winnerCount = new BigInteger(data).intValue();
        this.prizeDistributed = Pair.of(id, winnerCount);
        Assertion.assertTrue(!transactionHashes.containsKey(id));
        transactionHashes.put(id, Helper.bytesToHexStringWith0x(transactionHash));
        blockNumbers.put(id, blockNumber);
    }

    public void setWinners(Integer id, Address[] winners, byte[] transactionHash, long blockNumber) {
        this.winners = Pair.of(id, winners);
        Assertion.assertTrue(!transactionHashes.containsKey(id));
        transactionHashes.put(id, Helper.bytesToHexStringWith0x(transactionHash));
        blockNumbers.put(id, blockNumber);
    }

    public void addValueTransfer(Integer id, BigInteger value, byte[] transactionHash, long blockNumber) {
        Assertion.assertTrue(!transferredValues.containsKey(id));
        transferredValues.put(id, value);
        Assertion.assertTrue(!transactionHashes.containsKey(id));
        transactionHashes.put(id, Helper.bytesToHexStringWith0x(transactionHash));
        blockNumbers.put(id, blockNumber);
    }

    public Pair<Integer, Boolean> getStopped() {
        return isStopped;
    }

    public Pair<Integer, Integer> getPrizeDistributed() {
        return prizeDistributed;
    }

    public Pair<Integer, Address[]> getWinners() {
        return winners;
    }

    public BigInteger getTotalPrizeAmount() {
        return transferredValues.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
    }

    @JsonIgnore
    public Game getCopy() {
        return new Game(isStopped, prizeDistributed, winners, transferredValues, transactionHashes, blockNumbers);
    }

    public Map<Integer, String> getTransactionHashes() {
        return transactionHashes;
    }

    public Map<Integer, Long> getBlockNumbers() {
        return blockNumbers;
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
        prizeDistributed = Pair.of(-1, -1);
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
                ", blockNumbers=" + blockNumbers +
                '}';
    }
}
