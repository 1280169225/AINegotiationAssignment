
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.utility.UtilitySpace;

/**
 * The Acceptor class is used to decide when to accept a bid.
 * 
 * @author Alex Dirkzwager, Mark Hendrikx, Julian de Ruiter
 */
public class Acceptor {
	
	// utilityspace of the negotiation
	private UtilitySpace utilitySpace;
	// reference to the bidscollection
	private BidsCollection bidsCollection;
	
	/**
	 * Creates an Acceptor-object which determines which offers should be accepted
	 * during the negotiation.
	 * 
	 * @param utilitySpace
	 * @param bidsCollection of all possible bids (for us) and the partner bids
	 */
	public Acceptor(UtilitySpace utilitySpace, BidsCollection bidsCollection) {
		this.utilitySpace = utilitySpace;
		this.bidsCollection = bidsCollection;
	}
	
	/**
	 * Determine if it is wise to accept for a given phase on a given time.
	 * 
	 * @param phase of the negotiation
	 * @param minimum threshold
	 * @param time in negotiation
	 * @param movesLeft is the estimated moves left
	 * @return move to (not) accept
	 */
	public Action determineAccept(int phase, double threshold, double time, int movesLeft) {
		Action action = null;
		double utility = 0;

		try {
			// effectively this will ensure that the utility is 0 if our agent is first
			if (bidsCollection.getPartnerBids().size() > 0) {
				// get the last opponent bid
				utility = utilitySpace.getUtility(bidsCollection.getPartnerBid(0));
			}
		} catch (Exception e) {
		}
		
		if (phase == 1 || phase == 2) {
			if (utility >= threshold) {
				action = new Accept();
			}
		} else { // phase 3
			if (movesLeft >= 15) {
				if (utility >= threshold) {
					action = new Accept();
				}
			} else {
				if (movesLeft < 15) {
					action = new Accept();
				}
			}
		}
		return action;
	}
}