package fr.quatrevieux.singleinstance.ipc;

/**
 * The IPC message
 */
public interface Message {
    /**
     * The message name
     */
    public String name();

    /**
     * The message payload
     */
    public byte[] data();
}
