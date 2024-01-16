package cpen221.mp3.wikimediator;

public class Request {
    private final RequestType type;
    private final String requestString;
    private final long requestTime;

    /* Representation Invariants */
    // type is non-null.
    // requestString is non-null.
    // request Time is positive, non-null long value.
    // Request fields are immutable.

    /* Abstraction Function */
    // represents a request object containing type, request string, and request time fields.

    /**
     * Constructor for a Request object.
     *
     * @param type  enum of RequestType
     * @param query a non-null string
     */
    public Request(RequestType type, String query) {
        this.type = type;
        this.requestString = query;
        this.requestTime = System.currentTimeMillis();
    }

    /**
     * Overloaded constructor for a Request object, that sets the request type.
     * By default, sets requestString as an empty string and requestTime to the time of calling
     * constructor.
     *
     * @param type enum of RequestType
     */
    public Request(RequestType type) {
        this.type = type;
        this.requestString = "";
        if (type != RequestType.LOCK) {
            this.requestTime = System.currentTimeMillis();
        } else {
            this.requestTime = 0;
        }

    }

    /**
     * Overloaded constructor for a Request object, that sets the request type and request time.
     * By default, sets requestString as an empty String.
     *
     * @param type        enum of RequestType
     * @param requestTime a positive, long value
     */
    public Request(RequestType type, long requestTime) {
        this.type = type;
        this.requestString = "";
        this.requestTime = requestTime;
    }

    /**
     * Overloaded constructor for a Request object, that sets the request type, string query,
     * and request time.
     *
     * @param type        enum of RequestType
     * @param query       a non-null string
     * @param requestTime a positive, long value
     */
    public Request(RequestType type, String query, long requestTime) {
        this.type = type;
        this.requestString = query;
        this.requestTime = requestTime;
    }

    /**
     * Returns the enum RequestType of this Request
     *
     * @return this enum RequestType
     */
    public RequestType getType() {
        return type;
    }

    /**
     * Returns this requestString query
     *
     * @return this requestString as string
     */
    public String getQuery() {
        return requestString;
    }

    /**
     * Returns this requestTime as a long value
     *
     * @return this requestTime as long
     */
    public long getRequestTime() {
        return requestTime;
    }
}
