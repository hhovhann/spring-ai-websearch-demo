package am.hhovhann.spring_ai_websearch_demo.websearch;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebSearchController {

    private final ChatClient chatClient;
    private final Environment environment;

    public WebSearchController(Environment environment) {
        this.environment = environment;

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model("gpt-4o-search-preview")
                .build();

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().apiKey(environment.getProperty("OPENAI_API_KEY")).build())
                .defaultOptions(chatOptions)
                .build();

        this.chatClient = ChatClient.builder(openAiChatModel).build();
    }

    @GetMapping("/gpt5")
    public String gpt5() {

        OpenAiApi.ChatCompletionRequest.WebSearchOptions webSearchOptions = new OpenAiApi.ChatCompletionRequest.WebSearchOptions(
                OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize.LOW,
                null
        );

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .webSearchOptions(webSearchOptions)
                .build();

        return chatClient.prompt()
                .user("Tell me an interesting fact about GPT-5")
                .options(options)
                .call()
                .content();
    }

    @GetMapping("/news")
    public String currentNews() {
        OpenAiApi.ChatCompletionRequest.WebSearchOptions webSearchOptions = new OpenAiApi.ChatCompletionRequest.WebSearchOptions(
                OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize.LOW,
                null
        );

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .webSearchOptions(webSearchOptions)
                .build();


        String systemPrompt = """
                You are a Spring Framework expert and technical news analyst. Your role is to:
                
                1. Search for and identify the most recent and significant developments in the Spring ecosystem
                2. Focus on official announcements, major releases, security updates, and breaking changes
                3. Prioritize information from authoritative sources like:
                   - Official Spring blog (spring.io/blog)
                   - GitHub releases and changelogs
                   - Spring team announcements
                   - Major tech publications covering Spring
                
                4. Structure your response with:
                   - **Date and Source** for each news item
                   - **Impact Level** (Critical/Major/Minor)
                   - **Brief Summary** in 2-3 sentences
                   - **Action Items** for developers if applicable
                
                5. Filter out generic Java news unless directly Spring-related
                6. If no significant Spring news is found in the last week, expand to the last month
                7. Always verify information currency - ignore outdated articles
                
                Present the information in a clear, scannable format that busy developers can quickly digest.
                """;

        String userPrompt = """
                Search for the latest Spring Framework ecosystem news from the past 7 days, including:
                
                - Spring Boot releases and updates
                - Spring Security announcements
                - Spring Cloud developments
                - New Spring projects or major updates
                - Breaking changes or deprecations
                - Security vulnerabilities and patches
                - Spring Tools and IDE integration updates
                - Performance improvements or new features
                - Community events or important blog posts
                
                Focus on news that would impact Spring developers in their daily work.
                If limited recent news, include significant developments from the past month.
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .call()
                .content();
    }

    @GetMapping("/restaurants")
    public String findRestaurants(@RequestParam(defaultValue = "any") String cuisine,
                                  @RequestParam(defaultValue = "$") String priceRange,
                                  @RequestParam(defaultValue = "Cleveland") String city,
                                  @RequestParam(defaultValue = "Ohio") String region,
                                  @RequestParam(defaultValue = "US") String country,
                                  @RequestParam(defaultValue = "America/New_York") String timezone) {

        // Set user location using WebSearchOptions
        OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation userLocation = new OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation(
                "approximate",
                new OpenAiApi.ChatCompletionRequest.WebSearchOptions.UserLocation.Approximate(city, country, region, timezone)
        );

        OpenAiApi.ChatCompletionRequest.WebSearchOptions webSearchOptions = new OpenAiApi.ChatCompletionRequest.WebSearchOptions(
                OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize.MEDIUM, // Reduced from HIGH for faster response
                userLocation
        );

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .webSearchOptions(webSearchOptions)
                .build();

        String systemPrompt = String.format("""
                        You are a local restaurant expert. You MUST provide exactly 3 restaurant recommendations - no more, no less.
                        
                        **CRITICAL REQUIREMENTS:**
                        - Return exactly 3 restaurants (never 1, 2, or more than 3)
                        - All restaurants must be currently OPEN and accepting customers
                        - All must match: %s cuisine and %s price range
                        - All must have recent positive reviews
                        
                        **Response Format for each of the 3 restaurants:**
                        **Restaurant Name** - Neighborhood
                        **Specialty** - What they're famous for
                        **Price & Rating** - Price range and rating
                        **Status & Contact** - Current hours and reservation info
                        
                        If you cannot find 3 restaurants that meet the exact criteria, expand your search to include:
                        1. Nearby neighborhoods
                        2. Slightly broader cuisine categories
                        3. Adjacent price ranges
                        
                        But you MUST return exactly 3 recommendations.
                        """,
                cuisine.equals("any") ? "any" : cuisine,
                getPriceRangeDescription(priceRange));

        String userPrompt = String.format("""
                        I need exactly 3 %s restaurant recommendations near me in the %s price range.
                        
                        **Requirements:**
                        - All 3 must be currently OPEN (not closed)
                        - All 3 must be accepting customers today
                        - All 3 should have good recent reviews
                        - Give me practical info: address, hours, phone, reservation needs
                        
                        **Important:** I need exactly 3 options - please don't give me fewer than 3 restaurants.
                        """,
                cuisine.equals("any") ? "great" : cuisine,
                getPriceRangeDescription(priceRange));

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .call()
                .content();
    }

    private String getPriceRangeDescription(String priceRange) {
        return switch (priceRange) {
            case "$" -> "budget-friendly";
            case "$$" -> "moderate";
            case "$$$" -> "upscale";
            case "$$$$" -> "fine dining";
            default -> "moderate";
        };
    }
}