package agents.bayesianopponentmodel;

import negotiator.Bid;
import negotiator.utility.UtilitySpace;

public class OpponentModelUtilSpace extends UtilitySpace
{
	OpponentModel opponentmodel;
	
	public OpponentModelUtilSpace(OpponentModel opmod)
	{
		domain=opmod.getDomain();
		opponentmodel=opmod;
	}
	
	public double getUtility(Bid b)
	{ 
		double u=0.;
		try { u=opponentmodel.getNormalizedUtility(b); } 
		catch (Exception e) {System.out.println("getNormalizedUtility failed. returning 0");u=0.;}
		return u;
	} 
}
