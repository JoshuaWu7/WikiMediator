package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;

import java.util.NoSuchElementException;
import java.util.Objects;

public class BufferGetter implements Runnable{
    private final int currentId;
    private final int lastId;
    private final FSFTBuffer<TestObject> buffer;
    public int lastIdFound;

    public BufferGetter(FSFTBuffer<TestObject> buffer, int start, int end) {
        currentId = start;
        lastId = end;
        this.buffer = buffer;
    }

    public void run() {
        for (int i = currentId; i <= lastId; i++) {
            boolean found = false;
            while(!found) {
                try {
                    TestObject object = buffer.get(String.valueOf(i));
                    if (Objects.equals(object.id(), String.valueOf(i))) {
                        found = true;
                        lastIdFound = i;
                    }
                } catch (NoSuchElementException ignored) {}
            }
        }
    }
}
