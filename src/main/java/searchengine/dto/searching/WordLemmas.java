package searchengine.dto.searching;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
public class WordLemmas {
    private String word;
    private int index;
    private Set<String> lemmas;
}
