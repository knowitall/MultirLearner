package edu.uw.cs.multir.util;

import java.util.List;
import java.util.Map;

import edu.uw.cs.multir.learning.algorithm.Parameters;
import edu.uw.cs.multir.learning.algorithm.Parse;
import edu.uw.cs.multir.learning.data.MILDocument;
import edu.uw.cs.multir.preprocess.Mappings;
import edu.uw.cs.multir.util.DebugUtils.DebugData;

public class DebugUtils {

	public static boolean docMatchesIds(MILDocument doc, String id1, String id2) {
		if(doc.arg1.equals(id1) && doc.arg2.equals(id2)){
			return true;
		}
		return false;
	}

	public static void printFeatureWeights(MILDocument doc, Parameters params, Parameters avgParams, DebugData dd) {
		
		for(int i =0; i < doc.numMentions; i++){
			SparseBinaryVector features = doc.features[i];
			for(int k =0; k < params.relParameters.length; k++){
				String rel = dd.relID2RelMap.get(k);
				System.out.println("Mentation " + i + " = " + dd.sentences.get(i));
				System.out.println("Feature weights for relation " + rel + ":");
				for(int j =0; j < features.num; j++){
					String ft = dd.ftID2FtMap.get(features.ids[j]);
					System.out.println("Feature " + ft + " has weight of " + params.relParameters[k].vals[features.ids[j]] + " and scaled weight of " + avgParams.relParameters[k].vals[features.ids[j]]);
				}
				System.out.println("-------------------------------------------------------------------------------------------\n\n");
			}
		}
	}
	
	public static void printFeatureWeightsForRelMention(MILDocument doc, Parameters params, Parameters avgParams, DebugData dd, int r, int m) {
		
		SparseBinaryVector features = doc.features[m];
		String rel = dd.relID2RelMap.get(r);
		System.out.println("Mentation " + m + " = " + dd.sentences.get(m));
		System.out.println("Feature weights for relation " + rel + ":");
		for(int j =0; j < features.num; j++){
			String ft = dd.ftID2FtMap.get(features.ids[j]);
			System.out.println("Feature " + ft + " has weight of " + params.relParameters[r].vals[features.ids[j]]+ " and scaled weight of " + avgParams.relParameters[r].vals[features.ids[j]]);
		}
		System.out.println("-------------------------------------------------------------------------------------------\n\n");
	}
	
	
	public static class DebugData{
		
		public String id1;
		public String id2;
		public Mappings mapping;
		public Map<Integer,String> relID2RelMap;
		public Map<Integer,String> ftID2FtMap;
		public List<String> sentences;
		
		
		public DebugData(){
			
		}
	}


	public static void printPredictions(Parse parse, DebugData dd) {
		
		for(int i =0; i < parse.Z.length; i++){
			System.out.println("Mention " + i + " " + dd.sentences.get(i));
			System.out.println("Was predicted as " + dd.relID2RelMap.get(parse.Z[i]));
			System.out.println("---------------------------------------------------");
		}
	}

}
