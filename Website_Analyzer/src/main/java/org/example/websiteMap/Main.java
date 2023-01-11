package org.example.websiteMap;



import org.example.websiteMap.analyzer.AnalyzeController;
import org.example.websiteMap.connector.ConnectionController;
import org.example.websiteMap.storage.StorageController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 * Старт программы
 */
public class Main {

    private static final Properties properties = readProperties(
            "src/main/resources/configuration.properties");
    private static final String startLink = properties.getProperty("init.startlink");

    public static void main(String[] args) throws InterruptedException, FileNotFoundException {

        initControllers();
        //Запустить контроллеры из пула
        MyControllerPool.startAllControllersInPool(startLink);

        startDaemonLogThread(System.out, 1000);

        //Application Threads waiting
        List<String> resultList = waitForResult();

        saveLinksToLocalFile(resultList,
                Path.of(properties.getProperty("result.file_path").toString()));
        System.exit(0);
    }

    private static List<String> waitForResult() {
        //Условие завершения работы программы
        switch (properties.get("program.interrupt_trigger").toString()) {
            case "onClick":
                stopThreadsOnClick();
                break;
            case "waitEndScan":
            default:
                joinAnalyzeController();
                break;
        }

        StorageController storageController = (StorageController) MyControllerPool
                .getControllerInstanceByName(StorageController.class.getName());
        return storageController.getLinksList();
    }


    private static void initControllers() throws InterruptedException {
        //Создать объекты классов контроллеров
        MyController[] controllers = new MyController[] {
                new ConnectionController(),
                new StorageController(),
                new AnalyzeController()
        };
        //Добавить в пул все классы контроллеры
        for(MyController controller : controllers) {
            MyControllerPool.addToPoll(controller.poolKey(), controller);
        }
    }

    private static void startDaemonLogThread(PrintStream printStream, int logRateMillis) {
        final ScheduledExecutorService logService = Executors.newSingleThreadScheduledExecutor();
        final StorageController storage = (StorageController) MyControllerPool
                .getControllerInstanceByName(StorageController.class.getName());

        logService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                printStream.println(String.format(
                        "%s - %d links in storage",
                        new Date(),
                        storage.getLinksListSize()));
            }
        }, 0, logRateMillis, TimeUnit.MILLISECONDS);
    }


    private static void saveLinksToLocalFile(Collection<String> links, Path filePath) throws FileNotFoundException {
        final Path parentPath = filePath.getParent();
        if(!Files.exists(parentPath) || !Files.isDirectory(parentPath)) {
            throw new FileNotFoundException("Unknown directory!");
        }
        final File file = filePath.toFile();
        try(FileWriter writer = new FileWriter(file)) {
            for(String link : links) {
                StringBuilder builder = new StringBuilder();
                int spaceNumber = countSpaceNumber(link);
                while(--spaceNumber >= 0) {
                    builder.append("\t");
                }
                builder.append(String.format("%s\n", link));
                writer.write(builder.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int countSpaceNumber(String targetString) {

        final String clippedLink = targetString.substring(startLink.length());
        if(clippedLink.length() < 2) {
            return 0;
        }
        final String[] linkParts = clippedLink.split("/");
        return linkParts.length;
    }


    private static void stopThreadsOnClick() {
        new Scanner(System.in).nextLine();
        MyControllerPool.endAllControllersInPool();
    }

    private static void joinAnalyzeController() {
        Thread analyzeController = (Thread) MyControllerPool
                .getControllerInstanceByName(AnalyzeController.class.getName());
        try {
            analyzeController.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties readProperties(String path) {
        Properties properties = new Properties();
        try(InputStream inputStream = new FileInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
