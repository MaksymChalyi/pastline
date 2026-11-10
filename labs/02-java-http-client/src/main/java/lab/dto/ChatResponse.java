package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(
        ChatMessage message,
        @JsonProperty("total_duration") long totalDuration,
        @JsonProperty("eval_count") int evalCount) {
}
