package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.Bufferable;

public class WikiPage implements Bufferable {

    private final String id;
    private final String pageText;

    /* Representation Invariant */
    // id is non-null
    // pageText is non-null

    /* Abstraction Function */
    // represents a wiki page containing the page name and the page text.

    /**
     * Constructor for a WikiPage
     *
     * @param pageName is non-null
     * @param pageText is non-null
     */
    public WikiPage(String pageName, String pageText) {
        this.id = pageName;
        this.pageText = pageText;
    }

    /**
     * @return a reference to the this pageText
     */
    public String getPageText() {
        return pageText;
    }

    /**
     * @return a reference to this id
     */
    public String id() {
        return this.id;
    }
}
