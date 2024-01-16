package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Task2Tests {
    @Test
    public void multiThreadPut() {
        int capacity = 8;
        int timeout = 4;
        boolean interrupted = false;
        boolean foundObjects = false;

        FSFTBuffer<TestObject> buffer = new FSFTBuffer<>(capacity, timeout);

        Thread putterA = new Thread(() -> {
            for (int i = 0; i < capacity/2; i++) {
                TestObject object = new TestObject(i);
                buffer.put(object);
            }
        });
        Thread putterB = new Thread(() -> {
            for (int i = capacity/2; i < capacity; i++) {
                TestObject object = new TestObject(i);
                buffer.put(object);
            }
        });

        putterA.start();
        putterB.start();

        try {
            putterA.join();
            putterB.join();

            for (int i = 0; i < capacity; i++) {
                TestObject object = buffer.get(String.valueOf(i));
                foundObjects = buffer.update(object);
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }

        assertFalse(interrupted);
        assertTrue(foundObjects);
    }

    @Test
    public void multiThreadGet() {
        int capacity = 4;
        int timeout = 1234;
        boolean interrupted = false;
        boolean foundObjects = false;

        FSFTBuffer<TestObject> buffer = new FSFTBuffer<>(capacity, timeout);

        Thread putterA = new Thread(() -> {
            for (int i = 0; i < capacity; i++) {
                TestObject object = new TestObject(i);
                buffer.put(object);
            }
        });
        Thread getterA = new Thread(() -> {
            for (int i = 0; i < capacity; i++) {
                buffer.get(String.valueOf(i));
            }
        });
        Thread getterB = new Thread(() -> {
            for (int i = 0; i < capacity; i++) {
                buffer.get(String.valueOf(i));
            }
        });

        putterA.start();

        try {
            putterA.join();
            getterA.start();
            getterB.start();
            getterA.join();
            getterB.join();
            foundObjects = true;
        } catch (NoSuchElementException e) {
            System.out.println("Couldn't find the desired objects.");
        } catch (InterruptedException e) {
            interrupted = true;
        }

        assertFalse(interrupted);
        assertTrue(foundObjects);
    }

    @Test
    public void concurrentPutAndGet() {
        int capacity = 1000;
        int timeout = 10;
        boolean interrupted = false;
        FSFTBuffer<TestObject> buffer = new FSFTBuffer<>(capacity, timeout);

        Thread putter = new Thread(() -> {
            for (int i = 0; i < capacity; i++) {
                TestObject object = new TestObject(i);
                buffer.put(object);
            }
        });
        BufferGetter get = new BufferGetter(buffer, 0, capacity - 1);
        Thread getter = new Thread(get);

        getter.start();
        putter.start();

        try {
            getter.join();
            putter.join();
        } catch (InterruptedException e) {
            interrupted = true;
        }

        assertFalse(interrupted);
        assertEquals(get.lastIdFound, capacity - 1);
    }
}
