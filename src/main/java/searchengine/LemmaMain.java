package searchengine;

import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class LemmaMain {
    public static void main(String[] args) {
        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";

        Map<String, Integer> stringIntegerMap = null;
        try {
            stringIntegerMap = LemmaFinder.getInstance().collectLemmas(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(stringIntegerMap);
    }
}
