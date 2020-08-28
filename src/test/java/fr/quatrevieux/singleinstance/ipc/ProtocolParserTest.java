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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolParserTest {
    private ProtocolParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProtocolParser();
    }

    @Test
    void empty() {
        assertTrue(parser.packets().isEmpty());
    }

    @Test
    void parseEmptyBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.flip();
        parser.parse(buffer);
        assertTrue(parser.packets().isEmpty());
    }

    @Test
    void parseWithSingleEmptyPacket() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.flip();

        parser.parse(buffer);
        assertEquals(1, parser.packets().size());
        assertTrue(parser.packets().get(0).name().isEmpty());
        assertEquals(0, parser.packets().get(0).data().length);
    }

    @Test
    void parseWithPacketWithoutData() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 3);
        buffer.putShort((short) 0);
        buffer.put("Foo".getBytes());
        buffer.flip();

        parser.parse(buffer);
        assertEquals(1, parser.packets().size());
        assertEquals("Foo", parser.packets().get(0).name());
        assertEquals(0, parser.packets().get(0).data().length);
    }

    @Test
    void parseWithPacketWithData() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 3);
        buffer.putShort((short) 3);
        buffer.put("Foo".getBytes());
        buffer.put(new byte[] {12, 36, 14});
        buffer.flip();

        parser.parse(buffer);
        assertEquals(1, parser.packets().size());
        assertEquals("Foo", parser.packets().get(0).name());
        assertArrayEquals(new byte[] {12, 36, 14}, parser.packets().get(0).data());
    }

    @Test
    void parsePartialPacket() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 3);
        buffer.flip();

        parser.parse(buffer);
        assertTrue(parser.packets().isEmpty());

        buffer.clear();
        buffer.putShort((short) 3);
        buffer.flip();

        parser.parse(buffer);
        assertTrue(parser.packets().isEmpty());

        buffer.clear();
        buffer.put("Foo".getBytes());
        buffer.flip();

        parser.parse(buffer);
        assertTrue(parser.packets().isEmpty());

        buffer.clear();
        buffer.put(new byte[] {12, 36, 14});
        buffer.flip();
        parser.parse(buffer);
        assertEquals(1, parser.packets().size());
        assertEquals("Foo", parser.packets().get(0).name());
        assertArrayEquals(new byte[] {12, 36, 14}, parser.packets().get(0).data());
    }

    @Test
    void parseWithPartialName() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 3);
        buffer.putShort((short) 3);
        buffer.put("Fo".getBytes());
        buffer.flip();

        parser.parse(buffer);
        assertTrue(parser.packets().isEmpty());

        buffer.clear();
        buffer.put("o".getBytes());
        buffer.put(new byte[] {12, 36, 14});
        buffer.flip();
        parser.parse(buffer);
        assertEquals(1, parser.packets().size());
        assertEquals("Foo", parser.packets().get(0).name());
        assertArrayEquals(new byte[] {12, 36, 14}, parser.packets().get(0).data());
    }

    @Test
    void parseWithPartialData() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 3);
        buffer.putShort((short) 3);
        buffer.put("Foo".getBytes());
        buffer.put(new byte[] {12});
        buffer.flip();

        parser.parse(buffer);
        assertTrue(parser.packets().isEmpty());

        buffer.clear();
        buffer.put(new byte[] {36, 14});
        buffer.flip();
        parser.parse(buffer);
        assertEquals(1, parser.packets().size());
        assertEquals("Foo", parser.packets().get(0).name());
        assertArrayEquals(new byte[] {12, 36, 14}, parser.packets().get(0).data());
    }

    @Test
    void parseWithMultiplePackets() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 5);
        buffer.putShort((short) 3);
        buffer.put("Hello".getBytes());
        buffer.put(new byte[] {1, 2, 3});

        buffer.putShort((short) 5);
        buffer.putShort((short) 3);
        buffer.put("World".getBytes());
        buffer.put(new byte[] {4, 5, 6});
        buffer.flip();

        parser.parse(buffer);
        assertEquals(2, parser.packets().size());

        assertEquals("Hello", parser.packets().get(0).name());
        assertArrayEquals(new byte[] {1, 2, 3}, parser.packets().get(0).data());
        assertEquals("World", parser.packets().get(1).name());
        assertArrayEquals(new byte[] {4, 5, 6}, parser.packets().get(1).data());
    }

    @Test
    void clear() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 5);
        buffer.putShort((short) 3);
        buffer.put("Hello".getBytes());
        buffer.put(new byte[] {1, 2, 3});

        buffer.putShort((short) 5);
        buffer.putShort((short) 3);
        buffer.put("World".getBytes());
        buffer.put(new byte[] {4, 5, 6});
        buffer.flip();

        parser.parse(buffer);
        assertEquals(2, parser.packets().size());

        parser.clear();
        assertEquals(0, parser.packets().size());
    }

    @Test
    void packetToString() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.putShort((short) 5);
        buffer.putShort((short) 3);
        buffer.put("Hello".getBytes());
        buffer.put(new byte[] {1, 2, 3});
        buffer.flip();

        parser.parse(buffer);
        assertEquals("SimpleMessage(Hello: [1, 2, 3])", parser.packets().get(0).toString());
    }

    @Test
    void toBytesFunctional() {
        SimpleMessage message = new SimpleMessage("Hello", "World".getBytes());

        parser.parse(ByteBuffer.wrap(ProtocolParser.toBytes(message)));
        assertEquals(message, parser.packets().get(0));
    }
}
