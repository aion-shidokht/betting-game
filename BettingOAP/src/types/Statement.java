package types;

import internal.Assertion;
import org.aion.harness.kernel.Address;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.util.List;

public class Statement {
    private final Address player;
    private final int statementId;
    private final byte[] answerHash;
    private final String statement;
    private final byte[] transactionHash;

    private Statement(Address player, int statementId, byte[] answerHash, String statement, byte[] transactionHash) {
        this.player = player;
        this.statementId = statementId;
        this.answerHash = answerHash;
        this.statement = statement;
        this.transactionHash = transactionHash;
    }

    public static Statement from(List<byte[]> topics, byte[] data, byte[] transactionHash) {
        Assertion.assertTopicSize(topics, 4);
        return new Statement(new Address(topics.get(1)),
                new BigInteger(Hex.encodeHexString(topics.get(2)), 16).intValue(),
                topics.get(3),
                new String(data),
                transactionHash);
    }

    public Address getPlayer() {
        return player;
    }

    public int getStatementId() {
        return statementId;
    }

    public byte[] getAnswerHash() {
        return answerHash.clone();
    }

    public String getStatementString() {
        return statement;
    }

    public byte[] getTransactionHash() {
        return transactionHash.clone();
    }

    public Statement(Statement statement){
        this.player = statement.player;
        this.statementId = statement.statementId;
        this.answerHash = statement.answerHash;
        this.statement = statement.statement;
        this.transactionHash = statement.transactionHash;
    }

    @Override
    public String toString() {
        return "Statement{" +
                "player=" + player +
                ", statementId=" + statementId +
                ", answerHash=" + Hex.encodeHexString(answerHash) +
                ", statement='" + statement + '\'' +
                ", transactionHash=" + Hex.encodeHexString(transactionHash) +
                '}';
    }
}
