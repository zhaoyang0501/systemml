/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2014
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */


package com.ibm.bi.dml.runtime.matrix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.Counters.Group;

import com.ibm.bi.dml.lops.runtime.RunMRJobs.ExecMode;
import com.ibm.bi.dml.runtime.instructions.MRJobInstruction;
import com.ibm.bi.dml.runtime.matrix.io.InputInfo;
import com.ibm.bi.dml.runtime.matrix.io.OutputInfo;
import com.ibm.bi.dml.runtime.matrix.io.TaggedInt;
import com.ibm.bi.dml.runtime.matrix.io.WeightedCell;
import com.ibm.bi.dml.runtime.matrix.mapred.GroupedAggMRCombiner;
import com.ibm.bi.dml.runtime.matrix.mapred.GroupedAggMRMapper;
import com.ibm.bi.dml.runtime.matrix.mapred.GroupedAggMRReducer;
import com.ibm.bi.dml.runtime.matrix.mapred.MRJobConfiguration;
import com.ibm.bi.dml.runtime.matrix.mapred.MRJobConfiguration.ConvertTarget;
import com.ibm.bi.dml.runtime.util.MapReduceTool;


public class GroupedAggMR 
{
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2013\n" +
	                                         "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
		
	private static final Log LOG = LogFactory.getLog(GroupedAggMR.class.getName());
	public static JobReturn runJob(MRJobInstruction inst, String[] inputs, InputInfo[] inputInfos, long[] rlens, long[] clens, 
			int[] brlens, int[] bclens, String grpAggInstructions, String simpleReduceInstructions/*only scalar or reorg instructions allowed*/, 
			int numReducers, int replication, byte[] resultIndexes,	String dimsUnknownFilePrefix, String[] outputs, OutputInfo[] outputInfos) 
	throws Exception
	{
		JobConf job;
		job = new JobConf(GroupedAggMR.class);
		job.setJobName("GroupedAgg-MR");
		
		//whether use block representation or cell representation
		//MRJobConfiguration.setMatrixValueClassForCM_N_COM(job, true);
		MRJobConfiguration.setMatrixValueClass(job, false);
	
		//added for handling recordreader instruction
		String[] realinputs=inputs;
		InputInfo[] realinputInfos=inputInfos;
		long[] realrlens=rlens;
		long[] realclens=clens;
		int[] realbrlens=brlens;
		int[] realbclens=bclens;
		byte[] realIndexes=new byte[inputs.length];
		for(byte b=0; b<realIndexes.length; b++)
			realIndexes[b]=b;
		
		//set up the input files and their format information
		MRJobConfiguration.setUpMultipleInputs(job, realIndexes, realinputs, realinputInfos, 
				realbrlens, realbclens, true, ConvertTarget.WEIGHTEDCELL);
		
		//set up the dimensions of input matrices
		MRJobConfiguration.setMatricesDimensions(job, realIndexes, realrlens, realclens);
		MRJobConfiguration.setDimsUnknownFilePrefix(job, dimsUnknownFilePrefix);
		//set up the block size
		MRJobConfiguration.setBlocksSizes(job, realIndexes, realbrlens, realbclens);
		
		//set up the grouped aggregate instructions that will happen in the combiner and reducer
		MRJobConfiguration.setGroupedAggInstructions(job, grpAggInstructions);
		
		//set up the instructions that will happen in the reducer, after the aggregation instrucions
		MRJobConfiguration.setInstructionsInReducer(job, simpleReduceInstructions);
		
		//set up the number of reducers
		MRJobConfiguration.setNumReducers(job, numReducers, numReducers);
		
		//set up the replication factor for the results
		job.setInt("dfs.replication", replication);
		
		//set up what matrices are needed to pass from the mapper to reducer
		MRJobConfiguration.setUpOutputIndexesForMapper(job, realIndexes, null, null, 
				grpAggInstructions, resultIndexes);
		
		MatrixCharacteristics[] stats=new MatrixCharacteristics[resultIndexes.length];
		for( int i=0; i < resultIndexes.length; i++ )
			stats[i] = new MatrixCharacteristics();
		
		// Print the complete instruction
		if (LOG.isTraceEnabled())
			inst.printCompleteMRJobInstruction(stats);

		byte[] resultDimsUnknown=new byte[resultIndexes.length];
		// Update resultDimsUnknown based on computed "stats"
		for ( int i=0; i < resultIndexes.length; i++ )  
			resultDimsUnknown[i] = (byte) 2;
		
		//set up the multiple output files, and their format information
		MRJobConfiguration.setUpMultipleOutputs(job, resultIndexes, resultDimsUnknown, outputs, outputInfos, false);
		
		// configure mapper and the mapper output key value pairs
		job.setMapperClass(GroupedAggMRMapper.class);
		job.setCombinerClass(GroupedAggMRCombiner.class); 
		job.setMapOutputKeyClass(TaggedInt.class);
		job.setMapOutputValueClass(WeightedCell.class);
		
		//configure reducer
		job.setReducerClass(GroupedAggMRReducer.class);
		
		//set unique working dir
		MRJobConfiguration.setUniqueWorkingDir(job, ExecMode.CLUSTER); 
		
		//execute job
		RunningJob runjob=JobClient.runJob(job);
		
		//get important output statistics 
		Group group=runjob.getCounters().getGroup(MRJobConfiguration.NUM_NONZERO_CELLS);
		for(int i=0; i<resultIndexes.length; i++) {
			// number of non-zeros
			stats[i]=new MatrixCharacteristics();
			stats[i].nonZero=group.getCounter(Integer.toString(i));
		}
		
		String dir = dimsUnknownFilePrefix + "/" + runjob.getID().toString() + "_dimsFile";
		stats = MapReduceTool.processDimsFiles(dir, stats);
		MapReduceTool.deleteFileIfExistOnHDFS(dir);
		
		return new JobReturn(stats, outputInfos, runjob.isSuccessful());
	}
	
}
