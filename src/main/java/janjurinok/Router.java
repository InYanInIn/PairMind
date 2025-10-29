package janjurinok;

import janjurinok.agents.BillingAgent;
import janjurinok.agents.TechnicalAgent;

public class Router {
   private final TechnicalAgent techAgent;
   private final BillingAgent billingAgent;

   public Router() {
      this.techAgent = new TechnicalAgent();
      this.billingAgent = new BillingAgent();
   }

   public String handleUserInput(String input) {
      input = input.toLowerCase();
      if (input.contains("price") || input.contains("refund") || input.contains("payment")) {
         return billingAgent.respond(input);
      } else {
         return techAgent.respond(input);
      }
   }
}
