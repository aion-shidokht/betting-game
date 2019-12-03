package types;

public class AggregatedPlayer {
    private final Address playerAddress;
    private final String transactionHash;
    private final int score;
    private final long age;

    public AggregatedPlayer(Player player, long blockNumber, boolean prizeDistributed) {
        this.playerAddress = player.getPlayerAddress();
        this.transactionHash = player.getTransactionHash();
        this.score = prizeDistributed ? player.getScore() : 0;
        this.age = blockNumber - player.getBlockNumber();
    }

    public Address getPlayerAddress() {
        return playerAddress;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public int getScore() {
        return score;
    }

    public long getAge() {
        return age;
    }
}
