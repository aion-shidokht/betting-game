import avm.Address;
import avm.Blockchain;
import org.aion.avm.userlib.AionBuffer;

import java.util.Arrays;

public class BettingStorage {

    public static void putVote(Address user, Integer statementId, byte[] guessedAnswer) {
        byte[] key = getVoteKey(user, statementId);
        Blockchain.putStorage(key, guessedAnswer);
    }

    public static byte[] getVote(Address user, Integer statementId) {
        byte[] key = getVoteKey(user, statementId);
        return Blockchain.getStorage(key);
    }

    // statement is not stored since it is not necessary for the contract functionality. This reduces the cost as well.
    public static void putStatement(int statementId, Address caller, byte[] hashedAnswer) {
        byte[] key = getStatementKey(statementId);
        byte[] value = concatArrays(caller.toByteArray(), hashedAnswer);
        Blockchain.putStorage(key, value);
    }

    public static byte[] getStatementHashedAnswerAndClear(int statementId) {
        byte[] key = getStatementKey(statementId);
        // storage : Address caller, answer hash
        byte[] value = Blockchain.getStorage(key);
        Blockchain.putStorage(key, null);
        return value == null ? null : Arrays.copyOfRange(value, Address.LENGTH, value.length);
    }

    public static Address getStatementProducer(int statementId) {
        byte[] key = getStatementKey(statementId);
        // storage : Address caller, answer hash
        byte[] value = Blockchain.getStorage(key);
        return value == null ? null : new Address(Arrays.copyOfRange(value, 0, Address.LENGTH));
    }

    private static byte[] getVoteKey(Address user, Integer statementId) {
        int outputSize = Integer.BYTES + Address.LENGTH + Integer.BYTES;
        AionBuffer buffer = AionBuffer.allocate(outputSize);
        buffer.putInt(StorageSlots.VOTE.hashCode());
        buffer.putAddress(user);
        buffer.putInt(statementId);
        return Blockchain.blake2b(buffer.getArray());
    }

    private static byte[] getStatementKey(int statementId) {
        int outputSize = Integer.BYTES + Integer.BYTES;
        AionBuffer buffer = AionBuffer.allocate(outputSize);
        buffer.putInt(StorageSlots.STATEMENT.hashCode());
        buffer.putInt(statementId);
        return Blockchain.blake2b(buffer.getArray());
    }

    public static byte[] concatArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    private enum StorageSlots {
        VOTE, STATEMENT
    }
}
