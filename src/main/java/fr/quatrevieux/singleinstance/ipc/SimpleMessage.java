package fr.quatrevieux.singleinstance.ipc;

import java.util.Arrays;
import java.util.Objects;

/**
 * Basic implementation for IPC messages
 */
final public class SimpleMessage implements Message {
    final private String name;
    final private byte[] data;

    public SimpleMessage(String name) {
        this(name, new byte[0]);
    }

    public SimpleMessage(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public byte[] data() {
        return data;
    }

    @Override
    public String toString() {
        return "SimpleMessage(" + name + ": " + Arrays.toString(data) + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleMessage that = (SimpleMessage) o;
        return name.equals(that.name) && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
