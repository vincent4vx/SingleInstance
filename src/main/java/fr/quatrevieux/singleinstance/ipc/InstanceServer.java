package fr.quatrevieux.singleinstance.ipc;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

/**
 * IPC server for the first running instance
 *
 * Usage:
 * <code>
 *     try (InstanceServer server = new InstanceServer()) {
 *         server.open();
 *
 *         server.consume(message -> {
 *             // process received message
 *             if (message.name().equals(xxx)) {
 *                 //...
 *             }
 *         });
 *     }
 * </code>
 */
final public class InstanceServer implements Closeable {
    private ServerSocketChannel serverSocket;
    private Selector selector;

    /**
     * Open the server on a random port number
     *
     * @throws IOException When cannot start the server
     */
    public void open() throws IOException {
        open(new InetSocketAddress("localhost", 0));
    }

    /**
     * Open the server on the given address
     *
     * @param address The bind address
     *
     * @throws IOException When cannot start the server
     */
    public void open(InetSocketAddress address) throws IOException {
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(address);
        serverSocket.configureBlocking(false);
    }

    /**
     * Consume incoming messages
     * Note: This method is blocking : a thread is not created for consuming messages
     *
     * @param action The message consumer
     *
     * @throws IOException When an error occurs during reading message
     * @throws IllegalStateException If the server is not opened
     */
    public void consume(Consumer<Message> action) throws IOException {
        if (serverSocket == null) {
            throw new IllegalStateException("Server must be opened");
        }

        selector = Selector.open();
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        final ByteBuffer buffer = ByteBuffer.allocate(256);

        try {
            while (running()) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        SocketChannel client = serverSocket.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    }

                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        client.read(buffer);
                        buffer.flip();

                        final ProtocolParser parser = ProtocolParser.forKey(key);

                        ProtocolParser.forKey(key).parse(buffer);

                        parser.packets().forEach(action);
                        parser.clear();
                        buffer.clear();
                    }

                    iter.remove();
                }
            }
        } catch (ClosedSelectorException|CancelledKeyException e) {
            // Ignore
        }
    }

    /**
     * Get the bind port number
     *
     * @throws IllegalStateException If the server is not opened
     */
    public int port() {
        if (serverSocket == null) {
            throw new IllegalStateException("Server must be opened");
        }

        return serverSocket.socket().getLocalPort();
    }

    /**
     * Check if the server is running
     */
    public boolean running() {
        return selector != null && selector.isOpen() && serverSocket != null && serverSocket.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (selector != null) {
            selector.close();
            selector = null;
        }

        if (serverSocket != null) {
            serverSocket.close();
            serverSocket = null;
        }
    }
}
