package cpen221.mp3.server;

public class ServerRequest {
    String id;
    String type;
    String query;
    String pageTitle;
    String pageTitle1;
    String pageTitle2;
    String limit;
    String maxItems;
    String timeWindowInSeconds;
    String timeLimitInSeconds;
    String timeout;

    /* Representation Invariant */
    // All fields except id and type could be null

    /* Abstraction Function */
    // A representation of a request made to a WikiMediatorServer that contains fields
    // corresponding to different request parameters

    public ServerRequest() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public void setPageTitle1(String pageTitle1) {
        this.pageTitle1 = pageTitle1;
    }

    public void setPageTitle2(String pageTitle2) {
        this.pageTitle2 = pageTitle2;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public void setMaxItems(String maxItems) {
        this.maxItems = maxItems;
    }

    public void setTimeWindowInSeconds(String timeWindowInSeconds) {
        this.timeWindowInSeconds = timeWindowInSeconds;
    }

    public void setTimeLimitInSeconds(String timeLimitInSeconds) {
        this.timeLimitInSeconds = timeLimitInSeconds;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
}
