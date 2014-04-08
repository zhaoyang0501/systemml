/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2013
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */


package com.ibm.bi.dml.runtime.util;

public abstract class PRNGenerator 
{
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2013\n" +
                                             "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
	
	long seed = -1;
	
	public abstract void init(long sd);
	
	public PRNGenerator() {
		seed = -1;
	}
	
	public PRNGenerator(long sd) {
		seed = sd;
	}
	
	public abstract double nextDouble() ;
}
