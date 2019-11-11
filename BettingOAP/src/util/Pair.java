package util;

import java.util.Objects;

public class Pair<K, V> {
    public K key;
    public V value;

    private Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair(key, value);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Pair<?, ?> pair = (Pair) o;
            return this.key.equals(pair.key) && this.value.equals(pair.value);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.key, this.value});
    }
}
