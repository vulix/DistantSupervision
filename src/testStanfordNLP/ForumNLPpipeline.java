package testStanfordNLP;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

public class ForumNLPpipeline {
	private static Connection conn;
	private static Properties props= new Properties();
	private static StanfordCoreNLP pipeline;
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
	public static void updateDB(int post_id,int sent_seq, String tokens,String postags, String parsetree, String dependency) throws Exception{
		PreparedStatement ps = null;
		try {

			ps = conn.prepareStatement("UPDATE ADA_POST_SENT_201501 SET	TOKENS= ?, POSTAGS=?, PARSETREE=?, DEPENDENCY=?  WHERE POST_ID= ? AND SENT_SEQ= ?");
			
			ps.setString(1, tokens);
			ps.setString(2, postags);
			ps.setString(3, parsetree);
			ps.setString(4, dependency);
			ps.setInt(5, post_id);
			ps.setInt(6, sent_seq);
			
			ps.executeUpdate();
			conn.commit();
			//System.out.println("Inserting post: "+ sentence);
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
		// TODO Auto-generated method stub
		conn=getConnection();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		pipeline = new StanfordCoreNLP(props);	
		String Query1="select post_id, sent_seq,sentence from ada_post_sent_201501 where length(drugs)>5 and post_id> 2619 order by post_id";
		try{
			PreparedStatement stmt1=conn.prepareStatement(Query1);
			ResultSet rs1=null;
			rs1=stmt1.executeQuery();
			String[] sentences=null;
			
			while(rs1.next()){
				int post_id=rs1.getInt(1);
				int sent_seq=rs1.getInt(2); 
				String sentence=rs1.getString(3);
				System.out.println(post_id);
				if (sentence!=null && sentence.length()>1){
					List<String>result=processPipeLine(sentence);
					if (result.size()==4){
						try{
							String tokens=result.get(0);
							String postags=result.get(1);
							String parsetree= result.get(2);
							String dependency=result.get(3);
							if (tokens.length()<500 && dependency.length()<1000){
								updateDB(post_id,sent_seq,tokens,postags,parsetree,dependency);
							}

							}catch(Exception e){e.printStackTrace();}
						}
					}
				}
				
			stmt1.close();
			
		}catch(Exception e){e.printStackTrace();}
		
		conn.close();
	}
	public static List processPipeLine(String post){
		List<String> result=new ArrayList<String>();
		PrintWriter out=new PrintWriter(System.out);

		Annotation document=new Annotation(post);
		pipeline.annotate(document);
	    //pipeline.prettyPrint(document, out);
	    List<CoreMap> sentences=document.get(CoreAnnotations.SentencesAnnotation.class);
	    if (sentences.size()>0){
	    	CoreMap sentence=sentences.get(0);
	    	List<String> tokens=new ArrayList<String>();
	    	List<String> postags=new ArrayList<String>();
	    	for(CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)){
	    		
	    		String word=token.get(CoreAnnotations.TextAnnotation.class);
	    		tokens.add(word);
	    		String pos=token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
	    		postags.add(pos);
	    		//System.out.println(word+"\t"+pos);
	    	}
	    	result.add(tokens.toString());
	    	result.add(postags.toString());
	    	Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
	    	result.add(tree.toString());
	    	//System.out.println(sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).toString(SemanticGraph.OutputFormat.LIST));
	        //out.println("The first sentence collapsed, CC-processed dependencies are:");
	        SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
	        result.add(graph.toString(SemanticGraph.OutputFormat.LIST));
	        //System.out.println(graph.toString(SemanticGraph.OutputFormat.LIST));
	    }
	    
	    //Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		return result;
	}
}
