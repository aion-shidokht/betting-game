package org.aion;

import org.aion.harness.result.RpcResult;
import org.apache.commons.codec.binary.Hex;
import org.glassfish.grizzly.http.server.HttpServer;
import org.mockito.stubbing.Answer;
import server.SimpleHttpServer;
import state.ProjectedState;
import state.UserState;
import types.Player;
import types.Statement;
import types.Vote;
import util.LogBuilder;
import util.NodeConnection;
import util.QueuePopulator;
import worker.BlockNumberCollector;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockServer {
    private static NodeConnection nodeConnection = mock(NodeConnection.class);
    private static UserState userState;
    private static ProjectedState projectedState;
    private static BlockNumberCollector blockNumberCollector;
    private static Thread blockNumberCollectorThread;

    private static long currentBlockNumber = 10;
    private static QueuePopulator queuePopulator;


    public static void main(String[] args) throws InterruptedException, IOException {
        when(nodeConnection.blockNumber()).thenAnswer(getNextBlock);

        long pollingIntervalMillis = 10000;
        projectedState = new ProjectedState();

        blockNumberCollector = new BlockNumberCollector(nodeConnection, pollingIntervalMillis, 3);
        userState = new UserState(projectedState, nodeConnection, blockNumberCollector);
        LinkedBlockingDeque<byte[]> rawTransactions = new LinkedBlockingDeque<>(100);
        queuePopulator = new QueuePopulator(rawTransactions);

        blockNumberCollectorThread = new Thread(blockNumberCollector);
        populateProjectedState();
        HttpServer server = startServer();
        blockNumberCollectorThread.start();
    }

    private static void populateProjectedState() {
        int playerSize = 40;
        int statementSize = 200;
        int voteSize = 500;

        for (int i = 0; i < playerSize; i++) {
            projectedState.addPlayer(Player.from(TestingHelper.getRandomAddressBytes(), TestingHelper.getRandomAddressBytes(), currentBlockNumber));
        }
        Player p = projectedState.getPlayers().get(1);
        for (int i = 0; i < statementSize; i++) {
            byte[] data = LogBuilder.parseData("0x52616e646f6d205175657374696f6e205375626d6974746564");
            List<byte[]> topics = LogBuilder.parseJsonTopics("[" +
                    "'0x5375626d697474656453746174656d656e740000000000000000000000000000', '" +
                    p.getPlayerAddress().getAddressString() + "'," +
                    "'0x" + Hex.encodeHexString(BigInteger.valueOf(i).toByteArray()) + "'," +
                    "'0x02fb6869f9a056cdb67c30a8fb5d3da42208074ca718cb5bf5684df4f2b1abd8']");
            projectedState.addStatement(Statement.from(topics, data, TestingHelper.getRandomAddressBytes(), currentBlockNumber));
        }

        for (int i = 0; i < voteSize; i++) {
            int statementId = i;
            if (i >= statementSize) {
                statementId = 1;
            }
            byte[] data = LogBuilder.parseData("0x52616e646f6d20616e73776572");
            List<byte[]> topics = LogBuilder.parseJsonTopics("[" +
                    "'0x566f746564000000000000000000000000000000000000000000000000000000', '" +
                    p.getPlayerAddress().getAddressString() + "'," +
                    "'0x" + Hex.encodeHexString(BigInteger.valueOf(statementId).toByteArray()) + "']");

            projectedState.addVote(Vote.from(topics, data, TestingHelper.getRandomAddressBytes(), currentBlockNumber));
        }

        projectedState.stopGame(TestingHelper.getRandomAddressBytes(), currentBlockNumber);

        for (int i = 0; i < statementSize / 2; i++) {
                byte[] data = LogBuilder.parseData("0x616e7332");
                List<byte[]> topics = LogBuilder.parseJsonTopics("[" +
                        "'0x52657665616c6564416e73776572000000000000000000000000000000000000',"+
                        "'0x" + Hex.encodeHexString(BigInteger.valueOf(i).toByteArray()) + "']");
            projectedState.addAnswer(types.Answer.from(topics, data, TestingHelper.getRandomAddressBytes(), currentBlockNumber));
        }

        projectedState.distributedPrize(TestingHelper.getRandomAddressBytes(), currentBlockNumber);
    }


    private static HttpServer startServer() throws IOException {
        HttpServer server = SimpleHttpServer.startServer(userState, queuePopulator, "localhost", "8025");
        server.start();
        return server;
    }

    static Answer getNextBlock = invocation -> {
        currentBlockNumber++;
        return RpcResult.successful(
                currentBlockNumber,
                System.currentTimeMillis(),
                TimeUnit.MILLISECONDS);
    };
}
