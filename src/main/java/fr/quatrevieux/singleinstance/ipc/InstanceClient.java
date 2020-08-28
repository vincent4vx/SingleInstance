/*
 * This file is part of SingleInstance.
 *
 * SingleInstance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SingleInstance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SingleInstance.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 Vincent Quatrevieux
 */

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
