package types;

import org.aion.harness.kernel.Address;
import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

public class Player {
    private final Address playerAddress;
    private final byte[] transactionHash;

    public static Player from(byte[] data, byte[] transactionHash){
        return new Player(new Address(data),
                transactionHash);
    }

    private Player(Address playerAddress, byte[] transactionHash) {
        this.playerAddress = playerAddress;
        this.transactionHash = transactionHash;
    }

    public Address getPlayerAddress() {
        return playerAddress;
    }

    public byte[] getTransactionHash() {
        return transactionHash.clone();
    }

    public Player(Player player){
        this.playerAddress = player.playerAddress;
        this.transactionHash= player.transactionHash;
    }
    @Override
    public String toString() {
        return "Player{" +
                "playerAddress=" + playerAddress +
                ", transactionHash=" + Hex.encodeHexString(transactionHash) +
                '}';
    }
}
