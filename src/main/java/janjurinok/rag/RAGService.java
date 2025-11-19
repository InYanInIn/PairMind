package janjurinok.rag;

import janjurinok.database.QdrantService;
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
   private boolean isInitialized = false;
   private final String COLLECTION_NAME = "document_chunks";
   private final EmbeddingGenerator embeddingGenerator;
   private final DocumentLoader documentLoader;
   private final QdrantService qdrantService;


   public RAGService(EmbeddingGenerator embeddingGenerator, DocumentLoader documentLoader, QdrantService qdrantService) {
      this.embeddingGenerator = embeddingGenerator;
      this.documentLoader = documentLoader;
      this.qdrantService = qdrantService;
   }

   @PostConstruct
   public void init() {
      ensureInitialized();
   }

   public synchronized void ensureInitialized(){
      if (isInitialized) {
         return;
      }

//      System.out.println("🔹 Loading documents and generating embeddings...");
//      try {
//         if (Path.of("src/main/resources/chunks.json").toFile().exists()) {
//            this.documentChunks = DocumentLoader.loadDocsFromJson("src/main/resources/chunks.json");
//         } else {
//            this.documentChunks = documentLoader.loadAllDocs("src/main/resources/docs");
//         }
//         isInitialized = true;
//         System.out.println("✅ Document loading and embedding generation complete.");
//      }
//      catch (Exception e) {
//         throw new RuntimeException("Failed to initialize RAGService: " + e.getMessage(), e);
//      }
//      System.out.println("✅ Generated " + this.documentChunks.size() + " document chunks with embeddings.");

      try {
         System.out.println("🔹 Initializing Qdrant collection and upserting document chunks...");

         if (!collectionExists(COLLECTION_NAME)){
            System.out.println("Creating Qdrant collection: " + COLLECTION_NAME);

            List<DocumentChunk> chunks = documentLoader.loadAllDocs("src/main/resources/docs");
//            List<DocumentChunk> chunks = DocumentLoader.loadDocsFromJson("src/main/resources/chunks.json");
            int vectorSize = chunks.get(0).getEmbedding().length;

            qdrantService.createCollectionIfNotExists(COLLECTION_NAME, vectorSize);
            qdrantService.upsertChunks(COLLECTION_NAME, chunks);
            System.out.println("✅ Upserted " + chunks.size() + " document chunks into Qdrant collection.");
         }
         else {
            System.out.println("Qdrant collection already exists: " + COLLECTION_NAME);
         }

         isInitialized = true;
      } catch (Exception e) {
         throw new RuntimeException("Failed to initialize Qdrant collection: " + e.getMessage(), e);
      }
   }

   private boolean collectionExists(String collectionName) {
      return qdrantService.collectionExists(collectionName);
   }


   public List<DocumentChunk> getRelevantChunks(String query) throws Exception {
      ensureInitialized();

      File batch = BatchGenerator.createSingleBatchFile("src/main/resources/single_query", query);
      float[] queryEmbedding = embeddingGenerator.generateEmbeddings(batch);

//      return VectorSearch.findTopK(this.documentChunks, queryEmbedding, 3);
      return qdrantService.search(queryEmbedding, 3);
   }
}
