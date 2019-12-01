package org.aion;

import org.junit.Assert;
import org.junit.Test;
import types.Answer;
import types.Player;
import types.Statement;
import types.Vote;
import util.Helper;
import util.LogBuilder;

import java.math.BigInteger;
import java.util.List;

public class EventParsingTest {

    private byte[] transactionHash = TestingHelper.getRandomAddressBytes();

    @Test
    public void testRegister() {
        byte[] data = LogBuilder.parseData("0xa0feea479a3447b9c92775e6ac478e027ec1cc586424663715c73441dc6d74b1");
        List<byte[]> topics = LogBuilder.parseJsonTopics("['0x5265676973746572656400000000000000000000000000000000000000000000']");

        String eventTopic = new String(topics.get(0)).trim();
        Assert.assertEquals("Registered", eventTopic);
        Player p = Player.from(data, transactionHash);
        Assert.assertEquals("0xa0feea479a3447b9c92775e6ac478e027ec1cc586424663715c73441dc6d74b1", p.getPlayerAddress().getAddressString());
        Assert.assertEquals(0, p.getScore());
        Assert.assertEquals(Helper.bytesToHexStringWith0x(transactionHash), p.getTransactionHash());
    }

    @Test
    public void testSubmitStatement() {
        byte[] data = LogBuilder.parseData("0x52616e646f6d205175657374696f6e205375626d6974746564");
        List<byte[]> topics = LogBuilder.parseJsonTopics("[" +
                "'0x5375626d697474656453746174656d656e740000000000000000000000000000'," +
                "'0xa0127f71dd45a9a0bb0775cfd6d242c75a57e6e598d31ee4555d36a9d97734f0'," +
                "'0x000000000000000000000000000000000000000000000000000000000000000a'," +
                "'0x02fb6869f9a056cdb67c30a8fb5d3da42208074ca718cb5bf5684df4f2b1abd8']");
        Statement s = Statement.from(topics,
                data,
                transactionHash);
        String eventTopic = new String(topics.get(0)).trim();
        Assert.assertEquals("SubmittedStatement", eventTopic);

        Assert.assertEquals("0xa0127f71dd45a9a0bb0775cfd6d242c75a57e6e598d31ee4555d36a9d97734f0", s.getPlayerAddress().getAddressString());
        Assert.assertEquals(10, s.getStatementId());
        Assert.assertEquals("0x02fb6869f9a056cdb67c30a8fb5d3da42208074ca718cb5bf5684df4f2b1abd8", s.getAnswerHash());
        Assert.assertEquals("Random Question Submitted", s.getStatementString());
    }

    @Test
    public void testVote() {
        byte[] data = LogBuilder.parseData("0x52616e646f6d20616e73776572");
        List<byte[]> topics = LogBuilder.parseJsonTopics("[" +
                "'0x566f746564000000000000000000000000000000000000000000000000000000'," +
                "'0xa03c81c97b0dfaa35b8d74f09690b23c61b6330eee2d1325e191426bea0d04a5'," +
                "'0x000000000000000000000000000000000000000000000000000000000000000b']");
        Vote v = Vote.from(topics,
                data,
                transactionHash);
        String eventTopic = new String(topics.get(0)).trim();
        Assert.assertEquals("Voted", eventTopic);

        Assert.assertEquals("0xa03c81c97b0dfaa35b8d74f09690b23c61b6330eee2d1325e191426bea0d04a5", v.getPlayerAddress().getAddressString());
        Assert.assertEquals(11, v.getStatementId());
        Assert.assertEquals("Random answer", v.getGuessedAnswer());
    }

    @Test
    public void testStopGame() {
         byte[] data = LogBuilder.parseData("0x");
        List<byte[]> topics = LogBuilder.parseJsonTopics(
                "['0x47616d6553746f70706564000000000000000000000000000000000000000000']");
        String eventTopic = new String(topics.get(0)).trim();
        Assert.assertEquals("GameStopped", eventTopic);
    }

    @Test
    public void testTransfer() {
         byte[] data = LogBuilder.parseData("0x0186a0");
        List<byte[]> topics = LogBuilder.parseJsonTopics(
                "['0x5570646174656442616c616e6365000000000000000000000000000000000000']");
        String eventTopic = new String(topics.get(0)).trim();
        Assert.assertEquals("UpdatedBalance", eventTopic);
        Assert.assertEquals(BigInteger.valueOf(100000), new BigInteger(data));
    }

    @Test
    public void testRevealAnswer() {
         byte[] data = LogBuilder.parseData("0x616e7332");
        List<byte[]> topics = LogBuilder.parseJsonTopics("[" +
                "'0x52657665616c6564416e73776572000000000000000000000000000000000000',"+
                "'0x0000000000000000000000000000000000000000000000000000000000000007']");
        String eventTopic = new String(topics.get(0)).trim();
        Assert.assertEquals("RevealedAnswer", eventTopic);
        Answer a = Answer.from(topics,
                data, transactionHash);
        Assert.assertEquals("ans2", a.getAnswer());
        Assert.assertEquals(7, a.getStatementId());
    }

    @Test
    public void payout() {
         byte[] data = LogBuilder.parseData("0x");
        List<byte[]> topics = LogBuilder.parseJsonTopics(
                "['0x44697374726962757465645072697a6500000000000000000000000000000000']");
        String eventTopic = new String(topics.get(0)).trim();
        Assert.assertEquals("DistributedPrize", eventTopic);
    }
}
