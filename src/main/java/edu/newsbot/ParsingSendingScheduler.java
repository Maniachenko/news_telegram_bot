package edu.newsbot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.Map;

/**
 * The {@code ParsingSendingScheduler} class is responsible for scheduling and managing tasks that involve
 * parsing web pages for news links and sending extracted information to users. It uses a {@link ScheduledExecutorService}
 * to handle task scheduling in a thread-safe manner, ensuring that tasks are executed at specified delays.
 * <p>
 * This class maintains a map of user tasks to prevent duplicate scheduling for the same user. When a new task is
 * scheduled for a user, any existing task for that user is first cancelled. This ensures that each user has at most
 * one active task, preventing overlapping executions and potential data inconsistencies.
 */
public class ParsingSendingScheduler {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final ConcurrentHashMap<Long, ScheduledFuture<?>> userTasks = new ConcurrentHashMap<>();
    private static final AtomicInteger taskIdGenerator = new AtomicInteger(0);

    /**
     * Schedules a new task for parsing a link and sending messages to the user at fixed delays.
     * If a task is already scheduled for the user, it will be stopped before scheduling/rescheduling a new one.
     *
     * @param link The link of primary page (the page-list of title-links to news;
     *             example: https://www.sciencedaily.com/news/earth_climate/climate/) to be parsed.
     * @param delay The delay between consecutive executions of the task.
     * @param unit The time unit of the delay.
     * @param userId The telegram user ID of the user for whom the task is scheduled.
     */
    public static void scheduleLinkParsing(String link, long delay, TimeUnit unit, long userId) {
        stopUserTask(userId);
        int taskId = taskIdGenerator.incrementAndGet();

        Runnable task = () -> {
            parseLinkAndSendMessage(link, userId);
        };

        ScheduledFuture<?> scheduledTask = scheduler.scheduleWithFixedDelay(task, 0, delay, unit);
        userTasks.put(userId, scheduledTask);
    }

    /**
     * Stops the task associated with the specified user ID, if it is currently active.
     *
     * @param userId The telegram user ID of the user whose task is to be stopped.
     */
    public static void stopUserTask(long userId) {
        ScheduledFuture<?> scheduledTask = userTasks.get(userId);
        if (scheduledTask != null && !scheduledTask.isDone()) {
            boolean cancelled = scheduledTask.cancel(false);
            if (cancelled) {
                userTasks.remove(userId);
                System.out.println("Task for user " + userId + " was active and has been successfully cancelled.");
            } else {
                System.out.println("Task for user " + userId + " could not be cancelled.");
            }
        } else {
            System.out.println("No active task for user " + userId + " to stop.");
        }
    }

    /**
     * Stops all scheduled tasks and shuts down the scheduler.
     * This method is intended to be called during application shutdown to ensure proper resource management.
     */
    public static void stopAll() {
        scheduler.shutdownNow();
    }

    /**
     * Parses the provided link and sends a message to the user.
     * This is a private method used as a task for the scheduler.
     *
     * @param primaryPageLink The primary page (the page-list of title-links to news;
     *                        example: https://www.sciencedaily.com/news/earth_climate/climate/) link to parse.
     * @param userId The telegram user ID of the user to whom the message is sent.
     */
    private static void parseLinkAndSendMessage(String primaryPageLink, long userId) {
        NewsBot.sendMessage("News parsing...", userId, "");
        System.out.println("Parsing link for user " + userId + ": " + primaryPageLink);
        Parser.parsePrimaryPage(primaryPageLink);

        Map<String, String> firstUnreadArticle = DB.getFirstUnreadArticleForUser(userId);

        String unreadNumber = firstUnreadArticle.get("unread_number");

        if (unreadNumber.equals("0") || unreadNumber == null) {
            NewsBot.sendMessage("No more fresh news", userId, NewsBot.getNewsKeyboardArray(false, true, true));
        } else {

            String message = String.format("*Fresh News available (%s)*", unreadNumber);

            String newsKeyboardArray = NewsBot.getNewsKeyboardArray(false, true, false);

            NewsBot.sendMessage(message, userId, newsKeyboardArray);
        }
    }
}

