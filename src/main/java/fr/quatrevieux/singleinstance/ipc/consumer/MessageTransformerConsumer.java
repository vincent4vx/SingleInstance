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

package fr.quatrevieux.singleinstance.ipc.consumer;

import fr.quatrevieux.singleinstance.ipc.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link fr.quatrevieux.singleinstance.ipc.InstanceServer} consumer implementation
 * allowing transformation of the received message, based on the message name.
 *
 * Usage:
 * <pre>{@code
 * // Create the consumer
 * MessageTransformerConsumer consumer = new MessageTransformerConsumer(message -> {
 *     // Switch on the message type
 *     if (message instanceof Foo) {
 *         // ...
 *     } else if (message instanceof Bar) {
 *         // ...
 *     } else {
 *         System.out.println("Unsupported message");
 *     }
 * });
 *
 * // Register transformers
 * consumer.register("Foo", message -> new Foo(message.data()));
 * consumer.register("Bar", message -> new Bar(message.data()));
 *
 * // Start consume messages
 * SingleInstance.onMessage(consumer);
 * }</pre>
 *
 * @see fr.quatrevieux.singleinstance.ipc.InstanceServer#consume(Consumer)
 */
final public class MessageTransformerConsumer implements Consumer<Message> {
    final private Consumer<Message> next;
    final private Map<String, Function<Message, Message>> transformers = new HashMap<>();

    public MessageTransformerConsumer(Consumer<Message> next) {
        this.next = next;
    }

    /**
     * Register a new message transformer
     *
     * Usage:
     * <pre>{@code
     * MessageTransformerConsumer consumer = xxx;
     *
     * consumer
     *     .register("Foo", message -> new Foo(message.data()))
     *     .register("Bar", message -> new Bar(message.data()))
     * ;
     * }</pre>
     *
     * @param messageName The message name {@link Message#name()}
     * @param transformer The transformer function. If the transformer returns null, the message will be ignored.
     *
     * @return this instance
     */
    public MessageTransformerConsumer register(String messageName, Function<Message, Message> transformer) {
        transformers.put(messageName, transformer);

        return this;
    }

    @Override
    public void accept(Message message) {
        Function<Message, Message> transformer = transformers.get(message.name());

        if (transformer != null) {
            message = transformer.apply(message);
        }

        if (message != null) {
            next.accept(message);
        }
    }
}
