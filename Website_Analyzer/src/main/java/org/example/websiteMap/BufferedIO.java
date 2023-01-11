package org.example.websiteMap;

public interface BufferedIO<I, O> {
    boolean hasNextResponse();
    O pollNextResponse();
    boolean hasUnreadRequestsInQueue();
    void sendNewRequest(I request);
    void sendNewMultipleRequest(I[] requests);
}
