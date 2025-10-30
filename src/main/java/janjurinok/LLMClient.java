package janjurinok;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class LLMClient {
   private final String MODEL = "gemini-2.5-flash";

   private Client client;

   @Value("${google.api.key}")
   String apiKey;

   public LLMClient() {
   }

   @PostConstruct
   public void init() {
      if (apiKey == null || apiKey.isEmpty()) {
         throw new IllegalArgumentException("API key must be set in GOOGLE_API_KEY or application.properties");
      }
      this.client = Client.builder().apiKey(apiKey).build();
   }

   public String ask(String prompt) {
      int max_retries = 3;
      int delay = 2000;

      for (int attempt = 1; attempt <= max_retries; attempt++) {
         try {
            GenerateContentResponse response = client.models.generateContent
                  (
                        this.MODEL,
                        prompt,
                        null
                  );
            return response.text();
         } catch (Exception e) {
            if (attempt == max_retries) {
               e.printStackTrace();
               return "⚠️ Error connecting to Gemini after " + max_retries + " attempts: " + e.getMessage();
            }
            try {
               Thread.sleep(delay);
            } catch (InterruptedException ie) {
               Thread.currentThread().interrupt();
            }
            delay += 1000;
         }
      }
      return "⚠️ Unexpected error in LLMClient.";
   }

//   public static void main(String[] args) {
//      // The client gets the API key from the environment variable `GEMINI_API_KEY`.
//      Client client = new Client();
//
//      GenerateContentResponse response =
//            client.models.generateContent(
//                  "gemini-2.5-flash",
//                  "Explain how AI works in a few words",
//                  null);
//
//      System.out.println(response.text());
//   }
}
