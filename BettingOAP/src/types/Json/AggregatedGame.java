package types.Json;

import types.Game;

import java.math.BigInteger;
import java.util.ArrayList;

public class AggregatedGame {
    private boolean isStopped;
    private long stoppedAge;
    private String stoppedTransactionHash;

    private ArrayList<String> winners;
    private long winnerAge;
    private String winnersTransactionHash;

    private BigInteger prize;

    public AggregatedGame(Game game, ArrayList<String> winners, long blockNumber) {
        if (game.getStopped().key > 0) {
            int eventId = game.getStopped().key;
            this.isStopped = game.getStopped().value;
            this.stoppedAge = blockNumber - game.getBlockNumbers().get(eventId);
            this.stoppedTransactionHash = game.getTransactionHashes().get(eventId);
        }

        this.winners = winners;

        if (game.getPrizeDistributed().key > 0) {
            int eventId = game.getPrizeDistributed().key;
            this.winnerAge = blockNumber - game.getBlockNumbers().get(eventId);
            this.winnersTransactionHash = game.getTransactionHashes().get(eventId);
        }

        this.prize = game.getTotalPrizeAmount().divide(new BigInteger("1000000000000000000"));
    }

    public boolean isStopped() {
        return isStopped;
    }

    public long getStoppedAge() {
        return stoppedAge;
    }

    public String getStoppedTransactionHash() {
        return stoppedTransactionHash;
    }

    public ArrayList<String> getWinners() {
        return winners;
    }

    public long getWinnerAge() {
        return winnerAge;
    }

    public String getWinnersTransactionHash() {
        return winnersTransactionHash;
    }

    public BigInteger getPrize() {
        return prize;
    }
}
