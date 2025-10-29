package janjurinok.agents;

import janjurinok.LLMClient;
import janjurinok.utils.DocumentLoader;

public class TechnicalAgent implements Agent {
   private final LLMClient llm;
   private final DocumentLoader loader;

   public TechnicalAgent() {
      this.llm = new LLMClient();
      this.loader = new DocumentLoader("./docs");
   }

   @Override
   public String respond(String userInput) {
      String context = loader.findRelevantDocs(userInput);
      String prompt = "Answer the question using only the following docs:\n" + context +
            "\n\nQuestion: " + userInput;
      return llm.ask(prompt);
   }
}
