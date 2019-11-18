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

    private Vote(Address player, int statementId, byte[] guessedAnswer) {
        this.player = player;
        this.statementId = statementId;
        this.guessedAnswer = guessedAnswer;
    }

    public static Vote from(List<byte[]> topics, byte[] data) {
        Assertion.assertTopicSize(topics, 3);
        // topic[0] is Voted
        return new Vote(new Address(topics.get(1)),
                new BigInteger(Hex.encodeHexString(topics.get(2)), 16).intValue(),
                data);
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

    public Vote(Vote vote){
        this.player = vote.player;
        this.statementId = vote.statementId;
        this.guessedAnswer = vote.guessedAnswer;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "player=" + player +
                ", statementId=" + statementId +
                ", guessedAnswer=" + new String(guessedAnswer) +
                '}';
    }
}
