/**
 * (C) Copyright IBM Corp. 2010, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.ibm.bi.dml.lops;

import com.ibm.bi.dml.lops.LopProperties.ExecLocation;
import com.ibm.bi.dml.lops.LopProperties.ExecType;
import com.ibm.bi.dml.lops.WeightedSigmoid.WSigmoidType;
import com.ibm.bi.dml.lops.compile.JobType;
import com.ibm.bi.dml.parser.Expression.DataType;
import com.ibm.bi.dml.parser.Expression.ValueType;

/**
 * 
 */
public class WeightedSigmoidR extends Lop 
{

	public static final String OPCODE = "redwsigmoid";
	
	private WSigmoidType _wsType = null;

	private boolean _cacheU = false;
	private boolean _cacheV = false;
	
	public WeightedSigmoidR(Lop input1, Lop input2, Lop input3, DataType dt, ValueType vt, WSigmoidType wt, boolean cacheU, boolean cacheV, ExecType et) 
		throws LopsException 
	{
		super(Lop.Type.WeightedSigmoid, dt, vt);		
		addInput(input1); //X
		addInput(input2); //U
		addInput(input3); //V
		input1.addOutput(this); 
		input2.addOutput(this);
		input3.addOutput(this);
		
		//setup mapmult parameters
		_wsType = wt;
		_cacheU = cacheU;
		_cacheV = cacheV;
		setupLopProperties(et);
	}
	
	/**
	 * 
	 * @param et
	 * @throws LopsException 
	 */
	private void setupLopProperties( ExecType et ) 
		throws LopsException
	{
		if( et == ExecType.MR )
		{
			//setup MR parameters 
			boolean breaksAlignment = true;
			boolean aligner = false;
			boolean definesMRJob = false;
			lps.addCompatibility(JobType.GMR);
			lps.addCompatibility(JobType.DATAGEN);
			lps.setProperties( inputs, ExecType.MR, ExecLocation.Reduce, breaksAlignment, aligner, definesMRJob );
		}
		else //Spark/CP
		{
			//setup Spark parameters 
			boolean breaksAlignment = false;
			boolean aligner = false;
			boolean definesMRJob = false;
			lps.addCompatibility(JobType.INVALID);
			lps.setProperties( inputs, et, ExecLocation.ControlProgram, breaksAlignment, aligner, definesMRJob );
		}
	}

	public String toString() {
		return "Operation = WeightedSigmoidR";
	}
	
	@Override
	public String getInstructions(int input_index1, int input_index2, int input_index3, int output_index)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(getExecType());
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append(OPCODE);
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append( getInputs().get(0).prepInputOperand(input_index1));
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append( getInputs().get(1).prepInputOperand(input_index2));
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append( getInputs().get(2).prepInputOperand(input_index3));
	
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append( prepOutputOperand(output_index));
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append(_wsType);
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append(_cacheU);
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append(_cacheV);
		
		return sb.toString();
	}

	@Override
	public String getInstructions(String input1, String input2, String input3, String output)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(getExecType());
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append(OPCODE);
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append( getInputs().get(0).prepInputOperand(input1));
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append( getInputs().get(1).prepInputOperand(input2));
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append( getInputs().get(2).prepInputOperand(input3));
	
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append( prepOutputOperand(output));
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append(_wsType);
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append(_cacheU);
		
		sb.append(Lop.OPERAND_DELIMITOR);
		sb.append(_cacheV);
		
		return sb.toString();
	}
	
	@Override
	public boolean usesDistributedCache() 
	{
		if( _cacheU || _cacheV )
			return true;
		else
			return false;
	}
	
	@Override
	public int[] distributedCacheInputIndex() 
	{
		if( !_cacheU && !_cacheV )
			return new int[]{-1};
		else if( _cacheU && !_cacheV )
			return new int[]{2};
		else if( !_cacheU && _cacheV )
			return new int[]{3};
		else
			return new int[]{2,3};
	}
}
