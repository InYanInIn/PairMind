package janjurinok.agents;

import janjurinok.LLMClient;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class BillingAgent implements Agent {

   private final LLMClient llm;

   public BillingAgent(LLMClient llm) {
      this.llm = llm;
   }

   @Override
   public String respond(String userInput) {
      try {
         Intent intent = ClassifyIntent(userInput);

         return switch (intent) {
            case REQUEST_REFUND -> {
               String email = extractEmail(userInput);
               yield handleRequestRefund(email);
            }
            case CHECK_PLAN_AND_REFUND_POLICY -> {
               String planHint = extractPlanHint(userInput);
               yield handleCheckPlanAndRefundPolicy(planHint);
            }
            case CONFIRM_PLAN_PRICE -> {
               String planName = extractPlanHint(userInput);
               yield handleConfirmPlanPrice(planName);
            }
            default ->
                  "⚠️ I couldn't determine the billing intent. I can help with refunds, plan/pricing info, or payment confirmations. " +
                        "Please say e.g. \"I want a refund for order_123\" or \"What does the Pro plan cost?\"";
         };
      } catch (Exception ex) {
         ex.printStackTrace();
         return "⚠️ BillingAgent encountered an error: " + ex.getMessage();
      }
   }


   private enum Intent {
      REQUEST_REFUND,
      CHECK_PLAN_AND_REFUND_POLICY,
      CONFIRM_PLAN_PRICE,
      UNKNOWN
   }

   private Intent ClassifyIntent(String userInput) {
      String prompt = """
                You are a concise classifier that maps a user's billing question to exactly one of these labels:
                - RequestRefund
                - CheckPlanAndRefundPolicy
                - ConfirmPlanPrice

                Return ONLY the label (one of the three, nothing else).

                Examples:
                Q: "I was charged twice last month — I want a refund." => RequestRefund
                Q: "How long do refunds take?" => CheckPlanAndRefundPolicy
                Q: "How much does the Pro plan cost?" => ConfirmPlanPrice
                Q: "What about Premium?" => ConfirmPlanPrice
                Q: "I need to update my card on file." => RequestRefund

                Now classify:
                Q: "%s"
                """.formatted(escapeForPrompt(userInput));
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

   private String handleRequestRefund(String customerEmail) {
      String ticketId = "RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
      String formLink = "https://company.example.com/refund-form?ticket=" + ticketId;

      StringBuilder sb = new StringBuilder();
      sb.append("✅ Refund request initiated.\n");
      sb.append("Ticket ID: ").append(ticketId).append("\n");
      sb.append("Link to refund form was sent to your email: ").append(customerEmail != null ? customerEmail : "[not provided]").append("\n");
      // In real system, here we would send an email with the form link
      return sb.toString();
   }

   private String handleCheckPlanAndRefundPolicy(String planHint) {
      String proPolicy = "Pro plan: refunds processed within 3 business days.";
      String premiumPolicy = "Premium plan: refunds processed within 4 business days.";
      String standardPolicy = "Standard/Other plans: refunds processed within 5 business days.";

      StringBuilder sb = new StringBuilder();
      if (planHint != null && planHint.equalsIgnoreCase("pro")) {
         sb.append("Plan: Pro.\n");
         sb.append(proPolicy);
      } else if (planHint != null && planHint.equalsIgnoreCase("premium")) {
         sb.append("Plan: ").append(planHint).append(".\n");
         sb.append(premiumPolicy);
      } else if (planHint != null && planHint.equalsIgnoreCase("standard")) {
         sb.append("Plan: ").append(planHint).append(".\n");
         sb.append(standardPolicy);
      } else {
         sb.append("Refund policy summary:\n");
         sb.append(proPolicy).append("\n").append(premiumPolicy).append("\n").append(standardPolicy);
      }
      return sb.toString();
   }

   private String handleConfirmPlanPrice(String planName) {
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

   private String extractPlanHint(String text) {
      if (text == null) return null;
      String lower = text.toLowerCase(Locale.ROOT);
      if (lower.contains("pro")) return "pro";
      if (lower.contains("standard")) return "standard";
      if (lower.contains("premium")) return "premium";
      if (lower.contains("basic")) return "basic";
      return null;
   }
}
