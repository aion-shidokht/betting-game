package types;

import internal.Assertion;
import util.Helper;

import java.util.List;

public class Vote {
    private final Address playerAddress;
    private final int statementId;
    private final String guessedAnswer;
    private final String transactionHash;
    private final long blockNumber;

    private Vote(Address playerAddress, int statementId, String guessedAnswer, String transactionHash, long blockNumber) {
        this.playerAddress = playerAddress;
        this.statementId = statementId;
        this.guessedAnswer = guessedAnswer;
        this.transactionHash = transactionHash;
        this.blockNumber = blockNumber;
    }

    public static Vote from(List<byte[]> topics, byte[] data, byte[] transactionHash, long blockNumber) {
        Assertion.assertTopicSize(topics, 3);
        // topic[0] is Voted
        return new Vote(new Address(topics.get(1)),
                Helper.byteArrayToInteger(topics.get(2)),
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

    public String getGuessedAnswer() {
        return guessedAnswer;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public Vote(Vote vote){
        this.playerAddress = vote.playerAddress;
        this.statementId = vote.statementId;
        this.guessedAnswer = vote.guessedAnswer;
        this.transactionHash = vote.transactionHash;
        this.blockNumber = vote.blockNumber;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "playerAddress=" + playerAddress +
                ", statementId=" + statementId +
                ", guessedAnswer='" + guessedAnswer + '\'' +
                ", transactionHash='" + transactionHash + '\'' +
                ", blockNumber=" + blockNumber +
                '}';
    }
}
