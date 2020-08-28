package fr.quatrevieux.singleinstance.ipc;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * IPC client for sending actions to the distant instance
 *
 * Usage:
 * <code>
 *     try (InstanceClient client = new InstanceClient(1234)) {
 *         client.send("Hello", "my payload".getBytes());
 *     }
 * </code>
 */
final public class InstanceClient implements Closeable, MessageSender {
    final private int port;
    private Socket socket;
    private OutputStream output;

    /**
     * @param port The listening port
     */
    public InstanceClient(int port) {
        this.port = port;
    }

    /**
     * Open the client
     * Note: this method is implicitly called on {@link InstanceClient#send(Message)}
     *
     * @throws IOException When cannot open the socket
     */
    public void open() throws IOException {
        socket = new Socket("localhost", port);
        output = socket.getOutputStream();
    }

    @Override
    public void send(Message message) throws IOException {
        if (socket == null || socket.isClosed()) {
            open();
        }

        output.write(ProtocolParser.toBytes(message));
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            output.close();
            output = null;

            socket.close();
            socket = null;
        }
    }
}
