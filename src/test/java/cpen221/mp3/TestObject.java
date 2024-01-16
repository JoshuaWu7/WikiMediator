package cpen221.mp3;

import cpen221.mp3.fsftbuffer.Bufferable;

public class TestObject implements Bufferable {

    private final String id;

    public TestObject(int number) {
        this.id = String.valueOf(number);
    }

    public TestObject(TestObject other) {
        this.id = other.id;
    }

    public String id() {
        return this.id;
    }
}
