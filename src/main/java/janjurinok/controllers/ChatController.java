package janjurinok.controllers;
import janjurinok.agents.AgentRouter;
import janjurinok.models.MessageRequest;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

   private final AgentRouter agentRouter;

   public ChatController(AgentRouter agentRouter) {
      this.agentRouter = agentRouter;
   }

   @PostMapping("/chat")
   public String chat(@RequestBody MessageRequest request) {
      String sessionId = request.getSessionId();
      if (sessionId == null || sessionId.isEmpty()) {
         sessionId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
      }

      return agentRouter.handleUserMessage(request.getMessage(), request.getEmail(), sessionId);
   }
}
