package internal;

import java.util.List;

public class Assertion {
    public static void assertTopicSize(List<byte[]> topics, int size) {
        if (topics.size() != size) {
            throw new CriticalException("Invalid length for topics. Expected " + size + " values, received " + topics.size());
        }
    }

    public static void assertTrue(boolean statement) {
        if (!statement) {
            throw new CriticalException("Statement MUST be true");
        }
    }
}
