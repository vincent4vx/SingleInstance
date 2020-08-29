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

package fr.quatrevieux.singleinstance.ipc.consumer;

import fr.quatrevieux.singleinstance.ipc.InstanceClient;
import fr.quatrevieux.singleinstance.ipc.InstanceServer;
import fr.quatrevieux.singleinstance.ipc.Message;
import fr.quatrevieux.singleinstance.ipc.SimpleMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageTransformerConsumerTest {
    class Foo implements Message {
        final private String data;

        public Foo(byte[] data) {
            this(new String(data));
        }

        public Foo(String data) {
            this.data = data;
        }

        @Override
        public String name() {
            return "Foo";
        }

        @Override
        public byte[] data() {
            return data.getBytes();
        }
    }

    class Bar implements Message {
        final private int value;

        public Bar(byte[] data) {
            this(ByteBuffer.wrap(data).getInt());
        }

        public Bar(int value) {
            this.value = value;
        }

        @Override
        public String name() {
            return "Bar";
        }

        @Override
        public byte[] data() {
            return ByteBuffer.allocate(4).putInt(value).array();
        }
    }

    @Test
    void functional() throws IOException, InterruptedException {
        List<Message> messages = new ArrayList<>();
        MessageTransformerConsumer consumer = new MessageTransformerConsumer(messages::add);

        consumer
            .register("Foo", message -> new Foo(message.data()))
            .register("Bar", message -> new Bar(message.data()))
            .register("Null", message -> null)
        ;

        try (InstanceServer server = new InstanceServer()) {
            server.open();

            try (InstanceClient client = new InstanceClient(server.port())) {
                new Thread(() -> {
                    try {
                        server.consume(consumer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                client.send(new Foo("Hello World !"));
                client.send(new Bar(1258));
                client.send("Null");
                client.send("Other");

                Thread.sleep(100);

                assertEquals(3, messages.size());
                assertTrue(messages.get(0) instanceof Foo);
                assertEquals("Hello World !", Foo.class.cast(messages.get(0)).data);
                assertTrue(messages.get(1) instanceof Bar);
                assertEquals(1258, Bar.class.cast(messages.get(1)).value);
                assertEquals(new SimpleMessage("Other"), messages.get(2));
            }
        }
    }
}
