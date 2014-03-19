package edu.uw.cs.multir.test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.google.protobuf.Descriptors.FieldDescriptor;

import cc.factorie.protobuf.DocumentProtos.Relation;
import cc.factorie.protobuf.DocumentProtos.Relation.RelationMentionRef;
import edu.uw.cs.multir.learning.data.MILDocument;

public class ReadProtobufRelations {
	
	public static List<String> convert(String input) throws IOException {
		
	
	    InputStream is = new GZIPInputStream(
	    		new BufferedInputStream
	    		(new FileInputStream(input)));
	    Relation r = null;
	    MILDocument doc = new MILDocument();
	    
	    int count = 0;
	    boolean printInfo = false;
    	    
	    while ((r = Relation.parseDelimitedFrom(is))!=null) {

	    		
    		RelationMentionRef rmf = r.getMention(0);
    		System.out.println(r.getSourceGuid() + "\t" + r.getRelType() + "\t" + r.getDestGuid());
//    		if(r.getRelType().equals("/people/deceased_person/place_of_death") && rmf.getFeatureList().size() > 10){
//    			    Map<FieldDescriptor,Object> rmFeatures = r.getAllFields();
//    			    for(FieldDescriptor fd :rmFeatures.keySet()){
//    			    	System.out.println(fd.getFullName() + "\t" + rmFeatures.get(fd));
//    			    }
//    		        List<String> pbRelationAndFeatures= new ArrayList<String>();
//    				pbRelationAndFeatures.add(r.getRelType());
//    				pbRelationAndFeatures.add(r.getSourceGuid());
//    				pbRelationAndFeatures.add(r.getDestGuid());
//    				pbRelationAndFeatures.add(rmf.getSentence());
//    				for(String f : rmf.getFeatureList()){
//    					pbRelationAndFeatures.add(f);
//    				}
//    				return pbRelationAndFeatures;
//    		}
	    }
	    
	    return null;

	}
	
	public static void main (String[] args) throws IOException{
		convert("/scratch2/code/multir/multir-release/train-Multiple.pb.gz");
	}
	
	public static int numberOfFeatures(String input) throws IOException{
		
	    InputStream is = new GZIPInputStream(
	    		new BufferedInputStream
	    		(new FileInputStream(input)));
	    Relation r = null;
	    
	    Set<String> featureSet = new HashSet<String>();
	    while((r = Relation.parseDelimitedFrom(is))!=null){
    		List<RelationMentionRef> rmfList = r.getMentionList();
    		for(RelationMentionRef rmf : rmfList){
    		 featureSet.addAll(rmf.getFeatureList());
    		}
	    }
		
		return featureSet.size();
	}

}
