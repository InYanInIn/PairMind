package janjurinok.models;

public class MessageRequest {
   private String message;
   private String email;

   public MessageRequest() {   }

   public MessageRequest(String message, String email) {
      this.message = message;
      this.email = email;
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
