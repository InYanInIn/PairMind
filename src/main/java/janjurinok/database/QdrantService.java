package janjurinok.database;


import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import janjurinok.rag.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@Component
public class QdrantService implements AutoCloseable{
   private final QdrantClient client;
   private final String collectionName;

   public QdrantService(
         @Value("${qdrant.host}") String host,
         @Value("${qdrant.grpc-port}") int grpcPort,
         @Value("${qdrant.collection-name}") String collectionName
   ) {
      this.client = new QdrantClient(QdrantGrpcClient.newBuilder(host, grpcPort, false).build());
      this.collectionName = collectionName;
   }

   public void createCollectionIfNotExists(String collectionName, int vectorSize) throws Exception {
      try {
         if (client.collectionExistsAsync(collectionName).get()) return;

         VectorParams params = VectorParams.newBuilder()
               .setSize(vectorSize)
               .setDistance(Distance.Cosine) // или Euclid
               .build();

         client.createCollectionAsync(collectionName, params).get();
         System.out.println("Created Qdrant collection: " + collectionName);

      }catch (Exception e) {
         System.out.println("Failed to create Qdrant collection: " + e.getMessage());
      }
   }

   public void upsertChunks(String collectionName, List<DocumentChunk> chunks) throws Exception {
      List<Points.PointStruct> points = new ArrayList<>(chunks.size());
      long idCounter = 1;
      for (DocumentChunk chunk : chunks) {
         float[] emb = chunk.getEmbedding();
         List<Float> vector = new ArrayList<>(emb.length);
         for (float v : emb) {
            vector.add(v);
         }

         Map<String, JsonWithInt.Value> payload = new HashMap<>();
         payload.put("text", value(chunk.getText()));
         payload.put("source", value(chunk.getSourceFile()));

         Points.PointStruct p = Points.PointStruct.newBuilder()
               .setId(id(idCounter++))
               .setVectors(vectors(vector))
               .putAllPayload(payload)
               .build();
         points.add(p);
      }

      client.upsertAsync(collectionName, points).get();
   }

   public List<DocumentChunk> search(float[] queryEmbedding, int topK) throws Exception{
      List<Float> queryVec = new ArrayList<>(queryEmbedding.length);
      for (float v : queryEmbedding) {
         queryVec.add(v);
      }

      Points.SearchPoints request = Points.SearchPoints.newBuilder()
            .setCollectionName(collectionName)
            .addAllVector(queryVec)
            .setLimit(topK)
            .setWithPayload(io.qdrant.client.WithPayloadSelectorFactory.enable(true))
            .setWithVectors(io.qdrant.client.WithVectorsSelectorFactory.enable(false))
            .build();

      List<Points.ScoredPoint> results = client.searchAsync(request).get();

//      System.out.println("Qdrant search results count: " + results.size());
//      System.out.println("Qdrant search results: " + results.stream().toList());

      List<DocumentChunk> out = results.stream().map(sp -> {
         Map<String, JsonWithInt.Value> payload = sp.getPayloadMap();
         String text = payload.get("text").getStringValue();
         String source = payload.get("source").getStringValue();

         return new DocumentChunk(text, new float[0], source);
      }).toList();

      return out;
   }

   @Override
   public void close() throws Exception {
      client.close();
   }

   public boolean collectionExists(String collectionName) {
      try {
         return client.collectionExistsAsync(collectionName).get();
      } catch (InterruptedException | ExecutionException e) {
         throw new RuntimeException("Failed to check collection existence: " + e.getMessage(), e);
      }
   }

   public List<DocumentChunk> getAllChunks(String agentCollection) {
      try {
         Points.GetPoints request = Points.GetPoints.newBuilder()
               .setCollectionName(agentCollection)
               .build();

         List<Points.RetrievedPoint> points = client.retrieveAsync(request, null).get();

         List<DocumentChunk> out = points.stream().map(p -> {
            Map<String, JsonWithInt.Value> payload = p.getPayloadMap();
            String text = payload.get("text").getStringValue();
            String source = payload.get("source").getStringValue();

            return new DocumentChunk(text, new float[0], source);
         }).toList();

         return out;
      } catch (InterruptedException | ExecutionException e) {
         throw new RuntimeException("Failed to get all chunks: " + e.getMessage(), e);
      }
   }
}
