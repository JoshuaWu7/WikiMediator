package cpen221.mp3.fsftbuffer;

public class BufferObject<T extends Bufferable> {
    private static final int MILLIS_PER_SEC = 1000;

    private long nextTimeout;
    private long lastAccess;
    private final int timeout;

    private T bufferObject;
    private String id;
    /* Representation Invariant */
    // nextTimeout is not null and must be a positive long value.
    // lastAccess is not null and must be a positive long value.
    // timeout is not null and must be a positive integer value.
    // bufferObject is a generic type that extends the Bufferable interface.
    // id is a string that can be null.

    /* Abstraction Function */
    // bufferObject is an object that wraps a Bufferable object.
    // It contains information on it's last access time, next timeout time until it goes stale,
    // and it's string ID.

    /**
     * Creates a new BufferItem containing a null buffer object and timeout length in seconds.
     *
     * @param timeout positive integer representing seconds
     */
    public BufferObject(int timeout) {
        long currentTime = System.currentTimeMillis();
        this.timeout = timeout;
        this.nextTimeout = currentTime + (long) timeout * MILLIS_PER_SEC;
        this.lastAccess = currentTime;


        this.bufferObject = null;
        this.id = null;
    }

    /**
     * Thread-safe method that returns a boolean if this bufferObject is stale or not.
     *
     * @return boolean true if the current time is equal or exceeds the nextTimeout time
     * and false otherwise.
     */
    synchronized boolean isStale() {
        long currentTime = System.currentTimeMillis();
        return currentTime >= nextTimeout;
    }

    /**
     * Thread-safe method that updates the nextTimeout time with the current time
     * at method call and timeout.
     * The method caller cannot refresh on a stale bufferObject item.
     *
     * @return boolean true if the refresh method was called successfully.
     */
    synchronized boolean refresh() {
        long currentTime = System.currentTimeMillis();
        this.nextTimeout = currentTime + (long) timeout * MILLIS_PER_SEC;
        this.lastAccess = currentTime;
        return true;
    }

    /**
     * Thread-safe method that returns the lastAccess time of this buffer object.
     *
     * @return long access time of bufferObject representing milliseconds.
     */
    synchronized long timeLastAccessed() {
        return lastAccess;
    }

    /**
     * Thread-safe method that checks if this bufferObject is null.
     *
     * @return boolean true if bufferObjects is null otherwise false.
     */
    synchronized boolean isEmpty() {
        return this.bufferObject == null;
    }

    /**
     * Thread-safe method that returns the id string name of the buffer object.
     * It is possible for string ID to be null.
     *
     * @return String id of this bufferObject
     */
    synchronized String id() {
        return this.id;
    }

    /**
     * Thread-safe method that returns this bufferObject.
     * It is possible for bufferObject to be null.
     *
     * @return generic type, bufferObject, which extends Bufferable
     */
    synchronized T getItem() {
        return this.bufferObject;
    }

    /**
     * Thread-safe method that updates this bufferObject with another Bufferable object.
     *
     * @param object bufferObject extends Bufferable
     */
    synchronized void fill(T object) {
        this.bufferObject = object;
        this.id = object.id();
        long currentTime = System.currentTimeMillis();
        lastAccess = currentTime;
        nextTimeout = currentTime + (long) timeout * MILLIS_PER_SEC;
    }

}
