package testStanfordNLP;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ParallelScanOptions;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MongoDbConnector {

	public static void main(String[] args) throws Exception {
		MongoClient mongoClient= new MongoClient("localhost",27017);
		DB db = mongoClient.getDB( "diabetes" );
		DBCollection coll = db.getCollection("treatments");
		
		BasicDBObject query =new BasicDBObject("lang","en");
		System.out.println(coll.getCount(query));;
		DBCursor cursor=coll.find(query);
		try {
			while(cursor.hasNext()){
				System.out.println(cursor.next().get("text"));
			}
		}finally{
			cursor.close();
		}
		
	}

}
