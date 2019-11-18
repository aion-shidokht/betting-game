package org.aion;

import org.aion.harness.kernel.Address;
import org.aion.util.conversions.Hex;
import util.Log;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

public class TestingHelper {
    private static SecureRandom secureRandom = new SecureRandom();

    static Log getRegisteredLog(Address contractAddress,
                                BigInteger blockNumber,
                                Address player,
                                int transactionIndex,
                                byte[] blockHash) {
        return new Log(contractAddress,
                player.getAddressBytes(),
                Arrays.asList("Registered".getBytes()),
                blockNumber,
                transactionIndex,
                0,
                blockHash == null ? getRandomAddressBytes() : blockHash);
    }

    static Log getVotedLog(Address contractAddress,
                           BigInteger blockNumber,
                           Address player,
                           int statementId,
                           byte[] answer,
                           int transactionIndex,
                           byte[] blockHash) {
        return new Log(contractAddress,
                answer,
                Arrays.asList("Voted".getBytes(),
                        player.getAddressBytes(),
                        BigInteger.valueOf(statementId).toByteArray()),
                blockNumber,
                transactionIndex,
                0,
                blockHash == null ? getRandomAddressBytes() : blockHash);
    }

    static Log getSubmittedStatementLog(Address contractAddress,
                                        BigInteger blockNumber,
                                        Address player,
                                        int statementId,
                                        byte[] statement,
                                        byte[] answerHash,
                                        int transactionIndex,
                                        byte[] blockHash) {
        return new Log(contractAddress,
                statement,
                Arrays.asList("SubmittedStatement".getBytes(),
                        player.getAddressBytes(),
                        BigInteger.valueOf(statementId).toByteArray(),
                        answerHash),
                blockNumber,
                transactionIndex,
                0,
                blockHash == null ? getRandomAddressBytes() : blockHash);
    }

    static Log getRevealedAnswerLog(Address contractAddress,
                                    BigInteger blockNumber,
                                    int statementId,
                                    byte[] answer,
                                    int transactionIndex,
                                    byte[] blockHash) {
        return new Log(contractAddress,
                answer,
                Arrays.asList("RevealedAnswer".getBytes(),
                        BigInteger.valueOf(statementId).toByteArray()),
                blockNumber,
                transactionIndex,
                0,
                blockHash == null ? getRandomAddressBytes() : blockHash);
    }

    static Log getNoTopicEvent(Address contractAddress,
                               BigInteger blockNumber,
                               String data,
                               int transactionIndex,
                               byte[] blockHash) {
        return new Log(contractAddress,
                data.getBytes(),
                new ArrayList<>(),
                blockNumber,
                transactionIndex,
                0,
                blockHash == null ? getRandomAddressBytes() : blockHash);
    }

    static byte[] getRandomAddressBytes() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}
