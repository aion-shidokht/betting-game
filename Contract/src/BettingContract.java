import avm.Address;
import avm.Blockchain;
import avm.Result;
import org.aion.avm.tooling.abi.Callable;
import org.aion.avm.tooling.abi.Fallback;
import org.aion.avm.userlib.AionList;
import org.aion.avm.userlib.AionMap;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Simple Guessing game contract.
 * Each player has to register first to play the game
 * Players can submit statements about themselves, and others can guess their identity
 * Players can only vote once for a statement, and the vote cannot be changed
 * Players cannot vote for their own statement
 */
public class BettingContract {

    private static Map<Address, Integer> playerScores = new AionMap<>();
    private static boolean isGameOnGoing;
    private static boolean isPrizePayedOut;

    private static Address owner;
    private static int nextStatementId;

    static {
        owner = Blockchain.getCaller();
        isGameOnGoing = true;
        isPrizePayedOut = false;
        BettingEvents.deployed();
    }

    @Callable
    public static void register(Address player) {
        // Registering can only be done by the owner for now to keep it simple. A multi sig approach can be used as well
        Blockchain.require(Blockchain.getCaller().equals(owner));
        Blockchain.require(!isPlayerRegistered(player));
        requireNoValue();

        playerScores.put(player, 0);

        BettingEvents.registered(player);
    }

    @Callable
    public static void vote(int statementId, byte[] guessedAnswer) {
        Address caller = Blockchain.getCaller();

        // caller must be in the player map
        Blockchain.require(isPlayerRegistered(caller));

        // game has not finished
        Blockchain.require(isGameOnGoing);

        // check statement exist and the caller is not the statement producer
        Address statementProducer = BettingStorage.getStatementProducer(statementId);
        Blockchain.require(statementProducer != null && !caller.equals(statementProducer));

        // has not voted
        Blockchain.require(!hasVoted(caller, statementId));
        Blockchain.require(guessedAnswer != null);

        requireNoValue();

        // add vote
        BettingStorage.putVote(caller, statementId, guessedAnswer);

        // log event
        BettingEvents.voted(caller, statementId, guessedAnswer);
    }

    @Callable
    public static void submitStatement(byte[] statement, byte[] answerHash) {
        Address caller = Blockchain.getCaller();

        // caller must be in the player map
        Blockchain.require(isPlayerRegistered(caller));

        // game has not finished
        Blockchain.require(isGameOnGoing);

        requireNoValue();

        Blockchain.require(statement != null);
        // validate answer hash
        Blockchain.require(answerHash != null && answerHash.length == 32);

        // we don't store submitted statements to reduce the cost, since these are just English sentences

        // add statement
        nextStatementId = nextStatementId + 1;
        BettingStorage.putStatement(nextStatementId, caller, answerHash);

        // log event
        BettingEvents.submittedStatement(caller, nextStatementId, answerHash, statement);
    }

    @Callable
    public static void revealAnswer(int statementId, byte[] answer, byte[] salt) {
        // game is finished
        Blockchain.require(!isGameOnGoing);

        byte[] storedHash = BettingStorage.getStatementHashedAnswerAndClear(statementId);
        // validate statement exist
        Blockchain.require(storedHash != null);

        requireNoValue();

        // answer has correct info
        byte[] answerHash = Blockchain.blake2b(BettingStorage.concatArrays(answer, salt));
        Blockchain.require(Arrays.equals(answerHash, storedHash));

        // Increase all scores
        // This is done for each question since doing it once for all players and questions could cost more than the energy limit.
        adjustScores(statementId, answer);

        BettingEvents.revealedAnswer(statementId, answer);
    }

    @Callable
    public static void payout() {
        // game is finished
        Blockchain.require(!isGameOnGoing);
        Blockchain.require(!isPrizePayedOut);

        // only owner can perform the payout. This is not blocked by the revealed answers
        Blockchain.require(Blockchain.getCaller().equals(owner));

        requireNoValue();

        final List<Address> winnerList = new AionList<>();
        int currentMaxScore = -1;
        for (Map.Entry<Address, Integer> entry : playerScores.entrySet()) {
            int score = entry.getValue();
            if (score > currentMaxScore) {
                winnerList.clear();
                winnerList.add(entry.getKey());
                currentMaxScore = score;
            } else if (score == currentMaxScore) {
                winnerList.add(entry.getKey());
            }
        }

        BigInteger prize = Blockchain.getBalanceOfThisContract().divide(BigInteger.valueOf(winnerList.size()));
        for (Address winner : winnerList) {
            Result result = Blockchain.call(winner, prize, new byte[0], Blockchain.getRemainingEnergy());
            Blockchain.require(result.isSuccess());
        }

        isPrizePayedOut = true;
        BettingEvents.distributedPrize();
    }

    @Callable
    public static void stopGame() {
        // Only the owner can stop a game round
        Blockchain.require(Blockchain.getCaller().equals(owner));
        Blockchain.require(isGameOnGoing);

        requireNoValue();

        isGameOnGoing = false;
        BettingEvents.gameStopped();
    }

    @Callable
    public static void sefDestruct() {
        Blockchain.require(Blockchain.getCaller().equals(owner));
        Blockchain.selfDestruct(owner);
    }

    @Callable
    public static int getScore(Address playerAddress) {
        return playerScores.get(playerAddress);
    }

    @Fallback
    public static void fallback() {
        BettingEvents.updatedBalance(Blockchain.getBalanceOfThisContract());
    }

    private static boolean isPlayerRegistered(Address address) {
        return playerScores.containsKey(address);
    }

    private static boolean hasVoted(Address player, Integer statementId) {
        return BettingStorage.getVote(player, statementId) != null;
    }

    private static boolean hasProvidedAnswer(Address player, Integer statementId, byte[] answer) {
        byte[] vote = BettingStorage.getVote(player, statementId);
        return vote != null && Arrays.equals(answer, vote);
    }

    private static void adjustScores(Integer statementId, byte[] answer) {
        for (Address player : playerScores.keySet()) {
            if (hasProvidedAnswer(player, statementId, answer)) {
                playerScores.put(player, playerScores.get(player) + 1);
            }
        }
    }

    // todo value should not be transferred using normal calls?
    private static void requireNoValue() {
        Blockchain.require(Blockchain.getValue().signum() == 0);
    }
}

// todo choose the best answer type
// todo fallback -> event?
// todo add winners to the event, max number of winner?
// todo potentially store the player answers in a bit vector, ask players to ask for their score to be calculated once answers have been revealed

