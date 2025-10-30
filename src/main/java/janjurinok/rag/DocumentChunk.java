package janjurinok.rag;

public class DocumentChunk {
   private String text;
   private float[] embedding;
   private String sourceFile;

   public DocumentChunk(String text, float[] embedding, String sourceFile) {
      this.text = text;
      this.embedding = embedding;
      this.sourceFile = sourceFile;
   }

   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = text;
   }

   public float[] getEmbedding() {
      return embedding;
   }

   public void setEmbedding(float[] embedding) {
      this.embedding = embedding;
   }

   public String getSourceFile() {
      return sourceFile;
   }

   public void setSourceFile(String sourceFile) {
      this.sourceFile = sourceFile;
   }
}

