package testStanfordNLP;
import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.Mapping;
import gov.nih.nlm.nls.metamap.MetaMapApi;
import gov.nih.nlm.nls.metamap.MetaMapApiImpl;
import gov.nih.nlm.nls.metamap.PCM;
import gov.nih.nlm.nls.metamap.Result;
import gov.nih.nlm.nls.metamap.Utterance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import opennlp.tools.sentdetect.*;
import opennlp.tools.sentdetect.lang.*;


public class ForumProcessing {
	private static Connection conn;
	static MetaMapApi api = new MetaMapApiImpl("127.0.0.1");
	
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
	
	public static String[] detectSentencefromSource(String Content) throws Exception{
		SentenceDetectorME sentenceDetector=null;
		File file=new File("E:\\software\\apache-opennlp-1.5.2-incubating\\bin\\en-sent.bin");
		InputStream modelIn=new FileInputStream(file);
		try{
			//load sentence detection model
			SentenceModel model= new SentenceModel(modelIn);
			modelIn.close();
			sentenceDetector=new SentenceDetectorME(model);
		}catch (IOException e){
			e.printStackTrace();
		}finally{
			if (modelIn !=null){
				try{
					modelIn.close();
				}catch (IOException e){
					
				}
			}
		}
		
		String sentences[]=sentenceDetector.sentDetect(Content);
		return sentences;
	}
	
	public static List MetaMapAnnotation(String phrase) throws Exception{
		
		List<String> drugs=new ArrayList<String>();
		List<String> events=new ArrayList<String>();
		phrase=phrase.replaceAll("[\\.\\#\\&\\~\\^]", "");
		phrase=phrase.replaceAll("#", "");
		phrase=phrase.trim();
		if (phrase.trim().length()>10 && phrase.trim().length()<400){
			
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
				System.out.println(phrase);
				System.exit(-1);
				
			}
		}
  
		List<List> annotation=new ArrayList<List>();
		annotation.add(drugs);
		annotation.add(events);

	    return annotation;
	}
	
	public static void storeInDB(int post_id,int sent_seq, String sentence,String events, String drugs) throws Exception{
		PreparedStatement ps = null;
		try {

			ps = conn.prepareStatement("INSERT INTO ADA_POST_SENT_201501 (POST_ID, SENT_SEQ, SENTENCE, EVENTS, DRUGS) VALUES (?,?,?,?,?)");
			ps.setInt(1, post_id);
			ps.setInt(2, sent_seq);
			ps.setString(3, sentence);
			ps.setString(4, events);
			ps.setString(5, drugs);
			
			ps.executeUpdate();
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

		List<String> theOptions = new ArrayList<String>();
	    theOptions.add("-y");  // turn on Word Sense Disambiguation
	    if (theOptions.size() > 0) {
	      api.setOptions(theOptions);
	    }

		conn=getConnection();

		String Query1="select distinct post_id, content from ada_post_201501 where post_id > 458757 and board_id ='Adults-Living-with-Type-2' order by post_id";
		try{
			PreparedStatement stmt1=conn.prepareStatement(Query1);
			ResultSet rs1=null;
			rs1=stmt1.executeQuery();
			String[] sentences=null;
			while(rs1.next()){
				int post_id=rs1.getInt(1);
				String content=rs1.getString(2);
				if (content!=null && content.length()>1){
					sentences=detectSentencefromSource(content);
					try{
						for(int i=0; i<sentences.length;i++){
							String sentence=sentences[i];
							List annotation=MetaMapAnnotation(sentence);
							String drugs=annotation.get(0).toString();
							String events=annotation.get(1).toString();
							storeInDB(post_id,i,sentence,events, drugs);
						}
					}catch(Exception e){e.printStackTrace();}
					}
				}
				
			stmt1.close();
			
		}catch(Exception e){e.printStackTrace();}
		
		conn.close();

	}

}
