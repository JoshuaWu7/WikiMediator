package cpen221.mp3.fsftbuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class FSFTBuffer<T extends Bufferable> {

    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    private final List<BufferObject<T>> buffer = new ArrayList<>();
    private final int capacity;
    private final int timeout;

    /* Representation Invariant */
    // buffer size equals the capacity after creation.
    // capacity is an integer greater than or equal to zero.
    // timeout is an integer greater than or equal to zero.

    /* Abstraction Function */
    // the FSFTBuffer is a list containing Bufferobjects with an immutable capacity
    // and timeout once created

    /* Thread Safety Argument */
    // put(), get(), update() and touch() are all methods that must access
    // elements in buffer (the local list)
    // Every BufferObject in buffer is used as a lock throughout this datatype
    // In addition, every method in BufferObject is synchronized on the object itself:
    // for every BufferObject, only
    // one of its methods can execute at a given time.
    // Each of the four previously mentioned methods iterates over buffer,
    // and must acquire the BufferObject at their current position to proceed further.
    // Therefore, if multiple threads attempt to iterate over buffer concurrently,
    // they proceed through buffer one after the other, as each thread can only proceed
    // once the lock in the position it wants to go to is free.
    // Since the put() method must keep track of access time if buffer is full,
    // in order to replace an object it deems is less recently used, it must reacquire the
    // lock for that object.
    // After acquiring that lock, put() will only replace that BufferObject if
    // its last access time matches what put had recorded.
    // If there is a discrepancy, put() starts fresh.

    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, int timeout) {
        this.timeout = Math.max(timeout, 0);
        this.capacity = capacity;

        // Satisfy the RI by filling the list with bufferObjects containing null references
        for (int item = 0; item < capacity; item++) {
            buffer.add(new BufferObject<>(timeout));
        }
    }

    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this.timeout = DTIMEOUT;
        this.capacity = DSIZE;

        // Satisfy the RI by filling the list with bufferObjects containing null references
        for (int item = 0; item < capacity; item++) {
            buffer.add(new BufferObject<>(timeout));
        }
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     * <p>
     * Returns a true if put operation is successful, else false.
     */
    public boolean put(T t) {
        boolean put = false;
        long leastRecentAccess;
        long lastAccess;
        BufferObject<T> leastRecentObject;

        while (!put) {
            synchronized (buffer.get(0)) {
                leastRecentAccess = buffer.get(0).timeLastAccessed();
                leastRecentObject = buffer.get(0);
            }
            for (int item = 0; item < capacity; item++) {
                synchronized (buffer.get(item)) {
                    if (buffer.get(item).isEmpty() | buffer.get(item).isStale()) {
                        buffer.get(item).fill(t);
                        put = true;
                        break;
                    }
                    if (Objects.equals(buffer.get(item).id(), t.id())) {
                        put = true;
                        break;
                    }
                    lastAccess = buffer.get(item).timeLastAccessed();

                    if (lastAccess < leastRecentAccess) {
                        leastRecentAccess = lastAccess;
                        leastRecentObject = buffer.get(item);
                    }
                }
            }
            if (put) {
                break;
            }
            synchronized (buffer.get(buffer.indexOf(leastRecentObject))) {
                if (buffer.get(buffer.indexOf(leastRecentObject)).timeLastAccessed() ==
                    leastRecentAccess) {
                    buffer.get(buffer.indexOf(leastRecentObject)).fill(t);
                    put = true;
                }
            }
        }
        return put;
    }

    /**
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the
     * buffer
     */
    public T get(String id) throws NoSuchElementException {
        for (int item = 0; item < capacity; item++) {
            synchronized (buffer.get(item)) {
                if (Objects.equals(buffer.get(item).id(), id) && !buffer.get(item).isStale()) {
                    return buffer.get(item).getItem();
                }
            }
        }
        throw new NoSuchElementException("Id not found in the buffer!");
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public boolean touch(String id) {
        for (int item = 0; item < capacity; item++) {
            synchronized (buffer.get(item)) {
                if (Objects.equals(buffer.get(item).id(), id) && !buffer.get(item).isStale()) {
                    return buffer.get(item).refresh();
                }
            }
        }
        return false;
    }

    /**
     * Update an object in the buffer.
     * This method updates an object and acts like a "touch" to
     * renew the object in the cache.
     *
     * @param t the object to update
     * @return true if successful and false otherwise
     */
    public boolean update(T t) {
        String id = t.id();
        boolean returnValue = false;
        for (int item = 0; item < capacity; item++) {
            synchronized (buffer.get(item)) {
                if (Objects.equals(buffer.get(item).id(), id) && !buffer.get(item).isStale()) {
                    buffer.get(item).fill(t);
                    returnValue = true;
                    break;
                }
            }
        }
        return returnValue;
    }
}
