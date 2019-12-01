package types;

import internal.Assertion;
import org.aion.harness.main.types.TransactionLog;
import org.aion.harness.main.types.TransactionReceipt;
import util.Helper;

import java.math.BigInteger;

public class TransactionDetails {

    private RESULT_TYPE result;
    private TRANSACTION_TYPE transactionType;
    private BigInteger blockNumber;
    private String transactionHash;

    public static TransactionDetails fromReceipt(TransactionReceipt receipt) {
        // only keep the fields required
        TransactionDetails transactionDetails = new TransactionDetails();
        transactionDetails.blockNumber = receipt.getBlockNumber();
        transactionDetails.result = receipt.transactionWasSuccessful() ? RESULT_TYPE.RECEIPT_SUCCESS : RESULT_TYPE.RECEIPT_FAILURE;
        // todo validate all successful transactions produce logs
        if (receipt.transactionWasSuccessful()) {
            // only direct Betting contract calls should be going through the application
            Assertion.assertTrue(receipt.getLogs().size() == 1);
            TransactionLog log = receipt.getLogs().get(0);
            String eventName = new String(log.copyOfTopics().get(0)).trim();
            transactionDetails.transactionType = TRANSACTION_TYPE.fromString(eventName);
            // todo null case, throw an exception?
        }
        transactionDetails.transactionHash = Helper.bytesToHexString(receipt.getTransactionHash());
        return transactionDetails;
    }

    public static TransactionDetails fromFailedTransaction(byte[] transactionHash) {
        TransactionDetails transactionDetails = new TransactionDetails();
        transactionDetails.result = RESULT_TYPE.NOT_SEALED;
        transactionDetails.transactionHash = Helper.bytesToHexString(transactionHash);
        return transactionDetails;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public TRANSACTION_TYPE getTransactionType() {
        return transactionType;
    }

    public RESULT_TYPE getResult() {
        return result;
    }

    public BigInteger getBlockNumber() {
        return blockNumber;
    }

    public enum RESULT_TYPE {
        RECEIPT_SUCCESS, RECEIPT_FAILURE, NOT_SEALED
    }

    public enum TRANSACTION_TYPE {
        Registered("Registered"),
        Voted("Voted"),
        SubmittedStatement("SubmittedStatement"),
        RevealedAnswer("RevealedAnswer"),
        DistributedPrize("DistributedPrize"),
        UpdatedBalance("UpdatedBalance"),
        GameStopped("GameStopped"),
        ///
        BettingContractDeployed("BettingContractDeployed");

        private String eventType;

        TRANSACTION_TYPE(String eventType) {
            this.eventType = eventType;
        }

        public static TRANSACTION_TYPE fromString(String eventType) {
            for (TRANSACTION_TYPE type : TRANSACTION_TYPE.values()) {
                if (type.eventType.equals(eventType)) {
                    return type;
                }
            }
            return null;
        }
    }
}
