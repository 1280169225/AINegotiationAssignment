package agents2011.southampton.utils;

import java.util.HashMap;

import negotiator.Bid;
import negotiator.utility.UtilitySpace;

public class ProbabilityBidCreator extends BiasedBidCreator {

	public ProbabilityBidCreator(UtilitySpace utilitySpace) {
		super(utilitySpace);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see agents2011.southampton.utils.RandomBidCreator#getBid(negotiator.utility.UtilitySpace, double, double)
	 */
	@Override
	public Bid getBid(UtilitySpace utilitySpace, double min, double max) {
		int samples = 100;
		Bid[] bids = new Bid[samples];
		double[] weights = new double[samples];
		EBid[] ebids = new EBid[samples];
		double sumWeights = 0;
		//System.out.println("Making " + samples + " samples...");
		for(int i=0; i<samples; i++)
		{
			bids[i] = getRandomBid(utilitySpace, min, max);
			weights[i] = evaluate(bids[i]);
			ebids[i] = new EBid(bids[i], weights[i]);
			sumWeights += weights[i];
		}
		//System.out.println("Done");
	
		//for(int i = 0; i<samples; i++) {
		//	System.out.print(weights[i] + ",");
		//}
		//System.out.println();
		
		double rand = random.nextDouble() * sumWeights;
		int i=0;
		while(rand > 0) {
			rand -= ebids[i].getValue();
			i++;
		}
		return ebids[i].getBid();
	}
	
	protected double evaluate(Bid bid) {
		int i = 0;
		double evaluation = 1;
		for(int issueNumber : issueNumbers)
		{
			try {
				HashMap<String, Double> hm = issueWeights.get(i);
				double issueWeight = hm.get(bid.getValue(issueNumber));
				double issueWeightSum = issueWeightSums[i];
				evaluation *= issueWeight/issueWeightSum;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
		}
		return evaluation;
	}
}
