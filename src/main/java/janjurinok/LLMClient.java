package janjurinok;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class LLMClient {
   private final String MODEL = "gemini-2.5-flash";

   private final Client client;

   public LLMClient() {
      this.client = new Client();
   }

   public String ask(String prompt) {
      try {
         GenerateContentResponse response = client.models.generateContent
               (
                     this.MODEL,
                     prompt,
                     null
               );
         return response.text();
      } catch (Exception e) {
         e.printStackTrace();
         return "⚠️ Error connecting to Gemini: " + e.getMessage();
      }
   }

   public static void main(String[] args) {
      // The client gets the API key from the environment variable `GEMINI_API_KEY`.
      Client client = new Client();

      GenerateContentResponse response =
            client.models.generateContent(
                  "gemini-2.5-flash",
                  "Explain how AI works in a few words",
                  null);

      System.out.println(response.text());
   }
}
