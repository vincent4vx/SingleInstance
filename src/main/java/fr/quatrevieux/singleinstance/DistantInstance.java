package fr.quatrevieux.singleinstance;


import fr.quatrevieux.singleinstance.ipc.InstanceClient;
import fr.quatrevieux.singleinstance.ipc.Message;
import fr.quatrevieux.singleinstance.ipc.MessageSender;

import java.io.EOFException;
import java.io.IOException;

/**
 * Handle information and interactions with the distant instance (i.e. the first running instance)
 */
final public class DistantInstance implements MessageSender {
    static private class ProcessInfo {
        public int pid;
        public int port = -1;

        public ProcessInfo(int pid) {
            this.pid = pid;
        }

        public boolean hasPort() {
            return port != -1;
        }
    }

    final private LockFile lockFile;

    private ProcessInfo info;
    private InstanceClient client;

    public DistantInstance(LockFile lockFile) throws IOException {
        this.lockFile = lockFile;

        load();
    }

    /**
     * Get the PID of the distant instance
     */
    public int pid() {
        return info.pid;
    }

    /**
     * Get the opened port number
     *
     * @throws IllegalStateException When port cannot be resolved
     */
    public int port() {
        if (!info.hasPort()) {
            try {
                load();
            } catch (IOException e) {
                // Ignore
            }

            if (!info.hasPort()) {
                throw new IllegalStateException("The distant server is not started or cannot access to the port number");
            }
        }

        return info.port;
    }

    /**
     * Get the IPC client to communicate with the distant instance
     * Note: send methods can be used as shortcut
     *
     * @throws IllegalStateException When cannot find the port number
     */
    public InstanceClient client() {
        if (client != null) {
            return client;
        }

        return client = new InstanceClient(port());
    }

    @Override
    public void send(Message message) throws IOException {
        client().send(message);
    }

    /**
     * Load info from the lock file
     */
    private void load() throws IOException {
        info = lockFile.read(input -> {
            ProcessInfo processInfo = new ProcessInfo(input.readInt());

            try {
                processInfo.port = input.readInt();
            } catch (EOFException e) {
                processInfo.port = -1;
            }

            return processInfo;
        });
    }
}
