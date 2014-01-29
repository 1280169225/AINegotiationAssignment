package agents2011.southampton;

import agents2011.southampton.SouthamptonAgent;
import negotiator.Bid;

/**
 * @author Colin Williams
 * 
 *         The IAMcrazyHaggler Agent, created for ANAC 2010. Designed by C. R.
 *         Williams, V. Robu, E. H. Gerding and N. R. Jennings.
 * 
 */
public class IAMcrazyHaggler extends SouthamptonAgent {

	protected double breakOff = 0.9;// 0.93;

	public void init() {
		super.init();
		MAXIMUM_ASPIRATION = 0.85;// 93;
		if (this.utilitySpace.getDiscountFactor() == 0) {
			MAXIMUM_ASPIRATION = 0.9;
			breakOff = 0.95;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see negotiator.Agent#getName()
	 */
	@Override
	public String getName() {
		return "IAMcrazyHaggler";
	}

	@Override
	protected Bid proposeInitialBid() throws Exception {
		return proposeRandomBid();
	}

	@Override
	protected Bid proposeNextBid(Bid opponentBid) throws Exception {
		return proposeRandomBid();
	}

	private Bid proposeRandomBid() {
		Bid bid = null;
		try {
			do {
				bid = this.utilitySpace.getDomain().getRandomBid();
			} while (this.utilitySpace.getUtility(bid) <= breakOff);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bid;
	}

	/**
	 * @return
	 */
	public static String getVersion() {
		return "2.0";
	}
}
