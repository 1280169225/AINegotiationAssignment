package agents2011.southampton.utils;

import negotiator.Bid;
import negotiator.utility.UtilitySpace;

public class BayesianBidCreator extends BiasedBidCreator {

	private OpponentModel om;

	/* (non-Javadoc)
	 * @see agents2011.southampton.utils.BiasedBidCreator#evaluate(negotiator.Bid)
	 */
	@Override
	protected double evaluate(Bid bid) {
		try {
			return om.getExpectedUtility(bid);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see agents2011.southampton.utils.BiasedBidCreator#logBid(negotiator.Bid)
	 */
	@Override
	public void logBid(Bid bid, double time) {
		super.logBid(bid, time);
		try {
			System.out.println(bid);
			om.updateBeliefs(bid, time);
			om.myPrintHypotheses();
			om.printBestHypothesis();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public BayesianBidCreator(UtilitySpace utilitySpace) {
		super(utilitySpace);
		om = new OpponentModel(utilitySpace);
	}

}
