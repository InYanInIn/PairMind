package janjurinok.agents;

import janjurinok.LLMClient;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BillingAgent implements Agent {

   private final LLMClient llm;
   private final Map<String, ConversationContext> conversationContexts;
   private enum Tool {
      EXTRACT_EMAIL,
      EXTRACT_ORDER_ID,
      EXTRACT_PLAN_NAME,
      CREATE_REFUND_TICKET,
      GET_REFUND_POLICY,
      GET_PLAN_PRICE,
      ASK_FOR_EMAIL,
      ASK_FOR_ORDER_ID,
      ASK_FOR_PLAN_NAME
   }

   private static class ConversationContext {
      private final List<Map<String, Object>> messageHistory = new ArrayList<>();
      private final Map<String, Object> extractedData = new HashMap<>();

      public void removePlanName(){
         extractedData.remove("plan_name");
      }

      public void removeOrderId(){
         extractedData.remove("order_id");
      }

      public void addUserMessage(String message) {
         messageHistory.add(Map.of("role", "user", "content", message));
      }

      public void addAssistantMessage(String message) {
         messageHistory.add(Map.of("role", "assistant", "content", message));
      }

      public void addToolCall(String toolName, Map<String, Object> parameters, String result) {
         messageHistory.add(Map.of(
               "role", "tool",
               "tool_call", toolName,
               "parameters", parameters,
               "result", result
         ));
      }

      public void storeData(String key, Object value) {
         extractedData.put(key, value);
      }

      public Object getData(String key) {
         return extractedData.get(key);
      }

      public boolean hasData(String key) {
         return extractedData.containsKey(key);
      }

      public List<Map<String, Object>> getHistory() {
         return new ArrayList<>(messageHistory);
      }

      public String getConversationText() {
         StringBuilder sb = new StringBuilder();
         for (Map<String, Object> message : messageHistory) {
            String role = (String) message.get("role");
            if ("user".equals(role) || "assistant".equals(role)) {
               sb.append(role).append(": ").append(message.get("content")).append("\n");
            }
         }
         return sb.toString();
      }
   }

   private static class ToolCall {
      final Tool tool;
      final Map<String, Object> parameters;

      ToolCall(Tool tool, Map<String, Object> parameters) {
         this.tool = tool;
         this.parameters = parameters;
      }

      String getToolName() {
         return tool.name();
      }
   }

   public BillingAgent(LLMClient llm) {
      this.llm = llm;
      this.conversationContexts = new HashMap<>();
   }

   @Override
   public String respond(String userInput, String sessionId) {
      try {
         ConversationContext context = conversationContexts.computeIfAbsent("default", k -> new ConversationContext());

         context.addUserMessage(userInput);

         String response = processWithToolCalling(context);

         context.addAssistantMessage(response);
         return response;
      } catch (Exception ex) {
         ex.printStackTrace();
         return "⚠️ BillingAgent encountered an error: " + ex.getMessage();
      }
   }

   private String processWithToolCalling(ConversationContext context) {
      int maxIterations = 5;
      String finalResponse = null;

      for (int i = 0; i < maxIterations; i++) {
         String prompt = buildToolCallPrompt(context);
         String llmResponse = llm.ask(prompt);

         if (llmResponse == null){
            System.out.println("AWARIA 1");
            return getFallbackResponse();
         }
//         System.out.println("LLM Result: "+llmResponse+"\n\n");
         ToolCall toolCall = parseToolCall(llmResponse);

         if (toolCall != null){
            String toolResult = executeTool(toolCall, context);
            context.addToolCall(toolCall.getToolName(), toolCall.parameters, toolResult);
//            System.out.println("Tool Result: "+toolResult+"\n\n");

            if (shouldBreakLoop(toolCall, toolResult)) {
               finalResponse = toolResult;
               break;
            }
         } else {
            finalResponse = llmResponse;
            break;
         }
      }

      if (finalResponse != null){
         return finalResponse;
      } else {
         System.out.println("AWARIA 2");
         return getFallbackResponse();
      }
   }

   private boolean shouldBreakLoop(ToolCall toolCall, String toolResult){
      if (toolCall.tool.name().startsWith("ASK_FOR_")){
         return true;
      }

      if (toolCall.tool == Tool.EXTRACT_EMAIL ||
            toolCall.tool == Tool.EXTRACT_ORDER_ID ||
            toolCall.tool == Tool.EXTRACT_PLAN_NAME) {
         return false;
      }

      if (toolCall.tool == Tool.CREATE_REFUND_TICKET ||
            toolCall.tool == Tool.GET_REFUND_POLICY ||
            toolCall.tool == Tool.GET_PLAN_PRICE) {
         if (toolResult == null) return false;
         return true;
      }

      return false;
   }

   private String buildToolCallPrompt(ConversationContext context) {
      StringBuilder prompt = new StringBuilder();

      prompt.append("""
            You are a billing assistant that can help with refunds, plan pricing, and refund policies.
            
            AVAILABLE TOOLS:
            
            EXTRACTION TOOLS (try these first when user provides information):
            1. EXTRACT_EMAIL - Extract email address from conversation
            2. EXTRACT_ORDER_ID - Extract order ID from conversation  
            3. EXTRACT_PLAN_NAME - Extract plan name (pro, premium, standard)
            
            ACTION TOOLS (use when you have required data):
            4. CREATE_REFUND_TICKET - Create refund ticket (requires email and order_id)
            5. GET_REFUND_POLICY - Get refund policy for specific plan
            6. GET_PLAN_PRICE - Get pricing for specific plan
            
            ASK TOOLS (use when extraction fails and you need user input):
            7. ASK_FOR_EMAIL - Ask user to provide their email
            8. ASK_FOR_ORDER_ID - Ask user to provide their order ID (order_123, order #123)
            9. ASK_FOR_PLAN_NAME - Ask user to specify which plan (Pro, Standard, Premium, Any, All)
            
            TOOL CALLING FORMAT:
            If you need to call a tool, respond with exactly:
            TOOL_CALL: <TOOL_NAME>
            
            Otherwise, provide your final response to the user.
            
            CURRENT EXTRACTED DATA:
            """);

      if (context.extractedData.isEmpty()) {
         prompt.append("No data extracted yet.\n");
      } else {
         context.extractedData.forEach((key, value) ->
               prompt.append("- ").append(key).append(": ").append(value).append("\n"));
      }

      prompt.append("\nCONVERSATION HISTORY:\n");
      prompt.append(context.getConversationText());

      prompt.append("""
            
            DECISION PROCESS - FOLLOW THESE STEPS CAREFULLY:
            
            FOR REFUND REQUESTS:
            1. Try EXTRACT_EMAIL and EXTRACT_ORDER_ID from conversation
            2. If extraction fails for either, use ASK_FOR_EMAIL or ASK_FOR_ORDER_ID
            3. ONLY call CREATE_REFUND_TICKET when BOTH email and order_id are available
            
            FOR PLAN/POLICY QUESTIONS:
            1. Try EXTRACT_PLAN_NAME from conversation  
            2. If extraction fails, use ASK_FOR_PLAN_NAME
            3. Only call GET_PLAN_PRICE or GET_REFUND_POLICY when plan_name is available
            
            CRITICAL RULES:
            - DO NOT call action tools (CREATE_REFUND_TICKET, GET_PLAN_PRICE, GET_REFUND_POLICY) without required data
            - If an action tool returns "❌ Missing..." use the ASK_FOR tools it suggests
            - Only provide final response when you have complete information
            
            Your response (either final answer or TOOL_CALL):
            """);

      return prompt.toString();
   }

   private ToolCall parseToolCall(String response) {
      if (response != null && response.startsWith("TOOL_CALL:")) {
         String toolName = response.substring(10).trim();
         try {
            Tool tool = Tool.valueOf(toolName);
            return new ToolCall(tool, new HashMap<>());
         } catch (IllegalArgumentException e) {
            return null;
         }
      }
      return null;
   }

   private String executeTool(ToolCall toolCall, ConversationContext context) {
      return switch (toolCall.tool) {
         case EXTRACT_EMAIL -> {
            String email = extractEmailFromHistory(context);
            if (email != null) {
               context.storeData("email", email);
               yield "✅ Extracted email: " + email;
            }
            yield "❌ No email found in conversation";
         }

         case EXTRACT_ORDER_ID -> {
            String orderId = extractOrderIdFromHistory(context);
            if (orderId != null) {
               context.storeData("order_id", orderId);
               yield "✅ Extracted order ID: " + orderId;
            }
            yield "❌ No order ID found in conversation";
         }

         case EXTRACT_PLAN_NAME -> {
            String planName = extractPlanNameFromHistory(context);
            if (planName != null) {
               context.storeData("plan_name", planName);
               yield "✅ Extracted plan: " + planName;
            }
            yield "❌ No plan name found in conversation";
         }

         case ASK_FOR_EMAIL -> {
            yield "👀 To process your request, could you please provide the email associated with your account?";
         }

         case ASK_FOR_ORDER_ID -> {
            yield "👀 Could you please provide your order ID to proceed? Formats like 'order_123' or 'Order #123' work.";
         }

         case ASK_FOR_PLAN_NAME -> {
            yield "👀 Could you please specify which plan you are referring to (Pro, Premium, Standard)?";
         }

         case CREATE_REFUND_TICKET -> {
            if (!context.hasData("email")) {
               String emailCandidate = extractEmailFromHistory(context);
               if (emailCandidate != null) {
                  context.storeData("email", emailCandidate);
               }
            }
            if (!context.hasData("order_id")) {
               String orderCandidate = extractOrderIdFromHistory(context);
               if (orderCandidate != null) {
                  context.storeData("order_id", orderCandidate);
               }
            }

            boolean hasEmail = context.hasData("email");
            boolean hasOrder = context.hasData("order_id");

            if (hasEmail && hasOrder) {
               String email = (String) context.getData("email");
               String orderId = (String) context.getData("order_id");
               String ticketId = "RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
               String formLink = "https://company.example.com/refund-form?ticket=" + ticketId;

               context.removeOrderId();

               yield String.format("""
                 ✅ Refund request initiated for order %s.
                 Ticket ID: %s
                 A refund form has been sent to: %s
                 You can also access the form directly here: %s
                 """, orderId, ticketId, email, formLink);
            } else if (!hasEmail && !hasOrder) {
               yield "👀 I need both your email and order ID to process the refund. Please provide your email address and order ID.";
            } else if (!hasEmail) {
               yield "👀 I still need your email address to process the refund. Could you please provide the email associated with your account?";
            } else {
               yield "👀 I still need your order ID to process the refund. Could you please provide your order ID (e.g., order_123 or Order #123)?";
            }
         }

         case GET_REFUND_POLICY -> {
            String planName = null;
            if (context.hasData("plan_name")) {
               planName = (String) context.getData("plan_name");
            } else {
               planName = extractPlanNameFromHistory(context);
               if (planName != null) {
                  context.storeData("plan_name", planName);
               }
            }

            if (planName != null) {
               String result = getRefundPolicyForPlan(planName);
               context.removePlanName();
               yield result;
            } else {
               yield "👀 I need to know which plan you're asking about to provide the refund policy. Please specify Pro, Premium, or Standard.";
            }
         }

         case GET_PLAN_PRICE -> {
            String planName = null;
            if (context.hasData("plan_name")) {
               planName = (String) context.getData("plan_name");
            } else {
               planName = extractPlanNameFromHistory(context);
               if (planName != null) {
                  context.storeData("plan_name", planName);
               }
            }

            if (planName != null) {
               String result = getPlanPrice(planName);
               context.removePlanName();
               yield result;
            } else {
               yield "👀 I need to know which plan you're asking about to provide pricing. Please specify Pro, Premium, or Standard.";
            }
         }
      };
   }

   private String extractEmailFromHistory(ConversationContext context) {
      // Search through all user messages for email
      for (Map<String, Object> message : context.getHistory()) {
         if ("user".equals(message.get("role"))) {
            String content = (String) message.get("content");
            String email = extractEmail(content);
            if (email != null) return email;
         }
      }
      return null;
   }

   private String extractOrderIdFromHistory(ConversationContext context) {
      List<Map<String, Object>> temp_context;
      if (context.getHistory().size()>2){
         temp_context = context.getHistory().subList(context.getHistory().size()-2, context.getHistory().size());
      }
      else {
         temp_context = context.getHistory();
      }
      for (Map<String, Object> message : temp_context) {
         if ("user".equals(message.get("role"))) {
            String content = (String) message.get("content");
            String orderId = extractOrderId(content);
            if (orderId != null) return orderId;
         }
      }
      return null;
   }

   private String extractPlanNameFromHistory(ConversationContext context) {
      List<Map<String, Object>> temp_context;
      if (context.getHistory().size()>2){
         temp_context = context.getHistory().subList(context.getHistory().size()-2, context.getHistory().size());
      }
      else {
         temp_context = context.getHistory();
      }
      for (Map<String, Object> message : temp_context) {
         if ("user".equals(message.get("role"))) {
            String content = (String) message.get("content");
            String planName = extractPlanName(content);
            if (planName != null) return planName;
         }
      }
      return null;
   }

   private String getRefundPolicyForPlan(String planName) {
      String proPolicy = "Pro plan: refunds processed within 3 business days.";
      String premiumPolicy = "Premium plan: refunds processed within 4 business days.";
      String standardPolicy = "Standard/Other plans: refunds processed within 5 business days.";

      StringBuilder sb = new StringBuilder();
      if (planName != null && planName.equalsIgnoreCase("pro")) {
         sb.append("Plan: Pro.\n");
         sb.append(proPolicy);
      } else if (planName != null && planName.equalsIgnoreCase("premium")) {
         sb.append("Plan: ").append(planName).append(".\n");
         sb.append(premiumPolicy);
      } else if (planName != null && planName.equalsIgnoreCase("standard")) {
         sb.append("Plan: ").append(planName).append(".\n");
         sb.append(standardPolicy);
      } else {
         sb.append("Refund policy summary:\n");
         sb.append(proPolicy).append("\n").append(premiumPolicy).append("\n").append(standardPolicy);
      }
      return sb.toString();
   }

   private String getPlanPrice(String planName) {
      String proPrice = "$49.99/month";
      String premiumPrice = "$34.99/month";
      String standardPrice = "$20.00/month";

      if (planName != null && planName.equalsIgnoreCase("pro")) {
         return "The Pro plan costs " + proPrice + ". ";
      } else if (planName != null && planName.equalsIgnoreCase("premium")) {
         return "The Premium plan costs " + premiumPrice + ". ";
      } else if (planName != null && planName.equalsIgnoreCase("standard")) {
         return "The Standard plan costs " + standardPrice + ". ";
      } else {
         return "Current public pricing: Pro - " + proPrice + ", Premium - " + premiumPrice + ", Standard - " +
               standardPrice + ". ";
      }
   }

   public String getFallbackResponse() {
      return """
            ⚠️ I can help with:
            • Refund requests (I'll need your email and order ID)
            • Plan and pricing information
            • Refund policy details

            What would you like assistance with?""";
   }

   private static String escapeForPrompt(String s) {
      if (s == null) return "";
      return s.replace("\"", "\\\"").replace("\n", " ");
   }

   private String extractOrderId(String text) {
      if (text == null) return null;
      String lower = text.toLowerCase(Locale.ROOT);
      for (String token : lower.split("\\s+")) {
         if (token.startsWith("order_") || token.startsWith("order#") || token.startsWith("#")) return token;
      }
      return null;
   }

   private String extractEmail(String text) {
      if (text == null) return null;
      int at = text.indexOf('@');
      if (at <= 0) return null;
      String[] tokens = text.split("\\s+|,");
      for (String t : tokens) {
         if (t.contains("@") && t.contains(".")) return t.replaceAll("[^A-Za-z0-9@._-]", "");
      }
      return null;
   }

   private String extractPlanName(String text) {
      if (text == null) return null;
      String lower = text.toLowerCase(Locale.ROOT).trim();
      if (lower.contains("pro")) return "pro";
      if (lower.contains("standard")) return "standard";
      if (lower.contains("premium")) return "premium";
      if (lower.contains("any") || lower.contains("all")) return "all";
      return null;
   }
}