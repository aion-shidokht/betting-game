package types;

import internal.Assertion;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class Answer {
    private final int statementId;
    private final byte[] answer;

    public static Answer from(List<byte[]> topics, byte[] data) {
        Assertion.assertTopicSize(topics, 2);
        return new Answer(new BigInteger(Hex.encodeHexString(topics.get(1)), 16).intValue(),
                data);
    }

    private Answer(int statementId, byte[] answer) {
        this.statementId = statementId;
        this.answer = answer;
    }

    public int getStatementId() {
        return statementId;
    }

    public byte[] getAnswer() {
        return answer.clone();
    }

    public Answer(Answer answer){
        this.statementId = answer.statementId;
        this.answer = answer.answer;
    }

    @Override
    public String toString() {
        return "Answer{" +
                "statementId=" + statementId +
                ", answer=" + new String(answer) +
                '}';
    }
}
