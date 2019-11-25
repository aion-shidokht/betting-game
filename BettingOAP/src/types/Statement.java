package types;

import internal.Assertion;
import org.aion.util.bytes.ByteUtil;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Statement {
    private final Address playerAddress;
    private final int statementId;
    private final String answerHash;
    private final String statement;
    private final String transactionHash;
    // internal ids associated with events.
    private final Set<Integer> voteEventIds;
    private int answerEventId;

    private Statement(Address playerAddress, int statementId, String answerHash, String statement, String transactionHash) {
        this.playerAddress = playerAddress;
        this.statementId = statementId;
        this.answerHash = answerHash;
        this.statement = statement;
        this.transactionHash = transactionHash;
        voteEventIds = new HashSet<>();
        answerEventId = -1;
    }

    public static Statement from(List<byte[]> topics, byte[] data, byte[] transactionHash) {
        Assertion.assertTopicSize(topics, 4);
        return new Statement(new Address(topics.get(1)),
                new BigInteger(ByteUtil.toHexString(topics.get(2)), 16).intValue(),
                "0x" + ByteUtil.toHexString(topics.get(3)),
                new String(data),
                "0x" + ByteUtil.toHexString(transactionHash));
    }

    public Address getPlayerAddress() {
        return playerAddress;
    }

    public int getStatementId() {
        return statementId;
    }

    public String getAnswerHash() {
        return answerHash;
    }

    public String getStatementString() {
        return statement;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public Statement(Statement statement){
        this.playerAddress = statement.playerAddress;
        this.statementId = statement.statementId;
        this.answerHash = statement.answerHash;
        this.statement = statement.statement;
        this.transactionHash = statement.transactionHash;
        this.answerEventId = statement.answerEventId;
        this.voteEventIds = statement.voteEventIds;
    }

    public Set<Integer> getVoteEventIds() {
        return voteEventIds;
    }

    public Integer getAnswerEventId() {
        return answerEventId;
    }

    public void addVoteId(int id) {
        voteEventIds.add(id);
    }

    public boolean removeVoteId(int id) {
        return voteEventIds.remove(id);
    }

    public void setAnswerEventId(int answerEventId) {
        this.answerEventId = answerEventId;
    }

    public void resetAnswerId() {
        this.answerEventId = -1;
    }

    @Override
    public String toString() {
        return "Statement{" +
                "playerAddress=" + playerAddress +
                ", statementId=" + statementId +
                ", answerHash=" + answerHash +
                ", statement='" + statement + '\'' +
                ", transactionHash=" + transactionHash +
                ", voteEventIds=" + voteEventIds +
                ", answerEventId=" + answerEventId +
                '}';
    }
}
