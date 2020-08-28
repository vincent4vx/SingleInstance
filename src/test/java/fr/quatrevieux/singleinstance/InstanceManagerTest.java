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

class InstanceManagerTest {
    private LockFile lockFile;
    private InstanceManager manager;

    @BeforeEach
    void setUp() {
        lockFile = new LockFile("manager-test.lock");
        manager = new InstanceManager(lockFile);
    }

    @AfterEach
    void tearDown() {
        lockFile.release();
    }

    @Test
    void acquire() throws IOException {
        assertTrue(manager.acquire());
        assertEquals(getCurrentPid(), new DistantInstance(lockFile).pid());
    }

    @Test
    void find() throws IOException {
        assertFalse(manager.find().isPresent());
        assertEquals(getCurrentPid(), new DistantInstance(lockFile).pid());
    }

    @Test
    void server() throws IOException, InterruptedException {
        InstanceServer server = manager.server().get();

        DistantInstance instance = new DistantInstance(lockFile);

        assertEquals(getCurrentPid(), instance.pid());
        assertEquals(server.port(), instance.port());

        List<Message> messages = new ArrayList<>();
        new Thread(() -> {
            try {
                server.consume(messages::add);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        instance.send(new SimpleMessage("Hello World"));
        Thread.sleep(100);

        assertEquals(new SimpleMessage("Hello World"), messages.get(0));
        server.close();
    }

    private int getCurrentPid() {
        return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@", 2)[0]);
    }
}
