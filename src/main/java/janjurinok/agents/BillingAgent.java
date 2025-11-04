package janjurinok.agents;

import janjurinok.LLMClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
      GET_PLAN_PRICE
   }

   private enum Intent {
      REQUEST_REFUND,
      CHECK_PLAN_AND_REFUND_POLICY,
      CONFIRM_PLAN_PRICE,
      PROVIDE_MISSING_INFO,
      UNKNOWN
   }

   private static class ConversationContext {
      private final StringBuilder conversationHistory = new StringBuilder();
      private String pendingAction;
      private String missingField;

      public void addUserMessage(String message) {
         conversationHistory.append("User: ").append(message).append("\n");
      }

      public void addAgentMessage(String message) {
         conversationHistory.append("BillingAgent: ").append(message).append("\n");
      }

      public String getHistory(){
         return conversationHistory.toString();
      }

      public String getPendingAction() {
         return pendingAction;
      }

      public String getMissingField() {
         return missingField;
      }

      public boolean hasPendingAction(){
         return pendingAction != null;
      }

      public void setPendingAction(String action, String field) {
         this.pendingAction = action;
         this.missingField = field;
      }

      public void clearPendingAction() {
         this.pendingAction = null;
         this.missingField = null;
      }


      public void setMissingField(String missingField) {
         this.missingField = missingField;
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

         Intent intent = ClassifyIntent(userInput, context);

         String response = switch (intent) {
            case REQUEST_REFUND -> handleRequestRefund(userInput, context);
            case CHECK_PLAN_AND_REFUND_POLICY -> handleCheckPlanAndRefundPolicy(userInput, context);
            case CONFIRM_PLAN_PRICE -> handleConfirmPlanPrice(userInput, context);
            case PROVIDE_MISSING_INFO -> handleMissingInfo(userInput, context);
            default -> getFallbackResponse();
         };

         context.addAgentMessage(response);
         return response;
      } catch (Exception ex) {
         ex.printStackTrace();
         return "⚠️ BillingAgent encountered an error: " + ex.getMessage();
      }
   }




   private Intent ClassifyIntent(String userInput, ConversationContext context) {
      if (context.hasPendingAction()) {
         return Intent.PROVIDE_MISSING_INFO;
      }

      String prompt = """     
                You are a concise classifier that maps a user's billing question to exactly one of these labels:
                - RequestRefund
                - CheckPlanAndRefundPolicy
                - ConfirmPlanPrice

                Consider the conversation context:
                %s

                Return ONLY the label (one of the three, nothing else).

                Examples:
                Q: "I was charged twice last month — I want a refund." => RequestRefund
                Q: "How long do refunds take?" => CheckPlanAndRefundPolicy
                Q: "How much does the Pro plan cost?" => ConfirmPlanPrice
                Q: "What about Premium?" => ConfirmPlanPrice
                Q: "I need to update my card on file." => RequestRefund

                Now classify:
                Q: "%s"
                """.formatted(context.getHistory(), escapeForPrompt(userInput));
      String response = llm.ask(prompt);
//      System.out.println(response);
      if (response == null) return Intent.UNKNOWN;

      return switch (response.toLowerCase().trim()) {
         case "requestrefund" -> Intent.REQUEST_REFUND;
         case "checkplanandrefundpolicy" -> Intent.CHECK_PLAN_AND_REFUND_POLICY;
         case "confirmplanprice" -> Intent.CONFIRM_PLAN_PRICE;
         default -> Intent.UNKNOWN;
      };
   }

   private String handleRequestRefund(String userInput, ConversationContext context) {
      String email = extractEmail(userInput);
      String orderId = extractOrderId(userInput);

      if (email == null) {
         context.setPendingAction("REQUEST_REFUND", "email");
         return "👀 To process your refund, could you please provide the email associated with your account?";
      }
      if (orderId == null) {
         context.setPendingAction("REQUEST_REFUND", "order_id");
         return "👀 Could you please provide your order ID to proceed with the refund? Formats like 'order_123' or 'Order #123' work.";
      }

      context.clearPendingAction();

      String ticketId = "RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
      String formLink = "https://company.example.com/refund-form?ticket=" + ticketId;

      StringBuilder sb = new StringBuilder();
      sb.append("✅ Refund request initiated for order ").append(orderId).append(".\n");
      sb.append("Ticket ID: ").append(ticketId).append("\n");

      sb.append("A refund form has been sent to: ").append(email).append("\n");
      sb.append("You can also access the form directly here: ").append(formLink);

      // In real system, here we would send an email with the form link
      return sb.toString();
   }

   private String handleCheckPlanAndRefundPolicy(String userInput, ConversationContext context) {
      String planName = extractPlanName(userInput);
      if (planName == null) {
         context.setPendingAction("CHECK_PLAN_AND_REFUND_POLICY", "plan_name");
         return "👀 To provide accurate refund policy information, could you please specify which plan you are inquiring about (Pro, Premium, Standard)?";
      }

      context.clearPendingAction();
      return getRefundPolicyForPlan(planName);
   }

   private String handleConfirmPlanPrice(String userInput, ConversationContext context) {
      String planName = extractPlanName(userInput);
      if (planName == null) {
         context.setPendingAction("CONFIRM_PLAN_PRICE", "plan_name");
         return "👀 Could you please specify which plan you would like to know the price of (Pro, Premium, Standard)?";
      }

      context.clearPendingAction();
      return getPlanPrice(planName);
   }

   private String handleMissingInfo(String userInput, ConversationContext context) {
      String pendingAction = context.getPendingAction();
      String missingField = context.getMissingField();

      String extractedInfo = extractMissingInfo(userInput, missingField);

      if (extractedInfo != null){
         context.clearPendingAction();
         return switch (pendingAction) {
            case "REQUEST_REFUND" -> handleRequestRefund(userInput, context);
            case "CHECK_PLAN_AND_REFUND_POLICY" -> handleCheckPlanAndRefundPolicy(userInput, context);
            case "CONFIRM_PLAN_PRICE" -> handleConfirmPlanPrice(userInput, context);
            default -> getFallbackResponse();
         };
      }else{
         return switch (missingField){
            case "email" -> "👀 I couldn't identify a valid email in your response. Please provide the email associated with your account.";
            case "order_id" -> "👀 I still need your order ID to proceed with the refund. Please provide it.";
            case "plan_name" -> "👀 I couldn't determine the plan name from your response. Please specify which plan you are referring to (Pro, Premium, Standard).";
            default -> "👀 I need some additional info to help you. " + getFallbackResponse();
         };
      }
   }

   private String extractMissingInfo(String userInput, String missingField){
      return switch (missingField){
         case "email" -> extractEmail(userInput);
         case "order_id" -> extractOrderId(userInput);
         case "plan_name" -> extractPlanName(userInput);
         default -> null;
      };
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

   private String getPlanPrice(String planName){

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
      // simple pattern: order_123 or Order #123
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
      // naive: grab substring around @
      String[] tokens = text.split("\\s+|,");
      for (String t : tokens) {
         if (t.contains("@") && t.contains(".")) return t.replaceAll("[^A-Za-z0-9@._-]", "");
      }
      return null;
   }

   private String extractPlanName(String text) {
      if (text == null) return null;
      String lower = text.toLowerCase(Locale.ROOT);
      if (lower.contains("pro")) return "pro";
      if (lower.contains("standard")) return "standard";
      if (lower.contains("premium")) return "premium";
      if (lower.contains("any")||lower.contains("all")) return "all";
      return null;
   }
}
