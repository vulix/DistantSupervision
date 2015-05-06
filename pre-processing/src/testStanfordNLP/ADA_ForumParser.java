package testStanfordNLP;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ADA_ForumParser {
	private static Connection ada;
	public static void main (String[] args) throws Exception{
		ada=getConnection();
		//File input= new File("E:\\ADE data collection\\community.diabetes.org\\t5\\Adults-Living-with-Type-1\\155-Animas-Insets-9mm-23-inch-for-sale-Brand-New\\td-p\\439401");
		File folder=new File("E:\\ADE data collection\\community.diabetes.org\\t5\\The-Watering-Hole");
		listFilesForFolder(folder);
		//parse(input);
		ada.close();
	}
	
	public static void listFilesForFolder(final File folder) throws Exception {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry);
	        } else {
	            //System.out.println(fileEntry.getName());
	            if (fileEntry.getName().contains("WD3")||fileEntry.getName().contains("em")||fileEntry.getAbsolutePath().contains("3Dquot")){
	            	System.out.println("Skip file: "+fileEntry.getName());
	            }
	            else{
	            	System.out.println(fileEntry.getAbsoluteFile());
	            	if (fileEntry.getAbsolutePath().contains("td-p"))
	            	{
	            		parse(fileEntry);
	            		
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
	
	
	public static void parse(File input) throws Exception{
		Document doc=Jsoup.parse(input, "UTF-8");
		String url=input.getAbsolutePath().replace("E:\\ADE data collection", "http:/").replaceAll("\\\\", "/");
		int indent=url.indexOf("t5")+3;
		String boardID=url.substring(indent, indent+url.substring(indent).indexOf("/"));
		String topicID="NA";
		System.out.println("URL:"+url);
		System.out.println("boardID:"+boardID);
		System.out.println("topicID:"+topicID);
		
		Elements topicTitle=doc.select("title");
		String title=topicTitle.first().text();
		title=title.replaceAll("American Diabetes Association -", "").replaceAll("- American Diabetes Association Community", "");
		System.out.println("Title: " +title);

		Elements postID=doc.select("div[data-message-id]");
		Elements messageSEQ=doc.select("span[class=MessagesPositionInThread]>a");
		Elements userID=doc.select("span[class=UserName lia-user-name]");
		Elements postdate=doc.select("span[class=DateTime lia-message-posted-on]");
		Elements content=doc.select("div[class=lia-message-body-content]");
		
		System.out.println("postID:"+postID.size());
		if (content.size()>0){
		for(int i=0; i<postID.size(); i++){
			try{
					String pid=postID.get(i).attr("data-message-id");
					String seq=messageSEQ.get(i).text();
					String username=userID.get(i).text();
					String profile_num="";
					if (username.contains("anon")){
						profile_num="999999";
					}
					else {
						Elements profileurl=userID.get(i).select("a");
						profile_num=profileurl.first().attr("href");
						profile_num=profile_num.substring(profile_num.lastIndexOf("/")+1);
					}
			
					String date=postdate.get(i).text().substring(1);
					String text=content.get(i).text();
					System.out.println("postID: "+pid);
					System.out.println("post sequence: "+seq);
					System.out.println("username: "+username);
					System.out.println("profile number: "+profile_num);
					System.out.println("post time: "+date);
					System.out.println("post content:" +text);
					insert(ada,url,boardID,topicID,title,pid,seq,username,profile_num,date,text);
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
			
			ps = con.prepareStatement("INSERT INTO ADA_POST_201501 (URL, BOARD_ID, TOPIC_ID, TOPIC_TITLE, POST_ID, MSG_SEQ, USER_ID, PROFILE_NUM, POST_DATE, CONTENT) VALUES (?,?,?,?,?,?,?,?,?,?)");
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
}
