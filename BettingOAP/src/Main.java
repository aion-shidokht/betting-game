
import org.aion.harness.kernel.Address;
import util.*;

import java.math.BigInteger;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Jar requires config file location as an input.");
            System.exit(0);
        }
        // setup values from config
        Config config = Config.load(args[0]);
        String ip = config.getConfigValue("nodeIp");
        String port = config.getConfigValue("nodePort");
        Address contractAddress = new Address(Helper.hexStringToBytes(config.getConfigValue("contractAddress")));
        BigInteger startingBlockNumber = new BigInteger(config.getConfigValue("startingBlockNumber"));
        int capacity = Integer.parseInt(config.getConfigValue("queueCapacity"));
        long pollingIntervalMillis = Long.parseLong(config.getConfigValue("pollingIntervalMillis"));
        String serverHostName = config.getConfigValue("serverHostName");
        String serverPort = config.getConfigValue("serverPort");

        BettingGame bettingGame = new BettingGame(ip,
                port,
                contractAddress,
                capacity,
                startingBlockNumber,
                pollingIntervalMillis,
                serverHostName,
                serverPort);
        bettingGame.start();

    }
}
