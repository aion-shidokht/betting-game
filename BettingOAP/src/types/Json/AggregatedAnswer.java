package types.Json;

import types.Answer;

public class AggregatedAnswer {
    private final int statementId;
    private final String answer;
    private final String transactionHash;
    private final long age;

    public AggregatedAnswer(Answer answer, long blockNumber){
        this.statementId = answer.getStatementId();
        this.answer = answer.getAnswer();
        this.transactionHash = answer.getTransactionHash();
        this.age = blockNumber - answer.getBlockNumber();
    }

    public int getStatementId() {
        return statementId;
    }

    public String getAnswer() {
        return answer;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public long getAge() {
        return age;
    }
}
