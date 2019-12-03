package types;

public class AggregatedVote {
    private final Address playerAddress;
    private final int statementId;
    private final String guessedAnswer;
    private final String transactionHash;
    private final long age;
    private final boolean isCorrect;

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

    public long getAge() {
        return age;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public AggregatedVote(Vote vote, long blockNumber, boolean b) {
        this.playerAddress = vote.getPlayerAddress();
        this.statementId = vote.getStatementId();
        this.guessedAnswer = vote.getGuessedAnswer();
        this.transactionHash = vote.getTransactionHash();
        this.age = blockNumber - vote.getBlockNumber();
        this.isCorrect = b;
    }
}