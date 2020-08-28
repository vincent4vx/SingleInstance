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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse the incoming data to get IPC packet messages
 * The packet format is :
 * Packet {
 *     nameLength: short
 *     dataLength: short
 *     name: string
 *     data: byte[]
 * }
 *
 * Note: The parser instance must not be shared between clients
 *
 * Usage:
 * <code>
 *     ByteBuffer buffer = xxx;
 *     SelectionKey key = xxx;
 *
 *     // Data available for read
 *     if (key.isReadable()) {
 *         // Get the parser instance for the client
 *         ProtocolParser parser = ProtocolParser.forKey(key);
 *
 *         // Read from socket, to the buffer
 *         SocketChannel client = (SocketChannel) key.channel();
 *         client.read(buffer);
 *         buffer.flip();
 *
 *         // Parsing incoming packets
 *         parser.parse(buffer);
 *
 *         for (ProtocolParser.Packet packet : parser.packets()) {
 *             // Handle packet
 *         }
 *
 *         // Clear already handled packets
 *         parser.clear();
 *     }
 * </code>
 */
final public class ProtocolParser {
    final private List<Message> packets = new ArrayList<>();

    private byte[] nameBuffer;
    private int nameOffset;

    private byte[] dataBuffer;
    private int dataOffset;

    /**
     * Parse packets from the buffer
     * Handle partial packet : bufferize partial data for next call
     *
     * @param buffer The input buffer
     */
    public void parse(ByteBuffer buffer) {
        SimpleMessage packet;

        while ((packet = parsePacket(buffer)) != null) {
            packets.add(packet);
        }
    }

    /**
     * Get all parsed packets
     *
     * @return messages
     */
    public List<Message> packets() {
        return packets;
    }

    /**
     * Clear parsed packets
     * After call this method, {@link ProtocolParser#packets()} will return an empty list
     */
    public void clear() {
        packets.clear();
    }

    private SimpleMessage parsePacket(ByteBuffer buffer) {
        try {
            if (nameBuffer == null) {
                nameBuffer = new byte[buffer.getShort()];
            }

            if (dataBuffer == null) {
                dataBuffer = new byte[buffer.getShort()];
            }
        } catch (BufferUnderflowException e) {
            // Ignore
            return null;
        }

        if (nameOffset != nameBuffer.length) {
            nameOffset += read(buffer, nameBuffer, nameOffset);

            if (nameOffset != nameBuffer.length) {
                return null;
            }
        }

        if (dataOffset != dataBuffer.length) {
            dataOffset += read(buffer, dataBuffer, dataOffset);

            if (dataOffset != dataBuffer.length) {
                return null;
            }
        }

        SimpleMessage packet = new SimpleMessage(new String(nameBuffer), dataBuffer);

        nameBuffer = null;
        nameOffset = 0;
        dataBuffer = null;
        dataOffset = 0;

        return packet;
    }

    /**
     * Read byte array from buffer
     *
     * @return Number of read bytes
     */
    private int read(ByteBuffer src, byte[] dst, int offset) {
        int toRead = Math.min(dst.length - offset, src.remaining());
        src.get(dst, offset, toRead);

        return toRead;
    }

    /**
     * Get the {@link ProtocolParser} instance related to the key
     * If the parser is not attached, a new instance will be created
     *
     * @param key The key to attach
     *
     * @return The parser instance
     */
    static public ProtocolParser forKey(SelectionKey key) {
        if (key.attachment() instanceof ProtocolParser) {
            return (ProtocolParser) key.attachment();
        }

        ProtocolParser parser = new ProtocolParser();
        key.attach(parser);

        return parser;
    }

    /**
     * Format message to packet format bytes array
     *
     * @param message Message to format
     *
     * @return Formatted message
     */
    static public byte[] toBytes(Message message) {
        final byte[] name = message.name().getBytes();
        final byte[] data = message.data();

        ByteBuffer buffer = ByteBuffer.allocate(4 + name.length + data.length);

        buffer.putShort((short) name.length);
        buffer.putShort((short) data.length);
        buffer.put(name);
        buffer.put(data);

        return buffer.array();
    }
}
