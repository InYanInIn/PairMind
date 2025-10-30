package janjurinok.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BatchGenerator {

   private static final int BATCH_SIZE = 50;

   public static List<File> createBatches(List<String> texts, String outputDir) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      List<File> files = new java.util.ArrayList<>();

      int batchNum = 0;
      for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
         int end = Math.min(i + BATCH_SIZE, texts.size());
         List<String> batchTexts = texts.subList(i, end);

         ObjectNode root = mapper.createObjectNode();
         ArrayNode requests = mapper.createArrayNode();

         for (String text : batchTexts) {
            ObjectNode req = mapper.createObjectNode();
            req.put("model", "models/gemini-embedding-001");

            ObjectNode content = mapper.createObjectNode();
            ArrayNode parts = mapper.createArrayNode();

            ObjectNode part = mapper.createObjectNode();
            part.put("text", text);
            parts.add(part);

            content.set("parts", parts);
            req.set("content", content);

            requests.add(req);
         }

         root.set("requests", requests);

         File file = new File(outputDir, "batch_" + batchNum + ".json");
         mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
//         System.out.println("Created batch JSON file: " + file.getAbsolutePath());

         files.add(file);
         batchNum++;
      }

      return files;
   }

   public static File createSingleBatchFile(String outputDir, String text) {
      try {


         ObjectMapper mapper = new ObjectMapper();

         ObjectNode root = mapper.createObjectNode();
         root.put("model", "models/gemini-embedding-001");

         ObjectNode content = mapper.createObjectNode();
         ArrayNode parts = mapper.createArrayNode();

         ObjectNode part = mapper.createObjectNode();
         part.put("text", text);
         parts.add(part);

         content.set("parts", parts);
         root.set("content", content);

         File file = new File(outputDir, "single_batch.json");
         mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
//         System.out.println("Created batch JSON file: " + file.getAbsolutePath());

         return file;
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }
}
