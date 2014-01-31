
package ai_negotiation;

import java.util.HashMap;
import java.util.List;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.AcceptanceStrategy;
import negotiator.boaframework.Actions;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OfferingStrategy;

/**
 *
 * @author Benny
 */
public class Group9AS extends AcceptanceStrategy{

    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, HashMap<String, Double> parameters) throws Exception {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
    }

    @Override
    public Actions determineAcceptability() {
        BidHistory bh = this.negotiationSession.getOpponentBidHistory();
        double myBidUtility = this.offeringStrategy.getNextBid().getMyUndiscountedUtil();
        double oppBidUtility = bh.getLastBidDetails().getMyUndiscountedUtil();
        double timeNorm = this.negotiationSession.getTime();
        double currentTime = this.negotiationSession.getTimeline().getCurrentTime();
        BidHistory currentBH = new BidHistory();
        double timeWindow = 0;
        while(currentTime < timeWindow || currentBH.size() < 10){
            timeWindow += 10;
            if(currentTime < timeWindow){ 
                currentBH = bh.filterBetweenTime(0, currentTime);
            }
            else{
                currentBH = bh.filterBetweenTime(currentTime-timeWindow, currentTime);
            }
        }
        List<BidDetails> bids = currentBH.getHistory();
        double meanUtility = currentBH.getAverageUtility();
        double std = 0;
        for(int i=0;i<bids.size();i++){
            std = std + (bids.get(i).getMyUndiscountedUtil() - meanUtility);
        }
        std = std / bids.size();
        double disFactor = this.negotiationSession.getDiscountFactor();
        double bid = myBidUtility - (1 * std) * Math.pow(timeNorm,1-disFactor);
                
        if(oppBidUtility >= bid){
            return Actions.Accept;
        }
        return Actions.Reject;
    }

}
