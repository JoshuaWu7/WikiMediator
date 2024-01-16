package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.Objects;

public class Tests {
    @Test
    public void fillBuffer() {
        int startId = 1;
        int endId = 32;
        boolean wrongObject = false;

        FSFTBuffer<TestObject> buffer = new FSFTBuffer<>();

        for (int i = startId; i <= endId; i++) {
            TestObject object = new TestObject(i);
            buffer.put(object);
        }

        int id = 1;
        for (int i = startId; i <= 32; i++) {
            if (Integer.parseInt(buffer.get(String.valueOf(i)).id()) != id) {
                wrongObject = true;
            }
            id++;
        }

        assertFalse(wrongObject);
    }

    @Test
    public void getStaleItem() {
        int id = 1;
        boolean gotItem = false;
        boolean interrupted = false;

        FSFTBuffer<TestObject> buffer = new FSFTBuffer<>(10, 1);

        for (int i = id; i <= 10; i++) {
            TestObject object = new TestObject(i);
            buffer.put(object);
        }

        try {
            Thread.sleep(1000);
            TestObject object = new TestObject(buffer.get("1"));
            if (Objects.equals(object.id(), "1")) {gotItem = true;}
        } catch (InterruptedException e) {
            interrupted = true;
        } catch (NoSuchElementException e) {
            System.out.println("No such element");
        }

        assertFalse(gotItem);
        assertFalse(interrupted);
    }

    @Test
    public void replaceLeastUsed() {
        FSFTBuffer<TestObject> buffer = new FSFTBuffer<>(5, 5);
        boolean interrupted = false;
        int id = 17;

        for (int i = 1; i <= 5; i++) {
            TestObject object = new TestObject(i);
            boolean a = buffer.put(object);
        }

        try {
            Thread.sleep(1000);
            for (int i = 1; i < 5; i++) {
                buffer.update(new TestObject(i));
            }
            buffer.put(new TestObject(id));

        } catch (InterruptedException e) {
            interrupted = true;
        }

        assertTrue(buffer.touch(String.valueOf(id)));
        assertTrue(buffer.touch("1"));
        assertFalse(buffer.touch("5"));
        assertFalse(interrupted);
    }

    @Test
    public void touchStaleObjects() {
        int capacity = 20;
        int timeout = 0;
        boolean interrupted = false;
        boolean touched = false;
        FSFTBuffer<TestObject> buffer = new FSFTBuffer<>(capacity, timeout);

        for (int i = 0; i < capacity; i++) {
            TestObject object = new TestObject(i);
            buffer.put(object);
        }

        try {
            Thread.sleep(1000);
            for (int i = 0; i < capacity; i++) {
                touched = buffer.touch(String.valueOf(i));
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }

        assertFalse(interrupted);
        assertFalse(touched);
    }

    @Test
    public void putSameObject() {
        int capacity = 8;
        int timeout = 4;
        boolean interrupted = false;
        boolean gotItem = false;
        FSFTBuffer<TestObject> buffer = new FSFTBuffer<>(capacity, timeout);

        for (int i = 0; i < capacity; i++) {
            TestObject object = new TestObject(i);
            buffer.put(object);
        }

        try {
            Thread.sleep(1000);
            for (int i = 0; i < capacity; i++) {
                TestObject object = new TestObject(i);
                buffer.put(object);
            }

            TestObject object = new TestObject(buffer.get("4"));
            if (Objects.equals(object.id(), "4")) {gotItem = true;}

        } catch (InterruptedException e) {
            interrupted = true;
        } catch (NoSuchElementException e) {
            System.out.println("No such item");
        }

        assertFalse(interrupted);
        assertTrue(gotItem);
    }
}
