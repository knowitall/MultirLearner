package edu.uw.cs.multir.learning.algorithm;

import java.util.Arrays;
import java.util.Comparator;

import edu.uw.cs.multir.learning.data.MILDocument;
import edu.uw.cs.multir.util.DebugUtils;
import edu.uw.cs.multir.util.DebugUtils.DebugData;

public class ConditionalInference {

	public static Parse infer(MILDocument doc,
			Scorer parseScorer, Parameters params) {
		int numMentions = doc.numMentions;
		
		Parse parse = new Parse();
		parse.doc = doc;
		parseScorer.setParameters(params);
		
		Viterbi viterbi = new Viterbi(params.model, parseScorer);
		
		Viterbi.Parse[] vp = new Viterbi.Parse[numMentions];
		for (int m = 0; m < numMentions; m++) {
			vp[m] = viterbi.parse(doc, m);
		}
		
		// each mention can be linked to one of the doc relations or NA
		int numRelevantRelations = doc.Y.length + 1;
		
		// solve bipartite graph matching problem
		Edge[] es = new Edge[numMentions * numRelevantRelations];
		for (int m = 0; m < numMentions; m++) {
			// edge from m to NA
			es[numRelevantRelations*m + 0] =
				new Edge(m, 0, vp[m].scores[0]);
			// edge from m to any other relation
			for (int y = 1; y < numRelevantRelations; y++)
				es[numRelevantRelations*m + y] = 
					new Edge(m, y, vp[m].scores[doc.Y[y-1]]);
		}

		// NOTE: strictly speaking, no sorting is necessary
		// in the following steps; however, we do sorting
		// for easier code maintainability
		
		// array to hold solution (mapping from z's to y's)
		int[] z = new int[numMentions];
		for (int i=0; i < numMentions; i++) z[i] = -1;

		// there is a special case where there are more target
		// relations than there are mentions; in this case we
		// only add the highest scoring edges
		if (numMentions < doc.Y.length) {
			// sort edges by decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					double d = e2.score - e1.score;
					if (d < 0) return -1; else return 1;
				}});

			boolean[] ysCovered = new boolean[numRelevantRelations];			
			for (int ei = 0; ei < es.length; ei++) {
				Edge e = es[ei];
				if (e.y == 0) continue;
				if (z[e.m] < 0 && !ysCovered[e.y]) {
					z[e.m] = doc.Y[e.y-1];
					ysCovered[e.y] = true;
				}
			}
		} else {
			// more mentions than target relations: enforce all Ys
			
			// sort by y, then decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					int c = e1.y - e2.y;
					if (c != 0) {
						return c;
					}	
					double d = e1.score - e2.score;
					if(d < 0){
						return 1;
					}
					else if(d > 0){
						return -1;
					}
					else{
						return 0;
					}

				}});
			
			// note that after this step the "es" array has to
			// be indexed differently
			
			// iterate over y's
			for (int y=1; y < numRelevantRelations; y++) {
	
				// find highest weight edge to y, from a
				// mention m which does not yet have an
				// outgoing edge
				
				for (int j=0; j < numMentions; j++) {
					Edge e = es[numMentions*y + j];
					if (z[e.m] < 0) {
						// we can add this edge
						//System.out.println("adding " + doc.Y[y-1]);
						z[e.m] = (y==0)? 0 : doc.Y[y-1];
						break;
					}
				}
			}
			
			// there might be unmapped m's
			// sort by m, then decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					int c = e1.m - e2.m;
					if (c != 0) return c;
					double d = e2.score - e1.score;
					if (d < 0) return -1; else return 1;
				}});

			
			for (int m=0; m < numMentions; m++) {
				if (z[m] < 0) {
					// unmapped mention, need to take highest score
					Edge e = es[numRelevantRelations*m];
					z[m] = e.y == 0? 0 : doc.Y[e.y-1];
				}
			}
		}
		
		// we can now write the results
		parse.Y = doc.Y;
		parse.Z = z;
		parse.score = 0;
		for (int i=0; i < numMentions; i++) {
			parse.score += vp[i].scores[z[i]];
		}
		
//		if(doc.arg1.equals("/m/022vq5") && doc.arg2.equals("/m/0161c")){
//			System.out.println("Conditional Inference..");
//			System.out.println("Arg1 = " + doc.arg1 + " and Arg2 = " + doc.arg2);
//			System.out.println("Y rels...");
//			for(int j = 0; j < doc.Y.length; j++){
//				System.out.println(doc.Y[j]);
//			}
//			
//			System.out.println("Z rels...");
//			for(int j = 0; j < doc.Z.length; j++){
//				System.out.println(doc.Z[j]);
//			}
//			
//			for(int i =0; i < z.length; i++){
//				System.out.println("mention " + i + " has relation " + z[i]);
//			}
//		}
		
		
		return parse;
		
		
		
	}
	
	public static Parse inferDebug(MILDocument doc,
			Scorer parseScorer, Parameters params, Parameters avgParams, DebugData dd, int iteration) {
		int numMentions = doc.numMentions;
		
		if(iteration == 0 || iteration == 1 || iteration == 25 || iteration == 50){
			if(DebugUtils.docMatchesIds(doc,dd.id1,dd.id2)){
				System.out.println("\n\nRunning Conditional Inference..... assumes that each of the Relations labeled in the MILDoc must be expressed by at least one mention");
			}
		}
		
		
		Parse parse = new Parse();
		parse.doc = doc;
		parseScorer.setParameters(params);
		
		Viterbi viterbi = new Viterbi(params.model, parseScorer);
		
		Viterbi.Parse[] vp = new Viterbi.Parse[numMentions];
		for (int m = 0; m < numMentions; m++) {
			vp[m] = viterbi.parse(doc, m);
		}
		
		// each mention can be linked to one of the doc relations or NA
		int numRelevantRelations = doc.Y.length + 1;
		
		// solve bipartite graph matching problem
		Edge[] es = new Edge[numMentions * numRelevantRelations];
		for (int m = 0; m < numMentions; m++) {
			// edge from m to NA
			es[numRelevantRelations*m + 0] =
				new Edge(m, 0, vp[m].scores[0]);
			// edge from m to any other relation
			for (int y = 1; y < numRelevantRelations; y++)
				es[numRelevantRelations*m + y] = 
					new Edge(m, y, vp[m].scores[doc.Y[y-1]]);
		}

		// NOTE: strictly speaking, no sorting is necessary
		// in the following steps; however, we do sorting
		// for easier code maintainability
		
		// array to hold solution (mapping from z's to y's)
		int[] z = new int[numMentions];
		for (int i=0; i < numMentions; i++) z[i] = -1;

		// there is a special case where there are more target
		// relations than there are mentions; in this case we
		// only add the highest scoring edges
		if (numMentions < doc.Y.length) {
			// sort edges by decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					double d = e2.score - e1.score;
					if (d < 0) return -1; else return 1;
				}});

			boolean[] ysCovered = new boolean[numRelevantRelations];			
			for (int ei = 0; ei < es.length; ei++) {
				Edge e = es[ei];
				if (e.y == 0) continue;
				if (z[e.m] < 0 && !ysCovered[e.y]) {
					z[e.m] = doc.Y[e.y-1];
					ysCovered[e.y] = true;
				}
			}
		} else {
			// more mentions than target relations: enforce all Ys
			
			// sort by y, then decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					int c = e1.y - e2.y;
					if (c != 0) {
						return c;
					}	
					double d = e1.score - e2.score;
					if(d < 0){
						return 1;
					}
					else if(d > 0){
						return -1;
					}
					else{
						return 0;
					}

				}});
			
			// note that after this step the "es" array has to
			// be indexed differently
			
			// iterate over y's
			for (int y=1; y < numRelevantRelations; y++) {
	
				// find highest weight edge to y, from a
				// mention m which does not yet have an
				// outgoing edge
				
				for (int j=0; j < numMentions; j++) {
					Edge e = es[numMentions*y + j];
					if (z[e.m] < 0) {
						// we can add this edge
						//System.out.println("adding " + doc.Y[y-1]);
						z[e.m] = (y==0)? 0 : doc.Y[y-1];
						if(iteration == 0 || iteration == 1 || iteration == 25 || iteration == 50){
							if(DebugUtils.docMatchesIds(doc,dd.id1,dd.id2)){
								System.out.println("\nHighest Scoring Mention for Relation " + dd.relID2RelMap.get(doc.Y[y-1]) + " is\n"
										+ "mention " + e.m + " " + dd.sentences.get(e.m) +"\n"
										+ "This relation is enforced by Conditional Inference");
							}
						}
						break;
					}
				}
			}
			
			// there might be unmapped m's
			// sort by m, then decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					int c = e1.m - e2.m;
					if (c != 0) return c;
					double d = e2.score - e1.score;
					if (d < 0) return -1; else return 1;
				}});

			
			for (int m=0; m < numMentions; m++) {
				if (z[m] < 0) {
					// unmapped mention, need to take highest score
					Edge e = es[numRelevantRelations*m];
					z[m] = e.y == 0? 0 : doc.Y[e.y-1];
				}
			}
		}
		
		// we can now write the results
		parse.Y = doc.Y;
		parse.Z = z;
		parse.score = 0;
		for (int i=0; i < numMentions; i++) {
			parse.score += vp[i].scores[z[i]];
		}
		
		if(iteration == 0 || iteration == 1 || iteration == 25 || iteration == 50){
			if(DebugUtils.docMatchesIds(doc,dd.id1,dd.id2)){
				System.out.println("Conditional Inference Results:\n");
				DebugUtils.printPredictions(parse, dd);
			}
		}
		
		return parse;
		
		
		
	}
	
	
	public static Parse inferWithoutNA(MILDocument doc,
			Scorer parseScorer, Parameters params) {
		int numMentions = doc.numMentions;
		
		Parse parse = new Parse();
		parse.doc = doc;
		parseScorer.setParameters(params);
		
		Viterbi viterbi = new Viterbi(params.model, parseScorer);
		
		Viterbi.Parse[] vp = new Viterbi.Parse[numMentions];
		for (int m = 0; m < numMentions; m++) {
			vp[m] = viterbi.parse(doc, m);
		}
		
		//************CHANGE*************//
		// each mention can be linked to one of the doc relations or NA
		//int numRelevantRelations = doc.Y.length + 1;
		// there is no NA relation
		int numRelevantRelations = doc.Y.length;

		
		
		
		// solve bipartite graph matching problem
		Edge[] es = new Edge[numMentions * numRelevantRelations];
		for (int m = 0; m < numMentions; m++) {
			//************CHANGE*************//
//
//			// edge from m to NA
//			es[numRelevantRelations*m + 0] =
//				new Edge(m, 0, vp[m].scores[0]);
//			// edge from m to any other relation
			
//			for (int y = 1; y < numRelevantRelations; y++)
//				es[numRelevantRelations*m + y] = 
//					new Edge(m, y, vp[m].scores[doc.Y[y-1]]);
			// numRelevantRelations and doc.Y should match up
			for (int y = 0; y < numRelevantRelations; y++)
				es[numRelevantRelations*m + y] = 
					new Edge(m, y, vp[m].scores[doc.Y[y]]);
		}

		// NOTE: strictly speaking, no sorting is necessary
		// in the following steps; however, we do sorting
		// for easier code maintainability
		
		// array to hold solution (mapping from z's to y's)
		int[] z = new int[numMentions];
		for (int i=0; i < numMentions; i++) z[i] = -1;

		// there is a special case where there are more target
		// relations than there are mentions; in this case we
		// only add the highest scoring edges
		if (numMentions < doc.Y.length) {
			// sort edges by decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					double d = e2.score - e1.score;
					if (d < 0) return -1; else return 1;
				}});

			boolean[] ysCovered = new boolean[numRelevantRelations];			
			for (int ei = 0; ei < es.length; ei++) {
				Edge e = es[ei];
				//*****CHANGE*****//
				// comment out skipping on the NA relation
				//if (e.y == 0) continue;
				if (z[e.m] < 0 && !ysCovered[e.y]) {
					z[e.m] = doc.Y[e.y];
					ysCovered[e.y] = true;
				}
			}
		} else {
			// more mentions than target relations: enforce all Ys
			
			// sort by y, then decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					int c = e1.y - e2.y;
					if (c != 0) {
						return c;
					}	
					double d = e1.score - e2.score;
					if(d < 0){
						return 1;
					}
					else if(d > 0){
						return -1;
					}
					else{
						return 0;
					}

				}});
			
			// note that after this step the "es" array has to
			// be indexed differently
			
			//*************CHANGE***********//
			//iterate from index 0...
			// iterate over y's
			for (int y=0; y < numRelevantRelations; y++) {
	
				// find highest weight edge to y, from a
				// mention m which does not yet have an
				// outgoing edge
				
				for (int j=0; j < numMentions; j++) {
					Edge e = es[numMentions*y + j];
					if (z[e.m] < 0) {
						// we can add this edge
						//System.out.println("adding " + doc.Y[y-1]);
						z[e.m] = doc.Y[y];
						break;
					}
				}
			}
			
			// there might be unmapped m's
			// sort by m, then decreasing score
			Arrays.sort(es, new Comparator<Edge>() {
				public int compare(Edge e1, Edge e2) {
					int c = e1.m - e2.m;
					if (c != 0) return c;
					double d = e2.score - e1.score;
					if (d < 0) return -1; else return 1;
				}});

			
			for (int m=0; m < numMentions; m++) {
				if (z[m] < 0) {
					// unmapped mention, need to take highest score
					Edge e = es[numRelevantRelations*m];
					z[m] = doc.Y[e.y];
				}
			}
		}
		
		// we can now write the results
		parse.Y = doc.Y;
		parse.Z = z;
		parse.score = 0;
		for (int i=0; i < numMentions; i++) {
			parse.score += vp[i].scores[z[i]];
		}
		
		if(doc.arg1.equals("/m/03gzs3x") && doc.arg2.equals("/m/0fn2g")){
			System.out.println("Conditional Inference..");
			System.out.println("Arg1 = " + doc.arg1 + " and Arg2 = " + doc.arg2);
			System.out.println("Y rels...");
			for(int j = 0; j < doc.Y.length; j++){
				System.out.println(doc.Y[j]);
			}
			
			System.out.println("Z rels...");
			for(int j = 0; j < doc.Z.length; j++){
				System.out.println(doc.Z[j]);
			}
			
			for(int i =0; i < z.length; i++){
				System.out.println("mention " + i + " has relation " + z[i]);
			}
		}
		
		
		return parse;
		
		
		
	}

	static class Edge {
		int m;
		int y;
		double score;		
		Edge(int m, int y, double score) {
			this.m = m;
			this.y = y;
			this.score = score;
		}
	}
}
