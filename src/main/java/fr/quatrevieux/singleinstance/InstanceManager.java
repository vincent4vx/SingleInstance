package fr.quatrevieux.singleinstance;

import fr.quatrevieux.singleinstance.ipc.InstanceServer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Optional;

/**
 * Handle single instance system
 *
 * Usage :
 * <code>
 *     public static void main(String[] args) {
 *         // Check single instance
 *         InstanceManager manager = new InstanceManager();
 *
 *         manager.find().ifPresent(distant -> {
 *             // A running instance is present : send a message and close the application
 *             distant.send("My message", args[0].getBytes());
 *             System.exit(0);
 *         });
 *
 *         // Initialize application
 *         MyApp app = xxx;
 *
 *         // Start the IPC server
 *         manager.server().ifPresent(server -> {
 *             server.consume(message -> app.handleMessage(message));
 *         });
 *     }
 * </code>
 */
final public class InstanceManager {
    final private LockFile lockFile;

    public InstanceManager() {
        this(new LockFile());
    }

    public InstanceManager(LockFile lockFile) {
        this.lockFile = lockFile;
    }

    /**
     * Try to acquire the single instance (i.e. the lock file)
     * If the lock is acquired, the PID will be written on
     *
     * @return true if the current process as acquire the lock
     *
     * @throws IOException When cannot access to lock file
     */
    public boolean acquire() throws IOException {
        if (lockFile.acquire()) {
            lockFile.write(output -> output.writeInt(getCurrentPid()));
            return true;
        }

        return false;
    }

    /**
     * Try to get the distant instance (the first run process)
     *
     * Usage:
     * <code>
     *     InstanceManager im = new InstanceManager();
     *     im.find().ifPresent(distant -> {
     *         System.out.println("Process already running");
     *         // Send a message to the first process
     *         distant.send("Open", args[0].getBytes());
     *         System.exit(0);
     *     });
     * </code>
     *
     * @return An empty optional if the current process is the first running, or the distant instance if exists
     *
     * @throws IOException When cannot access to lock file
     */
    public Optional<DistantInstance> find() throws IOException {
        return acquire() ? Optional.empty() : Optional.of(new DistantInstance(lockFile));
    }

    /**
     * Try to open the IPC server
     * If the server is successfully opened, the port number will be written on the lock file
     *
     * Usage:
     * <code>
     *     InstanceManager im = new InstanceManager();
     *     im.server().ifPresent(server -> {
     *         server.consume(message -> {
     *             // Process received messages
     *             if (message.name().equals(xxx)) {
     *                 // ...
     *             }
     *         });
     *     });
     * </code>
     *
     * @return An empty optional if the current process is not the first running instance
     *
     * @throws IOException When cannot open the server
     */
    public Optional<InstanceServer> server() throws IOException {
        if (!acquire()) {
            return Optional.empty();
        }

        InstanceServer server = new InstanceServer();
        server.open();
        lockFile.write(output -> {
            output.writeInt(getCurrentPid());
            output.writeInt(server.port());
        });

        return Optional.of(server);
    }

    /**
     * Release the lock file
     */
    public void release() {
        lockFile.release();
    }

    private int getCurrentPid() {
        return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@", 2)[0]);
    }
}
