package testStanfordNLP;

import gov.nih.nlm.nls.metamap.AcronymsAbbrevs;
import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.Mapping;
import gov.nih.nlm.nls.metamap.MetaMapApi;
import gov.nih.nlm.nls.metamap.MetaMapApiImpl;
import gov.nih.nlm.nls.metamap.PCM;
import gov.nih.nlm.nls.metamap.Result;
import gov.nih.nlm.nls.metamap.Utterance;

import java.util.ArrayList;
import java.util.List;

public class TestMetaMap {

	public static void main(String args[]) throws Exception{
		TestMetaMap tt=new TestMetaMap();
		tt.run();
	}
	
	public void run() throws Exception{		
		MetaMapApi api = new MetaMapApiImpl("127.0.0.1");
		System.out.println("new instance");
		String terms="A pilot window of opportunity neoadjuvant study of metformin in localised prostate cancer metformin inhibits hepatic gluconeogenesis. ";
		List<String> theOptions = new ArrayList<String>();
	    theOptions.add("-y");  // turn on Word Sense Disambiguation
	    if (theOptions.size() > 0) {
	      api.setOptions(theOptions);
	    }
	    System.out.println("start");
	    List<Result> resultList = api.processCitationsFromString(terms.toLowerCase());
	    System.out.println("Initialize");
	    Result result = resultList.get(0);
	    List<AcronymsAbbrevs> aaList = result.getAcronymsAbbrevs();
	    if (aaList.size() > 0) {
	      System.out.println("Acronyms and Abbreviations:");
	      for (AcronymsAbbrevs e: aaList) {
	        System.out.println("Acronym: " + e.getAcronym());
	        System.out.println("Expansion: " + e.getExpansion());
	        System.out.println("Count list: " + e.getCountList());
	        System.out.println("CUI list: " + e.getCUIList());
	      }
	    } else {
	      System.out.println(" None.");
	    }
	    
	   
	    for (Utterance utterance: result.getUtteranceList()) {
	    	System.out.println("Utterance:");
	    	System.out.println(" Id: " + utterance.getId());
	    	System.out.println(" Utterance text: " + utterance.getString());
	    	System.out.println(" Position: " + utterance.getPosition());
	    	
	    	for (PCM pcm: utterance.getPCMList()) {
	    		System.out.println("Phrase:");
	    		System.out.println(" text: " + pcm.getPhrase().getPhraseText());
	             System.out.println("Mappings:");
	             for (Mapping map: pcm.getMappingList()) {
	               //System.out.println(" Map Score: " + map.getScore());
	               for (Ev mapEv: map.getEvList()) {
	                 //System.out.println("   Score: " + mapEv.getScore());
	                 //System.out.println("   Concept Id: " + mapEv.getConceptId());
	                 System.out.println("   Concept Name: " + mapEv.getConceptName());
	                 System.out.println("   Preferred Name: " + mapEv.getPreferredName());
	                 //System.out.println("   Matched Words: " + mapEv.getMatchedWords());
	                 System.out.println("   Semantic Types: " + mapEv.getSemanticTypes());

	               }
	             }
	    	}
	    	
	    	
	    	List<Result> resultList2 = api.processCitationsFromString(terms);
	    	Result result2 = resultList.get(0);
	    	String machineOutput = result2.getMachineOutput();
	    	System.out.println("machineOutput: "+machineOutput);
	    	
	    }
		
	}
}