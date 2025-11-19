package janjurinok.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import janjurinok.database.QdrantService;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static janjurinok.rag.BatchGenerator.createBatches;

@Component
public class DocumentLoader {
   private static final int BATCH_SIZE = 50;

   private final EmbeddingGenerator embeddingGenerator;
   private final QdrantService qdrantService;

   public DocumentLoader(EmbeddingGenerator embeddingGenerator, QdrantService qdrantService) {
      this.embeddingGenerator = embeddingGenerator;
      this.qdrantService = qdrantService;
   }


   public List<DocumentChunk> loadAllDocs(String dirPath) throws IOException {

      List<DocumentChunk> chunks = new ArrayList<>();
      File folder = new File(dirPath);

      List<String> allBlocks = new ArrayList<>();
      List<String> blockSources = new ArrayList<>();

      for (File file : Objects.requireNonNull(folder.listFiles())) {
         if (file.isFile() && file.getName().endsWith(".txt")) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (int i = 0; i < lines.size(); i += 5) {
               int startContext = Math.max(0, i - 1);
               int endContext = Math.min(lines.size(), i + 5 + 1);
               String contextText = String.join("\n", lines.subList(startContext, endContext));


               allBlocks.add(contextText);
               blockSources.add(file.getName());
            }
         }
      }
      File batchDir = new File("src/main/resources/batches");
      if (!batchDir.exists()) {
         batchDir.mkdirs();
      }

      List<File> batch_files = createBatches(allBlocks, "src/main/resources/batches");
      List<float[]> embeddingsList = embeddingGenerator.generateEmbeddingsBatch(batch_files, "src/main/resources/embeddings.json");

//      System.out.println(embeddingsList.size() + " embeddings generated.");
//      System.out.println(allBlocks.size() + " document blocks processed.");
      for (int i = 0; i < allBlocks.size(); i++) {
         DocumentChunk chunk = new DocumentChunk(allBlocks.get(i), embeddingsList.get(i), blockSources.get(i));
         chunks.add(chunk);
      }

//      saveChunksToJson(chunks, "src/main/resources/chunks.json");

      return chunks;
   }


//   public static List<DocumentChunk> generateChunks(List<String> texts, List<String> sources, List<float[]> embeddings) {
//      List<DocumentChunk> chunks = new ArrayList<>();
//      for (int i = 0; i < texts.size(); i++) {
//         DocumentChunk chunk = new DocumentChunk(texts.get(i), embeddings.get(i), sources.get(i));
//         chunks.add(chunk);
//      }
//      return chunks;
//   }


   public static List<DocumentChunk> loadDocsFromJson(String jsonFilePath) throws IOException {
      List<DocumentChunk> chunks = new ArrayList<>();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(new File(jsonFilePath));

      if (root.isArray()) {
         for (JsonNode node : root) {
            String source = node.get("source").asText();
            String text = node.get("text").asText();
            JsonNode embNode = node.get("embedding");
            float[] embedding = new float[embNode.size()];
            for (int i = 0; i < embNode.size(); i++) {
               embedding[i] = (float) embNode.get(i).asDouble();
            }
            DocumentChunk chunk = new DocumentChunk(text, embedding, source);
            chunks.add(chunk);
         }
      }

      return chunks;
   }

   public static void saveChunksToJson(List<DocumentChunk> chunks, String outputPath) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      ArrayNode array = mapper.createArrayNode();

      for (DocumentChunk chunk : chunks) {
         ObjectNode node = mapper.createObjectNode();
         node.put("source", chunk.getSourceFile());
         node.put("text", chunk.getText());
         ArrayNode embArray = mapper.createArrayNode();
         for (float v : chunk.getEmbedding()) embArray.add(v);
         node.set("embedding", embArray);
         array.add(node);
      }

      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), array);
      System.out.println("💾 Saved chunks to " + outputPath);
   }

   public static List<float[]> loadAgentsFromJson(String tech_path, String bill_path) throws IOException {
      List<float[]> agent_embeddings = new ArrayList<>();
      ObjectMapper mapper = new ObjectMapper();

      for (String path : new String[]{tech_path, bill_path}) {
         JsonNode root = mapper.readTree(new File(path));
         if (root.isArray()) {
            for (JsonNode node : root) {
               JsonNode embNode = node.get("embedding");
               float[] embedding = new float[embNode.size()];
               for (int i = 0; i < embNode.size(); i++) {
                  embedding[i] = (float) embNode.get(i).asDouble();
               }
               agent_embeddings.add(embedding);
            }
         }
      }

      return agent_embeddings;
   }


   public List<float[]> loadAllAgents(String AGENT_COLLECTION) throws Exception {

      if (qdrantService.collectionExists(AGENT_COLLECTION)) {
         List<DocumentChunk> existing_chunks = qdrantService.getAllChunks(AGENT_COLLECTION);
         if (existing_chunks.size() >= 2) {
            System.out.println("✅ Agent profiles already exist in Qdrant collection: " + AGENT_COLLECTION);
            List<float[]> agent_embeddings = new ArrayList<>();
            for (DocumentChunk chunk : existing_chunks) {
               agent_embeddings.add(chunk.getEmbedding());
            }
            return agent_embeddings;
         }
      }

      String tech_query = """
                     Technical support and product usage questions:
                     - Installing and setting up the application or service
                     - Troubleshooting crashes, errors, performance or startup issues
                     - Configuration, environment variables, API keys and integration steps
                     - Running commands, logs analysis, debugging steps, and stack traces
                     - Updating, upgrading, or uninstalling the software
                     - Connectivity, database, authentication and deployment problems
                     - "My app crashes on launch", "How to enable X feature", "Where are the logs?"
                     """;
      String bill_query = """
                  Billing, payments and subscription questions:
                  - Refund requests, charge disputes and billing errors
                  - Subscription plans, features by plan, and pricing differences
                  - Upgrading, downgrading or cancelling subscriptions
                  - Payment methods, invoices, tax receipts and billing cycles
                  - Refund timeframes, pro-rata charges and trial conversions
                  - Single-token examples: Pro, Premium, Standard, Basic
                  - Keywords & short forms: refund, cancel, invoice, charge, billing, price
                  - Example phrasings: "I was charged twice", "How do I cancel my subscription", "Where is my invoice?"
                  """;


      File tech_batch = BatchGenerator.createSingleBatchFile("src/main/resources/tech_agent", tech_query);
      File bill_batch = BatchGenerator.createSingleBatchFile("src/main/resources/bill_agent", bill_query);
      float[] tech_queryEmbedding = embeddingGenerator.generateEmbeddings(tech_batch);
      float[] bill_queryEmbedding = embeddingGenerator.generateEmbeddings(bill_batch);

      DocumentChunk tech_chunk = new DocumentChunk(tech_query, tech_queryEmbedding, "technical_agent");
      DocumentChunk bill_chunk = new DocumentChunk(bill_query, bill_queryEmbedding, "billing_agent");

      int vectorSize = tech_queryEmbedding.length;
      qdrantService.createCollectionIfNotExists(AGENT_COLLECTION, vectorSize);
      qdrantService.upsertChunks(AGENT_COLLECTION, List.of(tech_chunk, bill_chunk));

      System.out.println("✅ Upserted agent profiles into Qdrant collection: " + AGENT_COLLECTION);

//      saveChunksToJson(List.of(tech_chunk), "src/main/resources/agent_profiles/tech_chunk.json");
//      saveChunksToJson(List.of(bill_chunk), "src/main/resources/agent_profiles/bill_chunk.json");

      List<float[]> agent_embeddings = new ArrayList<>();
      agent_embeddings.add(tech_queryEmbedding);
      agent_embeddings.add(bill_queryEmbedding);
      return agent_embeddings;
   }

}
