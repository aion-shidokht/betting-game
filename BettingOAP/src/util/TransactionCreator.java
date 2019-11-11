package util;

import org.aion.avm.embed.hash.HashUtils;
import org.aion.avm.userlib.abi.ABIStreamingEncoder;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.SignedTransaction;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

/**
 * Utility to create signed contract transactions
 * Mainly used for testing purposes
 */
public class TransactionCreator {
    public static SignedTransaction buildRawTransaction(PrivateKey sender,
                                                        BigInteger nonce,
                                                        Address destination,
                                                        byte[] data,
                                                        BigInteger value) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        return SignedTransaction.newGeneralTransaction(
                sender,
                nonce,
                destination,
                data,
                2_000_000,
                10_000_000_000L,
                value,
                null);
    }

    public static byte[] getMethodWithNoArgumentTxData(String methodName) {
        return new ABIStreamingEncoder()
                .encodeOneString(methodName)
                .toBytes();
    }

    public static byte[] getRegisterTxData(avm.Address user) {
        return new ABIStreamingEncoder()
                .encodeOneString("register")
                .encodeOneAddress(user)
                .toBytes();
    }

    public static byte[] getVoteTxData(int statementId, byte[] answer) {
        return new ABIStreamingEncoder()
                .encodeOneString("vote")
                .encodeOneInteger(statementId)
                .encodeOneByteArray(answer)
                .toBytes();
    }

    public static byte[] getSubmitStatementTxData(byte[] statement, byte[] answer, byte[] salt) {
        byte[] concatenatedArray = new byte[answer.length + salt.length];
        System.arraycopy(answer, 0, concatenatedArray, 0, answer.length);
        System.arraycopy(salt, 0, concatenatedArray, answer.length, salt.length);

        byte[] answerHash = HashUtils.blake2b(concatenatedArray);

        return new ABIStreamingEncoder()
                .encodeOneString("submitStatement")
                .encodeOneByteArray(statement)
                .encodeOneByteArray(answerHash)
                .toBytes();
    }

    public static byte[] getRevealAnswerTxData(int statementId, byte[] answer, byte[] salt) {
        return new ABIStreamingEncoder()
                .encodeOneString("revealAnswer")
                .encodeOneInteger(statementId)
                .encodeOneByteArray(answer)
                .encodeOneByteArray(salt)
                .toBytes();
    }
}
