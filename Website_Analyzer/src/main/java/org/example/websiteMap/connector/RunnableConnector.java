package org.example.websiteMap.connector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;


/*
 *  Этот класс читает html код по указанной ссылке
 *  с учетом необходимой паузы между переподключениями.
 *  Чтение ссылок реализовано с помощью внутренного класса,
 *  наследующего Thread.
 */
class RunnableConnector implements Runnable {

    private final ConnectionBuffer connectionBuffer;

    private volatile long prevConnectionTime = 0;
    private static final int RECONNECTION_DELAY = 150;     //in milliseconds

    protected RunnableConnector(ConnectionBuffer connectionBuffer) {
        this.connectionBuffer = connectionBuffer;
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            final String linkForRead;
            try {
                linkForRead = connectionBuffer.pollLinkForRead();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            //Усыпляем поток, если прошло слишком мало времени с прошлого обращения
            final int sleepTime = whenCanBeConnectAgain();
            if(sleepTime != -1) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            Document document = null;
            try {
                document = readAndGet(linkForRead);
            } catch (org.jsoup.HttpStatusException e) {
                //TODO: Обработка ошибок подключения к серверу
                continue;
            } catch (org.jsoup.UnsupportedMimeTypeException e) {
                //TODO: Обработка ошибок неподдерживаемого формата ссылки (....pdf)
                continue;
            } catch (java.net.SocketTimeoutException e) {
                //Если истекло время ожидания, переходим к след. ссылке
                continue;
            } catch (java.net.UnknownHostException e) {
                continue;
            } catch (java.net.ConnectException e) {
                System.out.printf("ConnectionException: %s (link=%s)\n", e.getMessage(), linkForRead);
                continue;
            } catch (IOException e) {
                //Непредвиденная ошибка. Завершаем работу программы
//                MyControllerPool.endAllControllersInPool();
                throw new RuntimeException(e);
            }
            if(document != null) {
                connectionBuffer.addReadWebPage(document);
                continue;
            }

            throw new RuntimeException(String.format(
                    "%s: failed to read page by link %s",
                    Thread.currentThread().getName(),
                    linkForRead));
        }
    }

    private Document readAndGet(String link) throws IOException {
        if(whenCanBeConnectAgain() == -1) {
            Document document = null;
            synchronized (this) {
                document = Jsoup.connect(link).get();
                prevConnectionTime = System.currentTimeMillis();
            }
            if(document != null) {
                return document;
            }
            throw new java.net.ConnectException("Failed to connect to the specified link");
        }
        throw new java.net.ConnectException("Too short time between connections");
    }

    //Повторное подключение возможно только через определенное время
    private int whenCanBeConnectAgain() {
        final long timeAfterPrevConnection = System.currentTimeMillis() - prevConnectionTime;
        if(timeAfterPrevConnection < RECONNECTION_DELAY) {
            return RECONNECTION_DELAY - (int)timeAfterPrevConnection;
        }
        return -1;
    }
}
