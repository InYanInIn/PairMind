package janjurinok.models;

public class MessageRequest {
   private String message;
   private String email;
   private String sessionId;

   public MessageRequest() {   }

   public MessageRequest(String message, String email, String sessionId) {
      this.message = message;
      this.email = email;
      this.sessionId = sessionId;
   }

   public String getSessionId() {
      return sessionId;
   }

   public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getMessage() { return message; }
   public void setMessage(String message) { this.message = message; }
}
