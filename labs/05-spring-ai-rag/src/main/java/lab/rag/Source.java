package lab.rag;

import org.springframework.ai.document.Document;

/** score = 1 − cosine distance від pgvector. */
public record Source(String id, String date, Double score, String text) {

    static Source from(Document doc) {
        return new Source(
                String.valueOf(doc.getMetadata().get("entryId")),
                String.valueOf(doc.getMetadata().get("date")),
                doc.getScore(),
                doc.getText());
    }
}
