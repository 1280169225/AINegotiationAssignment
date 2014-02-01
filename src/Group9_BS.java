import java.util.HashMap;

import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.SortedOutcomeSpace;


public class Group9_BS extends OfferingStrategy{

	private SortedOutcomeSpace outcomeSpace;
	private BidDetails opponentLastBid;
	
	/**
	 * Empty constructor
	 */
	public Group9_BS(){
	}
	
	@Override
	/**
	 * init method called at the beginning
	 */
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, HashMap<String, Double> parameters) throws Exception {
			this.negotiationSession = negoSession;
			
			outcomeSpace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
			negotiationSession.setOutcomeSpace(outcomeSpace);
			
			
			this.opponentModel = model;
			this.omStrategy = oms;
	}
	
	
	@Override
	/**
	 * Tit-for-tat strategy
	 */
	public BidDetails determineNextBid() {
		BidDetails opponentSecondToLastBid = opponentLastBid;
		opponentLastBid = this.negotiationSession.getOpponentBidHistory().getLastBidDetails();
		
		double secondToLastUtility = this.opponentModel.getBidEvaluation(opponentSecondToLastBid.getBid());
		double lastUtility = this.opponentModel.getBidEvaluation(opponentLastBid.getBid());
		double difference = secondToLastUtility - lastUtility;
		
		BidDetails myLastBid = this.negotiationSession.getOwnBidHistory().getLastBidDetails();
		double newUtility = myLastBid.getMyUndiscountedUtil() - difference;
		
		return outcomeSpace.getBidNearUtility(newUtility);
	}

	@Override
	/**
	 * Opening bid is always the best possible one
	 */
	public BidDetails determineOpeningBid() {
		return this.outcomeSpace.getBidNearUtility(1.0);
	}

}
