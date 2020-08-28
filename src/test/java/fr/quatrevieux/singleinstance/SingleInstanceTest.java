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
