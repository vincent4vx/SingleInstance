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
