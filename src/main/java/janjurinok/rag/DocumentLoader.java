package janjurinok.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static janjurinok.rag.BatchGenerator.createBatches;
import static janjurinok.rag.EmbeddingGenerator.generateEmbeddingsBatch;

@Component
public class DocumentLoader {
   private static String API_KEY;
   private static final int BATCH_SIZE = 50;

   public DocumentLoader(@Value("${google.api.key}") String apiKey) {
      API_KEY = apiKey;
   }

   public static List<DocumentChunk> loadAllDocs(String dirPath) throws IOException {
      List<DocumentChunk> chunks = new ArrayList<>();
      File folder = new File(dirPath);

      List<String> allBlocks = new ArrayList<>();
      List<String> blockSources = new ArrayList<>();

      for (File file : Objects.requireNonNull(folder.listFiles())) {
         if (file.isFile() && file.getName().endsWith(".txt")) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (int i = 0; i < lines.size(); i += 5) {
               String blockText = String.join("\n", lines.subList(i, Math.min(i + 5, lines.size())));
               allBlocks.add(blockText);
               blockSources.add(file.getName());
            }
         }
      }

      List<File> batch_files = createBatches(allBlocks, "src/main/resources/batches");
      List<float[]> embeddingsList = generateEmbeddingsBatch(batch_files, "src/main/resources/embeddings.json");

//      System.out.println(embeddingsList.size() + " embeddings generated.");
//      System.out.println(allBlocks.size() + " document blocks processed.");
      for (int i = 0; i < allBlocks.size(); i++) {
         DocumentChunk chunk = new DocumentChunk(allBlocks.get(i), embeddingsList.get(i), blockSources.get(i));
         chunks.add(chunk);
      }

      saveChunksToJson(chunks, "src/main/resources/chunks.json");

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
}
