package edu.newsbot;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Entry point of the NewsBot application.
 * This class handles the initial database connection setup, table creation, and scheduling of periodic tasks
 * for getting bots messages updates from users. It also ensures graceful shutdown of resources.
 */
public class Main {

    /**
     * Default constructor.
     * Initializes the Main class, which serves as the entry point for the NewsBot application.
     */
    public Main() {
        super(); // This call is implicit even if you omit it, but it can be included for clarity.
    }

    /**
     * Main method that initializes the database connection, sets up scheduled tasks for getting bots messages updates from users
     *, and manages application lifecycle.
     *
     * @param args Command-line arguments passed to the program (not used).
     */
    public static void main(String[] args) {
        // Try-with-resources block to automatically close the database connection
        try (var connection = DB.connect()) {
            System.out.println("Connected to the PostgreSQL database.");
        } catch (SQLException e) {
            // If connecting to the database fails, print an error message
            System.err.println(e.getMessage());
        }

        // Clear all existing tables to reset the database state
        DB.clearAllTables();
        // Create new tables as defined in the DB class
        DB.createTables();

        // Attempt to insert RSS feeds into the database from a file
        try {
            DB.insertRssFeedsFromFile("/links.txt");
        } catch (SQLException e) {
            // Handle any SQL errors that occur during the insertion process
            System.err.println("SQL error occurred: " + e.getMessage());
            e.printStackTrace();
        }

        // Create a single-threaded ScheduledExecutorService to manage scheduled tasks
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        // Schedule the NewsBot.getUpdates method to run every 2 seconds, starting immediately
        executorService.scheduleAtFixedRate(NewsBot::getUpdates, 0, 2, TimeUnit.SECONDS);

        // Add a shutdown hook to properly shut down the executor service when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            // Attempt to stop all actively executing tasks and halt the processing of waiting tasks
            executorService.shutdownNow();
            // Close any resources (like HTTP clients) used by the NewsBot
            NewsBot.closeClient();
        }));
    }
}
