

import java.util.HashMap;
import java.util.Map.Entry;

import negotiator.Bid;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OpponentModel;
import negotiator.issue.Issue;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;
import negotiator.issue.IssueDiscrete;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.UtilitySpace;

/**
 * BOA framework implementation of a simple Frequency model. Based on
 * the hardheaded frequency model, but takes time into account.
 * 
 */
public class FrequencyOpponentModel extends OpponentModel {

	private double standardAddedWeight;
	private int numberOfIssues;
	private int standardValueAddition;


	/**
	 * Initializes the utility space of the opponent such that all value
	 * issue weights are equal.
	 */
	@Override
	public void init(NegotiationSession negotiationSession, HashMap<String, Double> parameters) throws Exception {
		this.negotiationSession = negotiationSession;
		if (parameters != null && parameters.get("l") != null) {
			standardAddedWeight = parameters.get("l");
		} else {
			standardAddedWeight = 0.2;
		}
		standardValueAddition = 1;
		initializeModel();
	}

	private void initializeModel(){
		opponentUtilitySpace = new UtilitySpace(negotiationSession.getUtilitySpace());
		numberOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		double commonWeight = 1D / (double)numberOfIssues;    

		// initialize the weights
		for(Entry<Objective, Evaluator> e: opponentUtilitySpace.getEvaluators()){
			// set the issue weights
			opponentUtilitySpace.unlock(e.getKey());
			e.getValue().setWeight(commonWeight);
			try {
				// set all value weights to one (they are normalized when calculating the utility)
				for(ValueDiscrete vd : ((IssueDiscrete)e.getKey()).getValues())
					((EvaluatorDiscrete)e.getValue()).setEvaluation(vd,1);  
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}


	/**
	 * A method to check which issues have changed between two bids. If a value
	 * is different then the corresponding issue is set to true.
	 * @param firstBid
	 * @param secondBid
	 * @return
	 */
	private HashMap<Integer, Boolean> bidDifference(BidDetails firstBid, BidDetails secondBid){
		HashMap<Integer, Boolean >issueChanged = new HashMap<Integer, Boolean>();

		int i = 0;
		for(Issue issue : opponentUtilitySpace.getDomain().getIssues()){
			try{
				Value firstBidValue = firstBid.getBid().getValue(issue.getNumber());
				Value secondBidValue = firstBid.getBid().getValue(issue.getNumber());
				if(firstBidValue.equals(secondBidValue))
					issueChanged.put(issue.getNumber(), false);
				else
					issueChanged.put(issue.getNumber(), true);
				i++;
			}catch(Exception  e){
				e.printStackTrace();
			}

		}
		return issueChanged;
	}

	/**
	 * Updates the opponent model given a bid.
	 */
	@Override
	public void updateModel(Bid opponentBid, double time) {

		int indexOfLastBid = negotiationSession.getOpponentBidHistory().getHistory().size()-1;

		BidDetails lastOppBid = negotiationSession.getOpponentBidHistory().getHistory().get(indexOfLastBid); 
		BidDetails prevOppBid = negotiationSession.getOpponentBidHistory().getHistory().get(indexOfLastBid-1);

		HashMap<Integer, Boolean> differenceBetweenBids = bidDifference(lastOppBid, prevOppBid);

		int numberOfUnchangedIssues = 0;
		for(Integer issue : differenceBetweenBids.keySet()){
			if(!differenceBetweenBids.get(issue))
				numberOfUnchangedIssues++;
		}

		//Now update the weight in the opponent utility space

		/*
		 * The later an issue is changed the more important it should be.
		 * If two issues are changed simeoultaneusly they must be regarded
		 * as equally important.
		 */
		double addedWeight = standardAddedWeight / (time*numberOfUnchangedIssues);
		double totalWeight = 1+addedWeight*(double)numberOfUnchangedIssues; //normalized weight+added weight

		//Normalize
		for(Integer issue : differenceBetweenBids.keySet()){
			Objective currentObjective = opponentUtilitySpace.getObjective(issue);
			double currentWeight = opponentUtilitySpace.getWeight(issue);
			if(differenceBetweenBids.get(issue)){
				opponentUtilitySpace.setWeight(currentObjective, currentWeight/totalWeight);
			}else
				opponentUtilitySpace.setWeight(currentObjective, (currentWeight+addedWeight)/totalWeight);
		}

		try{
			for(Entry<Objective, Evaluator> evaluator: opponentUtilitySpace.getEvaluators()){

				EvaluatorDiscrete evaluatorValue = (EvaluatorDiscrete) evaluator.getValue();

				Bid bid = lastOppBid.getBid();
				IssueDiscrete key = (IssueDiscrete)evaluator.getKey();

				evaluatorValue.setEvaluation(bid.getValue(key.getNumber()), 
						( standardValueAddition + (evaluatorValue).getEvaluationNotNormalized(((ValueDiscrete)bid.getValue(key.getNumber()) ) ))
						);
			}
		} catch(Exception ex){
			ex.printStackTrace();
		}








	}

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = opponentUtilitySpace.getUtility(bid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return "Frequency Model";
	}
}