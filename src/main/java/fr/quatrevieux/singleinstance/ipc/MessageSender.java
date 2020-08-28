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

package fr.quatrevieux.singleinstance.ipc;

import java.io.IOException;

/**
 * Send IPC message to the distant instance
 */
public interface MessageSender {
    /**
     * Send a simple message with payload
     *
     * @param message The message name
     * @param data The message payload
     *
     * @see SimpleMessage
     * @throws IOException When cannot send message
     */
    default public void send(String message, byte[] data) throws IOException {
        send(new SimpleMessage(message, data));
    }

    /**
     * Send a simple message without payload
     *
     * @param message The message name
     *
     * @see SimpleMessage
     * @throws IOException When cannot send message
     */
    default public void send(String message) throws IOException {
        send(new SimpleMessage(message));
    }

    /**
     * Send a message
     *
     * @param message The message
     *
     * @throws IOException When cannot send message
     */
    public void send(Message message) throws IOException;
}
