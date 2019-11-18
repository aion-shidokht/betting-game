package state;

import types.Answer;
import types.Statement;
import types.Vote;
import internal.CriticalException;
import org.aion.harness.kernel.Address;
import util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;

public class StatePopulator {
    private final ProjectedState projectedState;

    public StatePopulator(ProjectedState projectedState) {
        this.projectedState = projectedState;
    }

    public void populate(List<Log> logs) {
        Map<BigInteger, List<Log>> logsPerBlock = new TreeMap<>(logs.stream().collect(groupingBy(l -> l.blockNumber)));
        for (Map.Entry<BigInteger, List<Log>> e : logsPerBlock.entrySet()) {
            List<Integer> ids = new ArrayList<>();
            byte[] blockHash = e.getValue().get(0).blockHash;
            for (Log log : e.getValue()) {
                int addedLogId;
                byte[] data = log.copyOfData();
                List<byte[]> topics = log.copyOfTopics();
                if (topics.size() > 0) {
                    String eventTopic = new String(topics.get(0)).trim();
                    switch (eventTopic) {
                        case "Registered":
                            addedLogId = projectedState.addPlayer(new Address(data));
                            break;
                        case "Voted":
                            addedLogId = projectedState.addVote(Vote.from(topics, data));
                            break;
                        case "SubmittedStatement":
                            addedLogId = projectedState.addStatement(Statement.from(topics, data));
                            break;
                        case "RevealedAnswer":
                            addedLogId = projectedState.addAnswer(Answer.from(topics, data));
                            break;
                        default:
                            throw new CriticalException("First event topic not recognized. " + eventTopic);
                    }

                } else {
                    String eventData = new String(data).trim();
                    switch (eventData) {
                        // todo update after the contract finalization
                        case "DistributedPrize":
                            addedLogId = projectedState.distributedPrize();
                            break;
                        case "UpdatedBalance":
                            addedLogId = projectedState.addTransferValue(new BigInteger(eventData));
                            break;
                        case "GameStopped":
                            addedLogId = projectedState.stopGame();
                            break;
                        case "BettingContractDeployed":
                            addedLogId = projectedState.deployedContract();
                            break;
                        default:
                            throw new CriticalException("Event data not recognized. " + eventData);
                    }
                }
                ids.add(addedLogId);
            }
            projectedState.addBlockTuple(e.getKey(), blockHash, ids);
        }

    }
}
