/**
 * Provides a comprehensive solution for aggregating, processing, and delivering news content
 * through the Telegram platform. This package integrates various components necessary for the
 * NewsBot application, facilitating tasks such as database management, news parsing, and user interaction
 * via the Telegram API.
 * <p>
 * Key functionalities include:
 * <ul>
 *     <li>Database Operations: Manages connections, table creations, and data retrieval,
 *         supporting the application's data persistence requirements.</li>
 *     <li>Configuration Management: Loads and provides access to database connection details
 *         from a configuration file, ensuring modular and flexible setup.</li>
 *     <li>Application Initialization: Handles initial setup tasks including database connection,
 *         table creation, and scheduling of periodic updates for message processing.</li>
 *     <li>Telegram API Interaction: Offers capabilities to send, edit, and delete messages on Telegram,
 *         along with generating interactive keyboards for user responses.</li>
 *     <li>Content Parsing: Utilizes web scraping techniques to extract and process news content from
 *         specified sources, preparing it for user delivery.</li>
 *     <li>Task Scheduling: Manages periodic tasks for news parsing and message delivery, ensuring
 *         efficient and timely updates to users.</li>
 * </ul>
 * This package serves as the backbone of the NewsBot application, orchestrating the flow of information
 * from news sources to the end user via Telegram, with an emphasis on modularity, efficiency, and usability.
 *
 * @since 1.0
 */
package edu.newsbot;
