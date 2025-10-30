package janjurinok.rag;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class VectorSearch {
   public static float cosineSimilarity(float[] a, float[] b) {
      float dot = 0, normA = 0, normB = 0;
      for (int i = 0; i < a.length; i++) {
         dot += a[i] * b[i];
         normA += a[i] * a[i];
         normB += b[i] * b[i];
      }
      return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
   }
   public static List<DocumentChunk> findTopK(
         List<DocumentChunk> chunks, float[] queryEmbedding, int k) {

      return chunks.stream()
            .sorted(Comparator.comparingDouble(
                  c -> -cosineSimilarity(c.getEmbedding(), queryEmbedding)))
            .limit(k)
            .collect(Collectors.toList());
   }
}
