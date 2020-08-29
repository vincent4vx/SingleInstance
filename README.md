# SingleInstance
[![Build Status](https://scrutinizer-ci.com/g/vincent4vx/SingleInstance/badges/build.png?b=master)](https://scrutinizer-ci.com/g/vincent4vx/SingleInstance/build-status/master) [![Scrutinizer Code Quality](https://scrutinizer-ci.com/g/vincent4vx/SingleInstance/badges/quality-score.png?b=master)](https://scrutinizer-ci.com/g/vincent4vx/SingleInstance/?branch=master) [![Code Coverage](https://scrutinizer-ci.com/g/vincent4vx/SingleInstance/badges/coverage.png?b=master)](https://scrutinizer-ci.com/g/vincent4vx/SingleInstance/?branch=master) [![javadoc](https://javadoc.io/badge2/fr.quatrevieux/single-instance/javadoc.svg)](https://javadoc.io/doc/fr.quatrevieux/single-instance) [![Maven Central](https://img.shields.io/maven-central/v/fr.quatrevieux/single-instance)](https://search.maven.org/artifact/fr.quatrevieux/single-instance) 
 
Java library for prevent running multiple instances of an application, and offer a communication with the first instance.

It's based on the use of a lock file with exclusive lock to ensure that only the first process can write into this file.
For the IPC communication, a simple single thread socket server is start by the first process, and the port number is written 
on the lock file.

## Installation

For installing using maven, add this dependency into the `pom.xml` :

```xml
<dependency>
    <groupId>fr.quatrevieux</groupId>
    <artifactId>single-instance</artifactId>
    <version>1.0</version>
</dependency>
```

## Usage

### Basic usage

Use [SingleInstance](src/main/java/fr/quatrevieux/singleinstance/SingleInstance.java)`#onAlreadyRunning()` to check if an instance is running, and send a message.
Then start the IPC server using `SingleInstance#onMessages()`.

```java
/**
 * Forward the arguments to the first running application
 */
public class MyApp {
    public static void main(String[] args) {
        // Check if there is a running instance
        SingleInstance.onAlreadyRunning(instance -> {
            try {
                // Forward arguments to the instance
                for (String arg : args) {
                    instance.send("Open", arg.getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Quit the process
            System.exit(0);
        });

        // We are on the first running instance here : initialize application
        MyApp app = new MyApp();

        // Start the IPC server and consume messages
        SingleInstance.onMessage(message -> {
            // Handle the "Open" message
            if (message.name().equals("Open")) {
                app.open(new String(message.data()));
            }
        });

        // Continue execution
        for (String arg : args) {
            app.open(arg);
        }
    }

    public void open(String argument) {
        // ...
    }
}
```

## Licence

This project is licensed under the LGPLv3 licence. See [COPYING](./COPYING) and [COPYING.LESSER](./COPYING.LESSER) files for details.
