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

package fr.quatrevieux.singleinstance;

import fr.quatrevieux.singleinstance.ipc.InstanceServer;
import fr.quatrevieux.singleinstance.ipc.Message;
import fr.quatrevieux.singleinstance.ipc.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DistantInstanceTest {
    private DistantInstance distantInstance;
    private InstanceManager manager;
    private LockFile lock;

    @BeforeEach
    void setUp() throws IOException {
        lock = new LockFile("test-distant.lock");
        manager = new InstanceManager(lock);
        manager.acquire();

        distantInstance = new DistantInstance(lock);
    }

    @AfterEach
    void tearDown() {
        lock.release();
    }

    @Test
    void pid() {
        assertEquals(getCurrentPid(), distantInstance.pid());
    }

    @Test
    void port() throws IOException {
        assertThrows(IllegalStateException.class, () -> distantInstance.port());

        try (InstanceServer server = manager.server().get()) {
            assertEquals(server.port(), distantInstance.port());
        }
    }

    @Test
    void send() throws IOException, InterruptedException {
        try (InstanceServer server = manager.server().get()) {
            List<Message> messages = new ArrayList<>();
            new Thread(() -> {
                try {
                    server.consume(messages::add);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            SimpleMessage message = new SimpleMessage("Hello", "World".getBytes());
            distantInstance.send(message);
            Thread.sleep(100);

            assertEquals(message, messages.get(0));
        }
    }

    private int getCurrentPid() {
        return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@", 2)[0]);
    }
}
