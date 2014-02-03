
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
public class Group9_AS extends AcceptanceStrategy{

    @Override
    // Init of the Acceptance Strategy (AS)
    public void init(NegotiationSession negoSession, OfferingStrategy strat, HashMap<String, Double> parameters) throws Exception {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
    }

    @Override
    // Determine if the opponents bid can be accepted
    public Actions determineAcceptability() {
        // declaring and obtaining the factors that the AS depends on
        BidHistory bh = this.negotiationSession.getOpponentBidHistory();
        double myBidUtility = this.offeringStrategy.getNextBid().getMyUndiscountedUtil();
        double oppBidUtility = bh.getLastBidDetails().getMyUndiscountedUtil();
        double timeNorm = this.negotiationSession.getTime();
        double currentTime = this.negotiationSession.getTimeline().getCurrentTime();
        BidHistory currentBH = new BidHistory();
        double timeWindow = 0;
        
        // Obtain at least the 10 most recent bids
        while(currentBH.size() < 10){
            timeWindow += 5;
            if(currentTime <= timeWindow){ 
                currentBH = bh.filterBetweenTime(0, currentTime);
                break;
            }
            else{
                currentBH = bh.filterBetweenTime(currentTime-timeWindow, currentTime);
            }
        }

        // Calculate statistics of these obtained bids
        List<BidDetails> bids = currentBH.getHistory();
        double currentMean = currentBH.getAverageUtility();
        double totalMean = bh.getAverageUtility();
        double std = 0;
        for(int i=0;i<bids.size();i++){
            std = std + (bids.get(i).getMyUndiscountedUtil() - currentMean);
        }
        std = std / bids.size();
        double disFactor = this.negotiationSession.getDiscountFactor();
        
        // Calculate the minimum utility that a bid should have before it can be accepted
        double bid = myBidUtility - (currentMean-totalMean)* (currentMean*std) * Math.pow(timeNorm,disFactor);
                
        if(oppBidUtility > bid){
            return Actions.Accept;
        }
        else if(oppBidUtility > myBidUtility){
            return Actions.Accept;
        }
        return Actions.Reject;
    }

}
