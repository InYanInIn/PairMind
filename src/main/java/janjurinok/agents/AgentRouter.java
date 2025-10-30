package janjurinok.agents;

import janjurinok.LLMClient;

import java.util.ArrayList;
import java.util.List;

public class AgentRouter {
   private final LLMClient llm;
   private final BillingAgent billingAgent;
   private final TechnicalAgent technicalAgent;

   private final List<String> conversationHistory = new ArrayList<>();
   private String lastActiveAgent = null;

   public AgentRouter() {
      this.llm = new LLMClient();
      this.billingAgent = new BillingAgent();
      this.technicalAgent = new TechnicalAgent();
   }

   public String handleUserMessage(String userInput) {
      conversationHistory.add("User: " + userInput);

      String agentDecision = decideAgent(userInput);

      String agentResponse;
      switch (agentDecision) {
         case "TechnicalAgent":
            agentResponse = technicalAgent.respond(userInput);
            lastActiveAgent = "TechnicalAgent";
            break;
         case "BillingAgent":
            agentResponse = billingAgent.respond(userInput);
            lastActiveAgent = "BillingAgent";
            break;
         default:
            agentResponse = "🤖 I’m not sure which department should handle this. " +
                  "Could you please clarify whether this is a *technical* or *billing* question?";
      }

      conversationHistory.add(agentDecision + ": " + agentResponse);
      return agentResponse;
   }

   private String decideAgent(String userInput) {
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
