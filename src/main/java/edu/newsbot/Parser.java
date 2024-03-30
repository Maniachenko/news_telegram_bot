package edu.newsbot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code Parser} class is responsible for parsing web pages to extract news information.
 * It provides functionality to parse both primary pages, which list news articles, and detailed
 * pages, which contain the full content of news articles. This class leverages the Jsoup library
 * to select and retrieve content based on specified HTML selectors.
 * <p>
 * Usage involves specifying the URL of a primary news page. The class then extracts titles,
 * links, and summaries of articles. For detailed pages, it can extract specific categories of
 * information such as the full story, related topics, and citations.
 */
public class Parser {

    // Define categories for parsing detailed page information
    private static final String[] categories = {
            "full_story", "related_topics", "related_terms", "story_source",
            "journal_reference", "citation_mla", "citation_chicago", "citation_apa", "related_stories"
    };

    /**
     * Parses the primary page to extract links, titles, and summaries of detailed pages.
     *
     * @param primaryUrl The URL of the primary page to parse.
     */
    public static void parsePrimaryPage(String primaryUrl) {
        try {
            // Connect to the primary URL and parse the document
            Document primaryDocument = Jsoup.connect(primaryUrl).get();
            // Select items (adjust the selector based on your primary page's HTML structure)
            Elements items = primaryDocument.select("item");

            for (Element item : items) {
                // Extract title, link, and summary from each item
                String title = item.select("title").first().text();
                String link = item.select("link").first().text();
                String summary = item.select("description").first().text(); // Adjust selector as needed

                // Parse the detailed page for each item
                parseDetailedPage(primaryUrl, link, title, summary);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses detailed pages to extract and store information based on predefined categories.
     *
     * @param primaryUrl The URL of the primary page (for reference).
     * @param url        The URL of the detailed page to parse.
     * @param title      The title of the content.
     * @param summary    The summary of the content.
     */
    public static void parseDetailedPage(String primaryUrl, String url, String title, String summary) {
        Map<String, String> dataMap = new HashMap<>();

        // Initialize the map with title, summary, and link
        dataMap.put("title", title);
        dataMap.put("summary", summary);
        dataMap.put("link", url);

        try {
            // Connect to the detailed page URL and parse the document
            Document document = Jsoup.connect(url).get();

            // Parse each category and add the data to the map
            for (String category : categories) {
                String data = parseCategory(document, category);
                dataMap.put(category, data);
            }

            // Here you would save the parsed data to a database or further processing
            // This is a placeholder call to a method that should handle database interaction
            DB.saveArticleToDatabase(primaryUrl, dataMap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the content of a given category from the detailed page.
     *
     * @param soup The Jsoup Document object of the detailed page.
     * @param category The category to parse from the document.
     * @return The extracted data as a String.
     */
    public static String parseCategory(Document soup, String category) {
        String categoryData = "";

        switch (category) {
            case "full_story":
                Elements fullStoryElements = soup.select("div#story_text p");
                for (Element paragraph : fullStoryElements) {
                    categoryData += paragraph.text() + "\n\n";
                }
                break;

            case "related_topics":
                Elements relatedTopicsElements = soup.select("ul.nav.subnav#related_topics li a");
                for (Element aTag : relatedTopicsElements) {
                    categoryData += aTag.text() + "; ";
                }
                break;

            case "related_terms":
                Elements relatedTermsElements = soup.select("ul.nav.nav-condensed.fa-ul#related_terms li a");
                for (Element aTag : relatedTermsElements) {
                    categoryData += aTag.text() + "; ";
                }
                break;

            case "story_source":
                Element storySourceElement = soup.select("div#story_source").first();
                if (storySourceElement != null) {
                    categoryData += storySourceElement.text().replace("Story Source:", "").trim();
                }
                break;

            case "journal_reference":
                Element journalReferenceElement = soup.select("ol.journal").first();
                if (journalReferenceElement != null) {
                    categoryData += journalReferenceElement.text().trim();
                }
                break;

            case "citation_mla":
                Element citationMlaElement = soup.select("div.tab-content.tab-citations #citation_mla").first();
                if (citationMlaElement != null) {
                    categoryData += citationMlaElement.text().trim();
                }
                break;

            case "citation_chicago":
                Element citationChicagoElement = soup.select("div.tab-content.tab-citations #citation_chicago").first();
                if (citationChicagoElement != null) {
                    categoryData += citationChicagoElement.text().trim();
                }
                break;

            case "citation_apa":
                Element citationApaElement = soup.select("div.tab-content.tab-citations #citation_apa").first();
                if (citationApaElement != null) {
                    categoryData += citationApaElement.text().trim();
                }
                break;

            case "related_stories":
                Elements relatedStoriesElements = soup.select("div.related-headline.clearfix a[href]");
                for (Element aTag : relatedStoriesElements) {
                    String url = aTag.attr("href");
                    categoryData += url + " ";
                }
                break;

            default:
                break;
        }

        return categoryData.trim();
    }
}
