package janjurinok.rag;

import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Component
public class RAGService {
   List<DocumentChunk> documentChunks;


   public RAGService() {
      System.out.println("🔹 Loading documents and generating embeddings...");
      if (Path.of("src/main/resources/chunks.json").toFile().exists()) {
         try {
            this.documentChunks = DocumentLoader.loadDocsFromJson("src/main/resources/chunks.json");
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      } else {
         try {
            this.documentChunks = DocumentLoader.loadAllDocs("src/main/resources/docs");
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }
//      System.out.println("✅ Generated " + this.documentChunks.size() + " document chunks with embeddings.");
   }

   public List<DocumentChunk> getRelevantChunks(String query) throws Exception {
      File batch = BatchGenerator.createSingleBatchFile("src/main/resources/single_query", query);
      float[] queryEmbedding = EmbeddingGenerator.generateEmbeddings(batch);

      return VectorSearch.findTopK(this.documentChunks, queryEmbedding, 3);
   }
}
