package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;

import java.util.List;

public class WikiGetPage implements Runnable {
    private WikiMediator mediator;
    private String pageName;
    public String text;

    public WikiGetPage (WikiMediator mediator, String pageName) {
        this.mediator = mediator;
        this.pageName = pageName;
    }

    public void run() {
        if (pageName.equals("Y")) {
            try {
                Thread.sleep(3000);
            }
            catch(InterruptedException sleepEx) {

            }
        }
        for (int i = 0; i < 10; i++) {
            System.out.println("Thread " + pageName);
            text = mediator.getPage(pageName);
        }
    }
}
