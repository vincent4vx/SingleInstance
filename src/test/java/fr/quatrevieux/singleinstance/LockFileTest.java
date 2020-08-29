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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LockFileTest {
    private LockFile lock;

    @BeforeEach
    void setUp() {
        lock = new LockFile("test.lock");
    }

    @AfterEach
    void tearDown() {
        lock.release();
    }

    @Test
    void acquireReleaseFunctional() throws IOException {
        assertTrue(lock.acquire());
        assertTrue(Files.exists(Paths.get("test.lock")));

        assertTrue(lock.acquire());

        assertEquals("test.lock", lsof()[8]);
        assertEquals(getCurrentPid(), lsof()[1]);
        assertTrue(lsof()[3].matches(".*[Wu].*"), "Expects contains lock char on " + Arrays.toString(lsof()));

        lock.release();
        assertFalse(Files.exists(Paths.get("test.lock")));
    }

    @Test
    void lockReleaseFunctional() throws IOException {
        lock.lock();
        assertTrue(Files.exists(Paths.get("test.lock")));

        assertTrue(lock.acquire());

        assertEquals("test.lock", lsof()[8]);
        assertTrue(lsof()[3].matches(".*[Wu].*"), "Expects contains lock char on " + Arrays.toString(lsof()));

        lock.release();
        assertFalse(Files.exists(Paths.get("test.lock")));
    }

    @Test
    void writeRead() throws IOException {
        lock.acquire();

        lock.write(output -> {
            output.writeInt(144);
            output.writeBoolean(true);
        });

        assertEquals(true, lock.read(input -> {
            assertEquals(144, input.readInt());
            assertTrue(input.readBoolean());

            return true;
        }));

        lock.write(output -> output.writeInt(102));

        assertEquals(102, lock.read(DataInput::readInt));

        lock.release();
    }

    @Test
    void acquireMultiple() throws IOException {
        assertTrue(lock.acquire());

        LockFile other = new LockFile("test.lock");
        assertTrue(other.acquire());

        other.release();
        assertTrue(Files.exists(Paths.get("test.lock")));
        assertEquals("test.lock", lsof()[8]);
        assertTrue(lsof()[3].matches(".*[Wu].*"), "Expects contains lock char on " + Arrays.toString(lsof()));
        assertTrue(lock.acquire());

        lock.release();
        assertFalse(Files.exists(Paths.get("test.lock")));
    }

    @Test
    void equalsAndHash() {
        assertEquals(lock, lock);
        assertNotEquals(lock, null);
        assertEquals(lock, new LockFile("test.lock"));
        assertNotEquals(lock, new LockFile("other.lock"));
        assertNotEquals(lock, new Object());

        assertEquals(lock.hashCode(), lock.hashCode());
        assertEquals(lock.hashCode(), new LockFile("test.lock").hashCode());
        assertNotEquals(lock.hashCode(), new LockFile("other.lock").hashCode());
    }

    private String[] lsof() throws IOException {
        Process process = Runtime.getRuntime().exec("lsof test.lock");
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        br.readLine();

        return br.readLine().split("\\s+");
    }

    private String getCurrentPid() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@", 2)[0];
    }
}
