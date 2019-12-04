package types.Json;

import types.Address;
import types.Answer;
import types.Statement;

import java.util.Set;

// this is mainly used create a new json object from a statementString and an answer, and to reduce the number of GET calls
public class AggregatedStatement {
    private final Address playerAddress;
    private final int statementId;
    private final String answerHash;
    private final String statementString;
    private final String statementTransactionHash;
    private final long statementAge;
    // internal ids associated with events.
    private final Set<Integer> voteEventIds;

    private String answerString;
    private String answerTransactionHash;
    private long answerAge;

    public AggregatedStatement(Statement statement, Answer answer, long blockNumber){
        this.playerAddress = statement.getPlayerAddress();
        this.statementId = statement.getStatementId();
        this.answerHash = statement.getAnswerHash();
        this.statementString = statement.getStatementString();
        this.statementTransactionHash = statement.getTransactionHash();
        this.voteEventIds = statement.getVoteEventIds();
        this.statementAge = blockNumber - statement.getBlockNumber();
        if(answer != null) {
            this.answerString = answer.getAnswer();
            this.answerTransactionHash = answer.getTransactionHash();
            this.answerAge = blockNumber - answer.getBlockNumber();
        }
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
        return statementString;
    }

    public String getStatementTransactionHash() {
        return statementTransactionHash;
    }

    public Set<Integer> getVoteEventIds() {
        return voteEventIds;
    }

    public String getAnswerString() {
        return answerString;
    }

    public String getAnswerTransactionHash() {
        return answerTransactionHash;
    }

    public long getStatementAge() {
        return statementAge;
    }

    public long getAnswerAge() {
        return answerAge;
    }
}
