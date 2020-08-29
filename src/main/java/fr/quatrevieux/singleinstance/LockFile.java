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

import java.io.*;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handle lock file
 * A lock file can be acquire to block all other process, until it's released
 *
 * <pre>{@code
 *     LockFile lock = new LockFile("app.lock");
 *
 *     if (!lock.acquire()) {
 *         System.out.println("An other instance is running");
 *         System.exit(1);
 *     }
 *
 *     Runtime.getRuntime().addShutdownHook(new Thread(lock::release));
 * }</pre>
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

    /**
     * Handle sharing the FileLock instances
     */
    static private class SharedLock {
        @FunctionalInterface
        private interface FileLockFactory {
            public FileLock create() throws IOException;
        }

        /**
         * Store all lock instances
         */
        final static private Map<File, SharedLock> instances = new HashMap<>();

        final private File file;
        final private FileLock fileLock;
        private int count = 1;

        public SharedLock(File file, FileLock fileLock) {
            this.file = file;
            this.fileLock = fileLock;
        }

        /**
         * Try to remove the shared lock
         *
         * @return true if the lock is removed
         */
        private boolean remove() throws IOException {
            synchronized (instances) {
                --count;

                if (count <= 0) {
                    fileLock.release();
                    instances.remove(file);
                    return true;
                }

                return false;
            }
        }

        /**
         * Create the SharedLock instance
         *
         * @return The lock instance, or null if cannot get the lock
         */
        static private SharedLock create(File file, FileLockFactory factory) throws IOException {
            synchronized (instances) {
                SharedLock sharedLock = instances.get(file);

                if (sharedLock != null) {
                    ++sharedLock.count;
                    return sharedLock;
                }

                FileLock lock = factory.create();

                if (lock == null) {
                    return null;
                }

                sharedLock = new SharedLock(file, lock);
                instances.put(file, sharedLock);

                return sharedLock;
            }
        }
    }

    final static public String DEFAULT_FILENAME = ".lock";

    final private File file;

    private RandomAccessFile randomAccessFile;
    private SharedLock lock;

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
        this.file = file.getAbsoluteFile();
    }

    /**
     * Acquire the lock file
     * This method is not blocking : if the lock file is already acquired, false is immediately returned
     *
     * @return true if the lock is acquired, or false if the lock is already acquired
     * @throws IOException When lock file creation failed
     */
    public boolean acquire() throws IOException {
        if (lock == null) {
            lock = SharedLock.create(file, () -> file().getChannel().tryLock());
        }

        return lock != null;
    }

    /**
     * Wait until lock is release, and then acquire the lock file
     * This method is blocking
     *
     * @throws IOException When lock file creation failed
     */
    public void lock() throws IOException {
        if (lock == null) {
            lock = SharedLock.create(file, () -> file().getChannel().lock());
        }
    }

    /**
     * Release the lock file
     * This method will never fail
     */
    public void release() {
        try {
            boolean deleteFile = false;

            if (lock != null) {
                deleteFile = lock.remove();
                lock = null;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LockFile lockFile = (LockFile) o;
        return file.equals(lockFile.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }

    private RandomAccessFile file() throws FileNotFoundException {
        if (randomAccessFile == null) {
            randomAccessFile = new RandomAccessFile(file, "rw");
        }

        return randomAccessFile;
    }
}
