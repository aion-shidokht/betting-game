import org.aion.harness.kernel.Address;
import org.aion.harness.main.types.ReceiptHash;
import org.glassfish.grizzly.http.server.HttpServer;
import server.SimpleHttpServer;
import state.ProjectedState;
import state.StatePopulator;
import state.UserState;
import util.NodeConnection;
import util.Pair;
import util.QueuePopulator;
import worker.BlockNumberCollector;
import worker.EventListener;
import worker.ReceiptCollector;
import worker.TransactionSender;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BettingGame {

    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private volatile Lock shutdownLock = new ReentrantLock();

    private HttpServer server;
    private Thread eventListenerThread;
    private Thread transactionSenderThread;
    private Thread receiptCollectorThread;
    private Thread blockNumberCollectorThread;
    private EventListener eventListener;
    private BlockNumberCollector blockNumberCollector;
    private TransactionSender transactionSender;
    private ReceiptCollector receiptCollector;
    private UserState userState;
    private QueuePopulator queuePopulator;

    public BettingGame(String ip,
                       String port,
                       Address contractAddress,
                       int capacity,
                       long startingBlockNumber,
                       long pollingIntervalMillis,
                       String serverHostName,
                       String serverPort) {

        NodeConnection nodeConnection = new NodeConnection(ip, port);

        ProjectedState projectedState = new ProjectedState();
        StatePopulator statePopulator = new StatePopulator(projectedState);
        userState = new UserState(projectedState, nodeConnection);

        LinkedBlockingDeque<byte[]> rawTransactions = new LinkedBlockingDeque<>(capacity);
        LinkedBlockingDeque<Pair<ReceiptHash, Long>> transactionHashes = new LinkedBlockingDeque<>(capacity);

        queuePopulator = new QueuePopulator(rawTransactions);

        long range = 10;
        eventListener = new EventListener(
                nodeConnection,
                statePopulator,
                startingBlockNumber,
                pollingIntervalMillis,
                range,
                getContractTopics(),
                contractAddress);

        blockNumberCollector = new BlockNumberCollector(
                nodeConnection,
                pollingIntervalMillis,
                3);

        transactionSender = new TransactionSender(
                blockNumberCollector,
                rawTransactions,
                transactionHashes,
                nodeConnection,
                userState,
                pollingIntervalMillis,
                pollingIntervalMillis);

        int minDepth = 6;
        receiptCollector = new ReceiptCollector(
                blockNumberCollector,
                transactionHashes,
                nodeConnection,
                userState,
                minDepth,
                pollingIntervalMillis,
                3);

        eventListenerThread = new Thread(eventListener);
        eventListenerThread.setUncaughtExceptionHandler(new CriticalExceptionHandler());
        eventListenerThread.setName("EventListener");

        transactionSenderThread = new Thread(transactionSender);
        transactionSenderThread.setUncaughtExceptionHandler(new CriticalExceptionHandler());
        transactionSenderThread.setName("transactionSender");

        receiptCollectorThread = new Thread(receiptCollector);
        receiptCollectorThread.setUncaughtExceptionHandler(new CriticalExceptionHandler());
        receiptCollectorThread.setName("receiptCollector");

        blockNumberCollectorThread = new Thread(blockNumberCollector);
        blockNumberCollectorThread.setUncaughtExceptionHandler(new CriticalExceptionHandler());
        blockNumberCollectorThread.setName("blockNumberCollector");

        server = SimpleHttpServer.startServer(userState, queuePopulator, serverHostName, serverPort);

        Runtime.getRuntime().addShutdownHook(
                new Thread(this::shutdown)
        );

    }

    public void start() {
        try {
            server.start();
        } catch (IOException e) {
            shutdown();
        }
        eventListenerThread.start();
        transactionSenderThread.start();
        receiptCollectorThread.start();
        blockNumberCollectorThread.start();
    }

    private void shutdown() {
        shutdownLock.lock();
        try {
            if (isShuttingDown.get())
                return;
            isShuttingDown.getAndSet(true);
        } finally {
            shutdownLock.unlock();
        }

        server.shutdown();
        eventListener.shutdown();
        blockNumberCollector.shutdown();
        transactionSender.shutdown();
        receiptCollector.shutdown();

        try {
            eventListenerThread.join();
            transactionSenderThread.join();
            receiptCollectorThread.join();
            blockNumberCollectorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Set<byte[]> getContractTopics() {
        return Set.of(
                "BettingContractDeployed",
                "Registered",
                "Voted",
                "SubmittedStatement",
                "RevealedAnswer",
                "DistributedPrize",
                "UpdatedBalance",
                "GameStopped")
                .stream().map(String::getBytes).collect(Collectors.toSet());
    }

    private class CriticalExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            shutdown();
        }
    }
}
