package edu.uw.cs.multir.learning.algorithm;

import edu.uw.cs.multir.learning.data.MILDocument;
import edu.uw.cs.multir.util.DebugUtils;
import edu.uw.cs.multir.util.DebugUtils.DebugData;


public class FullInference {

	public static Parse infer(MILDocument doc,
			Scorer parseScorer, Parameters params) {
		Parse parse = new Parse();
		parse.doc = doc;
		parse.Z = new int[doc.numMentions];
		
		parseScorer.setParameters(params);
		
		Viterbi viterbi = new Viterbi(params.model, parseScorer);
		
		double[] scores = new double[params.model.numRelations];
		for (int i=0; i < scores.length; i++) scores[i] = Double.NEGATIVE_INFINITY;
		boolean[] binaryYs = new boolean[params.model.numRelations];
		int numYs = 0;
		// loop over all mentions of the instance, finding the highest probability
		// relation for each and storing it in scores..
		for (int m = 0; m < doc.numMentions; m++) {
			Viterbi.Parse p = viterbi.parse(doc, m);
			
			parse.Z[m] = p.state;
			if (p.state > 0 && !binaryYs[p.state]) {
				binaryYs[p.state] = true;
				numYs++;
			}
			
			if (p.score > scores[parse.Z[m]])
				scores[parse.Z[m]] = p.score;
		}

		// parse.y is an array the size of the number of relations that
		// were predicted for this instance it stores the relation id
		parse.Y = new int[numYs];
		int pos = 0;
		for (int i=1; i < binaryYs.length; i++)
			if (binaryYs[i]) {
				parse.Y[pos++] = i;
				if (pos == numYs) break;
			}
		
		parse.scores = scores;
		
		// It's important to ignore the _NO_RELATION_ type here, so
		// need to start at 1!
		// final value is avg of maxes
		int sumNum = 0;
		double sumSum = 0;
		for (int i=1; i < scores.length; i++)
			if (scores[i] > Double.NEGATIVE_INFINITY) { 
				sumNum++; sumSum += scores[i]; 
			}
		if (sumNum ==0) parse.score = Double.NEGATIVE_INFINITY;
		else parse.score = sumSum / sumNum;
		
		return parse;		
	}
	
	public static Parse inferDebug(MILDocument doc,
			Scorer parseScorer, Parameters params, Parameters avgParams, DebugData dd, int iteration) {
		Parse parse = new Parse();
		parse.doc = doc;
		parse.Z = new int[doc.numMentions];
		
		
		if(iteration == 0 || iteration == 1 || iteration == 25 || iteration == 50){
			if(DebugUtils.docMatchesIds(doc,dd.id1,dd.id2)){
				System.out.println("\nIteration " + iteration);
				System.out.println("Current Features: ");
				DebugUtils.printFeatureWeights(doc,params,avgParams,dd);
			}
		}
		
		parseScorer.setParameters(params);
		
		Viterbi viterbi = new Viterbi(params.model, parseScorer);
		
		double[] scores = new double[params.model.numRelations];
		for (int i=0; i < scores.length; i++) scores[i] = Double.NEGATIVE_INFINITY;
		boolean[] binaryYs = new boolean[params.model.numRelations];
		int numYs = 0;
		// loop over all mentions of the instance, finding the highest probability
		// relation for each and storing it in scores..
		for (int m = 0; m < doc.numMentions; m++) {
			Viterbi.Parse p = viterbi.parse(doc, m);
			
			parse.Z[m] = p.state;
			if (p.state > 0 && !binaryYs[p.state]) {
				binaryYs[p.state] = true;
				numYs++;
			}
			
			if (p.score > scores[parse.Z[m]])
				scores[parse.Z[m]] = p.score;
		}

		// parse.y is an array the size of the number of relations that
		// were predicted for this instance it stores the relation id
		parse.Y = new int[numYs];
		int pos = 0;
		for (int i=1; i < binaryYs.length; i++)
			if (binaryYs[i]) {
				parse.Y[pos++] = i;
				if (pos == numYs) break;
			}
		
		parse.scores = scores;
		
		// It's important to ignore the _NO_RELATION_ type here, so
		// need to start at 1!
		// final value is avg of maxes
		int sumNum = 0;
		double sumSum = 0;
		for (int i=1; i < scores.length; i++)
			if (scores[i] > Double.NEGATIVE_INFINITY) { 
				sumNum++; sumSum += scores[i]; 
			}
		if (sumNum ==0) parse.score = Double.NEGATIVE_INFINITY;
		else parse.score = sumSum / sumNum;
		
		
		
		if(iteration == 0 || iteration == 1 || iteration == 25 || iteration == 50){
			if(DebugUtils.docMatchesIds(doc,dd.id1,dd.id2)){
				System.out.println("Full Inference Results:\n");
				DebugUtils.printPredictions(parse,dd);
			}
		}
		
		
		return parse;		
	}
	
	
	
	
	public static Parse inferWithoutNA(MILDocument doc,
			Scorer parseScorer, Parameters params) {
		Parse parse = new Parse();
		parse.doc = doc;
		parse.Z = new int[doc.numMentions];
		
		parseScorer.setParameters(params);
		
		Viterbi viterbi = new Viterbi(params.model, parseScorer);
		
		double[] scores = new double[params.model.numRelations];
		for (int i=0; i < scores.length; i++) scores[i] = Double.NEGATIVE_INFINITY;
		boolean[] binaryYs = new boolean[params.model.numRelations];
		int numYs = 0;
		// loop over all mentions of the instance, finding the highest probability
		// relation for each and storing it in scores..
		for (int m = 0; m < doc.numMentions; m++) {
			Viterbi.Parse p = viterbi.parse(doc, m);
			
			parse.Z[m] = p.state;
			if (!binaryYs[p.state]) {
				binaryYs[p.state] = true;
				numYs++;
			}
			
			if (p.score > scores[parse.Z[m]])
				scores[parse.Z[m]] = p.score;
		}

		// parse.y is an array the size of the number of relations that
		// were predicted for this instance it stores the relation id
		parse.Y = new int[numYs];
		int pos = 0;
		for (int i=0; i < binaryYs.length; i++)
			if (binaryYs[i]) {
				parse.Y[pos++] = i;
				if (pos == numYs) break;
			}
		
		parse.scores = scores;
		
		// It's important to ignore the _NO_RELATION_ type here, so
		// need to start at 1!
		// final value is avg of maxes
		int sumNum = 0;
		double sumSum = 0;
		for (int i=0; i < scores.length; i++)
			if (scores[i] > Double.NEGATIVE_INFINITY) { 
				sumNum++; sumSum += scores[i]; 
			}
		if (sumNum ==0) parse.score = Double.NEGATIVE_INFINITY;
		else parse.score = sumSum / sumNum;
		
		return parse;		
	}
}
