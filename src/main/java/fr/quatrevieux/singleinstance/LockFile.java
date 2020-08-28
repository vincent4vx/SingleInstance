package fr.quatrevieux.singleinstance;

import java.io.*;
import java.nio.channels.FileLock;

/**
 * Handle lock file
 * A lock file can be acquire to block all other process, until it's released
 *
 * <code>
 *     LockFile lock = new LockFile("app.lock");
 *
 *     if (!lock.acquire()) {
 *         System.out.println("An other instance is running");
 *         System.exit(1);
 *     }
 *
 *     Runtime.getRuntime().addShutdownHook(new Thread(lock::release));
 * </code>
 *
 * @todo handle multiple instance : release should be performed only if all instance are released
 */
final public class LockFile {
    @FunctionalInterface
    public interface Writer {
        public void write(DataOutput output) throws IOException;
    }

    @FunctionalInterface
    public interface Reader<R> {
        public R read(DataInput input) throws IOException;
    }

    final static public String DEFAULT_FILENAME = ".lock";

    final private File file;

    private RandomAccessFile randomAccessFile;
    private FileLock fileLock;

    /**
     * Create the lock file using the default file name
     */
    public LockFile() {
        this(DEFAULT_FILENAME);
    }

    public LockFile(String filename) {
        this(new File(filename));
    }

    public LockFile(File file) {
        this.file = file;
    }

    /**
     * Acquire the lock file
     * This method is not blocking : if the lock file is already acquired, false is immediately returned
     *
     * @return true if the lock is acquired, or false if the lock is already acquired
     * @throws IOException When lock file creation failed
     */
    public boolean acquire() throws IOException {
        if (fileLock == null) {
            fileLock = file().getChannel().tryLock();
        }

        return fileLock != null;
    }

    /**
     * Wait until lock is release, and then acquire the lock file
     * This method is blocking
     *
     * @throws IOException When lock file creation failed
     */
    public void lock() throws IOException {
        if (fileLock == null) {
            fileLock = file().getChannel().lock();
        }
    }

    /**
     * Release the lock file
     * This method will never fail
     */
    public void release() {
        try {
            boolean deleteFile = false;

            if (fileLock != null) {
                fileLock.release();
                fileLock = null;
                deleteFile = true;
            }

            if (randomAccessFile != null) {
                randomAccessFile.close();
                randomAccessFile = null;
            }

            if (deleteFile) {
                file.delete();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Write data to the lock file
     * The file is truncated before write
     *
     * @param writer Write operation
     * @throws IOException When write failed
     */
    public void write(Writer writer) throws IOException {
        if (!acquire()) {
            throw new IllegalStateException("Cannot acquire write access to lock file");
        }

        randomAccessFile.getChannel().truncate(0);
        writer.write(randomAccessFile);
    }

    /**
     * Read data from lock file
     * The cursor is set to start of the file before read
     *
     * @param reader The data reader
     * @param <R> The return type
     *
     * @return The read data
     * @throws IOException When read failed
     */
    public <R> R read(Reader<R> reader) throws IOException {
        final RandomAccessFile file = file();

        file.seek(0);

        return reader.read(file());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        release();
    }

    private RandomAccessFile file() throws FileNotFoundException {
        if (randomAccessFile == null) {
            randomAccessFile = new RandomAccessFile(file, "rw");
        }

        return randomAccessFile;
    }
}
