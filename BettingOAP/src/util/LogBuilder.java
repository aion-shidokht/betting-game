package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.aion.harness.kernel.Address;
import org.aion.harness.main.tools.JsonStringParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LogBuilder {
    private Address address = null;
    private byte[] data = null;
    private List<byte[]> topics = null;
    private long blockNumber = 0;
    private byte[] blockHash = null;
    private Integer transactionIndex = null;
    private Integer logIndex = null;
    private byte[] transactionHash = null;

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

    public LogBuilder blockNumber(long blockNumber) {
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

    public LogBuilder transactionHash(byte[] transactionHash) {
        this.transactionHash = transactionHash;
        return this;
    }

    public Log build() {
        if (this.address == null) {
            throw new NullPointerException("Cannot build Log with null address!");
        }
        if (this.data == null) {
            throw new NullPointerException("Cannot build Log with null data!");
        }
        if (this.blockNumber == 0) {
            throw new NullPointerException("Cannot build Log without specifying blockNumber!");
        }
        if (this.transactionIndex == null) {
            throw new NullPointerException("Cannot build Log without specifying transactionIndex!");
        }
        if (this.logIndex == null) {
            throw new NullPointerException("Cannot build Log without specifying logIndex!");
        }

        List<byte[]> topics = (this.topics == null) ? Collections.emptyList() : this.topics;

        return new Log(this.address, this.data, topics, this.blockNumber, this.transactionIndex, this.logIndex, this.blockHash, this.transactionHash);
    }

    public Log buildFromJsonString(String jsonString) {
        JsonStringParser jsonParser = new JsonStringParser(jsonString);

        String address = jsonParser.attributeToString("address");
        String data = jsonParser.attributeToString("data");
        String topics = jsonParser.attributeToString("topics");
        String blockNumber = jsonParser.attributeToString("blockNumber");
        String blockHash = jsonParser.attributeToString("blockHash");
        String transactionIndex = jsonParser.attributeToString("transactionIndex");
        String logIndex = jsonParser.attributeToString("logIndex");
        String transactionHash = jsonParser.attributeToString("transactionHash");

        return new LogBuilder()
                .address(new Address(Helper.hexStringToBytes(address)))
                .data(parseData(data))
                .topics(parseJsonTopics(topics))
                .blockNumber(Helper.hexStringToLong(blockNumber))
                .transactionIndex(Integer.parseInt(transactionIndex, 16))
                .logIndex(Integer.parseInt(logIndex, 16))
                .blockHash(Helper.hexStringToBytes(blockHash))
                .transactionHash(Helper.hexStringToBytes(transactionHash))
                .build();
    }

    public static List<byte[]> parseJsonTopics(String jsonArrayOfTopics) {
        JsonArray jsonArray = (JsonArray) new JsonParser().parse(jsonArrayOfTopics);

        if (jsonArray.size() == 0) {
            return Collections.emptyList();
        } else {
            List<byte[]> topics = new ArrayList<>();

            Iterator<JsonElement> topicsIterator = jsonArray.iterator();
            while (topicsIterator.hasNext()) {
                topics.add(Helper.hexStringToBytes(stripQuotesAndHexSignifier(topicsIterator.next().toString())));
            }
            return topics;
        }
    }

    public static byte[] parseData(String data) {
        if (data != null) {
            return Helper.hexStringToBytes(data);
        } else {
            return new byte[0];
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
}
