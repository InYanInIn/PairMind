package janjurinok;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import janjurinok.agents.AgentRouter;
import janjurinok.agents.TechnicalAgent;
import janjurinok.rag.*;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Main {
   public static void main(String[] args) {
      String abc = "How can i uninstall application";

      AgentRouter agentRouter = new AgentRouter();
      Scanner scanner = new Scanner(System.in);

      System.out.println("🤖 Welcome to the Customer Support Chatbot!");

      while (true) {
         System.out.print("\nYou: ");
         String userInput = scanner.nextLine();

         if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
            System.out.println("🤖 Thank you for using the Customer Support Chatbot. Goodbye!");
            break;
         }

         String response = agentRouter.handleUserMessage(userInput);
         System.out.println("\n🤖 " + response);
      }

   }
}
