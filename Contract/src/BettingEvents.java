import avm.Address;
import avm.Blockchain;
import org.aion.avm.userlib.AionUtilities;

import java.math.BigInteger;

public class BettingEvents {

    public static void registered(Address player) {
        Blockchain.log("Registered".getBytes(),
                player.toByteArray());
    }

    public static void voted(Address caller, int statementId, byte[] answer) {
        Blockchain.log("Voted".getBytes(),
                caller.toByteArray(),
                AionUtilities.padLeft(BigInteger.valueOf(statementId).toByteArray()),
                answer);
    }

    public static void submittedStatement(Address caller, int statementId, byte[] answerHash, byte[] statement) {
        Blockchain.log("SubmittedStatement".getBytes(),
                caller.toByteArray(),
                AionUtilities.padLeft(BigInteger.valueOf(statementId).toByteArray()),
                answerHash,
                statement);
    }

    public static void revealedAnswer(int statementId, byte[] answer) {
        Blockchain.log("RevealedAnswer".getBytes(),
                AionUtilities.padLeft(BigInteger.valueOf(statementId).toByteArray()),
                answer);
    }

    public static void distributedPrize(int winnerCount) {
        Blockchain.log("DistributedPrize".getBytes(), BigInteger.valueOf(winnerCount).toByteArray());
    }

    public static void updatedBalance(BigInteger balance) {
        Blockchain.log("UpdatedBalance".getBytes(),
                balance.toByteArray());
    }

    public static void gameStopped() {
        Blockchain.log("GameStopped".getBytes(), new byte[0]);
    }

    public static void deployed() {
        Blockchain.log("BettingContractDeployed".getBytes(), new byte[0]);
    }

}
