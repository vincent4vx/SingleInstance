package fr.quatrevieux.singleinstance;


import fr.quatrevieux.singleinstance.ipc.InstanceServer;
import fr.quatrevieux.singleinstance.ipc.Message;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Facade for a simple usage of {@link InstanceManager}
 *
 * Usage:
 * <code>
 *     public static void main(String[] args) {
 *         // Check if an instance is running
 *         SingleInstance.onAlreadyRunning(instance -> {
 *             // Send the argument to the instance and stop the current process
 *             instance.send("Open", args[0].getBytes());
 *             System.exit(0);
 *         });
 *
 *         // Initialize application
 *         MyApp app = xxx;
 *
 *         // Start the IPC server
 *         SingleInstance.onMessage(message -> {
 *             // Receive an "Open" message
 *             if (message.name().equals("Open")) {
 *                 app.open(new String(message.data()));
 *             }
 *         });
 *     }
 * </code>
 */
final public class SingleInstance {
    static private InstanceManager manager;
    static private InstanceServer server;
    static private ExecutorService executor;
    static private DistantInstance distantInstance;
    static private Thread shutdownHook;

    /**
     * Initialize the SingleInstance system
     * Calling this method is not required : any other call will implicitly call this method
     *
     * @throws IOException When error occurs during acquiring the lock
     */
    static public void init() throws IOException {
        init(null);
    }

    /**
     * Initialize the SingleInstance system by specifying the lock file
     *
     * @param lockFile The lock file to use
     *
     * @throws IOException When error occurs during acquiring the lock
     */
    static public void init(LockFile lockFile) throws IOException {
        if (manager != null) {
            return;
        }

        if (lockFile == null) {
            lockFile = new LockFile();
        }

        manager = new InstanceManager(lockFile);
        manager.acquire();

        if (shutdownHook == null) {
            Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread(SingleInstance::close));
        }
    }

    /**
     * Check if the current process is the first running instance
     *
     * Usage:
     * <code>
     *     public static void main(String[] args) {
     *         if (!SingleInstance.isFirst()) {
     *             System.err.println("Already running");
     *             return;
     *         }
     *     }
     * </code>
     *
     * @return true if the current process is the first running instance
     *
     * @throws IOException When cannot initialize system
     * @see SingleInstance#onAlreadyRunning(Consumer) For perform action when an instance is already running
     */
    static public boolean isFirst() throws IOException {
        return manager().acquire();
    }

    /**
     * Start the IPC server if the current process is the first running instance,
     * and handle received messages
     *
     * If the current process is not the first running instance, this method will be a noop.
     *
     * Note: This method will start a new thread to consume messages
     *
     * <code>
     *     SingleInstance.onMessage(message -> {
     *         if (message.name().equals("MyMessage")) {
     *             // Handle message
     *         }
     *     });
     * </code>
     *
     * @param consumer The action to perform when receiving messages
     *
     * @throws IOException When cannot start the server
     * @throws IllegalStateException When the server is already started
     */
    static public void onMessage(Consumer<Message> consumer) throws IOException {
        if (server != null) {
            throw new IllegalStateException("IPC Server is already started");
        }

        manager().server().ifPresent(server -> {
            SingleInstance.server = server;
            executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    server.consume(consumer);
                } catch (IOException e) {
                    e.printStackTrace(); // @todo logger
                }
            });
        });
    }

    /**
     * Perform action is an already running instance is found
     *
     * Usage:
     * <code>
     *     SingleInstance.onAlreadyRunning(instance -> {
     *         instance.send("MyMessage"); // Send a message to the running instance
     *         System.exit(0); // Stop the current process
     *     });
     * </code>
     *
     * @param action Action to perform
     *
     * @throws IOException When cannot initialize the system
     */
    static public void onAlreadyRunning(Consumer<DistantInstance> action) throws IOException {
        if (distantInstance != null) {
            action.accept(distantInstance);
            return;
        }

        manager().find().ifPresent(instance -> {
            distantInstance = instance;
            action.accept(distantInstance);
        });
    }

    /**
     * Get (or create) the instance manager instance
     *
     * @return The manager instance
     * @throws IOException When cannot initialize the system
     */
    static public InstanceManager manager() throws IOException {
        init();

        return manager;
    }

    /**
     * Close the SingleInstance system and release the lock
     * This method should not be called manually : when init is called, a shutdown hook is registered
     */
    static public void close() {
        if (manager == null) {
            return;
        }

        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace(); // @todo logger
            }
            server = null;
        }

        if (executor != null) {
            executor.shutdown();
            executor = null;
        }

        distantInstance = null;

        manager.release();
    }
}