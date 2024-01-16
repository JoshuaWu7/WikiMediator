package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class Task5Tests {
    @Test
    public void shortPath() {
        int capacity = 1;
        int stalenessInterval = 10;
        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        try {
            assertEquals(Arrays.asList("Philosophy", "Academic bias", "Barack Obama"),
                    mediator.shortestPath("Philosophy", "Barack Obama", 60));

        }
        catch (TimeoutException e) {
            System.out.println("Timed Out");
        }
    }

    @Test
    public void reallyShortPath() {
        int capacity = 1;
        int stalenessInterval = 10;
        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        try {
            assertEquals(Arrays.asList("Philosophy", "Academic bias"),
                mediator.shortestPath("Philosophy", "Academic bias", 20));

        }
        catch (TimeoutException e) {
            System.out.println("Timed Out");
        }
    }

    @Test
    public void timeoutTest() {
        int capacity = 1;
        int stalenessInterval = 10;
        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        try {
            List<String> path =
                mediator.shortestPath("Philosophy", "Barack Obama", 1);
            System.out.println(path);

            fail();
        }
        catch (TimeoutException e) {
            System.out.println("Timed Out");
            assertTrue(true);
        }
        try {
            List<String> path =
                mediator.shortestPath("Goose", "Tuya",2);
            System.out.println(path);

            fail();
        }
        catch (TimeoutException e) {
            System.out.println("Timed Out");
            assertTrue(true);
        }
    }


    @Test
    public void fourStepPath() {
        int capacity = 1;
        int stalenessInterval = 10;
        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        try {
            assertEquals(Arrays.asList("Goose", "Canada", "Glacier", "Tuya"),
                    mediator.shortestPath("Goose", "Tuya",60));

        }
        catch (TimeoutException e) {
            System.out.println("Timed Out");
        }
    }


    @Test
    public void fourStepPath2() {
        int capacity = 1;
        int stalenessInterval = 10;
        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        try {
            assertEquals(Arrays.asList("Anatidae", "Extinction", "Asteroid impact avoidance",
                            "253 Mathilde"),
                    mediator.shortestPath("Anatidae", "253 Mathilde",60));

        }
        catch (TimeoutException e) {
            System.out.println("Timed Out");
        }
    }

    @Test
    public void fourStepPath3() {
        int capacity = 1;
        int stalenessInterval = 10;
        WikiMediator mediator = new WikiMediator(capacity, stalenessInterval);
        try {
            assertEquals(Arrays.asList("South Saskatchewan River", "Burbot", "Animal",
                "Tardigrade"),
                mediator.shortestPath("South Saskatchewan River", "Tardigrade",60));

        }
        catch (TimeoutException e) {
            System.out.println("Timed Out");
        }
    }
}
