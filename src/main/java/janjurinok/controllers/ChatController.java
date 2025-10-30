package janjurinok.controllers;
import janjurinok.agents.AgentRouter;
import janjurinok.models.MessageRequest;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

   private final AgentRouter agentRouter = new AgentRouter();

   @PostMapping("/chat")
   public String chat(@RequestBody MessageRequest request) {
      return agentRouter.handleUserMessage(request.getMessage());
   }
}
