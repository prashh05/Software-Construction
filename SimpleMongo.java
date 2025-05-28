import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class SimpleMongo {
    public static void main(String[] args) {
        // Simple connection string format: mongodb://[username:password@]host:port/database
        String connectionString = "mongodb://localhost:27017/mydatabase";
        
        // Create the client
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            // Get database
            MongoDatabase database = mongoClient.getDatabase("mydatabase");
            
            // Get collection
            MongoCollection<Document> collection = database.getCollection("customers");
            
            System.out.println("Successfully connected to MongoDB!");
        }
    }
}