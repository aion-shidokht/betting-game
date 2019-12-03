package types;

import internal.Assertion;
import util.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Player {
    private final Address playerAddress;
    private final String transactionHash;
    private final long blockNumber;
    // this is used to calculated the score
    private final List<Integer> correctVoteAssociatedAnswerEventId;

    public static Player from(byte[] data, byte[] transactionHash, long blockNumber) {
        return new Player(new Address(data),
                Helper.bytesToHexStringWith0x(transactionHash),
                blockNumber);
    }

    private Player(Address playerAddress, String transactionHash, Long blockNumber) {
        this.playerAddress = playerAddress;
        this.transactionHash = transactionHash;
        this.blockNumber = blockNumber;
        correctVoteAssociatedAnswerEventId = new ArrayList<>();
    }

    public Address getPlayerAddress() {
        return playerAddress;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void addAnswerEventId(int id) {
        Assertion.assertTrue(!correctVoteAssociatedAnswerEventId.contains(id));
        correctVoteAssociatedAnswerEventId.add(id);
    }

    public void removeAnswerEventIds(Set<Integer> ids){
        correctVoteAssociatedAnswerEventId.removeAll(ids);
    }

    public int getScore(){
        return correctVoteAssociatedAnswerEventId.size();
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public Player(Player player){
        this.playerAddress = player.playerAddress;
        this.transactionHash= player.transactionHash;
        this.blockNumber = player.blockNumber;
        this.correctVoteAssociatedAnswerEventId = player.correctVoteAssociatedAnswerEventId;
    }

    @Override
    public String toString() {
        return "Player{" +
                "playerAddress=" + playerAddress +
                ", transactionHash='" + transactionHash + '\'' +
                ", blockNumber=" + blockNumber +
                ", correctVoteAssociatedAnswerEventId=" + correctVoteAssociatedAnswerEventId +
                '}';
    }
}
