package fr.quatrevieux.singleinstance.ipc;

import java.io.IOException;

/**
 * Send IPC message to the distant instance
 */
public interface MessageSender {
    /**
     * Send a simple message with payload
     *
     * @param message The message name
     * @param data The message payload
     *
     * @see SimpleMessage
     * @throws IOException When cannot send message
     */
    default public void send(String message, byte[] data) throws IOException {
        send(new SimpleMessage(message, data));
    }

    /**
     * Send a simple message without payload
     *
     * @param message The message name
     *
     * @see SimpleMessage
     * @throws IOException When cannot send message
     */
    default public void send(String message) throws IOException {
        send(new SimpleMessage(message));
    }

    /**
     * Send a message
     *
     * @param message The message
     *
     * @throws IOException When cannot send message
     */
    public void send(Message message) throws IOException;
}
