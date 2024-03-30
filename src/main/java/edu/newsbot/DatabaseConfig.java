package edu.newsbot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides configuration details for database connections.
 * This class loads the database connection details from a properties file and
 * offers static methods to access these details. The properties file must be named
 * "db.properties" and located in the classpath.
 */
public class DatabaseConfig {
    private static final Properties properties = new Properties();

    /**
     * Default constructor.
     * Used for initializing the DatabaseConfig class. Properties are loaded
     * statically from the "db.properties" file found in the classpath.
     */
    public DatabaseConfig() {
        super(); // Explicit call to the superclass constructor (Object class here)
    }

    // Static initializer block to load the database properties
    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find db.properties");
                System.exit(1); // Consider changing this to throw an unchecked exception instead of exiting
            }

            // Load the properties file
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace(); // Consider logging this exception or rethrowing it as a unchecked runtime exception
        }
    }

    /**
     * Retrieves the database URL from the properties file.
     *
     * @return A string representing the database URL.
     */
    public static String getDbUrl() {
        return properties.getProperty("db.url");
    }

    /**
     * Retrieves the database username from the properties file.
     *
     * @return A string representing the database username.
     */
    public static String getDbUsername() {
        return properties.getProperty("db.username");
    }

    /**
     * Retrieves the database password from the properties file.
     *
     * @return A string representing the database password.
     */
    public static String getDbPassword() {
        return properties.getProperty("db.password");
    }
}
