package janjurinok.agents;

import janjurinok.LLMClient;
import janjurinok.rag.BatchGenerator;
import janjurinok.rag.DocumentLoader;
import janjurinok.rag.EmbeddingGenerator;
import janjurinok.rag.VectorSearch;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentRouter {
   private final LLMClient llm;
   private final BillingAgent billingAgent;
   private final TechnicalAgent technicalAgent;
   private final EmbeddingGenerator embeddingGenerator;
   private final DocumentLoader documentLoader;

   private final List<String> conversationHistory = new ArrayList<>();
   private final Map<String, float[]> agentEmbeddings = new HashMap<>();
   private String lastActiveAgent = null;
   private String userEmail = null;
   private final Path EMB_PATH = Path.of("src/main/resources/agent_embeddings.json");

   public AgentRouter(LLMClient llm, BillingAgent billingAgent, TechnicalAgent technicalAgent, EmbeddingGenerator embeddingGenerator, DocumentLoader documentLoader) {
      this.llm = llm;
      this.billingAgent = billingAgent;
      this.technicalAgent = technicalAgent;
      this.embeddingGenerator = embeddingGenerator;
      this.documentLoader = documentLoader;
   }

   @PostConstruct
   public void init() {
      initializeAgentProfiles();
   }


   private synchronized void initializeAgentProfiles() {
      try {
         List<float[]> agent_embeddings;
         if (Path.of("src/main/resources/agent_profiles/tech_chunk.json").toFile().exists() &&
               Path.of("src/main/resources/agent_profiles/bill_chunk.json").toFile().exists()) {
            agent_embeddings = DocumentLoader.loadAgentsFromJson("src/main/resources/agent_profiles/tech_chunk.json", "src/main/resources/agent_profiles/bill_chunk.json");
         } else {
            agent_embeddings = documentLoader.loadAllAgents("src/main/resources/agent_profiles");
         }

         agentEmbeddings.put("TechnicalAgent", agent_embeddings.get(0));
         agentEmbeddings.put("BillingAgent", agent_embeddings.get(1));
      } catch (IOException e) {
         throw new RuntimeException("Failed to load agent profiles: " + e.getMessage(), e);
      }
   }

   public String handleUserMessage(String userInput, String email, String sessionId) {
      if (email != null && !email.isEmpty()) {
         this.userEmail = email;
      }
      conversationHistory.add("User: " + userInput);

      String agentDecision;
      if (conversationHistory.size()>1&&conversationHistory.get(conversationHistory.size()-2).contains("👀")||userInput.split(" ").length<3){
         agentDecision = decideAgentWithLLM(userInput);
      }
      else {
//         System.out.println(conversationHistory);
         agentDecision = decideAgent(userInput);
      }

      String agentResponse;
      switch (agentDecision) {
         case "TechnicalAgent":
            agentResponse = technicalAgent.respond(userInput, sessionId);
            lastActiveAgent = "TechnicalAgent";
            break;
         case "BillingAgent":
            agentResponse = billingAgent.respond("Email: "+ this.userEmail + "\n" + userInput, sessionId);
            lastActiveAgent = "BillingAgent";
            break;
         default:
            agentResponse = "🤖 I’m not sure which department should handle this. ";
      }

      conversationHistory.add(agentDecision + ": " + agentResponse);
      return agentResponse;
   }

   private String decideAgent(String userInput) {

      File batch = BatchGenerator.createSingleBatchFile("src/main/resources/single_query", userInput);
      float[] queryEmbedding = embeddingGenerator.generateEmbeddings(batch);

      String bestAgent = "None";
      float bestScore = -Float.MAX_VALUE;

      for (Map.Entry<String, float[]> entry : agentEmbeddings.entrySet()) {
         String agentName = entry.getKey();
         float[] emb = entry.getValue();

         if (emb == null) continue;
         float score = VectorSearch.cosineSimilarity(emb, queryEmbedding);
         if (score > bestScore) {
            bestScore = score;
            bestAgent = agentName;
         }

      }
//      System.out.println("Agent routing scores: " + bestAgent + " with score " + bestScore);
      final float HIGH_CONF = 0.56f;
      final float LOW_CONF = 0.55f;

      if (bestScore >= HIGH_CONF) {
         return bestAgent;
      } else if (bestScore >= LOW_CONF) {
         return decideAgentWithLLM(userInput);
      } else {
         // low confidence — fallback
         return "None";
      }
   }

   private String decideAgentWithLLM(String userInput) {
      String history = String.join("\n", conversationHistory);

      String prompt = """
                You are an AI coordinator for a customer support system.
                There are two available agents:
                1. TechnicalAgent — answers questions about documentation, troubleshooting, installations, commands, or updates based on the documentation.
                2. BillingAgent — handles questions about refunds, payments, plans, prices, and billing policies.

                Based on the user's latest message and the conversation so far, 
                decide which agent should respond next.

                Respond with ONLY ONE of the following words:
                - "TechnicalAgent"
                - "BillingAgent"
                - "None" (if the message is unrelated to both)

                Conversation so far:
                %s

                User message: "%s"
                """.formatted(history, userInput);

      String raw = llm.ask(prompt);
      if (raw == null) return "None";

      raw = raw.toLowerCase();
      if (raw.contains("technical")) return "TechnicalAgent";
      if (raw.contains("billing")) return "BillingAgent";
      return "None";
   }

}
