package org.example.websiteMap.analyzer;

import org.example.websiteMap.BufferedIO;
import org.example.websiteMap.MyController;
import org.example.websiteMap.MyControllerPool;
import org.example.websiteMap.connector.ConnectionController;
import org.example.websiteMap.storage.StorageController;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
 * Класс-поток, реализующий динамическое распередение
 * нагрузки по чтению и анализу html страниц, которые находятся
 * в буфере объекта ConnectionController.
 * Класс автоматически завершает работу программы
 * по определенному условию: если после n обращений к буферу
 * объекта ConnectionController подряд, в буфере не появляется
 * новых страниц для чтения (где n - RECONNECTIONS_BEFORE_PROGRAM_EXIT)
 */
public class AnalyzeController extends Thread implements MyController {

    private static final int REQUESTS_BEFORE_PROGRAM_EXIT = 3;
    private static final long REPEAT_REQUEST_DELAY_TIME_MILLIS = 200;
    private List<String> resultSortedList = null;

    private volatile String startLink;
    private volatile BufferedIO<String, String> storageController;
    private volatile BufferedIO<String, Document> connectionController;


    private volatile boolean isActive = true;
    private volatile ExecutorService service;


    public AnalyzeController() {
        service = Executors.newCachedThreadPool();
    }

    @Override
    public void startWork(String startLink) {
        connectionController = (ConnectionController) MyControllerPool
                .getControllerInstanceByName(ConnectionController.class.getName());
        storageController = (StorageController) MyControllerPool
                .getControllerInstanceByName(StorageController.class.getName());
        this.startLink = startLink;

        RunnableAnalyzer.setDestinationBuffer(storageController);
        RunnableAnalyzer.setRootLink(startLink);
        this.start();
    }

    @Override
    public void endWork() {
//        service.shutdown();
        service.shutdownNow();
        while(!service.isTerminated()) {
            sleep();
        }
        Thread.currentThread().interrupt();
//        System.exit(0);
    }


    @Override
    public void run() {
        while(!isInterrupted()) {
            releaseStorageBuffer();
            checkInputBuffer();

            //Необходимо завершить работу, если в процессе проверок поток был прерван
            if(isInterrupted())
                break;

            //Если поток не был прерван в течение проверок, то добавить новую задачу
            service.execute(new RunnableAnalyzer(connectionController.pollNextResponse()));
        }

    }

    /*
     * Проверка входного буфера. Завершение работы,
     * если буфер пуст после некоторое кол-во обращений подряд
     */
    private void checkInputBuffer() {
        /*
         * Проверить состояние буфера подключения:
         * наличие ссылок на подключение и наличие
         * прочитанных html страниц, ожидающих проверку
         */
        int requestsCounter = 0;
        while(!connectionController.hasNextResponse() && !isInterrupted()) {
            //Если на входе буфера нет необработанных ссылок
            if(!connectionController.hasUnreadRequestsInQueue()) {
                requestsCounter++;
            }
            //Если превышено кол-во повторных запросов на чтение буфера
            //То завершаем работу потоков
            if(requestsCounter >= REQUESTS_BEFORE_PROGRAM_EXIT) {
                MyControllerPool.endAllControllersInPool();
            }
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(REPEAT_REQUEST_DELAY_TIME_MILLIS);
        } catch (InterruptedException e) {
            System.out.println("Analyze Controller was interrupted!");
            this.interrupt();
        }
    }

    //Отправить ранее найденные ссылки в очередь на подключение
    private void releaseStorageBuffer() {
        while (storageController.hasNextResponse()) {
            connectionController.sendNewRequest(storageController.pollNextResponse());
        }
    }
}
