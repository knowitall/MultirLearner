package edu.uw.cs.multir.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.uw.cs.multir.learning.algorithm.AveragedPerceptron;
import edu.uw.cs.multir.learning.algorithm.Model;
import edu.uw.cs.multir.learning.algorithm.Parameters;
import edu.uw.cs.multir.learning.data.Dataset;
import edu.uw.cs.multir.learning.data.MemoryDataset;
import edu.uw.cs.multir.preprocess.Mappings;
import edu.uw.cs.multir.util.DebugUtils;
import edu.uw.cs.multir.util.DebugUtils.DebugData;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;

public class Train {

	public static void train(String dir) throws IOException {
		
		Random random = new Random(1);
		
		Model model = new Model();
		model.read(dir + File.separatorChar + "model");
		
		AveragedPerceptron ct = new AveragedPerceptron(model, random);
		
		Dataset train = new MemoryDataset(dir + File.separatorChar + "train");

		System.out.println("starting training");
		
		long start = System.currentTimeMillis();
		Parameters params = ct.train(train);
		long end = System.currentTimeMillis();
		System.out.println("training time " + (end-start)/1000.0 + " seconds");

		params.serialize(dir + File.separatorChar + "params");
	}
	
	public static void trainDebug(String dir,DebugData dd) throws IOException {
		
		Random random = new Random(1);
		
		Model model = new Model();
		model.read(dir + File.separatorChar + "model");
		
		AveragedPerceptron ct = new AveragedPerceptron(model, random);
		
		Dataset train = new MemoryDataset(dir + File.separatorChar + "train");

		System.out.println("starting training");
		
		long start = System.currentTimeMillis();
		Parameters params = ct.trainDebug(train,dd);
		long end = System.currentTimeMillis();
		System.out.println("training time " + (end-start)/1000.0 + " seconds");

		params.serialize(dir + File.separatorChar + "params");
	}
	
	//args[0] is multirDir with train and model files
	//args[1] is feature file
	//args[2] is corpus DB
	//args[3] is mid 1
	//args[4] is mid 2
	public static void main(String[] args) throws IOException, SQLException{
		String multirDir = args[0];
		String featureFile = args[1];
		String corpusDB = args[2];
		
		String mid1 = args[3];
		String mid2 = args[4];
		
		Mappings m = new Mappings();
		m.read(multirDir+"/mapping");
		Map<String,Integer> ft2ftIdMap = m.getFt2FtID();
		Map<String,Integer> rel2relIdMap = m.getRel2RelID();
		Map<Integer,String> ftId2FtMap = new HashMap<Integer,String>();
		Map<Integer,String> relId2RelMap = new HashMap<Integer,String>();
		
		for(String f : ft2ftIdMap.keySet()){
			Integer k = ft2ftIdMap.get(f);
			ftId2FtMap.put(k, f);
		}
		
		for(String r : rel2relIdMap.keySet()){
			Integer k = rel2relIdMap.get(r);
			relId2RelMap.put(k,r);
		}
		
		DebugData dd = new DebugUtils.DebugData();
		dd.id1 =mid1;
		dd.id2 = mid2;
		dd.mapping = m;
		dd.relID2RelMap = relId2RelMap;
		dd.ftID2FtMap = ftId2FtMap;
		
		//get relevant sent Ids
		List<Integer> sentIds = new ArrayList<Integer>();
		BufferedReader br = new BufferedReader(new FileReader(new File(featureFile)));
		String nextLine;
		while((nextLine = br.readLine())!=null){
			String[] values = nextLine.split("\t");
			Integer sentId = Integer.parseInt(values[0]);
			String id1 = values[1];
			String id2 = values[2];
			if(id1.equals(mid1) && id2.equals(mid2)){
				sentIds.add(sentId);
			}
		}
		br.close();
		
		
		
		//get List<String> sentences from db from sentIds
		List<String> sentences = new ArrayList<String>();
		Corpus c = new Corpus(corpusDB,new DefaultCorpusInformationSpecification(),true);
		Map<Integer,Pair<CoreMap,Annotation>> annoPairMap = c.getAnnotationPairsForEachSentence(new HashSet<Integer>(sentIds));
		for(Integer i : sentIds){
			Pair<CoreMap,Annotation> p = annoPairMap.get(i);
			String senText = p.first.get(CoreAnnotations.TextAnnotation.class);
			sentences.add(senText);
		}
		
		dd.sentences = sentences;
		
		System.out.println("Running Multir Training Module in Debug Mode for the Following FB Entities: ");
		System.out.println("FB entity 1 " + mid1);
		System.out.println("FB entity 2 " + mid2);
		

		//get list of sentences matching the mention
		
		trainDebug(multirDir,dd);

	}
	
	
}
