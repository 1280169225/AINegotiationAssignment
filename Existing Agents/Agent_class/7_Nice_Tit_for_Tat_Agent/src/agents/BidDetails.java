package agents;

import negotiator.Bid;

public class BidDetails
{
	public final Bid bid;
	public final double myUndiscountedUtil;
	public final double time;
	
	public BidDetails(Bid bid, double myUndiscountedUtil, double time)
	{
		super();
		this.bid = bid;
		this.myUndiscountedUtil = myUndiscountedUtil;
		this.time = time;
	}

	public Bid getBid()
	{
		return bid;
	}

	public double getMyUndiscountedUtil()
	{
		return myUndiscountedUtil;
	}

	public double getTime()
	{
		return time;
	}
	
	@Override
	public String toString()
	{
		return "(u=" + myUndiscountedUtil + ", t=" + time + ")";
	}
	
}
