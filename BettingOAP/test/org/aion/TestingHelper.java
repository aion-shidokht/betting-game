package org.aion;

import types.Address;
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
                player.toBytes(),
                Arrays.asList("Registered".getBytes()),
                blockNumber,
                transactionIndex,
                0,
                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
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
                        player.toBytes(),
                        BigInteger.valueOf(statementId).toByteArray()),
                blockNumber,
                transactionIndex,
                0,
                                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
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
                        player.toBytes(),
                        BigInteger.valueOf(statementId).toByteArray(),
                        answerHash),
                blockNumber,
                transactionIndex,
                0,
                                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
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
                                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
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
                                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
    }

    static byte[] getRandomAddressBytes() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}
