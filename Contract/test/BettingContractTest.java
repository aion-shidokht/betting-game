import avm.Address;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.embed.hash.HashUtils;
import org.aion.avm.userlib.abi.ABIStreamingEncoder;
import org.aion.types.AionAddress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;

public class BettingContractTest {

    @Rule
    public AvmRule avmRule = new AvmRule(false);

    // default address with balance
    private Address preminedAddress = avmRule.getPreminedAccount();
    private BigInteger ENOUGH_BALANCE_TO_TRANSACT = BigInteger.TEN.pow(19);

    // contract address
    private Address bettingContract;
    private Class[] otherClasses = {BettingEvents.class, BettingStorage.class};

    @Before
    public void setup() {
        byte[] dappBytes = avmRule.getDappBytes(BettingContract.class, null, 1, otherClasses);
        AvmRule.ResultWrapper result = avmRule.deploy(preminedAddress, BigInteger.ZERO, dappBytes);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        bettingContract = result.getDappAddress();
    }

    @Test
    public void testVote() {
        Address user1 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
        Address user2 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
        Address user3 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);

        register(user1);
        register(user2);
        register(user3);

        byte[] answer = "a".getBytes();
        byte[] salt = "1".getBytes();
        submitStatement(user1, "who did that".getBytes(), answer, salt);

        vote(user2, 1, answer);
        vote(user3, 1, "b".getBytes());

        callMethod(preminedAddress, "stopGame");

        AvmRule.ResultWrapper result = revealAnswer(user1, 1, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        transferValueToContract(ENOUGH_BALANCE_TO_TRANSACT);
        BigInteger user2Balance = avmRule.kernel.getBalance(new AionAddress(user2.toByteArray()));
        BigInteger user3Balance = avmRule.kernel.getBalance(new AionAddress(user3.toByteArray()));

        callMethod(preminedAddress, "payout");

        Assert.assertEquals(user2Balance.add(ENOUGH_BALANCE_TO_TRANSACT), avmRule.kernel.getBalance(new AionAddress(user2.toByteArray())));
        Assert.assertEquals(user3Balance, avmRule.kernel.getBalance(new AionAddress(user3.toByteArray())));
    }

    @Test
    public void testVoteTieSameStatement() {
        Address user1 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
        Address user2 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
        Address user3 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);

        register(user1);
        register(user2);
        register(user3);

        byte[] answer = "a".getBytes();
        byte[] salt = "1".getBytes();
        submitStatement(user1, "who did that".getBytes(), answer, salt);

        vote(user2, 1, answer);
        vote(user3, 1, answer);

        callMethod(preminedAddress, "stopGame");
        AvmRule.ResultWrapper result = revealAnswer(user1, 1, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        transferValueToContract(ENOUGH_BALANCE_TO_TRANSACT);
        BigInteger user2Balance = avmRule.kernel.getBalance(new AionAddress(user2.toByteArray()));
        BigInteger user3Balance = avmRule.kernel.getBalance(new AionAddress(user3.toByteArray()));

        callMethod(preminedAddress, "payout");

        Assert.assertEquals(user2Balance.add(ENOUGH_BALANCE_TO_TRANSACT.divide(BigInteger.TWO)), avmRule.kernel.getBalance(new AionAddress(user2.toByteArray())));
        Assert.assertEquals(user3Balance.add(ENOUGH_BALANCE_TO_TRANSACT.divide(BigInteger.TWO)), avmRule.kernel.getBalance(new AionAddress(user3.toByteArray())));
    }

    @Test
    public void testVoteTieDifferentStatements() {
        Address user1 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
        Address user2 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
        Address user3 = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);

        register(user1);
        register(user2);
        register(user3);

        byte[] answer = "a".getBytes();
        byte[] salt = "1".getBytes();
        submitStatement(user1, "who did that".getBytes(), answer, salt);
        submitStatement(user1, "who said that".getBytes(), answer, salt);

        vote(user2, 1, answer);
        vote(user3, 2, answer);

        callMethod(preminedAddress, "stopGame");
        AvmRule.ResultWrapper result = revealAnswer(user1, 1, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        result = revealAnswer(user1, 2, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        transferValueToContract(ENOUGH_BALANCE_TO_TRANSACT);
        BigInteger user2Balance = avmRule.kernel.getBalance(new AionAddress(user2.toByteArray()));
        BigInteger user3Balance = avmRule.kernel.getBalance(new AionAddress(user3.toByteArray()));

        callMethod(preminedAddress, "payout");

        Assert.assertEquals(user2Balance.add(ENOUGH_BALANCE_TO_TRANSACT.divide(BigInteger.TWO)), avmRule.kernel.getBalance(new AionAddress(user2.toByteArray())));
        Assert.assertEquals(user3Balance.add(ENOUGH_BALANCE_TO_TRANSACT.divide(BigInteger.TWO)), avmRule.kernel.getBalance(new AionAddress(user3.toByteArray())));
    }

    @Test
    public void testRevealLimits() {
        Address[] users = new Address[215];
        for (int i = 0; i < users.length; i++) {
            users[i] = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
            register(users[i]);
        }

        byte[] answer = "a".getBytes();
        byte[] salt = "1".getBytes();
        submitStatement(users[0], "who did that".getBytes(), answer, salt);
        submitStatement(users[1], "who did that".getBytes(), answer, salt);

        for (int i = 1; i < users.length; i++) {
            vote(users[i], 1, answer);
        }
        vote(users[users.length -1], 2, answer);

        callMethod(preminedAddress, "stopGame");
        // reveal costs 1.9+ million
        AvmRule.ResultWrapper result = revealAnswer(users[0], 1, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        result = revealAnswer(users[1], 2, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        transferValueToContract(ENOUGH_BALANCE_TO_TRANSACT);

        BigInteger balance = avmRule.kernel.getBalance(new AionAddress(users[users.length -1].toByteArray()));
        callMethod(preminedAddress, "payout");
        BigInteger newBalance = avmRule.kernel.getBalance(new AionAddress(users[users.length -1].toByteArray()));
        Assert.assertEquals(balance.add(ENOUGH_BALANCE_TO_TRANSACT), newBalance);
    }

    @Test
    public void testAllPlayersSameAnswer() {
        Address[] users = new Address[112];
        for (int i = 0; i < users.length; i++) {
            users[i] = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
            register(users[i]);
        }

        byte[] answer = "a".getBytes();
        byte[] salt = "1".getBytes();
        submitStatement(users[0], "who did that".getBytes(), answer, salt);
        for (int i = 1; i < users.length; i++) {
            vote(users[i], 1, answer);
        }

        callMethod(preminedAddress, "stopGame");
        // reveal costs 1.9+ million
        AvmRule.ResultWrapper result = revealAnswer(users[0], 1, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        transferValueToContract(ENOUGH_BALANCE_TO_TRANSACT);

        BigInteger balance = avmRule.kernel.getBalance(new AionAddress(users[users.length -1].toByteArray()));
        callMethod(preminedAddress, "payout");
        BigInteger newBalance = avmRule.kernel.getBalance(new AionAddress(users[users.length -1].toByteArray()));
        Assert.assertEquals(balance.add(ENOUGH_BALANCE_TO_TRANSACT.divide(BigInteger.valueOf(111))), newBalance);
    }

    @Test
    public void testRevealAnswer() {
        Address[] users = new Address[3];
        for (int i = 0; i < users.length; i++) {
            users[i] = avmRule.getRandomAddress(ENOUGH_BALANCE_TO_TRANSACT);
            register(users[i]);
        }

        byte[] answer = "a".getBytes();
        byte[] salt = "1".getBytes();
        submitStatement(users[0], "who did that".getBytes(), answer, salt);
        vote(users[1], 1, answer);

        callMethod(preminedAddress, "stopGame");

        AvmRule.ResultWrapper result = revealAnswer(users[0], 1, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());

        Assert.assertEquals(1, getScore(users[1]));
        result = revealAnswer(users[0], 1, answer, salt);
        Assert.assertTrue(result.getReceiptStatus().isFailed());
        Assert.assertEquals(1, getScore(users[1]));
    }

    private void callMethod(Address user, String methodName) {
        byte[] txData = new ABIStreamingEncoder()
                .encodeOneString(methodName)
                .toBytes();

        AvmRule.ResultWrapper result = avmRule.call(user, bettingContract, BigInteger.ZERO, txData, 2_000_000L, 1L);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
    }

    private void register(Address user) {
        byte[] txData = new ABIStreamingEncoder()
                .encodeOneString("register")
                .encodeOneAddress(user)
                .toBytes();

        AvmRule.ResultWrapper result = avmRule.call(preminedAddress, bettingContract, BigInteger.ZERO, txData, 2_000_000L, 1L);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
    }

    private void vote(Address user, int statementId, byte[] answer) {
        byte[] txData = new ABIStreamingEncoder()
                .encodeOneString("vote")
                .encodeOneInteger(statementId)
                .encodeOneByteArray(answer)
                .toBytes();

        AvmRule.ResultWrapper result = avmRule.call(user, bettingContract, BigInteger.ZERO, txData, 2_000_000L, 1L);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
    }

    private void submitStatement(Address user, byte[] statement, byte[] answer, byte[] salt) {
        byte[] concatenatedArray = new byte[answer.length + salt.length];
        System.arraycopy(answer, 0, concatenatedArray, 0, answer.length);
        System.arraycopy(salt, 0, concatenatedArray, answer.length, salt.length);

        byte[] answerHash = HashUtils.blake2b(concatenatedArray);

        byte[] txData = new ABIStreamingEncoder()
                .encodeOneString("submitStatement")
                .encodeOneByteArray(statement)
                .encodeOneByteArray(answerHash)
                .toBytes();

        AvmRule.ResultWrapper result = avmRule.call(user, bettingContract, BigInteger.ZERO, txData, 2_000_000L, 1L);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
    }

    private AvmRule.ResultWrapper revealAnswer(Address user, int statementId, byte[] answer, byte[] salt) {
        byte[] txData = new ABIStreamingEncoder()
                .encodeOneString("revealAnswer")
                .encodeOneInteger(statementId)
                .encodeOneByteArray(answer)
                .encodeOneByteArray(salt)
                .toBytes();

        return avmRule.call(user, bettingContract, BigInteger.ZERO, txData, 2_000_000L, 1L);
    }

    private int getScore(Address user) {
        byte[] txData = new ABIStreamingEncoder()
                .encodeOneString("getScore")
                .encodeOneAddress(user)
                .toBytes();

        AvmRule.ResultWrapper result = avmRule.call(user, bettingContract, BigInteger.ZERO, txData, 2_000_000L, 1L);
        return (int) result.getDecodedReturnData();
    }

    private void transferValueToContract(BigInteger amount) {
        AvmRule.ResultWrapper result = avmRule.balanceTransfer(preminedAddress, bettingContract, amount, 2_000_000L, 1L);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
    }
}
