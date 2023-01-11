package org.example.websiteMap;

public interface MyController {
    void startWork(String rootLink);
    void endWork();
    default String poolKey() {
        return this.getClass().getName();
    }
}
