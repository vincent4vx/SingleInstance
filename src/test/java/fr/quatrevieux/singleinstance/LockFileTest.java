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
        assertTrue(lsof()[3].contains("W"), "Expects contains W on " + lsof()[8]);

        lock.release();
        assertFalse(Files.exists(Paths.get("test.lock")));
    }

    @Test
    void lockReleaseFunctional() throws IOException {
        lock.lock();
        assertTrue(Files.exists(Paths.get("test.lock")));

        assertTrue(lock.acquire());

        assertEquals("test.lock", lsof()[8]);
        assertTrue(lsof()[3].contains("W"), "Expects contains W on " + lsof()[8]);

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
