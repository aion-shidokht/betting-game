package state;

import internal.Assertion;
import types.Answer;
import types.BlockTuple;
import types.Statement;
import types.Vote;
import types.Player;
import internal.CriticalException;
import util.Log;

import java.math.BigInteger;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

public class StatePopulator {
    private final ProjectedState projectedState;

    public StatePopulator(ProjectedState projectedState) {
        this.projectedState = projectedState;
    }

    public void populate(List<Log> logs) {
        Map<Long, List<Log>> logsPerBlock = new TreeMap<>(logs.stream().collect(groupingBy(l -> l.blockNumber)));
        for (Map.Entry<Long, List<Log>> e : logsPerBlock.entrySet()) {
            List<Integer> ids = new ArrayList<>();
            byte[] blockHash = e.getValue().get(0).blockHash;
            for (Log log : e.getValue()) {
                int addedLogId;
                byte[] data = log.copyOfData();
                List<byte[]> topics = log.copyOfTopics();
                Assertion.assertTrue(topics.size() > 0);
                String eventTopic = new String(topics.get(0)).trim();
                switch (eventTopic) {
                    case "Registered":
                        addedLogId = projectedState.addPlayer(Player.from(data, log.copyOfTransactionHash(), log.blockNumber));
                        break;
                    case "Voted":
                        addedLogId = projectedState.addVote(Vote.from(topics, data, log.copyOfTransactionHash(), log.blockNumber));
                        break;
                    case "SubmittedStatement":
                        addedLogId = projectedState.addStatement(Statement.from(topics, data, log.copyOfTransactionHash(), log.blockNumber));
                        break;
                    case "RevealedAnswer":
                        addedLogId = projectedState.addAnswer(Answer.from(topics, data, log.copyOfTransactionHash(), log.blockNumber));
                        break;
                    case "DistributedPrize":
                        addedLogId = projectedState.distributedPrize(data, log.copyOfTransactionHash(), log.blockNumber);
                        break;
                    case "UpdatedBalance":
                        addedLogId = projectedState.addTransferValue(new BigInteger(data), log.copyOfTransactionHash(), log.blockNumber);
                        break;
                    case "GameStopped":
                        addedLogId = projectedState.stopGame(log.copyOfTransactionHash(), log.blockNumber);
                        break;
                    case "BettingContractDeployed":
                        addedLogId = projectedState.deployedContract();
                        break;
                    default:
                        throw new CriticalException("First event topic not recognized. " + eventTopic);
                }
                ids.add(addedLogId);
            }
            projectedState.addBlockTuple(BlockTuple.of(e.getKey(), blockHash, ids));
        }

    }

    public void revertBlocks(int count) {
        projectedState.revertBlocks(count);
    }

    public void clear() {
        projectedState.clear();
    }

    public ListIterator<BlockTuple> getBlocksIterator() {
        List<BlockTuple> blocks = projectedState.getBlocks();
        return blocks.listIterator(blocks.size() - 1);
    }
}
