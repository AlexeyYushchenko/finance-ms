package ru.utlc.integration;

import org.testcontainers.containers.PostgreSQLContainer;

public class SingletonPostgresContainer extends PostgreSQLContainer<SingletonPostgresContainer> {
    private static final String IMAGE_VERSION = "postgres:15.2";
    private static SingletonPostgresContainer container;

    private SingletonPostgresContainer() {
        super(IMAGE_VERSION);
    }

    public static SingletonPostgresContainer getInstance() {
        if (container == null) {
            container = new SingletonPostgresContainer();
            container.start();
        }
        return container;
    }

    @Override
    public void stop() {
        // Do nothing. Let the JVM shutdown handle stopping the container.
    }
}
