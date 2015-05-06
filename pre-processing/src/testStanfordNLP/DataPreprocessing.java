package testStanfordNLP;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.lang.Object;
import java.io.*;
import java.util.*;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;

public class DataPreprocessing {


	public static void main(String[] args) throws Exception {

		PrintWriter out;
	    if (args.length > 1) {
	      out = new PrintWriter(args[1]);
	    } else {
	      out = new PrintWriter(System.out);
	    }
	    PrintWriter xmlOut = null;
	    if (args.length > 2) {
	      xmlOut = new PrintWriter(args[2]);
	    }
		
		String paragraph="I have been eating all the right foods for 3 months now.  " +
				"I haven't had any sweets or foods that I'm not supposed to have.  " +
				"My numbers have been perfect, maybe a little low.  " +
				"We have had to decrease my insulin a few times.  Dr says I am being a little obsessive.  " +
				"lol  Anyway, I have only lost a few lbs in the past 3 months.  " +
				"I am really depressed.  " +
				"The nutritionist explained to me about how insulin works and if I hadn't changed my eating habit that I would have gained a lot of weight by now.  " +
				"I work 7 days a week as a hotel manager and I am always running around the motel but not at a steady pace.  " +
				"I am tired at the end of the day so I have started walking in the morning before I go to work.  " +
				"I am just wondering if anyone has any idea how long it takes before I will see a weight loss?   " +
				"I am not a patient person but I feel like my body is cheating me.  I am being good to it, now it needs to be good to me.  " +
				"lol Lori T2 Metformin 500mg evening and 100mg metformin morning Fenofibrate 160mg A1C 12 Feb 2013 A1C 6.2 JUNE 2013 YIPEE!";
		
	    StanfordCoreNLP pipeline = new StanfordCoreNLP();
	    Annotation annotation;
	    
	    if (args.length > 0) {
	      annotation = new Annotation(IOUtils.slurpFileNoExceptions(args[0]));
	    } else {
	      annotation = new Annotation(paragraph);
	    }
	    
	    pipeline.annotate(annotation);
	    /*pipeline.prettyPrint(annotation, out);
	    out.println();
	    
	    out.println("The top level annotation");
	    out.println(annotation.toShorterString());
	    */
	 

	    // An Annotation is a Map and you can get and use the various analyses individually.
	    // For instance, this gets the parse tree of the first sentence in the text.
	    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
	    if (sentences != null && sentences.size() > 0) {
	    	for (CoreMap sentence: sentences){
	    		String textString=sentence.toString();
	    		out.println("Sentence: " +textString);
	    		//out.println(sentence.toShorterString());
	    		out.println("The sentence parse tree is:");
	    		Tree parsetree=sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
	    		parsetree.pennPrint(out);
	    		SemanticGraph basic=sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
	    		out.println("The sentence basic dependencies are:"); 
	    		out.println(basic.toString(SemanticGraph.OutputFormat.LIST));
	    		out.println("The sentence collapsed, CC-processed dependencies are:");
	    	    SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
	    	    out.println(graph.toString(SemanticGraph.OutputFormat.LIST));
	    		 /*out.println("The first sentence tokens are:");
	    	      for (CoreMap token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
	    	        ArrayCoreMap aToken = (ArrayCoreMap) token;
	    	        out.println(aToken.toShorterString());
	    	     }*/
	    		
	    		
	    	}
	    }
	    
	}

}
