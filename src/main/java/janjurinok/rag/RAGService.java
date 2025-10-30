package janjurinok.rag;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Component
@Lazy
public class RAGService {
   List<DocumentChunk> documentChunks;
   private boolean isLoaded = false;

   private final EmbeddingGenerator embeddingGenerator;
   private final DocumentLoader documentLoader;


   public RAGService(EmbeddingGenerator embeddingGenerator, DocumentLoader documentLoader) {
      this.embeddingGenerator = embeddingGenerator;
      this.documentLoader = documentLoader;
   }

   @PostConstruct
   public void init() {
      ensureInitialized();
   }

   public synchronized void ensureInitialized(){
      if (isLoaded) {
         return;
      }

      System.out.println("🔹 Loading documents and generating embeddings...");
      try {
         if (Path.of("src/main/resources/chunks.json").toFile().exists()) {
            this.documentChunks = DocumentLoader.loadDocsFromJson("src/main/resources/chunks.json");
         } else {
            this.documentChunks = documentLoader.loadAllDocs("src/main/resources/docs");
         }
         isLoaded = true;
         System.out.println("✅ Document loading and embedding generation complete.");
      }
      catch (Exception e) {
         throw new RuntimeException("Failed to initialize RAGService: " + e.getMessage(), e);
      }
//      System.out.println("✅ Generated " + this.documentChunks.size() + " document chunks with embeddings.");
   }


   public List<DocumentChunk> getRelevantChunks(String query) throws Exception {
      ensureInitialized();

      File batch = BatchGenerator.createSingleBatchFile("src/main/resources/single_query", query);
      float[] queryEmbedding = embeddingGenerator.generateEmbeddings(batch);

      return VectorSearch.findTopK(this.documentChunks, queryEmbedding, 3);
   }
}
