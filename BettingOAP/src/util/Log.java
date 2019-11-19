package util;

import org.aion.harness.kernel.Address;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// todo replace with a modified version in node_test_harness
public class Log {
    public final Address address;
    private final byte[] data;
    private final List<byte[]> topics;
    public final BigInteger blockNumber;
    public final byte[] blockHash;
    public final int transactionIndex;
    public final int logIndex;
    public final byte[] transactionHash;

    public Log(Address address, byte[] data, List<byte[]> topics,
               BigInteger blockNumber, int transactionIndex, int logIndex, byte[] blockHash, byte[] transactionHash) {
        this.address = address;
        this.data = Arrays.copyOf(data, data.length);
        this.topics = copyOfBytesList(topics);
        this.blockNumber = blockNumber;
        this.blockHash = Arrays.copyOf(blockHash, blockHash.length);
        this.transactionIndex = transactionIndex;
        this.logIndex = logIndex;
        this.transactionHash = transactionHash;
    }

    public byte[] copyOfBlockHash() {
        return (null != blockHash)
                ? Arrays.copyOf(this.blockHash, this.blockHash.length)
                : null;
    }

    public byte[] copyOfData() {
        return Arrays.copyOf(this.data, this.data.length);
    }

    public List<byte[]> copyOfTopics() {
        return copyOfBytesList(this.topics);
    }

    public byte[] copyOfTransactionHash() {
        return (null != transactionHash)
                ? Arrays.copyOf(this.transactionHash, this.transactionHash.length)
                : null;
    }

    @Override
    public String toString() {
        return "Log { address = " + this.address + ", topics = [" + topicsToString()
                + "], data = 0x" + Hex.encodeHexString(this.data) + ", block number = " + this.blockNumber +
                ", blockHash = 0x" + Hex.encodeHexString(this.blockHash) + ", transaction index = " + this.transactionIndex +
                ", log index = " + this.logIndex + " }";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Log)) {
            return false;
        } else if (other == this) {
            return true;
        }

        Log otherLog = (Log) other;
        if (!this.address.equals(otherLog.address)) {
            return false;
        }
        if (!Arrays.equals(this.data, otherLog.data)) {
            return false;
        }
        if (!Arrays.equals(this.blockHash, otherLog.blockHash)) {
            return false;
        }

        return bytesListsAreEqual(this.topics, otherLog.topics);
    }

    @Override
    public int hashCode() {
        int hash = this.address.hashCode() + Arrays.hashCode(this.data);
        for (byte[] topic : this.topics) {
            hash += Arrays.hashCode(topic);
        }
        return hash;
    }

    private String topicsToString() {
        StringBuilder builder = new StringBuilder();

        int index = 0;
        for (byte[] topic : this.topics) {
            builder.append("0x").append(Hex.encodeHexString(topic));

            if (index < this.topics.size() - 1) {
                builder.append(", ");
            }

            index++;
        }
        return builder.toString();
    }

    private static List<byte[]> copyOfBytesList(List<byte[]> bytesList) {
        List<byte[]> copy = new ArrayList<>();
        for (byte[] bytes : bytesList) {
            copy.add(Arrays.copyOf(bytes, bytes.length));
        }
        return copy;
    }

    private static boolean bytesListsAreEqual(List<byte[]> bytesList1, List<byte[]> bytesList2) {
        if (bytesList1.size() != bytesList2.size()) {
            return false;
        } else {
            int length = bytesList1.size();

            for (int i = 0; i < length; i++) {
                if (!Arrays.equals(bytesList1.get(i), bytesList2.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
