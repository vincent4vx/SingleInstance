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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstanceServerTest {
    private InstanceServer server;
    private List<Message> messages;
    private Thread thread;

    @BeforeEach
    void setUp() {
        server = new InstanceServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void port() throws IOException {
        assertThrows(IllegalStateException.class, () -> server.port());

        server.open(new InetSocketAddress(7458));
        assertEquals(7458, server.port());
        server.close();

        server.open();
        assertTrue(server.port() > 0);
    }

    @Test
    void functionalSimple() throws IOException, InterruptedException {
        start();

        try (InstanceClient client = new InstanceClient(server.port())) {
            client.send("Hello");
            Thread.sleep(100);

            assertEquals(1, messages.size());
            assertEquals(new SimpleMessage("Hello"), messages.get(0));

            SimpleMessage withData = new SimpleMessage("MessageName", "my complex payload".getBytes());
            client.send(withData);
            Thread.sleep(100);

            assertEquals(2, messages.size());
            assertEquals(withData, messages.get(1));
        }
    }

    @Test
    void functionalMultipleClients() throws Exception {
        start();

        try (
            InstanceClient client1 = new InstanceClient(server.port());
            InstanceClient client2 = new InstanceClient(server.port());
            InstanceClient client3 = new InstanceClient(server.port());
        ) {
            client1.send("message 1");
            Thread.sleep(100);
            assertEquals(new SimpleMessage("message 1"), messages.get(0));

            client2.send("message 2");
            Thread.sleep(100);
            assertEquals(new SimpleMessage("message 2"), messages.get(1));

            client3.send("message 3");
            Thread.sleep(100);
            assertEquals(new SimpleMessage("message 3"), messages.get(2));

            client2.send("message 4");
            Thread.sleep(100);
            assertEquals(new SimpleMessage("message 4"), messages.get(3));
        }
    }

    @Test
    void functionalClose() throws IOException, InterruptedException {
        start();

        try (InstanceClient client = new InstanceClient(server.port())) {
            assertTrue(server.running());
            server.close();
            assertFalse(server.running());

            assertThrows(IOException.class, () -> client.send("Hello"));
            Thread.sleep(100);

            assertFalse(thread.isAlive());
            assertTrue(messages.isEmpty());
        }
    }

    private void start() throws IOException, InterruptedException {
        messages = new ArrayList<>();
        server.open();

        thread = new Thread(() -> {
            try {
                server.consume(messages::add);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        thread.start();
        Thread.sleep(100);
    }
}
