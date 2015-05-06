package testStanfordNLP;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

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
import edu.stanford.nlp.util.*;

public class TestTagger {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Twokenize tk=new Twokenize();
		Tagger tg=new Tagger();
		tg.loadModel("C:\\EclipseWorkspaces\\csse120\\Watson\\src\\model.ritter_ptb_alldata_fixed.20130723");
		String tweet="RT: lol I was really glad to see you on fb! :):):)";
		System.out.println(tk.tokenize(tweet));
		List<TaggedToken> tks=tg.tokenizeAndTag(tweet);
		List<TaggedWord> sent=new ArrayList<TaggedWord>();
		for(TaggedToken tt: tks){
			System.out.println(tt.token+"\t"+tt.tag);
			sent.add(new TaggedWord(tt.token,tt.tag));
		}
		LexicalizedParser lp=LexicalizedParser.loadModel();
		Tree parse=lp.parse(sent);
		
		parse.pennPrint();

		SemanticGraph deps = SemanticGraphFactory.generateCollapsedDependencies(parse);
	    SemanticGraph uncollapsedDeps = SemanticGraphFactory.generateUncollapsedDependencies(parse);
	    SemanticGraph ccDeps = SemanticGraphFactory.generateCCProcessedDependencies(parse);
	    System.out.println(deps.toString(SemanticGraph.OutputFormat.LIST));
	    System.out.println(uncollapsedDeps.toString(SemanticGraph.OutputFormat.LIST));
	    System.out.println(ccDeps.toString(SemanticGraph.OutputFormat.LIST));
		//System. out.println(parse.dependencies());
		//processPipeLine(tweet);
	
	}
	
	public static void processPipeLine(String tweet){
		PrintWriter out=new PrintWriter(System.out);
		Properties props= new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("pos.model", "gate-EN-twitter.model");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);	
		Annotation document=new Annotation(tweet);
		pipeline.annotate(document);
	    //pipeline.prettyPrint(document, out);
	    List<CoreMap> sentences=document.get(CoreAnnotations.SentencesAnnotation.class);
	    for(CoreMap sentence: sentences){
	    	for(CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)){
	    		String word=token.get(CoreAnnotations.TextAnnotation.class);
	    		String pos=token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
	    		System.out.println(word+"\t"+pos);
	    	}
	    	 Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
	    	 tree.pennPrint(out);
	    	 System.out.println(sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).toString(SemanticGraph.OutputFormat.LIST));
	         out.println("The first sentence collapsed, CC-processed dependencies are:");
	         SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
	         System.out.println(graph.toString(SemanticGraph.OutputFormat.LIST));
	    }
	}

}
