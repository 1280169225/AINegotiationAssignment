/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai_negotiation;

import java.util.HashMap;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.SortedOutcomeSpace;

public class Group9_BS extends OfferingStrategy {

	private SortedOutcomeSpace outcomeSpace;
	private BidDetails opponentLastBid;
	private BidDetails opponentMaxBid;
	private double start = 1;

	/**
	 * Empty constructor
	 */
	public Group9_BS() {
	}

	@Override
	/**
	 * init method called at the beginning
	 */
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, HashMap<String, Double> parameters) {
		this.negotiationSession = negoSession;

		outcomeSpace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negotiationSession.setOutcomeSpace(outcomeSpace);


		this.opponentModel = model;
		this.omStrategy = oms;
	}

	@Override
	/**
	 * Tit-for-tat approach for creating a bid
	 */
	public BidDetails determineNextBid() {
		if(opponentLastBid == null){
			opponentLastBid = this.negotiationSession.getOpponentBidHistory().getFirstBidDetails(); 
		}
		if(opponentMaxBid == null){
			opponentMaxBid = this.negotiationSession.getOpponentBidHistory().getFirstBidDetails();
		}

		// Calculate and obtain factors for the bid creation process
		BidDetails opponentCurrentBid = this.negotiationSession.getOpponentBidHistory().getLastBidDetails();
		double timeNorm = this.negotiationSession.getTime();
		double disFactor = this.negotiationSession.getDiscountFactor();

		double lastOppUtility = opponentLastBid.getMyUndiscountedUtil();
		double currentOppUtility = opponentCurrentBid.getMyUndiscountedUtil();
		double maxOppUtility = opponentMaxBid.getMyUndiscountedUtil();
		double difference;

		// Two cases: current utility of opponent bid is lower than his maximum bid
		// or it is higher: difference are calculated different then.
		if(currentOppUtility > maxOppUtility){
			difference = (maxOppUtility - currentOppUtility);
		}
		else{
			difference = (currentOppUtility - lastOppUtility) * (1-(maxOppUtility-currentOppUtility));
		}

		// Same story again, but this time for the differences
		if(difference > 0){
			difference *= Math.pow(timeNorm, disFactor);
		}
		else{
			difference *= Math.pow(1-timeNorm, disFactor);
		}

		BidDetails myLastBid = this.negotiationSession.getOwnBidHistory().getLastBidDetails();
		double newUtility;
		// When no agreement is met before almost the end of negotiation 
		// then concede slowly to opponent maximum bid so far
		if(timeNorm > 0.95){
			double diff = start-maxOppUtility;
			double time = this.negotiationSession.getTimeline().getTotalTime();

			newUtility = (maxOppUtility + diff) - (1 / time);
			start = newUtility;
			if(newUtility < opponentMaxBid.getMyUndiscountedUtil()){
				nextBid = opponentMaxBid;
				return nextBid;
			}
		}
		else{
			newUtility = myLastBid.getMyUndiscountedUtil() - (0.2*difference);
		}
		nextBid = omStrategy.getBid(outcomeSpace, newUtility);
		opponentLastBid = opponentCurrentBid;
		opponentMaxBid = this.negotiationSession.getOpponentBidHistory().getBestBidDetails();

		return nextBid;
	}

	@Override
	/**
	 * Opening bid is always the best possible one
	 */
	public BidDetails determineOpeningBid() {
		nextBid = outcomeSpace.getMaxBidPossible();
		return nextBid;
	}
}
