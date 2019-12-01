package util;

import java.math.BigInteger;

public class Helper {
    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHexString(byte[] bytes) {
        int length = bytes.length;

        char[] hexChars = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToHexStringWith0x(byte[] bytes) {
        return "0x" + bytesToHexString(bytes);
    }

    public static byte[] hexStringToBytes(String s) {
        if (s.startsWith("0x")) {
            s = s.substring(2);
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static int byteArrayToInteger(byte[] bytes) {
        return new BigInteger(Helper.bytesToHexString(bytes), 16).intValue();
    }
}
