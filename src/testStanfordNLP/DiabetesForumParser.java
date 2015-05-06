package testStanfordNLP;

import java.sql.Connection;
import java.io.File;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
public class DiabetesForumParser {

	private static Connection diabetesforum;
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		diabetesforum=getConnection();
		//File input= new File("E:\\ADE data collection\\www.diabetesforum.com\\diabetes\\1037-wondering-about-pumps.html");
		File folder=new File("E:\\ADE data collection\\www.diabetesforum.com\\members-blood-sugar-readings");
		listFilesForFolder(folder);
		//parse(input);
		diabetesforum.close();
	}
	
	public static void parse(File input) throws Exception{
		Document doc=Jsoup.parse(input, "UTF-8");
		
		String url=input.getAbsoluteFile().toString().replace("E:\\ADE data collection", "http:/").replaceAll("\\\\", "/");
		url=url.replaceAll("/%&Ovr\\d*","");
		String boardID="";
		String topicID="";
		String title="";
		try {
		boardID=url.replace("http://www.diabetesforum.com/", "");
		boardID=boardID.substring(0, boardID.indexOf("/"));
		topicID=input.getName().substring(0, input.getName().indexOf("-"));
		Elements topicTitle=doc.select("title");
		title=topicTitle.first().text();
		
		System.out.println("URL:"+url);
		System.out.println("boardID:"+boardID);
		System.out.println("topicID:"+topicID);
		System.out.println("Title: " +title);
		}catch (Exception e) {
			e.printStackTrace();
		}
		Elements postID=doc.select("div[id~=post_message_(\\d*)]");
		Elements messageSEQ=doc.select("a[id~=postcount(\\d*)]");
		Elements userID=doc.select("div[id~=postmenu(\\d*)]");
		Elements postdate=doc.select("table[id~=post(\\d*)]");
		Elements content=doc.select("div[id~=post_message_(\\d*)]");
		
		System.out.println("postID:"+postID.size());
		if (postID.size()>0){
		for(int i=0; i<postID.size(); i++){
			try{
					String pid=postID.get(i).attr("id");
					pid=pid.substring(pid.lastIndexOf("_")+1);
					String seq=messageSEQ.get(i).text();
					String username=userID.get(i).text();
					String profile_num="999999";
					
					String date=postdate.get(i).select("td").first().text();
					String text=content.get(i).text();
					System.out.println("postID: "+pid);
					System.out.println("post sequence: "+seq);
					System.out.println("username: "+username);
					System.out.println("profile number: "+profile_num);
					System.out.println("post time: "+date);
					System.out.println("post content:" +text);
					insert(diabetesforum,url,boardID,topicID,title,pid,seq,username,profile_num,date,text);
				}catch (Exception e) {
					e.printStackTrace();
				} 
			
		}
		}
		
	}
	
	public static void insert(Connection con, String url, String boardID, String topicID, String title, String pid, String seq, String username, String profile_num, String date, String text ){
		PreparedStatement ps = null;
		try {
			int postid=Integer.parseInt(pid);
			int msgseq=Integer.parseInt(seq);
			int profilenum=Integer.parseInt(profile_num);
			
			ps = con.prepareStatement("INSERT INTO DIABETESFORUM_POST_201501 (URL, BOARD_ID, TOPIC_ID, TOPIC_TITLE, POST_ID, MSG_SEQ, USER_ID, PROFILE_NUM, POST_DATE, CONTENT) VALUES (?,?,?,?,?,?,?,?,?,?)");
			ps.setString(1, url);
			ps.setString(2, boardID);
			ps.setString(3, topicID);
			ps.setString(4, title);
			ps.setInt(5, postid);
			ps.setInt(6, msgseq);
			ps.setString(7, username);
			ps.setInt(8, profilenum);
			ps.setString(9, date);
			ps.setString(10, text);
			
			ps.executeUpdate();
			System.out.println("Inserting post: "+ pid);
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
	public static void listFilesForFolder(final File folder) throws Exception {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry);
	        } else {
	            //System.out.println(fileEntry.getName());
	            if (fileEntry.getName().contains("print")||fileEntry.getName().contains("post")||fileEntry.getName().contains("index")){
	            	System.out.println("Skip file: "+fileEntry.getName());
	            }
	            else{
	            	System.out.println(fileEntry.getAbsoluteFile());
	            	int i = fileEntry.getName().lastIndexOf('.');
	            	if (i >= 0) {
	            	    String extension = fileEntry.getName().substring(i+1);
	            	    if (extension.endsWith("html")){
	            	    	parse(fileEntry);
	            	    }
	            	}
	            }
	            
	        }
	    }
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
	
}
