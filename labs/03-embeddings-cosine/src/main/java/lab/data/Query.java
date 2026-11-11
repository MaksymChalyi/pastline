package lab.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Порожній expected — правильної відповіді немає. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Query(String id, String text, List<String> expected) {

    public boolean hasAnswer() {
        return expected != null && !expected.isEmpty();
    }
}
