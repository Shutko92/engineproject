package searchengine.dto.searching;

import lombok.Data;

import java.util.Set;

@Data
public class WordLemmas {
    private String word;
    private int index;
    private Set<String> lemmas;
}
