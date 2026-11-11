package lab.ollama;

import java.util.List;

/** input — список, тому всі вектори рахуються за один виклик. */
public record EmbedRequest(String model, List<String> input) {
}
