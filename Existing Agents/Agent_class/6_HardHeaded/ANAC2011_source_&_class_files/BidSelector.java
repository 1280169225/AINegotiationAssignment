

import java.util.ArrayList;
import java.util.TreeMap;
import negotiation.group_smith.util.Log;
import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.Value;
import negotiator.utility.UtilitySpace;
import negotiator.issue.IssueDiscrete;
import java.util.HashMap;

/**
 * This class generates all possible bids that can be offered according to a given domain. 
 * The bids are stored in a treeMap structure sorted by the order of their utility for the agent.
 * 
 */
public class BidSelector
{
	protected UtilitySpace utilitySpace;
	TreeMap<Double, Bid> BidList;
	/**
	 * BidSelector constructor
	 * 
	 * @param pUtilitySpace a passed {@link UtilitySpace} that is used to generate all possible bid of its domain
	 */
	public BidSelector(UtilitySpace pUtilitySpace){
		
		this.utilitySpace=pUtilitySpace;
		BidList = new TreeMap<Double, Bid>();
		
		ArrayList<Issue> issues = utilitySpace.getDomain().getIssues();
			
		HashMap<Integer, Value> InitialBid = new HashMap<Integer,Value>();
		for (Issue lIssue:issues) {
			Value v=((IssueDiscrete) lIssue).getValue(0);
			InitialBid.put(lIssue.getNumber(), v);
		}
		try{
			Bid b= new Bid(utilitySpace.getDomain(),InitialBid);
			BidList.put(utilitySpace.getUtility(b) , b);
			}
		
		catch (Exception e){
			e.printStackTrace();
		}
		
		for (Issue lIssue:issues) 
		{ 
		
			TreeMap<Double, Bid> TempBids = new TreeMap<Double, Bid>();

			IssueDiscrete lIssueDiscrete = (IssueDiscrete)lIssue;
			int optionIndex=lIssueDiscrete.getNumberOfValues();
			
			// a small value is added to bid utilities, so that their ordering inside the 
			// TreeMap would be attained
			double d = -0.00000001;
			for(Bid TBid: BidList.values())
			{
				for(int i=0;i<optionIndex; i++)
				{
					HashMap<Integer,Value> NewBidV= Bidconfig(TBid);
					NewBidV.put(lIssue.getNumber(),((IssueDiscrete) lIssue).getValue(i));
					try{
					Bid webid= new Bid(utilitySpace.getDomain(),NewBidV);
					TempBids.put(utilitySpace.getUtility(webid) + d, webid);
					d = d - 0.00000001;
					}
					catch(Exception e){}
					
					
				}
			BidList = TempBids;	
			}
		}
	}
	
	/**
	 * receives a bid an generates all possible configurations
	 * of it
	 * 
	 * @param pBid passed bid 
	 * @return a {@link HashMap} containing all configurations of a possible bid.
	 */
	private HashMap<Integer,Value> Bidconfig(Bid pBid) {
		HashMap<Integer, Value> lNewBidValues = new HashMap<Integer, Value>();
		
		
		for(Issue lIssue:this.utilitySpace.getDomain().getIssues()) {
			try {
				lNewBidValues.put(lIssue.getNumber(), pBid.getValue(lIssue.getNumber()));
			} catch(Exception e) {
				Log.logger.warning("Setting value error" + e.toString());
			}
		}
		return lNewBidValues;
	}
}
