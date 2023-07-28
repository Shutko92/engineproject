package searchengine;

import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LemmaMain {
    public static void main(String[] args) {
        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";

//        Map<String, Integer> stringIntegerMap = null;
//        try {
//            stringIntegerMap = LemmaFinder.getInstance().collectLemmas(text);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println(stringIntegerMap);
        Map<String, Integer> mapToSort = new HashMap<>();
        mapToSort.put("a", 23);
        mapToSort.put("s", 4);
        mapToSort.put("f", 47);
        mapToSort.put("d", 18);
        mapToSort.put("h", 8);

//        LinkedHashMap<String, Integer> collect = mapToSort.entrySet().stream()
//                .sorted(Map.Entry.comparingByValue())
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue,
//                        (a, b) -> a,
//                        LinkedHashMap::new));
//        System.out.println(collect.keySet());

        List<Integer> myList = new ArrayList<>();
        myList.add(2);
        myList.add(4);
        myList.add(5);
        myList.add(8);
        System.out.println(myList.get(0));
    }
}
