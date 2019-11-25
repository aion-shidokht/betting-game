package types;

import internal.Assertion;
import org.aion.util.bytes.ByteUtil;

import java.math.BigInteger;
import java.util.List;

public class Vote {
    private final Address playerAddress;
    private final int statementId;
    private final String guessedAnswer;
    private final String transactionHash;

    private Vote(Address playerAddress, int statementId, String guessedAnswer, String transactionHash) {
        this.playerAddress = playerAddress;
        this.statementId = statementId;
        this.guessedAnswer = guessedAnswer;
        this.transactionHash = transactionHash;
    }

    public static Vote from(List<byte[]> topics, byte[] data, byte[] transactionHash) {
        Assertion.assertTopicSize(topics, 3);
        // topic[0] is Voted
        return new Vote(new Address(topics.get(1)),
                new BigInteger(ByteUtil.toHexString(topics.get(2)), 16).intValue(),
                new String(data),
                "0x" + ByteUtil.toHexString(transactionHash));
    }

    public Address getPlayerAddress() {
        return playerAddress;
    }

    public int getStatementId() {
        return statementId;
    }

    public String getGuessedAnswer() {
        return guessedAnswer;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public Vote(Vote vote){
        this.playerAddress = vote.playerAddress;
        this.statementId = vote.statementId;
        this.guessedAnswer = vote.guessedAnswer;
        this.transactionHash = vote.transactionHash;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "playerAddress=" + playerAddress +
                ", statementId=" + statementId +
                ", guessedAnswer=" + guessedAnswer +
                ", transactionHash=" + transactionHash +
                '}';
    }
}
