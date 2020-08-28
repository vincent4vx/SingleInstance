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

import fr.quatrevieux.singleinstance.ipc.Message;
import fr.quatrevieux.singleinstance.ipc.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SingleInstanceTest {
    private LockFile lockFile;

    @BeforeEach
    void setUp() throws IOException {
        SingleInstance.init(lockFile = new LockFile("single-instance.lock"));
    }

    @AfterEach
    void tearDown() {
        SingleInstance.close();
    }

    @Test
    void isFirst() throws IOException {
        assertTrue(SingleInstance.isFirst());
    }

    @Test
    void onAlreadyRunning() throws IOException {
        AtomicBoolean a = new AtomicBoolean();
        SingleInstance.onAlreadyRunning(instance -> a.set(true));

        assertFalse(a.get());
    }

    @Test
    void onMessage() throws IOException, InterruptedException {
        List<Message> messages = new ArrayList<>();
        SingleInstance.onMessage(messages::add);

        assertThrows(IllegalStateException.class, () -> SingleInstance.onMessage(messages::add));

        DistantInstance instance = new DistantInstance(lockFile);
        instance.send("Hello World");
        Thread.sleep(100);

        assertEquals(new SimpleMessage("Hello World"), messages.get(0));
    }
}
