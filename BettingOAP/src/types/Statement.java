package types;

import internal.Assertion;
import util.Helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Statement {
    private final Address playerAddress;
    private final int statementId;
    private final String answerHash;
    private final String statement;
    private final String transactionHash;
    private final long blockNumber;

    // internal ids associated with events.
    private final Set<Integer> voteEventIds;
    private int answerEventId;

    private Statement(Address playerAddress, int statementId, String answerHash, String statement, String transactionHash, long blockNumber) {
        this.playerAddress = playerAddress;
        this.statementId = statementId;
        this.answerHash = answerHash;
        this.statement = statement;
        this.transactionHash = transactionHash;
        this.blockNumber = blockNumber;
        voteEventIds = new HashSet<>();
        answerEventId = -1;
    }

    public static Statement from(List<byte[]> topics, byte[] data, byte[] transactionHash, long blockNumber) {
        Assertion.assertTopicSize(topics, 4);
        return new Statement(new Address(topics.get(1)),
                Helper.byteArrayToInteger(topics.get(2)),
                Helper.bytesToHexStringWith0x(topics.get(3)),
                new String(data),
                Helper.bytesToHexStringWith0x(transactionHash),
                blockNumber);
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
        this.blockNumber = statement.blockNumber;
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

    public long getBlockNumber() {
        return blockNumber;
    }

    @Override
    public String toString() {
        return "Statement{" +
                "playerAddress=" + playerAddress +
                ", statementId=" + statementId +
                ", answerHash='" + answerHash + '\'' +
                ", statement='" + statement + '\'' +
                ", transactionHash='" + transactionHash + '\'' +
                ", blockNumber=" + blockNumber +
                ", voteEventIds=" + voteEventIds +
                ", answerEventId=" + answerEventId +
                '}';
    }
}
