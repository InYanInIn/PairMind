package janjurinok.agents;

public class BillingAgent implements Agent {
   @Override
   public String respond(String userInput) {
      if (userInput.contains("refund")) {
         return "You can request a refund using our online form. Refunds are processed within 5 business days.";
      } else if (userInput.contains("price")) {
         return "Our standard plan costs $20/month. Do you want me to check your current plan?";
      } else {
         return "For billing-related inquiries, please provide more details.";
      }
   }
}
