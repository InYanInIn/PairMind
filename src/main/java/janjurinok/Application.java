package janjurinok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;
import java.net.InetAddress;

@SpringBootApplication
public class Application {
   public static void main(String[] args) {
      SpringApplication.run(Application.class, args);
   }

   // Print server access information after startup
   // Use it if you have problems using it with vpn
   @PostConstruct
   public void printServerInfo() {
      try {
         String ip = InetAddress.getLocalHost().getHostAddress();
         System.out.println("\n" + "=".repeat(50));
         System.out.println("🌐 SERVER ACCESS INFORMATION");
         System.out.println("=".repeat(50));
         System.out.println("📍 Local access: http://localhost:8080");
         System.out.println("📍 Network access: http://" + ip + ":8080");
         System.out.println("📍 Your detected IP: " + ip);
         System.out.println("📍 Your static IP: 192.168.1.23");
         System.out.println("💡 Frontend should use: http://" + ip + ":8080/api/chat");
         System.out.println("=".repeat(50) + "\n");
      } catch (Exception e) {
         System.err.println("❌ Error getting server info: " + e.getMessage());
      }
   }
}
