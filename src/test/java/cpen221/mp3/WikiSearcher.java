package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;

import java.util.List;

public class WikiSearcher implements Runnable {

    private final WikiMediator mediator;
    private final int numSearches;
    public List<String> result;

    public WikiSearcher (WikiMediator mediator, int numSearches) {
        this.mediator = mediator;
        this.numSearches = numSearches;
    }

    public void run() {
        for (int i = 0; i < numSearches; i++) {
            result = mediator.search("A", 1);
        }
    }
}
