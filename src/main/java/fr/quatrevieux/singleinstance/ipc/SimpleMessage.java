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

import java.util.Arrays;
import java.util.Objects;

/**
 * Basic implementation for IPC messages
 */
final public class SimpleMessage implements Message {
    final private String name;
    final private byte[] data;

    public SimpleMessage(String name) {
        this(name, new byte[0]);
    }

    public SimpleMessage(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public byte[] data() {
        return data;
    }

    @Override
    public String toString() {
        return "SimpleMessage(" + name + ": " + Arrays.toString(data) + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleMessage that = (SimpleMessage) o;
        return name.equals(that.name) && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
