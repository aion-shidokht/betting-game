package types;

import internal.Assertion;
import org.aion.harness.kernel.Address;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class Vote {
    private final Address player;
    private final int statementId;
    private final byte[] guessedAnswer;
    private final byte[] transactionHash;

    private Vote(Address player, int statementId, byte[] guessedAnswer, byte[] transactionHash) {
        this.player = player;
        this.statementId = statementId;
        this.guessedAnswer = guessedAnswer;
        this.transactionHash = transactionHash;
    }

    public static Vote from(List<byte[]> topics, byte[] data, byte[] transactionHash) {
        Assertion.assertTopicSize(topics, 3);
        // topic[0] is Voted
        return new Vote(new Address(topics.get(1)),
                new BigInteger(Hex.encodeHexString(topics.get(2)), 16).intValue(),
                data,
                transactionHash);
    }

    public Address getPlayer() {
        return player;
    }

    public int getStatementId() {
        return statementId;
    }

    public byte[] getGuessedAnswer() {
        return guessedAnswer.clone();
    }

    public byte[] getTransactionHash() {
        return transactionHash.clone();
    }

    public Vote(Vote vote){
        this.player = vote.player;
        this.statementId = vote.statementId;
        this.guessedAnswer = vote.guessedAnswer;
        this.transactionHash = vote.transactionHash;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "player=" + player +
                ", statementId=" + statementId +
                ", guessedAnswer=" + new String(guessedAnswer) +
                ", transactionHash=" + Hex.encodeHexString(transactionHash) +
                '}';
    }
}
