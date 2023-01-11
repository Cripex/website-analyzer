package org.example.websiteMap.connector;


import org.example.websiteMap.BufferedIO;
import org.example.websiteMap.MyController;
import org.jsoup.nodes.Document;


/*
 *  Этот класс осуществляет подключение и чтение страниц по ссылкам в буфере
 */
public class ConnectionController implements MyController, BufferedIO<String, Document> {
    private final ConnectionBuffer connectionBuffer;
    private final Thread connector;
    private volatile String startLink;


    public ConnectionController() {
        connectionBuffer = new ConnectionBuffer();
        connector = new Thread(new RunnableConnector(connectionBuffer));
    }


    @Override
    public void startWork(String startLink) {
        this.startLink = startLink;
        connectionBuffer.addLinkForRead(startLink);
        connector.start();
    }

    @Override
    public void endWork() {
        connector.interrupt();
    }

    @Override
    public void sendNewRequest(String request) {
        connectionBuffer.addLinkForRead(request);
    }

    @Override
    public void sendNewMultipleRequest(String[] requests) {
        connectionBuffer.addMultipleLinksForRead(requests);
    }

    @Override
    public Document pollNextResponse() {
        return connectionBuffer.pollReadWebPage();
    }

    @Override
    public boolean hasUnreadRequestsInQueue() {
        return connectionBuffer.getLinksForReadCount() > 0;
    }

    @Override
    public boolean hasNextResponse() {
        return connectionBuffer.getReadWebPagesCount() > 0;
    }
}
