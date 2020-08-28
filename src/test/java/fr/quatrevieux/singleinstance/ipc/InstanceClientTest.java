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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstanceClientTest {
    @Test
    void openInvalidPort() {
        InstanceClient client = new InstanceClient(11457);

        assertThrows(IOException.class, client::open);
    }

    @Test
    void sendFunctional() throws IOException, InterruptedException {
        try (InstanceServer server = new InstanceServer()) {
            server.open();
            List<Message> messages = new ArrayList<>();
            new Thread(() -> {
                try {
                    server.consume(messages::add);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            try (InstanceClient client = new InstanceClient(server.port())) {
                client.send("Hello World");
                Thread.sleep(100);
                assertEquals(new SimpleMessage("Hello World"), messages.get(0));

                client.send("Hello", "World".getBytes());
                Thread.sleep(100);
                assertEquals(new SimpleMessage("Hello", "World".getBytes()), messages.get(1));

                SimpleMessage message = new SimpleMessage("foo", "bar".getBytes());
                client.send(message);
                Thread.sleep(100);
                assertEquals(message, messages.get(2));
            }
        }
    }

    @Test
    void closedClientShouldBeReopenedOnSend() throws Exception {
        try (InstanceServer server = new InstanceServer()) {
            server.open();
            List<Message> messages = new ArrayList<>();
            new Thread(() -> {
                try {
                    server.consume(messages::add);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            InstanceClient client = new InstanceClient(server.port());
            client.open();
            client.close();

            client.send("Hello World");
            Thread.sleep(100);
            assertEquals(new SimpleMessage("Hello World"), messages.get(0));

            client.close();
        }
    }

    @Test
    void sendErrorOnServerClosed() throws IOException {
        try (InstanceServer server = new InstanceServer()) {
            server.open();

            try (InstanceClient client = new InstanceClient(server.port())) {
                client.open();
                server.close();

                assertThrows(IOException.class, () -> client.send("Hello World"));
            }
        }
    }
}
