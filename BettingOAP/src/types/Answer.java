package types;

import internal.Assertion;
import org.aion.util.bytes.ByteUtil;

import java.math.BigInteger;
import java.util.List;

public class Answer {
    private final int statementId;
    private final String answer;
    private final String transactionHash;

    public static Answer from(List<byte[]> topics, byte[] data, byte[] transactionHash) {
        Assertion.assertTopicSize(topics, 2);
        return new Answer(new BigInteger(ByteUtil.toHexString(topics.get(1)), 16).intValue(),
                new String(data),
                "0x" + ByteUtil.toHexString(transactionHash));
    }

    private Answer(int statementId, String answer, String transactionHash) {
        this.statementId = statementId;
        this.answer = answer;
        this.transactionHash = transactionHash;
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

    public Answer(Answer answer){
        this.statementId = answer.statementId;
        this.answer = answer.answer;
        this.transactionHash = answer.transactionHash;
    }

    @Override
    public String toString() {
        return "Answer{" +
                "statementId=" + statementId +
                ", answer=" + answer +
                ", transactionHash=" + transactionHash +
                '}';
    }
}
