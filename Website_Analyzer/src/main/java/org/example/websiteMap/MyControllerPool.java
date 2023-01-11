package org.example.websiteMap;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Пулл контроллеров реализован в виде статического класса
 * Map<String key, MyController obj> pool - хранит карту ключей и объектов контроллеров
 * List<String key> sorkedList - хранит ключи в порядке добавления. Необходим для
 *      реализации запуска и остановки в порядке добавления
 */
public class MyControllerPool {

    private final static Map<String, MyController> pool = new ConcurrentHashMap<>();
    private final static List<String> sortedKeys = new ArrayList<>();

    public static MyController getControllerInstanceByName(String key) {
        synchronized (sortedKeys) {
            return pool.get(key);
        }
    }

    public static void addToPoll(String key, MyController controller) {
        synchronized (sortedKeys) {
            sortedKeys.add(key);
            pool.put(key, controller);
        }
    }

    public static void startAllControllersInPool(String startLink) {
        synchronized (sortedKeys) {
            for(String key : sortedKeys) {
                pool.get(key).startWork(startLink);
            }
        }
    }

    public static void endAllControllersInPool() {
        synchronized (sortedKeys) {
            for(String key : sortedKeys) {
                pool.get(key).endWork();
            }
        }
    }
}
