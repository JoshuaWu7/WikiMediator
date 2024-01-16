package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;

public class WikiGetPageA implements Runnable {
    private WikiMediator mediator;
    public String text;

    public WikiGetPageA (WikiMediator mediator) {
        this.mediator = mediator;
    }

    public void run() {
        for (int i = 0; i < 4; i++) {
            System.out.println("Thread A");
            text = mediator.getPage("A");
        }
    }
}
