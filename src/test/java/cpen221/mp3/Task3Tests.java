package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fastily.jwiki.core.Wiki;

public class Task3Tests {
    private static final String FILEPATH = "local/request_data";

    public static void clearCache() {
        try {
            PrintWriter writer = new PrintWriter(FILEPATH);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    @BeforeAll
    public static void init() {
        clearCache();
    }

    @Test
    public void basicWikiMediator() {
        int capacity = 5;
        int stalenessInterval = 10;
        WikiMediator currentWikiMediator = new WikiMediator(capacity, stalenessInterval);

        List<String> searchResult =
                currentWikiMediator.search("University of British Columbia", 3);

        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        List<String> expectedResult = wiki.search("University of British Columbia", 3);

        assertEquals(expectedResult, searchResult);

        List<String> requests = Arrays.asList(
                currentWikiMediator.getPage("Moose River (British Columbia)"),
                currentWikiMediator.search("Moose River (British Columbia)", 3).toString(),
                currentWikiMediator.search("1", 3).toString()
        );

        System.out.println(currentWikiMediator.zeitgeist(4));
        System.out.println(currentWikiMediator.zeitgeist(3));
        System.out.println(currentWikiMediator.zeitgeist(2));
        System.out.println(currentWikiMediator.zeitgeist(1));

        clearCache();
    }

    @Test
    public void trendingTest() {
        int capacity = 5;
        int stalenessInterval = 10;
        boolean interrupted = false;
        WikiMediator currentWikiMediator = new WikiMediator(capacity, stalenessInterval);
        List<String> expected = Arrays.asList("1", "2", "4");

        List<String> firstRequests = Arrays.asList(
                currentWikiMediator.search("1", 3).toString(),
                currentWikiMediator.getPage("2"),
                currentWikiMediator.getPage("3"),
                currentWikiMediator.getPage("1")
        );
        System.out.println(System.currentTimeMillis());

        try {
            Thread.sleep(5000);
            String a5 = currentWikiMediator.search("1", 3).toString();
            String a6 = currentWikiMediator.getPage("2");
            String a7 = currentWikiMediator.getPage("4");
            System.out.println(System.currentTimeMillis());
        }
        catch(InterruptedException e) {
            interrupted = true;
        }

        assertEquals(currentWikiMediator.trending(3, 5), expected);
        assertFalse(interrupted);

        clearCache();
    }

    @Test
    public void trendingTestProblematic() {
        int capacity = 5;
        int stalenessInterval = 10;
        WikiMediator currentWikiMediator = new WikiMediator(capacity, stalenessInterval);

        List<String> firstRequests = Arrays.asList(
                currentWikiMediator.search("1", 3).toString(),
                currentWikiMediator.getPage("2"),
                currentWikiMediator.getPage("3"),
                currentWikiMediator.getPage("1")
        );
        System.out.println(System.currentTimeMillis());


        try {
            Thread.sleep(5000);
            String a5 = currentWikiMediator.search("1", 3).toString();
            String a6 = currentWikiMediator.getPage("2");
            String a8 = currentWikiMediator.getPage("4");
            System.out.println(System.currentTimeMillis());
        }
        catch(InterruptedException sleepEx) {

        }

        try {
            Thread.sleep(10000);
        }
        catch(InterruptedException sleepEx) {

        }
        System.out.println(currentWikiMediator.trending(10, 5));

        clearCache();
    }

    @Test
    // Bad test, results not deterministic
    public void windowedPeakLoad() {
        int capacity = 1;
        int stalenessInterval = 1;
        boolean interrupted = false;
        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);

        mediator.search("1", 1); // 1
        mediator.search("2", 1); // 2
        mediator.search("1", 3); // 3
        mediator.search("3", 10); // 4
        List<String> zeitgeistResult = mediator.zeitgeist(3); // 5

        assertEquals(Arrays.asList("1", "2", "3"), zeitgeistResult);

        try {
            Thread.sleep(3000);
            assertEquals(3, mediator.windowedPeakLoad(3)); // 6

            Thread.sleep(2000);
            mediator.search("1", 10); // 7
            mediator.search("4", 1); // 8

            assertEquals(3, mediator.windowedPeakLoad(3)); // 9
        } catch (InterruptedException e) {
            interrupted = true;
        }
        assertFalse(interrupted);

        clearCache();
    }

    @Test
    public void emptyPeakLoad() {
        int capacity = 1;
        int stalenessInterval = 1;
        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);

        assertEquals(1, mediator.windowedPeakLoad());

        clearCache();
    }

    @Test
    public void multiThreadSearch() {
        int capacity = 4;
        int stalenessInterval = 10;
        boolean interrupted = false;

        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();

        List<String> expectedA = wiki.search("A", 1);
        List<String> expectedB = wiki.search("B", 1);

        WikiSearcher a = new WikiSearcher(mediator, capacity);
        WikiSearcher2 b = new WikiSearcher2(mediator, capacity);

        Thread searcherA = new Thread(a);
        Thread searcherB = new Thread(b);

        searcherA.start();
        searcherB.start();

        try {
            searcherA.join();
            searcherB.join();

            assertEquals(expectedA, a.result);
            assertEquals(expectedB, b.result);

            List<String> zeitgeistResult = mediator.zeitgeist(3);

            assertEquals(Arrays.asList("A", "B"), zeitgeistResult);

        } catch (InterruptedException e) {
            interrupted = true;
        }

        assertFalse(interrupted);

        clearCache();
    }

    @Test
    public void multiThreadGetPage() {
        int capacity = 1;
        int stalenessInterval = 10;
        boolean interrupted = false;

        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();

        String pageA = wiki.getPageText("A");
        String pageB = wiki.getPageText("B");

        WikiGetPageA a = new WikiGetPageA(mediator);
        WikiGetPageB b = new WikiGetPageB(mediator);

        Thread getterA = new Thread(a);
        Thread getterB = new Thread(b);

        getterA.start();
        getterB.start();

        try {
            getterA.join();
            getterB.join();

            assertEquals(pageA, a.text);
            assertEquals(pageB, b.text);
        } catch (InterruptedException e) {
            interrupted = true;
        }
        assertFalse(interrupted);

        clearCache();
    }

    @Test
    public void multiThreadZeitgeistTrending() {
        int capacity = 1;
        int stalenessInterval = 10;
        boolean interrupted = false;

        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();

        String pageA = wiki.getPageText("A");
        String pageB = wiki.getPageText("B");

        WikiGetPageA a = new WikiGetPageA(mediator);
        WikiGetPageB b = new WikiGetPageB(mediator);
        WikiGetPageA c = new WikiGetPageA(mediator);

        Thread getterA = new Thread(a);
        Thread getterB = new Thread(b);
        Thread getterC = new Thread(c);


        getterA.start();
        getterB.start();
        getterC.start();

        try {
            getterA.join();
            getterB.join();
            getterC.join();

            assertEquals(pageA, a.text);
            assertEquals(pageB, b.text);
            assertEquals(pageA, c.text);
        } catch (InterruptedException e) {
            interrupted = true;
        }

        List<String> expected = Arrays.asList("A", "B");

        assertEquals(expected, mediator.zeitgeist(3));
        assertEquals(expected, mediator.trending(2, 3));

        assertFalse(interrupted);

        clearCache();
    }

    @Test
    public void multiThreadStressTest() {
        int capacity = 1;
        int stalenessInterval = 10;
        boolean interrupted = false;

        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();

        List<String> letters = Arrays.asList("A","B","C","D","E","F","G","H","I","J","K","L","M",
                "N","O","P","Q","R","S","T","U","V","W","X","Y","Z");

        List<String> pages = new ArrayList<>();
        for (String letter: letters) {
            pages.add(wiki.getPageText(letter));
        }

        ArrayList<WikiGetPage> runnables = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < letters.size(); i++) {
            runnables.add(new WikiGetPage(mediator, letters.get(i)));
            threads.add(new Thread(runnables.get(i)));
        }

        System.out.println("STARTING THREADS");

        for (int i = 0; i < letters.size(); i++) {
            threads.get(i).start();
        }

        try {
            for (int i = 0; i < letters.size(); i++) {
                threads.get(i).join();
            }

            for (int i = 0; i < letters.size(); i++) {
                assertEquals(pages.get(i), runnables.get(i).text);
            }

        } catch (InterruptedException e) {
            interrupted = true;
        }

        assertEquals(letters, mediator.zeitgeist(26));

        assertEquals(List.of("Y"), mediator.trending(1, 26));

        assertFalse(interrupted);

        clearCache();
    }

    @Test
    public void mediatorReadFromFile() {
        int capacity = 1;
        int stalenessInterval = 10;

        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);

        clearCache();
    }
}
