package org.example.websiteMap.storage;



import org.example.websiteMap.BufferedIO;
import org.example.websiteMap.MyController;

import java.util.List;


/*
 * Класс-контроллер реализует управление хранилищем ссылок
 * - Ссылка, полученная впервые записывается в хранилище через буфер записи.
 * - После записи, ссылка маркируется, как отправленная на проверку и
 *      выгружается в буфер отправки, откуда может быть прочитана с помощью
 *      специальных методов потоками другого контроллера
 */
public class StorageController implements MyController, BufferedIO<String, String> {

    private volatile String startLink;
    private volatile LinksStorage linksStorage;


    public StorageController() {}

    @Override
    public void startWork(String startLink) {
        this.startLink = startLink;
        linksStorage = new LinksStorage(startLink);
    }

    @Override
    public void endWork() {
    }

    @Override
    public boolean hasNextResponse() {
        return linksStorage.hasUnreadLinks();
    }

    @Override
    public String pollNextResponse() {
        return linksStorage.pollUnreadLink();
    }

    @Override
    public boolean hasUnreadRequestsInQueue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendNewRequest(String request) {
        linksStorage.createIfNotAlreadyExist(request);
    }

    @Override
    public void sendNewMultipleRequest(String[] requests) {
        linksStorage.createIfNotAlreadyExist(requests);
    }

    public List<String> getLinksList() {
        return linksStorage.getSortedList();
    }

    public int getLinksListSize() {
        return linksStorage.getLinkSizeInStorage();
    }

    public boolean constains(String link) {
        return linksStorage.constains(link);
    }

}
