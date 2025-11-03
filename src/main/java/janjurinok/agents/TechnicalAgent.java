package janjurinok.agents;

import janjurinok.LLMClient;
import janjurinok.rag.DocumentChunk;
import janjurinok.rag.DocumentLoader;
import janjurinok.rag.RAGService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TechnicalAgent implements Agent {
   private final LLMClient llm;
   private final RAGService ragService;


   public TechnicalAgent(LLMClient llm, RAGService ragService) {
      this.llm = llm;
      this.ragService = ragService;
   }

   @Override
   public String respond(String userInput, String sessionId) {
      try {

         List<DocumentChunk> relevantChunks = ragService.getRelevantChunks(userInput);

         StringBuilder contextBuilder = new StringBuilder();
         int i = 1;
         for (DocumentChunk chunk : relevantChunks) {
            contextBuilder
                  .append("=== Document ").append(i++).append(" (source: ").append(chunk.getSourceFile()).append(") ===\n")
                  .append(chunk.getText()).append("\n\n");
         }

         String prompt = """
                     You are a Technical Support Specialist.
                     Use ONLY the information from the documentation below to answer the question.
                     If none of the documents clearly answer, say: "The documentation does not specify this."
                     
                     Documentation excerpts:
                     %s
                     
                     Question: %s
                     """.formatted(contextBuilder.toString(), userInput);

//         System.out.println("📝 Prompt to LLM:\n" + prompt);

         return llm.ask(prompt);

      } catch (Exception e) {
         e.printStackTrace();
         return "⚠️ Error in TechnicalAgent: " + e.getMessage();
      }
   }
}
