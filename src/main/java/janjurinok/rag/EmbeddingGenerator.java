package janjurinok.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EmbeddingGenerator {
   private static final int API_DELAY = 70000;
   private static final String API_KEY = System.getenv("GOOGLE_API_KEY");
   public static List<float[]> generateEmbeddingsBatch(List<File> batch_files, String outputFile) {
      Path outputFilePath = Path.of(outputFile);
      List<float[]> embeddingsList = new ArrayList<>();
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode combinedResponse = mapper.createObjectNode();
      ArrayNode allResponses = mapper.createArrayNode();


      try {

         HttpClient client = HttpClient.newHttpClient();

         for (File batchFile : batch_files){
            long start = System.currentTimeMillis();

            System.out.println("Sleeping for delay to respect API rate limits...");
            while (System.currentTimeMillis() - start < API_DELAY) {
               try {
                  Thread.sleep(1000); // sleep 1 second, check again
               } catch (InterruptedException e) {
                  System.err.println("Sleep interrupted, resuming...");
                  // Just continue sleeping until full 2 minutes have passed
               }
            }
            System.out.println("Resuming after delay.");


            System.out.println("Processing batch file: " + batchFile.getName());
            HttpRequest request = HttpRequest.newBuilder()
                  .uri(new URI("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:batchEmbedContents"))
                  .header("x-goog-api-key", API_KEY)
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofFile(batchFile.toPath()))
                  .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//            System.out.println("Response for batch " + ": " + response.body());

            JsonNode json = mapper.readTree(response.body());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("output.json"), json);
            JsonNode embeddingsNode = json.get("embeddings");
            if (embeddingsNode == null || !embeddingsNode.isArray()) {
               System.err.println("❌ No 'embeddings' field in API output:\n" + json.toPrettyString());
               continue;
            }

            for (JsonNode embNode : embeddingsNode) {
               JsonNode valuesNode = embNode.get("values");
               if (valuesNode == null || !valuesNode.isArray()) continue;

               float[] emb = new float[valuesNode.size()];
               for (int i = 0; i < valuesNode.size(); i++) {
                  emb[i] = (float) valuesNode.get(i).asDouble();
               }
               embeddingsList.add(emb);
               allResponses.add(embNode);
            }
         }

         combinedResponse.set("responses", allResponses);
         mapper.writerWithDefaultPrettyPrinter().writeValue(outputFilePath.toFile(), combinedResponse);
         System.out.println("Saved combined response to " + outputFilePath.toAbsolutePath());

      } catch (Exception e) {
         e.printStackTrace();
      }

      return embeddingsList;
   }

   public static float[] generateEmbeddings(File batchFile) {;
      ObjectMapper mapper = new ObjectMapper();

      HttpClient client = HttpClient.newHttpClient();

      try {
         HttpRequest request = HttpRequest.newBuilder()
               .uri(new URI("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent"))
               .header("x-goog-api-key", API_KEY)
               .header("Content-Type", "application/json")
               .POST(HttpRequest.BodyPublishers.ofFile(batchFile.toPath()))
               .build();

         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

         JsonNode json = mapper.readTree(response.body());
         mapper.writerWithDefaultPrettyPrinter().writeValue(new File("output.json"), json);
         JsonNode embeddingNode = json.get("embedding");
         if (embeddingNode == null) {
            System.err.println("❌ No 'embeddings' field in API output:\n" + json.toPrettyString());
            return new float[0];
         }

         JsonNode valuesNode = embeddingNode.get("values");
         if (valuesNode == null || !valuesNode.isArray()) {
            System.err.println("❌ No 'values' field in embedding node:\n" + embeddingNode.toPrettyString());
            return new float[0];
         }

         float[] emb = new float[valuesNode.size()];
         for (int i = 0; i < valuesNode.size(); i++) {
            emb[i] = (float) valuesNode.get(i).asDouble();
         }
         return emb;


      } catch (URISyntaxException | IOException | InterruptedException e) {
         throw new RuntimeException(e);
      }
   }
}
