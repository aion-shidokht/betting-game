package types;

import internal.Assertion;
import org.aion.util.bytes.ByteUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Player {
    private final Address playerAddress;
    private final String transactionHash;
    // this is used to calculated the score
    private final List<Integer> correctVoteAssociatedAnswerEventId;

    public static Player from(byte[] data, byte[] transactionHash) {
        return new Player(new Address(data),
                "0x" + ByteUtil.toHexString(transactionHash));
    }

    private Player(Address playerAddress, String transactionHash) {
        this.playerAddress = playerAddress;
        this.transactionHash = transactionHash;
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

    public Player(Player player){
        this.playerAddress = player.playerAddress;
        this.transactionHash= player.transactionHash;
        this.correctVoteAssociatedAnswerEventId = player.correctVoteAssociatedAnswerEventId;
    }

    @Override
    public String toString() {
        return "Player{" +
                "playerAddress=" + playerAddress +
                ", transactionHash='" + transactionHash + '\'' +
                ", correctVoteAssociatedAnswerEventId=" + correctVoteAssociatedAnswerEventId +
                '}';
    }
}
