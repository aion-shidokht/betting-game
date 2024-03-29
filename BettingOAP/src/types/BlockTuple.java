package types;

import util.Helper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BlockTuple {
    private long blockNumber;
    private byte[] blockHash;
    private List<Integer> logIds;

    private BlockTuple(long blockNumber, byte[] blockHash, List<Integer> logIds) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.logIds = logIds;
    }

   public static BlockTuple of(long blockNumber, byte[] blockHash, List<Integer> logIds) {
        return new BlockTuple(blockNumber, blockHash, logIds);
   }

    public long getBlockNumber() {
        return blockNumber;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public List<Integer> getIncluededLogIds() {
        return logIds;
    }

    @Override
    public String toString() {
        return "BlockTuple{" +
                "blockNumber=" + blockNumber +
                ", blockHash=" + Helper.bytesToHexString(blockHash) +
                ", logIds=" + logIds +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockTuple that = (BlockTuple) o;
        return blockNumber == that.blockNumber &&
                Arrays.equals(blockHash, that.blockHash) &&
                logIds.equals(that.logIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(blockNumber, logIds);
        result = 31 * result + Arrays.hashCode(blockHash);
        return result;
    }
}
