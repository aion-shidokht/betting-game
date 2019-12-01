package types;

import util.Helper;

import java.util.Arrays;

public class Address {
    private final byte[] addressBytes;

    public Address(byte[] addressBytes) {
        if (addressBytes == null) {
            throw new NullPointerException("address bytes cannot be null");
        } else if (addressBytes.length != 32) {
            throw new IllegalArgumentException("bytes of an address must have a length of 32");
        } else {
            this.addressBytes = copyByteArray(addressBytes);
        }
    }

    public byte[] toBytes() {
        return copyByteArray(this.addressBytes);
    }

    private static byte[] copyByteArray(byte[] byteArray) {
        return Arrays.copyOf(byteArray, byteArray.length);
    }

    public String getAddressString() {
        return "0x" + Helper.bytesToHexString(this.addressBytes);
    }

    public boolean equals(Object other) {
        if (!(other instanceof Address)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            Address otherAddress = (Address) other;
            return Arrays.equals(this.addressBytes, otherAddress.toBytes());
        }
    }

    public int hashCode() {
        return Arrays.hashCode(this.addressBytes);
    }
}
