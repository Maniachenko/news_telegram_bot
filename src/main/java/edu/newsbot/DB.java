package edu.newsbot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.Collections;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Handles database operations for the application, including connection management,
 * table creation, RSS feed insertion, and DB tables information retrieval.
 */
public class DB {

    /**
     * Default constructor.
     * Used for initializing the DB class. This class provides methods to establish connections to the database.
     */
    public DB() {
        super(); // This call to the superclass constructor is implicit, but you can include it for clarity.
    }

    /**
     * Establishes a connection to the database using credentials (username, password, url) from the DatabaseConfig class.
     *
     * @return A Connection object to the database.
     * @throws SQLException if a database access error occurs or the url is null.
     */
    public static Connection connect() throws SQLException {
        try {
            // Get database credentials from DatabaseConfig class
            var jdbcUrl = DatabaseConfig.getDbUrl();
            var user = DatabaseConfig.getDbUsername();
            var password = DatabaseConfig.getDbPassword();

            // Open a connection
            return DriverManager.getConnection(jdbcUrl, user, password);

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    /**
     * Creates necessary tables in the database if they do not already exist.
     * tables:
     * bot_user (id, telegram_id UNIQUE, sources, age, language, delta_time)
     * science_daily_feeds (id, link, name)
     * article (id, title, summary, link, datetime, full_story, source. related_topics, related_terms,
     * journal_reference, cite_page_mla, cite_page_apa, cite_page_chicago TEXT, related_stories_links TEXT)
     * user_article_status (id, telegram_id REFERENCES bot_user(telegram_id), article_id INT REFERENCES article(id),
     * read_status DEFAULT FALSE, UNIQUE(telegram_id, article_id))
     */
    public static void createTables() {
        String createBotUserTable = """
                CREATE TABLE IF NOT EXISTS bot_user (
                    id SERIAL PRIMARY KEY,
                    telegram_id BIGINT UNIQUE NOT NULL,
                    sources TEXT,
                    age VARCHAR(10),
                    language VARCHAR(10),
                    delta_time INTEGER
                );
                """;

        String createSourcesTable = """
                CREATE TABLE science_daily_feeds (
                    id SERIAL PRIMARY KEY,
                    link VARCHAR(255) NOT NULL,
                    name VARCHAR(100) NOT NULL
                );
                """;

        String createArticleTable = """
                CREATE TABLE IF NOT EXISTS article (
                    id SERIAL PRIMARY KEY,
                    title VARCHAR(255),
                    summary TEXT,
                    link VARCHAR(255) UNIQUE,
                    datetime TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    full_story TEXT,
                    source TEXT,
                    related_topics TEXT,
                    related_terms TEXT,
                    journal_reference TEXT,
                    cite_page_mla TEXT,
                    cite_page_apa TEXT,
                    cite_page_chicago TEXT,
                    related_stories_links TEXT
                );
                """;

        String createUserArticleStatusTable = """
                CREATE TABLE IF NOT EXISTS user_article_status (
                    id SERIAL PRIMARY KEY,
                    telegram_id BIGINT REFERENCES bot_user(telegram_id),
                    article_id INT REFERENCES article(id),
                    read_status BOOLEAN DEFAULT FALSE,
                    UNIQUE(telegram_id, article_id)
                );
                """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createBotUserTable);
            stmt.execute(createSourcesTable);
            stmt.execute(createArticleTable);
            stmt.execute(createUserArticleStatusTable);

            System.out.println("Tables created successfully");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Extracts the name of an RSS (used as a name for sources button in bots settings)
     * feed from its URL, assuming the name is the last segment before ".xml".
     *
     * @param url The URL of the RSS feed.
     * @return The extracted name of the RSS feed.
     */
    private static String extractNameOutOfTheLink(String url) {
        // Assuming the name is the last segment of the URL before ".xml" and after "rss/"
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart.replace(".xml", "");
    }

    /**
     * Reads lines from a file located in the resources folder.
     *
     * @param filePath The path to the file within the resources folder.
     * @return A List of strings, each representing a line from the file.
     */
    private static List<String> readLinesFromResourcesFile(String filePath) {
        try (InputStream input = DatabaseConfig.class.getResourceAsStream(filePath)) {
            if (input == null) {
                return Collections.emptyList(); // Resource not found
            }
            return new BufferedReader(new InputStreamReader(input))
                    .lines()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); // Handle the error appropriately
        }
    }

    /**
     * Inserts RSS feeds into the database from a specified file.
     *
     * @param filePath The path to the file containing RSS feed URLs.
     * @throws SQLException If a database access error occurs.
     */
    public static void insertRssFeedsFromFile(String filePath) throws SQLException {
        List<String> lines = readLinesFromResourcesFile(filePath);
        try (Connection conn = connect()) { // Ensure connect() returns a valid connection
            for (String line : lines) {
                String name = extractNameOutOfTheLink(line);
                // Insert into the database
                try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO science_daily_feeds (link, name) VALUES (?, ?)")) {
                    pstmt.setString(1, line);
                    pstmt.setString(2, name);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error collecting table names: " + e.getMessage());
        }
    }

    /**
     * Retrieves the names of all RSS feeds stored in the database.
     *
     * @return An ArrayList of RSS feed names.
     */
    public static ArrayList<String> getRssFeedsNames() {
        ArrayList<String> rssNames = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet tables = stmt.executeQuery(
                     "SELECT name FROM science_daily_feeds ")) {

            // Collect table names
            while (tables.next()) {
                rssNames.add(tables.getString("name"));
            }

        } catch (SQLException e) {
            System.err.println("Error collecting table names: " + e.getMessage());
        }
        return rssNames;
    }

    /**
     * Retrieves the link for a specific RSS feed by its name.
     *
     * @param name The name of the RSS feed.
     * @return The link of the RSS feed, or null if not found.
     */
    public static String getRssFeedsLinkByName(String name) {
        String rssLink = null; // Initialize rssLink to null
        try (Connection conn = connect()) {
            // Prepare the statement within the try block but outside of the try-with-resources header
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM science_daily_feeds WHERE name = ?;");
            pstmt.setString(1, name); // Correctly use setString for a String parameter
            try (ResultSet rs = pstmt.executeQuery()) { // Correctly place ResultSet in try-with-resources

                // Collect the RSS link
                if (rs.next()) { // Use if instead of while if you're expecting a single result
                    rssLink = rs.getString("link"); // Fetch the correct column, assuming it's called "rss_link"
                }

            } catch (SQLException e) {
                System.err.println("Error executing query: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
        }
        return rssLink; // Return the rssLink, which will be null if not found or in case of an error
    }

    /**
     * Clears all tables in the database, effectively resetting the database.
     */
    public static void clearAllTables() {
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet tables = stmt.executeQuery(
                     "SELECT tablename FROM pg_catalog.pg_tables " +
                             "WHERE schemaname != 'pg_catalog' AND schemaname != 'information_schema';")) {

            // Collect table names
            while (tables.next()) {
                tableNames.add(tables.getString("tablename"));
            }

        } catch (SQLException e) {
            System.err.println("Error collecting table names: " + e.getMessage());
        }

        // Drop tables
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            for (String tableName : tableNames) {
                // Properly quote tableName to prevent SQL injection
                stmt.execute("DROP TABLE IF EXISTS \"" + tableName + "\" CASCADE;");
            }
            System.out.println("All tables cleared successfully");
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
        }
    }

    /**
     * Retrieves information about a user from the database based on their user ID.
     *
     * @param userId The user's Telegram ID.
     * @return A Map containing user information, or null if the user is not found.
     */
    public static Map<String, Object> getUserInfoByUserId(long userId) {
        Map<String, Object> userInfo = new HashMap<>();

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement("""
                     SELECT * FROM bot_user WHERE telegram_id = ?;
                     """)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Long telegramId = rs.getLong("telegram_id");
                userInfo.put("telegram_id", rs.wasNull() ? null : telegramId);

                String sources = rs.getString("sources");
                userInfo.put("sources", sources); // getString can return null directly

                String age = rs.getString("age");
                userInfo.put("age", age);

                String language = rs.getString("language");
                userInfo.put("language", language); // Again, getString can return null

                int deltaTime = rs.getInt("delta_time");
                userInfo.put("delta_time", rs.wasNull() ? null : deltaTime);

                return userInfo;
            } else {
                return null;
            }
        } catch (SQLException e) {
            System.err.println("SQL error occurred: " + e.getMessage());
            return null; // Return null or consider throwing a custom exception
        }
    }

    /**
     * Sets the value of a parameter in a PreparedStatement based on the SQL type provided.
     *
     * @param pstmt the PreparedStatement to modify
     * @param index the index of the parameter to set in prepareStatement
     * @param value the value of the parameter as a String
     * @param sqlType the SQL type of the parameter
     * @throws SQLException if a database access error occurs or this method is called on a closed PreparedStatement
     */
    private static void setPreparedStatementValue(PreparedStatement pstmt, int index, String value, int sqlType) throws SQLException {
        switch (sqlType) {
            case java.sql.Types.INTEGER:
                pstmt.setInt(index, Integer.parseInt(value));
                break;
            case java.sql.Types.BIGINT:
                pstmt.setLong(index, Long.parseLong(value));
                break;
            default:
                pstmt.setString(index, value);
        }
    }

    /**
     * Updates or inserts user information into the database. Used for registration and settings changes.
     *
     * @param updateData a Map containing the data to update or insert
     */
    public static void updateOrInsertUserInfo(Map<String, String> updateData) {
        if (!updateData.containsKey("telegram_id")) {
            throw new IllegalArgumentException("Update data must contain 'telegram_id'.");
        }

        String tableName = "bot_user"; // Specify your table name here

        try (Connection conn = connect()) {

            if (updateData.containsKey("sources")) {
                // Directly fetch and update the 'sources' entry with the RSS link
                updateData.put("sources", getRssFeedsLinkByName(updateData.get("sources")));
            }

            // Prepare column types info
            Map<String, Integer> columnTypes = new HashMap<>();
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
                while (columns.next()) {
                    columnTypes.put(columns.getString("COLUMN_NAME"), columns.getInt("DATA_TYPE"));
                }
            }

            // Fetch telegram_id once and use it throughout
            Long telegramId = Long.parseLong(updateData.remove("telegram_id"));

            // Construct SQL statement as before
            String columnsPart = String.join(", ", updateData.keySet()) + ", telegram_id";
            String placeholders = updateData.keySet().stream().map(key -> "?").collect(Collectors.joining(", ")) + ", ?";
            String updates = updateData.keySet().stream().map(key -> key + " = EXCLUDED." + key).collect(Collectors.joining(", "));
            String sql = "INSERT INTO " + tableName + " (" + columnsPart + ") VALUES (" + placeholders + ") ON CONFLICT (telegram_id) DO UPDATE SET " + updates + ";";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int index = 1;
                for (String key : updateData.keySet()) {
                    setPreparedStatementValue(pstmt, index++, updateData.get(key), columnTypes.getOrDefault(key, java.sql.Types.VARCHAR));
                }
                pstmt.setLong(index, telegramId); // Set telegram_id at the end

                // Execute and handle the update or insert
                if (pstmt.executeUpdate() > 0) {
                    System.out.println("User info upserted successfully.");
                }

                // Determine if there's a need to reschedule tasks based on the presence of 'delta_time' or 'sources' in the updateData
                boolean shouldReschedule = updateData.containsKey("delta_time") || updateData.containsKey("sources");

                if (shouldReschedule) {
                    rescheduleTasks(telegramId);
                }

            }
        } catch (SQLException e) {
            // Handle SQLException here
            System.err.println("SQL error occurred during the upsert: " + e.getMessage());
        }
    }

    /**
     * Reschedules tasks based on updated user information.
     *
     * @param telegramId the telegram ID of the user
     * @throws SQLException if a database access error occurs
     */
    public static void rescheduleTasks(Long telegramId) throws SQLException {
        Map<String, Object> userInfo = getUserInfoByUserId(telegramId);

        // Fetch the latest 'delta_time' and 'sources' from userInfo after the update operation
        String delta_time = userInfo.get("delta_time").toString();
        Object sourcesObject = userInfo.get("sources");

        // Only proceed if 'delta_time' is available and 'sources' is not null
        if (delta_time != null && sourcesObject != null) {
            String link = sourcesObject.toString();

            // Convert 'delta_time' to seconds for scheduling
            long delayInSeconds = TimeUnit.MINUTES.toSeconds(Long.parseLong(delta_time));

            // Reschedule the link parsing task with the new delay
            ParsingSendingScheduler.scheduleLinkParsing(link, delayInSeconds, TimeUnit.SECONDS, telegramId);
        }
    }


    /**
     * Saves an article to the database and updates the user-article status accordingly.
     *
     * @param primaryUrl the primary URL associated with the article
     * @param dataMap a Map containing the article data
     */
    public static void saveArticleToDatabase(String primaryUrl, Map<String, String> dataMap) {

        String insertArticleSQL = """
                INSERT INTO article (title, summary, link, full_story, source, related_topics, related_terms, journal_reference, cite_page_mla, cite_page_apa, cite_page_chicago, related_stories_links) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                ON CONFLICT (link) DO NOTHING RETURNING id;
                """;

        String selectUsersSQL = """
                SELECT telegram_id FROM bot_user 
                WHERE sources = ?;
                """;


        String insertStatusSQL = """
                INSERT INTO user_article_status (telegram_id, article_id, read_status) 
                VALUES (?, ?, FALSE) 
                ON CONFLICT (telegram_id, article_id) DO NOTHING;
                """;

        try (Connection conn = connect();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM article WHERE link = ?"); // It's better to check by link than by title
             PreparedStatement insertStmt = conn.prepareStatement(insertArticleSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement selectUsersStmt = conn.prepareStatement(selectUsersSQL);
             PreparedStatement insertStatusStmt = conn.prepareStatement(insertStatusSQL)) {

            // Initially, check if the article exists and get its ID if it does
            int articleId = -1; // Default value indicating not found
            checkStmt.setString(1, dataMap.get("link"));
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    articleId = rs.getInt("id"); // Article exists, get its ID
                } else {
                    // Article doesn't exist, try inserting it
                    int index = 1;
                    for (String key : new String[]{"title", "summary", "link", "full_story", "source", "related_topics", "related_terms", "journal_reference", "cite_page_mla", "cite_page_apa", "cite_page_chicago", "related_stories_links"}) {
                        insertStmt.setString(index++, dataMap.get(key));
                    }
                    insertStmt.executeUpdate();
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            articleId = generatedKeys.getInt(1); // Get the newly inserted article ID
                        }
                    }
                }
            }

            // If we have an article ID, either from insertion or pre-existence
            if (articleId != -1) {
                // Select users to associate with the article
                selectUsersStmt.setString(1, primaryUrl); // Adjust as necessary for your logic
                try (ResultSet usersRs = selectUsersStmt.executeQuery()) {
                    while (usersRs.next()) {
                        long userId = usersRs.getLong("telegram_id");
                        // Insert user-article association, avoiding duplicates
                        insertStatusStmt.setLong(1, userId);
                        insertStatusStmt.setInt(2, articleId);
                        insertStatusStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error occurred: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Retrieves the first unread article for a given user, along with the total number of unread articles.
     *
     * @param telegramId The telegram ID of the user for whom to retrieve the article.
     * @return A map containing details of the first unread article and the count of unread articles.
     */
    public static Map<String, String> getFirstUnreadArticleForUser(long telegramId) {
        // SQL to find the first unread article for the given user, selecting all columns
        String fetchSql = """
                SELECT a.*
                FROM article a
                JOIN user_article_status uas ON a.id = uas.article_id
                WHERE uas.telegram_id = ? AND uas.read_status = FALSE
                ORDER BY a.datetime ASC
                LIMIT 1;
                """;

        // SQL to count the total number of unread articles for the user
        String countSql = """
                SELECT COUNT(*)
                FROM user_article_status
                WHERE telegram_id = ? AND read_status = FALSE;
                """;

        // Initialize an empty map to hold the article details
        Map<String, String> articleDetails = new HashMap<>();

        try (Connection conn = connect();
             PreparedStatement fetchStmt = conn.prepareStatement(fetchSql);
             PreparedStatement countStmt = conn.prepareStatement(countSql)) {

            // Set the telegramId parameter for both statements
            fetchStmt.setLong(1, telegramId);
            countStmt.setLong(1, telegramId);

            // Execute the fetch query
            try (ResultSet rs = fetchStmt.executeQuery()) {
                // Check if an unread article exists
                if (rs.next()) {
                    // Extract article details and put them in the map
                    articleDetails.put("id", String.valueOf(rs.getLong("id")));
                    articleDetails.put("title", rs.getString("title"));
                    articleDetails.put("summary", rs.getString("summary"));
                    articleDetails.put("link", rs.getString("link"));
                    articleDetails.put("datetime", rs.getString("datetime"));
                    articleDetails.put("full_story", rs.getString("full_story"));
                    articleDetails.put("source", rs.getString("source"));
                    articleDetails.put("related_topics", rs.getString("related_topics"));
                    articleDetails.put("related_terms", rs.getString("related_terms"));
                    articleDetails.put("journal_reference", rs.getString("journal_reference"));
                    articleDetails.put("cite_page_mla", rs.getString("cite_page_mla"));
                    articleDetails.put("cite_page_apa", rs.getString("cite_page_apa"));
                    articleDetails.put("cite_page_chicago", rs.getString("cite_page_chicago"));
                    articleDetails.put("related_stories_links", rs.getString("related_stories_links"));
                } else {
                    // No unread articles found, put a placeholder message
                    articleDetails.put("message", "No unread articles available.");
                }
            }

            // Execute the count query
            try (ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    int unreadCount = rs.getInt(1); // Get the count of unread articles
                    articleDetails.put("unread_number", String.valueOf(unreadCount));
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error occurred: " + e.getMessage());
            articleDetails.put("error", "An error occurred while fetching the article.");
        }

        return articleDetails;
    }

    /**
     * Marks the first unread article as read for a specific user.
     * This method updates the read status of the user's first unread article in the database.
     *
     * @param telegramId The telegram ID of the user for whom the article should be marked as read.
     */
    public static void markFirstUnreadArticleAsRead(long telegramId) {
        // SQL to find the first unread article for the given user
        String fetchSql = """
                SELECT a.id
                FROM article a
                JOIN user_article_status uas ON a.id = uas.article_id
                WHERE uas.telegram_id = ? AND uas.read_status = FALSE
                ORDER BY a.datetime ASC
                LIMIT 1;
                """;

        // SQL to update the read status of the article
        String updateSql = """
                UPDATE user_article_status
                SET read_status = TRUE
                WHERE telegram_id = ? AND article_id = ?;
                """;

        try (Connection conn = connect();
             PreparedStatement fetchStmt = conn.prepareStatement(fetchSql)) {

            // Set the telegramId parameter for fetching
            fetchStmt.setLong(1, telegramId);

            try (ResultSet rs = fetchStmt.executeQuery()) {
                // Check if an unread article exists
                if (rs.next()) {
                    long articleId = rs.getLong("id");

                    // Proceed to mark the article as read
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        // Set parameters for updating
                        updateStmt.setLong(1, telegramId);
                        updateStmt.setLong(2, articleId);

                        updateStmt.executeUpdate();
                        // Article is marked as read, no return value needed
                    }
                }
                // If no unread articles are found, the method simply completes without action
            }
        } catch (SQLException e) {
            System.err.println("SQL error occurred: " + e.getMessage());
        }
    }

    /**
     * Marks all unread articles as read for a specific user.
     * This method updates the read status of all the user's unread articles in the database.
     *
     * @param telegramId The telegram ID of the user for whom all unread articles should be marked as read.
     */
    public static void markAllUnreadArticlesAsRead(long telegramId) {
        // SQL to update the read status of all unread articles for the user
        String updateSql = """
                UPDATE user_article_status
                SET read_status = TRUE
                WHERE telegram_id = ? AND read_status = FALSE;
                """;

        try (Connection conn = connect();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            // Set the telegramId parameter
            updateStmt.setLong(1, telegramId);

            // Execute the update
            updateStmt.executeUpdate();
            // All unread articles for the user are now marked as read
        } catch (SQLException e) {
            System.err.println("SQL error occurred: " + e.getMessage());
        }
    }

    /**
     * Deletes all articles associated with a specific user.
     * This method removes all user-article associations for a given user from the database.
     *
     * @param telegramId The telegram ID of the user for whom all articles should be deleted.
     */
    public static void deleteAllUserArticles(long telegramId) {
        // SQL to delete all articles for the user
        String deleteSql = """
                DELETE FROM user_article_status
                WHERE telegram_id = ?;
                """;

        try (Connection conn = connect();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

            // Set the telegramId parameter
            deleteStmt.setLong(1, telegramId);

            // Execute the delete
            deleteStmt.executeUpdate();
            // All articles for the user are now deleted
        } catch (SQLException e) {
            System.err.println("SQL error occurred: " + e.getMessage());
        }
    }
}
