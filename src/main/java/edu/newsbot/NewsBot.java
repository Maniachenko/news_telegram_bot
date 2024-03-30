package edu.newsbot;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.Collections;
import java.io.InputStream;


import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.sql.SQLException;
import java.util.UUID;

/**
 * The {@code NewsBot} class represents the main functionality for a Telegram bot.
 * It includes methods for sending messages, handling user commands, and managing bot properties.
 * <p>
 * This class uses the Telegram Bot API to interact with users by sending messages,
 * processing commands, and responding to user actions. It requires a bot.properties
 * file with a valid Telegram bot token to authenticate API requests.
 */
public class NewsBot {

    private static String token;
    private static WebTarget baseTarget;
    private static Client client = ClientBuilder.newClient();
    private static final int MAX_MESSAGE_LENGTH = 4096; // Adjust based on your platform's limit

    static {
        loadProperties();
        initClient();
    }

    /**
     * Loads bot properties from the configuration file.
     */
    private static void loadProperties() {
        try (InputStream input = NewsBot.class.getClassLoader().getResourceAsStream("bot.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.err.println("Sorry, unable to find bot.properties");
                return;
            }
            prop.load(input);
            token = prop.getProperty("telegram.token");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Initialization of base target url with bots token.
     */
    private static void initClient() {
        Client client = ClientBuilder.newClient();
        baseTarget = client.target("https://api.telegram.org/bot{token}")
                .resolveTemplate("token", token);
    }

    /**
     * Sends a message to a user with optional reply markup (inline or reply telegram keyboard) in JSON format.
     *
     * @param message The message text to be sent.
     * @param userId The telegram user ID to whom the message will be sent.
     * @param replyMarkupJson The JSON string representing the reply markup (keyboard).
     */
    public static void sendMessage(String message, long userId, String replyMarkupJson) {
        try {
            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("chat_id", userId);
            if (message != null && !message.isEmpty()) {
                jsonBody.addProperty("text", message);
            } else {
                jsonBody.addProperty("text", " "); // Placeholder text or adjust as needed
            }
            if (replyMarkupJson != null && !replyMarkupJson.isEmpty()) {
                jsonBody.add("reply_markup", JsonParser.parseString(replyMarkupJson).getAsJsonObject());
            }
            jsonBody.addProperty("parse_mode", "Markdown");

            Response response = baseTarget.path("sendMessage")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jsonBody.toString(), MediaType.APPLICATION_JSON));

            String jsonString = response.readEntity(String.class);
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

            if (jsonObject.get("ok").getAsBoolean()) {
                JsonObject result = jsonObject.getAsJsonObject("result");
            } else {
                System.err.println("Couldn't successfully send message");
            }
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deletes a message for a user.
     *
     * @param userId The telegram user ID from whom the message will be deleted.
     * @param messageId The ID of the message to delete.
     */
    public static void deleteMessage(long userId, long messageId) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("chat_id", userId);
        jsonBody.addProperty("message_id", messageId);


        Response response = baseTarget.path("deleteMessage")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(jsonBody.toString(), MediaType.APPLICATION_JSON));
    }

    /**
     * Edits an existing message.
     *
     * @param newText The new text to replace the existing message.
     * @param userId The user ID for whom the message will be edited.
     * @param messageId The ID of the message to edit.
     * @param replyMarkupJson JSON string for the new reply markup (keyboard) to be attached with the edited message.
     */
    public static void editMessage(String newText, long userId, long messageId, String replyMarkupJson) {
        try {
            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("chat_id", userId);
            jsonBody.addProperty("message_id", messageId);
            jsonBody.addProperty("text", newText);
            if (replyMarkupJson != null && !replyMarkupJson.isEmpty()) {
                jsonBody.add("reply_markup", JsonParser.parseString(replyMarkupJson).getAsJsonObject());
            }
            jsonBody.addProperty("parse_mode", "Markdown");

            Response response = baseTarget.path("editMessageText")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jsonBody.toString(), MediaType.APPLICATION_JSON));

            String jsonString = response.readEntity(String.class);
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

            if (!jsonObject.get("ok").getAsBoolean()) {
                System.err.println("Couldn't successfully edit message. Error: " + jsonObject.toString());
            } else {
                System.out.println("Message edited successfully.");
            }

        } catch (Exception e) {
            System.err.println("Error editing message with keyboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates a keyboard layout for selecting language preferences.
     *
     * @return A JSON string representing the keyboard layout for language selection.
     */
    public static String getLanguageKeyboardArray() {
        JsonArray keyboardArray = new JsonArray();
        JsonArray languageRow = new JsonArray();
        JsonObject engButton = new JsonObject();
        engButton.addProperty("text", "ENG");
        engButton.addProperty("callback_data", "bot_user|language|eng");
        languageRow.add(engButton);

        JsonObject rusButton = new JsonObject();
        rusButton.addProperty("text", "RUS");
        rusButton.addProperty("callback_data", "bot_user|language|rus");
        languageRow.add(rusButton);

        keyboardArray.add(languageRow);
        JsonObject replyMarkup = new JsonObject();
        replyMarkup.add("inline_keyboard", keyboardArray);
        return replyMarkup.toString();
    }

    /**
     * Generates a keyboard layout for selecting age groups.
     *
     * @return A JSON string representing the keyboard layout for age group selection.
     */
    public static String getAgeKeyboardArray() {
        JsonArray keyboardArray = new JsonArray();
        JsonArray ageRows = new JsonArray();
        String[] ageGroups = {"Under 18", "18-24", "25-34", "35-44", "45-54", "55 and older"};
        for (String ageGroup : ageGroups) {
            JsonArray ageRow = new JsonArray();
            JsonObject ageButton = new JsonObject();
            ageButton.addProperty("text", ageGroup);
            ageButton.addProperty("callback_data", "bot_user|age|" + ageGroup);
            ageRow.add(ageButton);
            ageRows.add(ageRow);
        }
        keyboardArray = ageRows; // Replace the main keyboard array with age-specific rows
        JsonObject replyMarkup = new JsonObject();
        replyMarkup.add("inline_keyboard", keyboardArray);
        return replyMarkup.toString();
    }

    /**
     * Generates a keyboard layout for selecting update frequency (delta time).
     *
     * @return A JSON string representing the keyboard layout for delta time selection.
     */
    public static String getDeltaTimeKeyboardArray() {
        JsonArray keyboardArray = new JsonArray();
        JsonArray timeRows = new JsonArray();
        String[] timeOptions = {"1", "30", "45", "60", "75", "90"};
        for (String timeOption : timeOptions) {
            JsonArray timeRow = new JsonArray();
            JsonObject timeButton = new JsonObject();
            timeButton.addProperty("text", timeOption);
            timeButton.addProperty("callback_data", "bot_user|delta_time|" + timeOption);
            timeRow.add(timeButton);
            timeRows.add(timeRow);
        }
        keyboardArray = timeRows; // Replace the main keyboard array with time-specific rows
        JsonObject replyMarkup = new JsonObject();
        replyMarkup.add("inline_keyboard", keyboardArray);
        return replyMarkup.toString();
    }

    /**
     * Generates a keyboard layout for selecting news sources (In future will be replaced by webhook query search).
     *
     * @return A JSON string representing the keyboard layout for source selection.
     */
    public static String getSourcesKeyboardArray() {
        JsonArray keyboardArray = new JsonArray();
        JsonArray sourceRow = new JsonArray();

        // Assuming you have a method to read lines from a file into a List
        ArrayList<String> lines = DB.getRssFeedsNames();

        for (String line : lines) {
            String buttonName = line;
            // Create the button object
            JsonObject button = new JsonObject();
            button.addProperty("text", buttonName);
            button.addProperty("callback_data", "bot_user|sources|" + line); // Use the full line as callback data
            // Add the button to the sourceRow
            sourceRow.add(button);
        }

        // Add the sourceRow to the keyboardArray
        keyboardArray.add(sourceRow);
        JsonObject replyMarkup = new JsonObject();
        replyMarkup.add("inline_keyboard", keyboardArray);
        return replyMarkup.toString();
    }

    /**
     * Generates a keyboard layout for navigating through news articles.
     *
     * @param isFullArticle Indicates if the full article is displayed. Useful for showing a "More Info" button.
     * @param isFirstAfterParsing True if it's the first article shown after parsing, enabling a "Read Articles" button instead of "Next Article".
     * @param isSettings True if settings options should be displayed, which includes "Change ..." buttons without news navigation, useful when there's no new news to display.
     * @return JSON string representing the keyboard layout for news navigation.
     */
    public static String getNewsKeyboardArray(boolean isFullArticle, boolean isFirstAfterParsing, boolean isSettings) {
        JsonArray keyboardArray = new JsonArray();

        // First Row
        JsonArray firstRow = new JsonArray();
        if (!isSettings) {
            if (isFirstAfterParsing) {
                JsonObject articleButton = new JsonObject();
                articleButton.addProperty("text", "Read Articles");
                firstRow.add(articleButton);
            } else {
                JsonObject nextButton = new JsonObject();
                nextButton.addProperty("text", "Next Article");
                firstRow.add(nextButton);
            }
            // Add the first row to the keyboard
            keyboardArray.add(firstRow);

            // Second Row (if applicable)
            if (!isFullArticle && !isFirstAfterParsing) {
                JsonArray secondRow = new JsonArray();
                JsonObject moreButton = new JsonObject();
                moreButton.addProperty("text", "More Info");
                secondRow.add(moreButton);
                // Add the second row to the keyboard
                keyboardArray.add(secondRow);
            }

            JsonArray thirdRow = new JsonArray();
            JsonObject clearButton = new JsonObject();
            clearButton.addProperty("text", "Clear");
            thirdRow.add(clearButton);

            // Add the third row to the keyboard
            keyboardArray.add(thirdRow);
        }

        // Fourth Row
        JsonArray fourthRow = new JsonArray();
        JsonObject sourceButton = new JsonObject();
        sourceButton.addProperty("text", "Change Source");
        fourthRow.add(sourceButton);

        JsonObject deltaTimeButton = new JsonObject();
        deltaTimeButton.addProperty("text", "Change Parsing Time");
        fourthRow.add(deltaTimeButton);

        // Add the fourth row to the keyboard
        keyboardArray.add(fourthRow);

        JsonArray fifthRow = new JsonArray();
        JsonObject languageButton = new JsonObject();
        languageButton.addProperty("text", "Change Language");
        fifthRow.add(languageButton);

        // Add the fifth row to the keyboard
        keyboardArray.add(fifthRow);

        JsonObject replyMarkup = new JsonObject();
        replyMarkup.add("keyboard", keyboardArray);
        replyMarkup.addProperty("one_time_keyboard", true);
        replyMarkup.addProperty("resize_keyboard", true); // Optionally, to make keyboard more compact

        return replyMarkup.toString();
    }

    /**
     * Handles the registration process for a new user by guiding through language, age, source, and update frequency selections.
     *
     * @param userId The telegram user ID undergoing the registration/settings update process.
     * @param language Whether the language selection has been completed.
     * @param age Whether the age selection has been completed.
     * @param sources Whether the source selection has been completed.
     * @param deltaTime Whether the delta time selection has been completed.
     */
    public static void handleRegistration(long userId, boolean language, boolean age, boolean sources, boolean deltaTime) {
        String message = "";
        String messageKeyboardArray = "";
        if (!language) {
            message = "Choose your language:";
            messageKeyboardArray = getLanguageKeyboardArray();
        } else if (!age) {
            message = "Choose your age group:";
            messageKeyboardArray = getAgeKeyboardArray();
        } else if (!deltaTime) {
            message = "Choose how often you want to receive updates (minutes):";
            messageKeyboardArray = getDeltaTimeKeyboardArray();
        } else if (!sources) {
            message = "Suggest one of these links for news sources:";
            messageKeyboardArray = getSourcesKeyboardArray();
        }
        sendMessage(message, userId, messageKeyboardArray);
    }

    /**
     * Checks user registration status and triggers registration flow if necessary.
     * It verifies if the user's information is complete; otherwise, it initiates the registration process.
     *
     * @param userId The telegram user ID.
     * @return true if the registration process is initiated due to incomplete information,
     *         false if the user's information is already complete.
     */
    public static boolean checkAndRunRegistration(long userId) {
        Map<String, Object> userInfo = DB.getUserInfoByUserId(userId);

        if (userInfo == null) {
            handleRegistration(userId, false, false, false, false);
            return true;
        } else {
            boolean hasTelegramId = userInfo.get("telegram_id") != null;
            boolean hasSources = userInfo.get("sources") != null; // Assuming you need to check this for registration
            boolean hasAge = userInfo.get("age") != null;
            boolean hasLanguage = userInfo.get("language") != null;
            boolean hasDeltaTime = userInfo.get("delta_time") != null;

            // If any required information is missing, call the registration handler
            // Adjust the logic based on which fields are actually required for registration
            if (!hasTelegramId || !hasAge || !hasLanguage || !hasDeltaTime || !hasSources) {
                handleRegistration(userId, hasLanguage, hasAge, hasSources, hasDeltaTime);
                return true;
            }
            return false;
        }
    }

    /**
     * Splits a long message into smaller parts to comply with messaging platform limitations.
     *
     * @param fullMessage The full message that might exceed the platform's maximum message length.
     * @return A list of message parts, each conforming to the maximum message length.
     */
    private static List<String> splitIntoParts(String fullMessage) {
        List<String> parts = new ArrayList<>();
        String[] lines = fullMessage.split("\n", -1); // Split by newlines, keeping empty lines
        StringBuilder part = new StringBuilder();

        for (String line : lines) {
            // Process each line, checking if it needs to be split further
            String[] words = line.split("\\s+", -1);
            for (String word : words) {
                // Check if adding the next word exceeds max length
                if (part.length() + word.length() + 1 > MAX_MESSAGE_LENGTH) {
                    // Avoid adding an empty part if it's just space
                    if (part.length() > 0) {
                        parts.add(part.toString());
                        part = new StringBuilder();
                    }
                    // If a single word exceeds MAX_MESSAGE_LENGTH, split the word itself
                    if (word.length() > MAX_MESSAGE_LENGTH) {
                        part.append(word.substring(0, MAX_MESSAGE_LENGTH));
                        word = word.substring(MAX_MESSAGE_LENGTH);
                        parts.add(part.toString()); // Add the first part of the word
                        part = new StringBuilder(word); // Start the next part with the rest of the word
                    }
                }
                if (part.length() > 0 && !word.isEmpty()) {
                    part.append(" ");
                }
                part.append(word);
            }
            part.append("\n"); // Preserve line breaks by adding a newline at the end of each line
        }

        // Remove the last newline character to avoid an extra empty part
        if (part.length() > 0 && part.charAt(part.length() - 1) == '\n') {
            part.deleteCharAt(part.length() - 1);
        }

        // Add the last part if there's any left
        if (part.length() > 0) {
            parts.add(part.toString());
        }

        return parts;
    }

    /**
     * Processes incoming data from the user, handling various commands and interactions.
     *
     * @param data The command or data received from the reply/inline keyboard.
     * @param userId The telegram user ID.
     * @param messageId The unique identifier of the message (if applicable).
     * @param messageText The text of the message received from the user (used for deactivation of inline keyboard after first use).
     */
    public static void processData(String data, long userId, Long messageId, String messageText) {
        if (data.equals("More Info")) {
            Map<String, String> firstUnreadArticle = DB.getFirstUnreadArticleForUser(userId);

            String title = firstUnreadArticle.get("title");
            String summary = firstUnreadArticle.get("summary");
            String link = firstUnreadArticle.get("link");
            String fullStory = firstUnreadArticle.get("full_story");
            String unreadNumber = firstUnreadArticle.get("unread_number");

            if (unreadNumber.equals("0") || unreadNumber == null) {
                NewsBot.sendMessage("No more fresh news", userId, NewsBot.getNewsKeyboardArray(false, true, true));
            }
            else {

                // Format the message using Markdown
                String message = String.format("*Fresh News available (%s)*\n\n*%s*\n\n*Summary:* %s\n\n*Full Story:* %s\n\n[Read More](%s)", unreadNumber, title, summary, fullStory, link);

                List<String> messageParts = splitIntoParts(message);
                for (int i = 0; i < messageParts.size(); i++) {
                    boolean isFirstPart = i == 0;
                    boolean isLastPart = i == messageParts.size() - 1;
                    String messagePart = messageParts.get(i);

                    String newsKeyboardArray = getNewsKeyboardArray(true, false, false);
                    if (isLastPart) {
                        sendMessage(messagePart, userId, newsKeyboardArray);

                    } else {
                        sendMessage(messagePart, userId, "");
                    }
                }
            }
        } else if (data.equals("Next Article") || data.equals("Read Articles")) {
            // Format the message using Markdown
            if (data.equals("Next Article")) {
                DB.markFirstUnreadArticleAsRead(userId);
            }

            Map<String, String> firstUnreadArticle = DB.getFirstUnreadArticleForUser(userId);

            String title = firstUnreadArticle.get("title");
            String summary = firstUnreadArticle.get("summary");
            String link = firstUnreadArticle.get("link");
            String unreadNumber = firstUnreadArticle.get("unread_number");

            if (unreadNumber.equals("0") || unreadNumber == null) {
                NewsBot.sendMessage("No more fresh news", userId, NewsBot.getNewsKeyboardArray(false, true, true));
            }
            else {
                String message = String.format("*Fresh News available (%s)*\n\n*%s*\n\n*Summary:* %s\n\n[Read More](%s)", unreadNumber, title, summary, link);


                String newsKeyboardArray = getNewsKeyboardArray(false, false, false);
                sendMessage(message, userId, newsKeyboardArray);
            }
        } else if (data.equals("Clear")) {
            DB.markAllUnreadArticlesAsRead(userId);
            sendMessage("Feed is clear", userId, getNewsKeyboardArray(false, false, true));
        } else if (data.equals("Change Parsing Time")) {
            handleRegistration(userId, true, true, true, false);
        } else if (data.equals("Change Source")) {
            DB.deleteAllUserArticles(userId);
            handleRegistration(userId, true, true, false, true);
        } else if (data.equals("Change Language")) {
            DB.deleteAllUserArticles(userId);
            handleRegistration(userId, false, true, true, true);
        } else {
            editMessage(messageText, userId, messageId, "");
            String[] parts = data.split("\\|", 3);
            String table = parts[0];
            String column = parts[1];

            String value = parts[2];

            Map<String, String> newUserInfo = new HashMap<>();

            newUserInfo.put("telegram_id", Long.toString(userId));
            newUserInfo.put(column, value);

            DB.updateOrInsertUserInfo(newUserInfo);
        }
    }

    /**
     * Processes a callback query received from the messaging platform.
     *
     * @param callbackQuery The callback query JSON object containing the data and user information.
     */
    private static void processCallbackQuery(JsonObject callbackQuery) {
        String data = callbackQuery.get("data").getAsString();
        long userId = callbackQuery.getAsJsonObject("from").get("id").getAsLong();
        JsonObject message = callbackQuery.getAsJsonObject("message");
        long messageId = message.get("message_id").getAsLong();
        String text = message.has("text") ? message.get("text").getAsString() : "Non-text message";

        processData(data, userId, messageId, text);
        checkAndRunRegistration(userId);
    }

    /**
     * Processes a message received from the messaging platform.
     *
     * @param message The message JSON object containing the text and user information.
     */
    private static void processMessage(JsonObject message) {
        long userId = message.getAsJsonObject("chat").get("id").getAsLong();
        long messageId = message.get("message_id").getAsLong();
        String text = message.has("text") ? message.get("text").getAsString() : "Non-text message";

        if (text.equals("Change Parsing Time") || text.equals("Next Article") || text.equals("Change Source")
                || text.equals("More Info") || text.equals("Clear") || text.equals("Read Articles") || text.equals("Change Language")) {
            processData(text, userId, null, text);
        } else if (!text.equals("/start")) {
            deleteMessage(userId, messageId);
        }
        boolean isRegisteredUser = checkAndRunRegistration(userId);
        if (text.equals("/start") && !isRegisteredUser) {
            try {
                DB.rescheduleTasks(userId);
            } catch (SQLException e) {
                System.err.println("SQL error occurred: " + e.getMessage());
            }
        }
    }

    /**
     * Continuously checks for and processes new updates from the messaging platform.
     * This method implements long polling to receive updates and acts upon them.
     */
    public static void getUpdates() {
        int lastUpdateId = 0; // Keep track of the last update processed

        while (true) { // Infinite loop to keep checking for updates
            try {
                Response response = baseTarget.path("getUpdates")
                        .queryParam("offset", lastUpdateId + 1) // Process only new updates
                        .queryParam("timeout", 30) // Long polling timeout
                        .request()
                        .get();

                String jsonString = response.readEntity(String.class);
                JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

                if (!jsonObject.get("ok").getAsBoolean()) {
                    System.err.println("Error retrieving updates");
                    continue;
                }

                JsonArray updates = jsonObject.getAsJsonArray("result");
                for (JsonElement updateElement : updates) {
                    JsonObject updateObject = updateElement.getAsJsonObject();
                    int updateId = updateObject.get("update_id").getAsInt();

                    if (updateObject.has("callback_query")) {
                        processCallbackQuery(updateObject.getAsJsonObject("callback_query"));
                    } else if (updateObject.has("message")) {
                        processMessage(updateObject.getAsJsonObject("message"));
                    }

                    lastUpdateId = Math.max(lastUpdateId, updateId);
                }
            } catch (Exception e) {
                System.err.println("Error processing updates: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Determines the type of content contained in a message.
     *
     * @param message The message JSON object.
     * @return A string representing the type of content (e.g., "text", "photo").
     */
    private static String determineMessageType(JsonObject message) {
        if (message.has("text")) {
            return "text";
        } else if (message.has("photo")) {
            return "photo";
        } else if (message.has("video")) {
            return "video";
        }
        // Add more conditions as needed for different types of content
        return "unknown";
    }

    /**
     * Safely closes the client connection to the messaging platform.
     * Ensures resources are freed and the client is properly disposed of.
     */
    public static void closeClient() {
        if (client != null) {
            client.close();
            client = null; // Ensure the client reference is cleared to prevent reuse
        }
    }
}
