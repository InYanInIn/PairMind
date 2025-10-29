package janjurinok.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DocumentLoader {
   private final List<String> docs;

   public DocumentLoader(String dir) {
      this.docs = loadDocs(dir);
   }

   private List<String> loadDocs(String dir) {
      File folder = new File(dir);
      List<String> result = new ArrayList<>();
      for (File f : folder.listFiles()) {
         if (f.isFile() && f.getName().endsWith(".txt")) {
            try {
               result.add(Files.readString(f.toPath()));
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
      return result;
   }

   public String findRelevantDocs(String query) {
      // упрощённо: просто верни первые 2 документа
      return String.join("\n\n", docs.subList(0, Math.min(2, docs.size())));
   }
}

