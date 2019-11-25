package types;

import org.aion.util.bytes.ByteUtil;

public class Player {
    private final Address playerAddress;
    private final String transactionHash;

    public static Player from(byte[] data, byte[] transactionHash) {
        return new Player(new Address(data),
                "0x" + ByteUtil.toHexString(transactionHash));
    }

    private Player(Address playerAddress, String transactionHash) {
        this.playerAddress = playerAddress;
        this.transactionHash = transactionHash;
    }

    public Address getPlayerAddress() {
        return playerAddress;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public Player(Player player){
        this.playerAddress = player.playerAddress;
        this.transactionHash= player.transactionHash;
    }
    @Override
    public String toString() {
        return "Player{" +
                "playerAddress=" + playerAddress +
                ", transactionHash=" + transactionHash +
                '}';
    }
}
