package edu.uw.cs.multir.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;



public class TestFeatures {

	private static Properties props;
	private static StanfordCoreNLP pipeline;
	private static GrammaticalStructureFactory gsf;
	
	
	public static void main(String[] args) throws IOException{
		
		/** Stanford parser */
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
		pipeline = new StanfordCoreNLP(props);
		gsf = new PennTreebankLanguagePack().grammaticalStructureFactory();
		
		String pathToPBGZFile = args[0];
		List<String> pbRelation = ReadProtobufRelations.convert(pathToPBGZFile);
		if(pbRelation != null){
			String rel = pbRelation.get(0);
			String arg1  = pbRelation.get(1);
			String arg2 = pbRelation.get(2);
			String sentence = pbRelation.get(3);
			String[] features = pbRelation.subList(4, pbRelation.size()).toArray(new String[pbRelation.size() - 4]);
			
			//get comparable features from two different sources
			System.out.println(sentence);
			System.out.println(arg1);
			System.out.println(arg2);
			System.out.println(rel);
			for(String f: features){
				System.out.println(f);
			}
			
			System.out.println();
			
			System.out.println("ADEPT FEATURES:");
			getFeaturesAdept(sentence);
			
			System.out.println("Distant FEATURES:");
			getFeaturesDistant(sentence);
			
			
			
		}
		
		int totalFeatures = ReadProtobufRelations.numberOfFeatures(pathToPBGZFile);
		System.out.println(totalFeatures);
	}
	
	private static void getFeaturesAdept(String text) {
		
		
		
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int sentenceOffset = 0;
		for (int k = 0; k < sentences.size(); k++) {
			CoreMap sentence = sentences.get(k);
			List<CoreLabel> labeledtokens = sentence
					.get(TokensAnnotation.class);
			int len = labeledtokens.size();
			String[] tokens = new String[len];
			int[] beginPosition = new int[len];
			String[] pos = new String[len];
			String[] ner = new String[len];
			String[] lmma = new String[len];

			for (int i = 0; i < labeledtokens.size(); i++) {
				CoreLabel token = labeledtokens.get(i);
				beginPosition[i] = token.beginPosition();
				tokens[i] = token.get(TextAnnotation.class);
				// System.out.println(tokens[i] + "\t" + beginPosition[i]);
				pos[i] = token.get(PartOfSpeechAnnotation.class);
				lmma[i] = token.get(LemmaAnnotation.class);
				ner[i] = token.get(NamedEntityTagAnnotation.class);
			}
			List<String[]> nerpairs = pairwiseNers(ner);
			// {
			// for (String[] np : nerpairs) {
			// System.out.println(Util.stringJoin(np, "\t"));
			// }
			// }
			Tree tree = sentence.get(TreeAnnotation.class);
			GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
			Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
			// Collection<TypedDependency> tdl = gs
			// .typedDependenciesCCprocessed(true);
			int[] depParents = new int[tokens.length];
			String[] depTypes = new String[tokens.length];
			{
				for (TypedDependency td : tdl) {
					// TypedDependency td = tdl.(i);
					String name = td.reln().getShortName();
//					if (td.reln().getSpecific() != null)
//						name += "-" + td.reln().getSpecific();
					int gov = td.gov().index();
					int dep = td.dep().index();
					gov = gov - 1;
					dep = dep - 1;
					// System.out.println("gov,dep\t" + gov + "\t" + dep);
					if (gov == dep || gov < 0 || dep < 0
							|| gov >= tokens.length || dep >= tokens.length) {
						continue;
					}
					depParents[dep] = gov;
					// System.out.println(name+"\t" + tokens[dep] +
					// "\t"+tokens[gov]);
					// System.out.println(name + "\t" + dep + "\t" + gov + "\t"
					// + tokens[dep] + "\t"
					// + tokens[gov]);
					depTypes[dep] = name;
				}
			}

			for (int m = 0; m < nerpairs.size(); m++) {
				for (int n = 0; n < nerpairs.size(); n++) {
					if (m == n)
						continue;
					String[] l1 = nerpairs.get(m);
					String[] l2 = nerpairs.get(n);
					int[] arg1Pos = new int[] { Integer.parseInt(l1[0]),
							Integer.parseInt(l1[1]) };
					int[] arg2Pos = new int[] { Integer.parseInt(l2[0]),
							Integer.parseInt(l2[1]) };
					String arg1ner = l1[2];
					String arg2ner = l2[2];

					//features
					List<String> features = new ArrayList<String>();
			
					// it's easier to deal with first, second
					int[] first = arg1Pos, second = arg2Pos;
					String firstNer = arg1ner, secondNer = arg2ner;
					if (arg1Pos[0] > arg2Pos[0]) {
						second = arg1Pos;
						first = arg2Pos;
						firstNer = arg2ner;
						secondNer = arg1ner;
					}
			
					// define the inverse prefix
					String inv = (arg1Pos[0] > arg2Pos[0]) ? "inverse_true"
							: "inverse_false";
			
					// define the middle parts
					StringBuilder middleTokens = new StringBuilder();
					for (int i = first[1]; i < second[0]; i++) {
						if (i > first[1]) {
							middleTokens.append(" ");
						}
						middleTokens.append(tokens[i]);
					}
			
					if (second[0] - first[1] > 10) {
						middleTokens.setLength(0);
						middleTokens.append("*LONG*");
					}
			
					// define the prefixes and suffixes
					String[] prefixTokens = new String[2];
					String[] suffixTokens = new String[2];
			
					for (int i = 0; i < 2; i++) {
						int tokIndex = first[0] - i - 1;
						if (tokIndex < 0)
							prefixTokens[i] = "B_" + tokIndex;
						else
							prefixTokens[i] = tokens[tokIndex];
					}
			
					for (int i = 0; i < 2; i++) {
						int tokIndex = second[1] + i;
						if (tokIndex >= tokens.length)
							suffixTokens[i] = "B_" + (tokIndex - tokens.length + 1);
						else
							suffixTokens[i] = tokens[tokIndex];
					}
			
					String[] prefixes = new String[3];
					String[] suffixes = new String[3];
			
					prefixes[0] = suffixes[0] = "";
					prefixes[1] = prefixTokens[0];
					prefixes[2] = prefixTokens[1] + " " + prefixTokens[0];
					suffixes[1] = suffixTokens[0];
					suffixes[2] = suffixTokens[0] + " " + suffixTokens[1];
			
					// generate the features in the same order as in ecml data
					String mto = middleTokens.toString();
			
					features.add(inv + "|" + firstNer + "|" + mto + "|" + secondNer);
					features.add(inv + "|" + prefixes[1] + "|" + firstNer + "|" + mto + "|"
							+ secondNer + "|" + suffixes[1]);
					features.add(inv + "|" + prefixes[2] + "|" + firstNer + "|" + mto + "|"
							+ secondNer + "|" + suffixes[2]);
			
					// dependency features
					if (depParents == null || depParents.length < tokens.length)
						return;
			
					// identify head words of arg1 and arg2
					// (start at end, while inside entity, jump)
					int head1 = arg1Pos[1] - 1;
					{
						Set<Integer> nodesSeen = new HashSet<Integer>();
						while (depParents[head1] >= arg1Pos[0]
								&& depParents[head1] < arg1Pos[1]) {
							nodesSeen.add(head1);
							head1 = depParents[head1];
							if (nodesSeen.contains(head1)) {
								head1 = arg1Pos[1] - 1;
								break;
							}
						}
					}
					int head2 = arg2Pos[1] - 1;
					// System.out.println(head1 + " " + head2);
					{
						Set<Integer> nodesSeen = new HashSet<Integer>();
						while (depParents[head2] >= arg2Pos[0]
								&& depParents[head2] < arg2Pos[1]) {
							nodesSeen.add(head2);
							head2 = depParents[head2];
							if (nodesSeen.contains(head2)) {
								head2 = arg2Pos[1] - 1;
								break;
							}
						}
					}
					// find path of dependencies from first to second
					int[] path1 = new int[tokens.length];
					for (int i = 0; i < path1.length; i++)
						path1[i] = -1;
					path1[0] = head1; // last token of first argument
					for (int i = 1; i < path1.length; i++) {
						path1[i] = depParents[path1[i - 1]];
						if (path1[i] == -1)
							break;
					}
					int[] path2 = new int[tokens.length];
					for (int i = 0; i < path2.length; i++)
						path2[i] = -1;
					path2[0] = head2; // last token of first argument
					for (int i = 1; i < path2.length; i++) {
						path2[i] = depParents[path2[i - 1]];
						if (path2[i] == -1)
							break;
					}
					int lca = -1;
					int lcaUp = 0, lcaDown = 0;
					outer: for (int i = 0; i < path1.length; i++)
						for (int j = 0; j < path2.length; j++) {
							if (path1[i] == -1 || path2[j] == -1) {
								break; // no path
							}
							if (path1[i] == path2[j]) {
								lca = path1[i];
								lcaUp = i;
								lcaDown = j;
								break outer;
							}
						}
			
					if (lca < 0)
						return; // no dependency path (shouldn't happen)
			
					String[] dirs = new String[lcaUp + lcaDown];
					String[] strs = new String[lcaUp + lcaDown];
					String[] rels = new String[lcaUp + lcaDown];
			
					StringBuilder middleDirs = new StringBuilder();
					StringBuilder middleRels = new StringBuilder();
					StringBuilder middleStrs = new StringBuilder();
			
					if (lcaUp + lcaDown < 12) {
			
						for (int i = 0; i < lcaUp; i++) {
							dirs[i] = "->";
							strs[i] = i > 0 ? tokens[path1[i]] : "";
							rels[i] = depTypes[path1[i]];
							// System.out.println("[" + depTypes[path1[i]] + "]->");
						}
						for (int j = 0; j < lcaDown; j++) {
							// for (int j=lcaDown-1; j >= 0; j--) {
							dirs[lcaUp + j] = "<-";
							strs[lcaUp + j] = (lcaUp + j > 0) ? tokens[path2[lcaDown - j]]
									: ""; // word taken from above
							rels[lcaUp + j] = depTypes[path2[lcaDown - j]];
							// System.out.println("[" + depTypes[path2[j]] + "]<-");
						}
			
						for (int i = 0; i < dirs.length; i++) {
							middleDirs.append(dirs[i]);
							middleRels.append("[" + rels[i] + "]" + dirs[i]);
							middleStrs.append(strs[i] + "[" + rels[i] + "]" + dirs[i]);
						}
					} else {
						middleDirs.append("*LONG-PATH*");
						middleRels.append("*LONG-PATH*");
						middleStrs.append("*LONG-PATH*");
					}
			
					String basicDir = arg1ner + "|" + middleDirs.toString() + "|" + arg2ner;
					String basicDep = arg1ner + "|" + middleRels.toString() + "|" + arg2ner;
					String basicStr = arg1ner + "|" + middleStrs.toString() + "|" + arg2ner;
			
					// new left and right windows: all elements pointing to first arg, but
					// not on path
					// List<Integer> lws = new ArrayList<Integer>();
					// List<Integer> rws = new ArrayList<Integer>();
			
					List<String> arg1dirs = new ArrayList<String>();
					List<String> arg1deps = new ArrayList<String>();
					List<String> arg1strs = new ArrayList<String>();
					List<String> arg2dirs = new ArrayList<String>();
					List<String> arg2deps = new ArrayList<String>();
					List<String> arg2strs = new ArrayList<String>();
			
					// pointing out of argument
					for (int i = 0; i < tokens.length; i++) {
						// make sure itself is not either argument
						// if (i >= first[0] && i < first[1]) continue;
						// if (i >= second[0] && i < second[1]) continue;
						if (i == head1)
							continue;
						if (i == head2)
							continue;
			
						// make sure i is not on path
						boolean onPath = false;
						for (int j = 0; j < lcaUp; j++)
							if (path1[j] == i)
								onPath = true;
						for (int j = 0; j < lcaDown; j++)
							if (path2[j] == i)
								onPath = true;
						if (onPath)
							continue;
						// make sure i points to first or second arg
						// if (depParents[i] >= first[0] && depParents[i] < first[1])
						// lws.add(i);
						// if (depParents[i] >= second[0] && depParents[i] < second[1])
						// rws.add(i);
						if (depParents[i] == head1) {
							// lws.add(i);
							arg1dirs.add("->");
							arg1deps.add("[" + depTypes[i] + "]->");
							arg1strs.add(tokens[i] + "[" + depTypes[i] + "]->");
						}
						if (depParents[i] == head2) {
							// rws.add(i);
							arg2dirs.add("->");
							arg2deps.add("[" + depTypes[i] + "]->");
							arg2strs.add("[" + depTypes[i] + "]->" + tokens[i]);
						}
					}
			
					// case 1: pointing into the argument pair structure (always attach to
					// lhs):
					// pointing from arguments
					if (lcaUp == 0 && depParents[head1] != -1 || depParents[head1] == head2) {
						arg1dirs.add("<-");
						arg1deps.add("[" + depTypes[head1] + "]<-");
						arg1strs.add(tokens[head1] + "[" + depTypes[head1] + "]<-");
			
						if (depParents[depParents[head1]] != -1) {
							arg1dirs.add("<-");
							arg1deps.add("[" + depTypes[depParents[head1]] + "]<-");
							arg1strs.add(tokens[depParents[head1]] + "["
									+ depTypes[depParents[head1]] + "]<-");
						}
					}
					// if parent is not on path or if parent is
					if (lcaDown == 0 && depParents[head2] != -1
							|| depParents[head2] == head1) { // should this actually attach
																// to rhs???
						arg1dirs.add("<-");
						arg1deps.add("[" + depTypes[head2] + "]<-");
						arg1strs.add(tokens[head2] + "[" + depTypes[head2] + "]<-");
			
						if (depParents[depParents[head2]] != -1) {
							arg1dirs.add("<-");
							arg1deps.add("[" + depTypes[depParents[head2]] + "]<-");
							arg1strs.add(tokens[depParents[head2]] + "["
									+ depTypes[depParents[head2]] + "]<-");
						}
					}
			
					// case 2: pointing out of argument
			
					// features.add("dir:" + basicDir);
					// features.add("dep:" + basicDep);
			
					// left and right, including word
					for (String w1 : arg1strs)
						for (String w2 : arg2strs)
							features.add("str:" + w1 + "|" + basicStr + "|" + w2);
			
					/*
					 * for (int lw : lws) { for (int rw : rws) { features.add("str:" +
					 * tokens[lw] + "[" + depTypes[lw] + "]<-" + "|" + basicStr + "|" + "["
					 * + depTypes[rw] + "]->" + tokens[rw]); } }
					 */
			
					// only left
					for (int i = 0; i < arg1dirs.size(); i++) {
						features.add("str:" + arg1strs.get(i) + "|" + basicStr);
						features.add("dep:" + arg1deps.get(i) + "|" + basicDep);
						features.add("dir:" + arg1dirs.get(i) + "|" + basicDir);
					}
			
					// only right
					for (int i = 0; i < arg2dirs.size(); i++) {
						features.add("str:" + basicStr + "|" + arg2strs.get(i));
						features.add("dep:" + basicDep + "|" + arg2deps.get(i));
						features.add("dir:" + basicDir + "|" + arg2dirs.get(i));
					}
					features.add("str:" + basicStr);

					String arg1 = "";
					for(String w : l1){
						arg1 += w + " ";
					}
					String arg2 = "";
					for(String w: l2){
						arg2 += w + " ";
					}
					System.out.println(arg1.trim());
					System.out.println(arg2.trim());
					features.add("str:" + basicStr);
					for(String feature : features){
						System.out.println(feature);
					}
				}
			}
		}

	}
	
	private static void getFeaturesDistant(String text) {
		
		
		
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int sentenceOffset = 0;
		for (int k = 0; k < sentences.size(); k++) {
			CoreMap sentence = sentences.get(k);
			List<CoreLabel> labeledtokens = sentence
					.get(TokensAnnotation.class);
			int len = labeledtokens.size();
			String[] tokens = new String[len];
			int[] beginPosition = new int[len];
			String[] pos = new String[len];
			String[] ner = new String[len];
			String[] lmma = new String[len];

			for (int i = 0; i < labeledtokens.size(); i++) {
				CoreLabel token = labeledtokens.get(i);
				beginPosition[i] = token.beginPosition();
				tokens[i] = token.get(TextAnnotation.class);
				// System.out.println(tokens[i] + "\t" + beginPosition[i]);
				pos[i] = token.get(PartOfSpeechAnnotation.class);
				lmma[i] = token.get(LemmaAnnotation.class);
				ner[i] = token.get(NamedEntityTagAnnotation.class);
			}
			List<String[]> nerpairs = pairwiseNers(ner);
			// {
			// for (String[] np : nerpairs) {
			// System.out.println(Util.stringJoin(np, "\t"));
			// }
			// }
			Tree tree = sentence.get(TreeAnnotation.class);
			GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
			Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
			// Collection<TypedDependency> tdl = gs
			// .typedDependenciesCCprocessed(true);
			int[] depParents = new int[tokens.length];
			String[] depTypes = new String[tokens.length];
			{
				for (TypedDependency td : tdl) {
					// TypedDependency td = tdl.(i);
					String name = td.reln().getShortName();
					if (td.reln().getSpecific() != null)
						name += "-" + td.reln().getSpecific();
					int gov = td.gov().index();
					int dep = td.dep().index();
					gov = gov - 1;
					dep = dep - 1;
					// System.out.println("gov,dep\t" + gov + "\t" + dep);
					if (gov == dep || gov < 0 || dep < 0
							|| gov >= tokens.length || dep >= tokens.length) {
						continue;
					}
					depParents[dep] = gov;
					// System.out.println(name+"\t" + tokens[dep] +
					// "\t"+tokens[gov]);
					// System.out.println(name + "\t" + dep + "\t" + gov + "\t"
					// + tokens[dep] + "\t"
					// + tokens[gov]);
					depTypes[dep] = name;
				}
			}

			for (int m = 0; m < nerpairs.size(); m++) {
				for (int n = 0; n < nerpairs.size(); n++) {
					if (m == n)
						continue;
					String[] l1 = nerpairs.get(m);
					String[] l2 = nerpairs.get(n);
					int[] arg1Pos = new int[] { Integer.parseInt(l1[0]),
							Integer.parseInt(l1[1]) };
					int[] arg2Pos = new int[] { Integer.parseInt(l2[0]),
							Integer.parseInt(l2[1]) };
					String arg1ner = l1[2];
					String arg2ner = l2[2];

					List<String> features = new ArrayList<String>();

					// it's easier to deal with first, second
					int[] first = arg1Pos, second = arg2Pos;
					String firstNer = arg1ner, secondNer = arg2ner;
					if (arg1Pos[0] > arg2Pos[0]) {
						second = arg1Pos; first = arg2Pos;
						firstNer = arg2ner; secondNer = arg1ner;
					}
					
					// define the inverse prefix
					String inv = (arg1Pos[0] > arg2Pos[0])? 
							"inverse_true" : "inverse_false";
					
					// define the middle parts
					StringBuilder middleTokens = new StringBuilder();
					for (int i=first[1]; i < second[0]; i++) {
						if (i > first[1]) {
							middleTokens.append(" ");
						}
						middleTokens.append(tokens[i]);
					}
					
					if (second[0] - first[1] > 10) {
						middleTokens.setLength(0);
						middleTokens.append("*LONG*");
					}
					
					// define the prefixes and suffixes
					String[] prefixTokens = new String[2];
					String[] suffixTokens = new String[2];
					
					for (int i=0; i < 2; i++) {
						int tokIndex = first[0] - i - 1;
						if (tokIndex < 0) prefixTokens[i] = "B_" + tokIndex;
						else prefixTokens[i] = tokens[tokIndex];
					}

					for (int i=0; i < 2; i++) {
						int tokIndex = second[1] + i;
						if (tokIndex >= tokens.length) suffixTokens[i] = "B_" + (tokIndex - tokens.length + 1);
						else suffixTokens[i] = tokens[tokIndex];
					}

					String[] prefixes = new String[3];
					String[] suffixes = new String[3];

					prefixes[0] = suffixes[0] = "";
					prefixes[1] = prefixTokens[0];
					prefixes[2] = prefixTokens[1] + " " + prefixTokens[0];
					suffixes[1] = suffixTokens[0];
					suffixes[2] = suffixTokens[0] + " " + suffixTokens[1];
					
					// generate the features in the same order as in ecml data
					String mto = middleTokens.toString();
					
					features.add(inv + "|" + firstNer + "|" + mto + "|" + secondNer);
					features.add(inv + "|" + prefixes[1] + "|" + firstNer + "|" + mto + "|" + secondNer + "|" + suffixes[1]);
					features.add(inv + "|" + prefixes[2] + "|" + firstNer + "|" + mto + "|" + secondNer + "|" + suffixes[2]);
					
					// dependency features
					if (depParents == null || depParents.length < tokens.length) return;
					
					// identify head words of arg1 and arg2
					// (start at end, while inside entity, jump)
					int head1 = arg1Pos[1]-1;
					while (depParents[head1] >= arg1Pos[0] && depParents[head1] < arg1Pos[1]) head1 = depParents[head1];
					int head2 = arg2Pos[1]-1;
					//System.out.println(head1 + " " + head2);
					while (depParents[head2] >= arg2Pos[0] && depParents[head2] < arg2Pos[1]) head2 = depParents[head2];
					
					
					// find path of dependencies from first to second
					int[] path1 = new int[tokens.length];
					for (int i=0; i < path1.length; i++) path1[i] = -1;
					path1[0] = head1; // last token of first argument
					for (int i=1; i < path1.length; i++) {
						path1[i] = depParents[path1[i-1]];
						if (path1[i] == -1) break;
					}	
					int[] path2 = new int[tokens.length];
					for (int i=0; i < path2.length; i++) path2[i] = -1;
					path2[0] = head2; // last token of first argument
					for (int i=1; i < path2.length; i++) {
						path2[i] = depParents[path2[i-1]];
						if (path2[i] == -1) break;
					}
					int lca = -1;
					int lcaUp = 0, lcaDown = 0;
					outer:
					for (int i=0; i < path1.length; i++)
						for (int j=0; j < path2.length; j++) {
							if (path1[i] == -1 || path2[j] == -1) {
								break; // no path
							}
							if (path1[i] == path2[j]) {
								lca = path1[i];
								lcaUp = i;
								lcaDown = j;
								break outer;
							}
						}
					
					if (lca < 0) return ; // no dependency path (shouldn't happen)
					
					String[] dirs = new String[lcaUp + lcaDown];
					String[] strs = new String[lcaUp + lcaDown];
					String[] rels = new String[lcaUp + lcaDown];

					StringBuilder middleDirs = new StringBuilder();
					StringBuilder middleRels = new StringBuilder();
					StringBuilder middleStrs = new StringBuilder();

					if (lcaUp + lcaDown < 12) {
						
						for (int i=0; i < lcaUp; i++) {
							dirs[i] = "->";
							strs[i] = i > 0? tokens[path1[i]] : "";
							rels[i] = depTypes[path1[i]];
							//System.out.println("[" + depTypes[path1[i]] + "]->");
						}
						for (int j=0; j < lcaDown; j++) {
						//for (int j=lcaDown-1; j >= 0; j--) {
							dirs[lcaUp + j] = "<-";
							strs[lcaUp + j] = (lcaUp + j > 0)? tokens[path2[lcaDown-j]] : ""; // word taken from above
							rels[lcaUp + j] = depTypes[path2[lcaDown-j]];
							//System.out.println("[" + depTypes[path2[j]] + "]<-");
						}
						
						for (int i=0; i < dirs.length; i++) {
							middleDirs.append(dirs[i]);
							middleRels.append("[" + rels[i] + "]" + dirs[i]);
							middleStrs.append(strs[i] + "[" + rels[i] + "]" + dirs[i]);
						}
					}
					else {
							middleDirs.append("*LONG-PATH*");
							middleRels.append("*LONG-PATH*");
							middleStrs.append("*LONG-PATH*");
					}
				
					String basicDir = arg1ner + "|" + middleDirs.toString() + "|" + arg2ner;
					String basicDep = arg1ner + "|" + middleRels.toString() + "|" + arg2ner;
					String basicStr = arg1ner + "|" + middleStrs.toString() + "|" + arg2ner;
					

					// new left and right windows: all elements pointing to first arg, but not on path
					//List<Integer> lws = new ArrayList<Integer>();
					//List<Integer> rws = new ArrayList<Integer>();
					
					List<String> arg1dirs = new ArrayList<String>();
					List<String> arg1deps = new ArrayList<String>();
					List<String> arg1strs = new ArrayList<String>();
					List<String> arg2dirs = new ArrayList<String>();
					List<String> arg2deps = new ArrayList<String>();
					List<String> arg2strs = new ArrayList<String>();
					
					// pointing out of argument
					for (int i=0; i < tokens.length; i++) {
						// make sure itself is not either argument
						//if (i >= first[0] && i < first[1]) continue;
						//if (i >= second[0] && i < second[1]) continue;
						if (i == head1) continue;
						if (i == head2) continue;
						
						// make sure i is not on path
						boolean onPath = false;
						for (int j=0; j < lcaUp; j++) if (path1[j] == i) onPath = true;
						for (int j=0; j < lcaDown; j++) if (path2[j] == i) onPath = true;
						if (onPath) continue;
						// make sure i points to first or second arg
						//if (depParents[i] >= first[0] && depParents[i] < first[1]) lws.add(i);
						//if (depParents[i] >= second[0] && depParents[i] < second[1]) rws.add(i);
						if (depParents[i] == head1) {
							//lws.add(i);
							arg1dirs.add("->");				
							arg1deps.add("[" + depTypes[i] + "]->");
							arg1strs.add(tokens[i] + "[" + depTypes[i] + "]->");
						}
						if (depParents[i] == head2) {
							//rws.add(i);			
							arg2dirs.add("->");				
							arg2deps.add("[" + depTypes[i] + "]->");
							arg2strs.add("[" + depTypes[i] + "]->" + tokens[i]);
						}
					}
					
					
					// case 1: pointing into the argument pair structure (always attach to lhs):
					// pointing from arguments
					if (lcaUp == 0 && depParents[head1] != -1 || depParents[head1] == head2) {
						arg1dirs.add("<-");				
						arg1deps.add("[" + depTypes[head1] + "]<-");
						arg1strs.add(tokens[head1] + "[" + depTypes[head1] + "]<-");
						
						if (depParents[depParents[head1]] != -1) {
							arg1dirs.add("<-");
							arg1deps.add("[" + depTypes[depParents[head1]] + "]<-");
							arg1strs.add(tokens[depParents[head1]] + "[" + depTypes[depParents[head1]] + "]<-");
						}
					}
					// if parent is not on path or if parent is 
					if (lcaDown == 0 && depParents[head2] != -1 || depParents[head2] == head1) { // should this actually attach to rhs???
						arg1dirs.add("<-");
						arg1deps.add("[" + depTypes[head2] + "]<-");
						arg1strs.add(tokens[head2] + "[" + depTypes[head2] + "]<-");
						
						if (depParents[depParents[head2]] != -1) {
							arg1dirs.add("<-");
							arg1deps.add("[" + depTypes[depParents[head2]] + "]<-");
							arg1strs.add(tokens[depParents[head2]] + "[" + depTypes[depParents[head2]] + "]<-");
						}
					}
					
					// case 2: pointing out of argument
					
					//features.add("dir:" + basicDir);		
					//features.add("dep:" + basicDep);

					
					// left and right, including word
					for (String w1 : arg1strs)
						for (String w2 : arg2strs)
							features.add("str:" + w1 + "|" + basicStr + "|" + w2);
					
					/*
					for (int lw : lws) {
						for (int rw : rws) {
							features.add("str:" + tokens[lw] + "[" + depTypes[lw] + "]<-" + "|" + basicStr
									+ "|" + "[" + depTypes[rw] + "]->" + tokens[rw]);
						}
					}
					*/
					
					
					
					// only left
					for (int i=0; i < arg1dirs.size(); i++) {
						features.add("str:" + arg1strs.get(i) + "|" + basicStr);
						features.add("dep:" + arg1deps.get(i) + "|" + basicDep);
						features.add("dir:" + arg1dirs.get(i) + "|" + basicDir);
					}
					
					
					// only right
					for (int i=0; i < arg2dirs.size(); i++) {
						features.add("str:" + basicStr + "|" + arg2strs.get(i));
						features.add("dep:" + basicDep + "|" + arg2deps.get(i));
						features.add("dir:" + basicDir + "|" + arg2dirs.get(i));
					}

					features.add("str:" + basicStr);
			
					String arg1 = "";
					for(String w : l1){
						arg1 += w + " ";
					}
					String arg2 = "";
					for(String w: l2){
						arg2 += w + " ";
					}
					System.out.println(arg1.trim());
					System.out.println(arg2.trim());
					features.add("str:" + basicStr);
					for(String feature : features){
						System.out.println(feature);
					}
					
				}
			}
		}

	}
	
	

	public static List<String[]> pairwiseNers(String[] ner) {
		List<String[]> ret = new ArrayList<String[]>();
		int[] help = new int[ner.length];
		help[0] = 0;
		for (int i = 1; i < ner.length; i++) {
			if (ner[i].equals(ner[i - 1])) {
				help[i] = help[i - 1] + 1;
			}
		}
		for (int k = ner.length - 1; k >= 0;) {
			if (ner[k].equals("PERSON") || ner[k].equals("LOCATION")
					|| ner[k].equals("ORGANIZATION")) {
				int end = k + 1;
				int start = k - help[k];
				String type = ner[k];
				k = start - 1;
				ret.add(new String[] { start + "", end + "", type });
			} else {
				k--;
			}
		}
		Collections.reverse(ret);
		return ret;

	}

	
	
	

}
