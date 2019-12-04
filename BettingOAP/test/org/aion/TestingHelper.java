package org.aion;

import org.aion.harness.kernel.Address;
import util.Log;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TestingHelper {
    private static SecureRandom secureRandom = new SecureRandom();

    static Log getRegisteredLog(Address contractAddress,
                                long blockNumber,
                                Address player,
                                int transactionIndex,
                                byte[] blockHash) {
        return new Log(contractAddress,
                player.getAddressBytes(),
                Arrays.asList("Registered".getBytes()),
                blockNumber,
                transactionIndex,
                0,
                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
    }

    static Log getVotedLog(Address contractAddress,
                           long blockNumber,
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
                                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
    }

    static Log getSubmittedStatementLog(Address contractAddress,
                                        long blockNumber,
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
                                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
    }

    static Log getRevealedAnswerLog(Address contractAddress,
                                    long blockNumber,
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

    static Log getDistributedPrizeLog(Address contractAddress,
                                    long blockNumber,
                                    int winnerCount,
                                    int transactionIndex,
                                    byte[] blockHash) {
        return new Log(contractAddress,
                BigInteger.valueOf(winnerCount).toByteArray(),
                Arrays.asList("DistributedPrize".getBytes()),
                blockNumber,
                transactionIndex,
                0,
                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
    }

    static Log getOneTopicEvent(Address contractAddress,
                                long blockNumber,
                                String topic,
                                int transactionIndex,
                                byte[] blockHash) {
        return new Log(contractAddress,
                new byte[0],
                Arrays.asList(topic.getBytes()),
                blockNumber,
                transactionIndex,
                0,
                                blockHash == null ? getRandomAddressBytes() : blockHash,
                getRandomAddressBytes());
    }

    static Set<byte[]> getContractTopics(){
        Set<String> topics = new HashSet<>(Arrays.asList("BettingContractDeployed",
                "Registered",
                "Voted",
                "SubmittedStatement",
                "RevealedAnswer",
                "DistributedPrize",
                "UpdatedBalance",
                "GameStopped"));
        return topics.stream().map(String::getBytes).collect(Collectors.toSet());
    }
    static byte[] getRandomAddressBytes() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}
