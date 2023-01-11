package org.example.websiteMap.analyzer;


import org.example.websiteMap.BufferedIO;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class RunnableAnalyzer implements Runnable {

    private final Document document;
    private static volatile String rootLink;
    private static volatile BufferedIO<String, ?> destinationBuffer;

    protected static void setRootLink(String link) {
        rootLink = link;
    }

    protected static void setDestinationBuffer(BufferedIO<String, ?> buffer) {
        destinationBuffer = buffer;
    }


    protected RunnableAnalyzer(Document document) {
        this.document = document;
    }

    @Override
    public void run() {
        //Парсинг ссылок со страницы, приведение к верному формату
        final Elements elements = document.select("a[href]");
        final Set<String> readLinks = elements.stream()
                .map(link -> link.attr("href"))
                .collect(Collectors.toSet());
        final Set<String> validLinks = convertAllLinksToValid(readLinks);

        //Загрузка новых ссылок в хранилище
        if(validLinks.size() > 0)
            uploadNewLinksToStorage(validLinks);
    }

    //Фильтрация и преобразование ссылок к нужному формату
    private Set<String> convertAllLinksToValid(Set<String> links) {
        Set<String> newSet = new HashSet<>();
        for(String link : links) {
            if(link.length() < 2)
                continue;
            link = getAbsoluteLink(link);
            if(isLinkPermitted(link)) {
                newSet.add(link);
            }
        }
        return newSet;
    }

    private String getAbsoluteLink(String link) {
        if(!link.startsWith("http") && link.charAt(0) == '/' && link.charAt(1) != '/') {
            link = rootLink + link.substring(1);
        }
        return link;
    }

    /*
     *  Фильтрация ссылок по всем приведенным параметрам:
     *  - удаление ссылок на другие сайты и поддомены
     *  - удаление ссылок на внутренние элементы страницы (содержат # после адреса страницы)
     *  False, если ссылка не соответствует требованиям
     */
    private boolean isLinkPermitted(String link) {
        if(link.startsWith(rootLink) && !link.contains("#")){
            return true;
        }
        return false;
    }

    private void uploadNewLinksToStorage(Set<String> links) {
        destinationBuffer.sendNewMultipleRequest(links.toArray(new String[0]));
    }
}
