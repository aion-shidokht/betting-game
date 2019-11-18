package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.aion.harness.kernel.Address;
import org.aion.harness.main.tools.JsonStringParser;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// todo replace with a modified version in node_test_harness
public class LogBuilder {
    private Address address = null;
    private byte[] data = null;
    private List<byte[]> topics = null;
    private BigInteger blockNumber = null;
    private byte[] blockHash = null;
    private Integer transactionIndex = null;
    private Integer logIndex = null;

    public LogBuilder address(Address address) {
        this.address = address;
        return this;
    }

    public LogBuilder data(byte[] data) {
        this.data = data;
        return this;
    }

    public LogBuilder topics(List<byte[]> topics) {
        this.topics = topics;
        return this;
    }

    public LogBuilder blockNumber(BigInteger blockNumber) {
        this.blockNumber = blockNumber;
        return this;
    }

    public LogBuilder transactionIndex(int transactionIndex) {
        this.transactionIndex = transactionIndex;
        return this;
    }

    public LogBuilder logIndex(int logIndex) {
        this.logIndex = logIndex;
        return this;
    }

    public LogBuilder blockHash(byte[] blockHash) {
        this.blockHash = blockHash;
        return this;
    }

    public Log build() {
        if (this.address == null) {
            throw new NullPointerException("Cannot build Log with null address!");
        }
        if (this.data == null) {
            throw new NullPointerException("Cannot build Log with null data!");
        }
        if (this.blockNumber == null) {
            throw new NullPointerException("Cannot build Log without specifying blockNumber!");
        }
        if (this.transactionIndex == null) {
            throw new NullPointerException("Cannot build Log without specifying transactionIndex!");
        }
        if (this.logIndex == null) {
            throw new NullPointerException("Cannot build Log without specifying logIndex!");
        }

        List<byte[]> topics = (this.topics == null) ? Collections.emptyList() : this.topics;

        return new Log(this.address, this.data, topics, this.blockNumber, this.transactionIndex, this.logIndex, this.blockHash);
    }

    public Log buildFromJsonString(String jsonString) throws DecoderException {
        JsonStringParser jsonParser = new JsonStringParser(jsonString);

        String address = jsonParser.attributeToString("address");
        String data = jsonParser.attributeToString("data");
        String topics = jsonParser.attributeToString("topics");
        String blockNumber = jsonParser.attributeToString("blockNumber");
        String blockHash = jsonParser.attributeToString("blockHash");
        String transactionIndex = jsonParser.attributeToString("transactionIndex");
        String logIndex = jsonParser.attributeToString("logIndex");

        return new LogBuilder()
                .address(new Address(Hex.decodeHex(address)))
                .data(Hex.decodeHex(data))
                .topics(parseJsonTopics(topics))
                .blockNumber(new BigInteger(blockNumber, 16))
                .transactionIndex(Integer.parseInt(transactionIndex, 16))
                .logIndex(Integer.parseInt(logIndex, 16))
                .blockHash(Hex.decodeHex(blockHash))
                .build();
    }

    private static List<byte[]> parseJsonTopics(String jsonArrayOfTopics) throws DecoderException {
        JsonArray jsonArray = (JsonArray) new JsonParser().parse(jsonArrayOfTopics);

        if (jsonArray.size() == 0) {
            return Collections.emptyList();
        } else {
            List<byte[]> topics = new ArrayList<>();

            Iterator<JsonElement> topicsIterator = jsonArray.iterator();
            while (topicsIterator.hasNext()) {
                topics.add(Hex.decodeHex(stripQuotesAndHexSignifier(topicsIterator.next().toString())));
            }
            return topics;
        }
    }

    /**
     * Assumes the input string begins with "0x and ends with " and this method strips these
     * characters from it.
     */
    private static String stripQuotesAndHexSignifier(String string) {
        String stripStart = string.substring(3);
        return stripStart.substring(0, stripStart.length() - 1);
    }

    public void clear() {
        this.address = null;
        this.data = null;
        this.topics = null;
        this.blockNumber = null;
        this.transactionIndex = null;
        this.logIndex = null;
    }
}