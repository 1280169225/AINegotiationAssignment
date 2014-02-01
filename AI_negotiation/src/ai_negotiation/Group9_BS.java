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
     * Tit-for-tat strategy
     */
    public BidDetails determineNextBid() {
        BidDetails opponentSecondToLastBid = opponentLastBid;
        opponentLastBid = this.negotiationSession.getOpponentBidHistory().getLastBidDetails();
        double timeNorm = this.negotiationSession.getTime();
        double disFactor = this.negotiationSession.getDiscountFactor();

        double secondToLastUtility = this.opponentModel.getBidEvaluation(opponentSecondToLastBid.getBid());
        double lastUtility = this.opponentModel.getBidEvaluation(opponentLastBid.getBid());
        double difference = lastUtility - secondToLastUtility;

        BidDetails myLastBid = this.negotiationSession.getOwnBidHistory().getLastBidDetails();
        double newUtility = myLastBid.getMyUndiscountedUtil() - difference * Math.pow(timeNorm, disFactor);
        nextBid = outcomeSpace.getBidNearUtility(newUtility);
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
