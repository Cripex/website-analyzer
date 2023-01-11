package org.example.websiteMap.storage;

import java.util.*;

class LinksStorage {

    private final Set<String> storage;
    private final Queue<String> linksToConnect;


    protected LinksStorage(String startLink) {
        storage = new HashSet<>();
        linksToConnect = new LinkedList<>();
        storage.add(startLink);
        linksToConnect.add(startLink);
    }

    public synchronized void createIfNotAlreadyExist(String linkName) {
        if (!storage.contains(linkName)) {
            storage.add(linkName);
            linksToConnect.add(linkName);
        }
    }

    public void createIfNotAlreadyExist(String[] links) {
        for(String linkName : links) {
            createIfNotAlreadyExist(linkName);
        }
    }

    public synchronized boolean constains(String linkName) {
        return storage.contains(linkName);
    }

    public List<String> getSortedList() {
        final List<String> sortedList = new ArrayList<>(storage.size());
        synchronized (this) {
            storage.stream().forEach(link -> sortedList.add(link));
        }
        Collections.sort(sortedList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        return sortedList;
    }

    public int getLinkSizeInStorage() {
        return storage.size();
    }

    public synchronized String pollUnreadLink() {
        return linksToConnect.poll();
    }

    public synchronized boolean hasUnreadLinks() {
        return linksToConnect.size() > 0;
    }
}
