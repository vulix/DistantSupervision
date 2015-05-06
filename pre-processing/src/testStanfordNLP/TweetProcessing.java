package testStanfordNLP;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import cmu.arktweetnlp.*;
import cmu.arktweetnlp.Tagger.TaggedToken;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.*;
import gov.nih.nlm.nls.metamap.AcronymsAbbrevs;
import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.Mapping;
import gov.nih.nlm.nls.metamap.MetaMapApi;
import gov.nih.nlm.nls.metamap.MetaMapApiImpl;
import gov.nih.nlm.nls.metamap.PCM;
import gov.nih.nlm.nls.metamap.Result;
import gov.nih.nlm.nls.metamap.Utterance;


public class TweetProcessing {
	static MetaMapApi api = new MetaMapApiImpl("127.0.0.1");
	static LexicalizedParser lp=LexicalizedParser.loadModel();
	static Twokenize tk=new Twokenize();
	static Tagger tg=new Tagger();
	private static Connection conn;
	public static List<Tree> GetNounPhrases(Tree parse)
	{
	    List<Tree> phraseList=new ArrayList<Tree>();
	    for (Tree subtree: parse)
	    {

	      if(subtree.label().value().equals("NNP"))
	      {
	        phraseList.add(subtree);
	        System.out.println(subtree);
	      }
	    }
	    return phraseList;
	}
	public static List tweetProcessPipeline(String tweet) throws Exception{
		
		List<String> results=new ArrayList<String>();   
		tg.loadModel("C:\\EclipseWorkspaces\\csse120\\Watson\\src\\model.ritter_ptb_alldata_fixed.20130723");
		//System.out.println(tk.tokenize(tweet));
		String tokenized=tk.tokenize(tweet).toString();
		//results[0]=tokens
		results.add(tokenized);
		
		List<TaggedToken> tks=tg.tokenizeAndTag(tweet);
		List<String> postags= new ArrayList<String>();
		List<TaggedWord> sent=new ArrayList<TaggedWord>();
		String mtweet="";
		for(TaggedToken tt: tks){
			//System.out.println(tt.token+"\t"+tt.tag);
			postags.add(tt.tag);
			sent.add(new TaggedWord(tt.token,tt.tag));
			if (!tt.tag.contains("RT")&& !tt.tag.contains("USR")&& !tt.tag.contains("URL")&& !tt.tag.contains("UH")
					){
				mtweet+=tt.token.replaceAll("#", "")+" ";
			}
		}
		System.out.println(postags.toString());
		//results[1]=postags
		results.add(postags.toString());
		
		Tree parse=lp.parse(sent);
		//System.out.println(parse.toString());
		//results[2]=parse trees
		results.add(parse.toString());
		SemanticGraph deps = SemanticGraphFactory.generateCollapsedDependencies(parse);
		//System.out.println(deps.toString(SemanticGraph.OutputFormat.LIST));
		//results[3]=dependency graph
		System.out.println("MetaMap Process:"+ mtweet);
		results.add(deps.toString(SemanticGraph.OutputFormat.LIST));
		results.add(mtweet);
		return results;
	}
	public static List MetaMapAnnotation(String phrase) throws Exception{
		
		List<String> drugs=new ArrayList<String>();
		List<String> events=new ArrayList<String>();
		if (phrase.length()>5){
			phrase=phrase.replaceAll("[\\.\\#\\&\\~\\^]", "");
			try{
				
				List<Result> resultList = api.processCitationsFromString(phrase.toLowerCase());
				   
			    Result result = resultList.get(0); 
			    for (Utterance utterance: result.getUtteranceList()) {
			
			    	for (PCM pcm: utterance.getPCMList()) {
			             for (Mapping map: pcm.getMappingList()) {
			               
			               for (Ev mapEv: map.getEvList()) {
			            	 String sem=mapEv.getSemanticTypes().toString();
			            	 if (sem.contains("dsyn")||sem.contains("fndg")||sem.contains("neop")||sem.contains("blor")||sem.contains("moft")||sem.contains("sosy")||sem.contains("patf")||sem.contains("acab")||sem.contains("emod")){
			            		 String event=mapEv.getPreferredName().toString();
			            		 event=event+"\t"+mapEv.getSemanticTypes().toString();
			            		 event=event+"\t"+mapEv.getPositionalInfo().toString();
			            		 event=event+"\t"+mapEv.getConceptName().toString();
				                 events.add(event);
			            	 }
			                
			            	 if (sem.contains("horm")||sem.contains("phsu")||sem.contains("orch")){
			            		 String drug=mapEv.getPreferredName().toString();
			            		 drug=drug+"\t"+mapEv.getSemanticTypes().toString();
			            		 drug=drug+"\t"+mapEv.getPositionalInfo().toString();
			            		 drug=drug+"\t"+mapEv.getConceptName().toString();
				                 drugs.add(drug);
			            	 }
			               }
			             }
			    	}	
			    }
				
			}catch (Exception e) {
				System.out.println("MetaMap failed to parse.");
				System.exit(-1);
			}
		}
  
		List<List> annotation=new ArrayList<List>();
		annotation.add(drugs);
		annotation.add(events);

	    return annotation;
	}
	
	public static Connection getConnection() throws Exception {
		String className=null;
		String url=null;
		String user=null;
		String password=null;
		Connection con=null;
	
		className="oracle.jdbc.driver.OracleDriver";
		url="jdbc:oracle:thin:@128.196.253.120:1522:ORCL2";
		user="website";
		password="O_ri_ga_me";
		
		try {
			Class.forName(className);
		} catch (Exception e) {
			System.out.println(className + " driver failed to load.");
			System.exit(-1);
		}
	

		try {
			con = DriverManager.getConnection(url, user, password);
			//con.close();
		} catch (Exception e) {
			System.out.println(e.toString());
			System.exit(-1);
		}
		return con;
			
	}
	public static void storeInDB(long id, String tweet,String tokens, String postags, String parsetree, String dependency, String events, String drugs) throws Exception{
		PreparedStatement ps = null;
		try {

			ps = conn.prepareStatement("INSERT INTO TWEETPROCESSED (ID, TWEET, TOKENS, POSTAGS, PARSETREE, DEPENDENCY, EVENTS, DRUGS) VALUES (?,?,?,?,?,?,?,?)");
			ps.setLong(1, id);
			ps.setString(2, tweet);
			ps.setString(3, tokens);
			ps.setString(4, postags);
			ps.setString(5, parsetree);
			ps.setString(6, dependency);
			ps.setString(7, events);
			ps.setString(8, drugs);
			
			ps.executeUpdate();
			System.out.println("Inserting post: "+ tweet);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
				
			} catch (SQLException e) {
					// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
		}
	
	}

	public static void main(String[] args) throws Exception {
		conn=getConnection();
		MongoClient mongoClient= new MongoClient("localhost",27017);
		DB db = mongoClient.getDB( "diabetes" );
		DBCollection coll = db.getCollection("treatments");
		
		List<String> theOptions = new ArrayList<String>();
	    theOptions.add("-y");  // turn on Word Sense Disambiguation
	    if (theOptions.size() > 0) {
	      api.setOptions(theOptions);
	    }

		BasicDBObject query =new BasicDBObject("lang","en");
		
		DBCursor cursor=coll.find(query);
		cursor.addOption(com.mongodb.Bytes.QUERYOPTION_NOTIMEOUT);
		//cursor.sort(new BasicDBObject("id", -1));
		while(cursor.hasNext()){
			try {
				String tweet=cursor.next().get("text").toString();
				long id=(Long) cursor.next().get("id");
				System.out.println(id);
				System.out.println(tweet);
				List pipeline=tweetProcessPipeline(tweet);
				String tokens=pipeline.get(0).toString();
				String postags=pipeline.get(1).toString();
				String parsetree=pipeline.get(2).toString();
				String dependency=pipeline.get(3).toString();
				
				List annotation=MetaMapAnnotation(pipeline.get(4).toString());
				String drugs=annotation.get(0).toString();
				String events=annotation.get(1).toString();
				storeInDB(id,tweet,tokens,postags,parsetree,dependency,events,drugs);
				Thread.sleep(1000);  
			
			}catch (SQLException e) {
				e.printStackTrace();
			}
		}
		cursor.close();
		conn.close();
	}
	
}
