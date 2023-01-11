package org.example.websiteMap.connector;

import org.jsoup.nodes.Document;
import java.util.LinkedList;
import java.util.Queue;


/*
 *  Этот класс инкапсулирует буферное хранилище для
 *  ссылок на чтение новых Web страниц (в формате String)
 *  и для прочитанных Web страниц (в формате org.jsoup.nodes.Document)
 */
class ConnectionBuffer {
    private final Queue<String> linksToRead;
    private final Queue<Document> readWebPages;

    private static final long NEXT_LINK_WAITING_TIME = 200;     //in milliseconds

    protected ConnectionBuffer() {
        linksToRead = new LinkedList<>();
        readWebPages = new LinkedList<>();
    }


    public String pollLinkForRead() throws InterruptedException {
        //Ожидание следующей ссылки для чтения в буфере
        synchronized (linksToRead) {
            while (linksToRead.isEmpty()) {
                linksToRead.wait(NEXT_LINK_WAITING_TIME);
            }
            return linksToRead.poll();
        }
    }

    public void addLinkForRead(String link) {
        synchronized (linksToRead) {
            linksToRead.offer(link);
            linksToRead.notify();
        }
    }

    /*
     *  Метод добавляет несколько ссылок сразу,
     *  и при этом открывает монитор для другого потока,
     *  ждущего данные из коллекции linksToRead, чтобы тот
     *  мог прочитать данные в однопоточном контексте и
     *  продолжил свое выполнение в многопоточном
     */
    public void addMultipleLinksForRead(String[] links) {
        synchronized (linksToRead) {
            for (String link : links) {
                linksToRead.offer(link);
                linksToRead.notify();
                Thread.yield();
            }
        }
    }

    public Document pollReadWebPage() {
        //Ожидание следующего объекта Document для чтения в буфере
        synchronized (readWebPages) {
            while (readWebPages.isEmpty()) {
                try {
                    readWebPages.wait(NEXT_LINK_WAITING_TIME);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return readWebPages.poll();
        }
    }

    public void addReadWebPage(Document document) {
        synchronized (readWebPages) {
            readWebPages.offer(document);
            readWebPages.notify();
        }
    }

    public int getLinksForReadCount() {
        int count;
        synchronized (linksToRead) {
            count = linksToRead.size();
        }
        return count;
    }

    public int getReadWebPagesCount() {
        int count;
        synchronized (readWebPages) {
            count = readWebPages.size();
        }
        return count;
    }
}
