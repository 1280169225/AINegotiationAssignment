
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.Domain;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Objective;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.UtilitySpace;


/**
 * This class contains main agent methods and algorithms that agent uses
 * in a negotiation session based on Alternating Offers protocol.
 * 
 * @author Siamak Hajizadeh, Thijs van Krimpen, Daphne Looije 
 * 
 */
public class KLH extends Agent
{
	private BidHistory bidHistory;
	private BidSelector BSelector;
	private double MINIMUM_BID_UTILITY = 0.585D;
	private final int TOP_SELECTED_BIDS = 4;
	private final double LEARNING_COEF = 0.2D;
	private final int LEARNING_VALUE_ADDITION = 1;
	private final double UTILITY_TOLORANCE = 0.01D;
	private double Ka=0.05;
	private double e=0.05;
	private double discountF = 1D;
	private double lowestYetUtility = 1D;


	private LinkedList<Entry<Double, Bid>> offerQueue =  null;
	private Bid opponentLastBid = null;
	private boolean firstRound = true;
	
	private Domain domain = null;
	private UtilitySpace oppUtility = null; 
	private int numberOfIssues = 0;

	private double maxUtil=1;
	private double minUtil=MINIMUM_BID_UTILITY;
	
	private Bid opponentbestbid=null;
	private Entry<Double,Bid> opponentbestentry;
	
	/**
	 * handles some initializations. it is called when agent object 
	 * is created to start a negotiation session
	 * 
	 */
	public void init()
	{
		BSelector = new BidSelector(utilitySpace);
		bidHistory = new BidHistory(utilitySpace);
		oppUtility = new UtilitySpace(this.utilitySpace);
		offerQueue  = new LinkedList<Entry<Double, Bid>>();
		domain = utilitySpace.getDomain();
		numberOfIssues = domain.getIssues().size();
		
		if(utilitySpace.getDiscountFactor() <= 1D && utilitySpace.getDiscountFactor() > 0D )
			discountF = utilitySpace.getDiscountFactor();

		Entry<Double, Bid> highestBid = BSelector.BidList.lastEntry();
		
		try
		{
			maxUtil = utilitySpace.getUtility(highestBid.getValue());
		} catch (Exception e)
		{	
			e.printStackTrace();
		}

//		double highestUtil = highestBid.getKey();
//		double secondUtil = highestUtil;
		
		// retrieves the 5th highest utility, 
		// then checked whether this can still be reached with the current Ka value
//		for(int a=0;a<5;a++)
//		{
//			secondUtil = BSelector.BidList.lowerEntry(secondUtil).getKey();
//		}
//		if(secondUtil < maxUtil-Ka*(maxUtil-minUtil))
//		{
//			Ka = (maxUtil-secondUtil)/(maxUtil-minUtil);
//		}
		
		// get the number of issues and set a weight for each equal to 1/number_of_issues
		// the initialization of opponent's preference profile
		double w = 1D/(double)numberOfIssues;    
		for(Entry<Objective, Evaluator> e: oppUtility.getEvaluators()){
			oppUtility.unlock(e.getKey());
			e.getValue().setWeight(w);
			try{
				// set the initial weight for each value of each issue to 1.
				for(ValueDiscrete vd : ((IssueDiscrete)e.getKey()).getValues())
					((EvaluatorDiscrete)e.getValue()).setEvaluation(vd,1);  
			} catch(Exception ex){
				ex.printStackTrace();
			}
		}		
		if(utilitySpace.getReservationValue() != null)
			MINIMUM_BID_UTILITY = utilitySpace.getReservationValue();
	}

	public static String getVersion() { return "1.2"; }

	/**
	 * Receives opponent's action
	 * 
	 */
	public void ReceiveMessage(Action pAction) {
		double opbestvalue;
		if(pAction instanceof Offer) {
			opponentLastBid = ((Offer) pAction).getBid();
			bidHistory.addOpponentBid(opponentLastBid);
			updateLearner();
			try{
				if(opponentbestbid==null)
					opponentbestbid=opponentLastBid;
				else if( utilitySpace.getUtility(opponentLastBid)>utilitySpace.getUtility(opponentbestbid)){
					opponentbestbid=opponentLastBid;
				}
					
				opbestvalue=BSelector.BidList.floorEntry(utilitySpace.getUtility(opponentbestbid)).getKey();
				
				while (!BSelector.BidList.floorEntry(opbestvalue).getValue().equals(opponentbestbid)){
					opbestvalue=BSelector.BidList.lowerEntry(opbestvalue).getKey();
				}
				opponentbestentry=BSelector.BidList.floorEntry(opbestvalue);	
			}
			catch(Exception ex){
					System.out.println("KutZooi hier ging iets mis");					
			}
		}
		
		
	}
	
	/**
	 * Contains an object of type {@link UtilitySpace} that is updated over time and
	 * as bids are received, to match the preference profile of the opponent.
	 * 
	 */

	private void updateLearner(){
		
		if(bidHistory.getOpponentBidCount() < 2)
			return;
		
		int numberOfUnchanged = 0;
		HashMap<Integer, Integer> lastDiffSet = bidHistory.BidDifferenceofOpponentsLastTwo();
		
		// counting the number of unchanged issues 
		for(Integer i: lastDiffSet.keySet()){
			if(lastDiffSet.get(i) == 0)
				numberOfUnchanged ++;
		}
		
		// This is the value to be added to weights of unchanged issues before normalization. 
		// Also the value that is taken as the minimum possible weight, (therefore defining the maximum possible also). 
		double goldenValue = LEARNING_COEF / (double)numberOfIssues;
		// The total sum of weights before normalization.
		double totalSum = 1D + goldenValue*(double)numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - ((double)numberOfIssues)*goldenValue/totalSum; 
		
		// re-weighing issues while making sure that the sum remains 1 
		for(Integer i: lastDiffSet.keySet()){
			if(lastDiffSet.get(i) == 0 && oppUtility.getWeight(i)< maximumWeight)
				oppUtility.setWeight(domain.getObjective(i), (oppUtility.getWeight(i) + goldenValue)/totalSum);
			else
				oppUtility.setWeight(domain.getObjective(i), oppUtility.getWeight(i)/totalSum);
		}
		
		// Then for each issue value that has been offered last time, a constant value is added to its corresponding ValueDiscrete.  
		try{
			for(Entry<Objective, Evaluator> e: oppUtility.getEvaluators()){
				
				( (EvaluatorDiscrete)e.getValue() ).setEvaluation(opponentLastBid.getValue(((IssueDiscrete)e.getKey()).getNumber()), 
					( LEARNING_VALUE_ADDITION + 
						((EvaluatorDiscrete)e.getValue()).getEvaluationNotNormalized( 
							( (ValueDiscrete)opponentLastBid.getValue(((IssueDiscrete)e.getKey()).getNumber()) ) 
						)
					)
				);
			}
		} catch(Exception ex){
			ex.printStackTrace();
		}
	}

	/**
	 * This function calculates the concession amount based on remaining time, initial parameters, 
	 * and, the discount factor.
	 * 
	 * @return double: concession step
	 */
	public double get_p (){
		
		double time = timeline.getTime(); 
		double Fa;
		double p = 1D;
		double step_point = discountF;
		double tempMax = maxUtil;
		double tempMin = minUtil;
		double tempE = e;
		double ignoreDiscountThreshold = 0.9D;
		
		if(step_point >= ignoreDiscountThreshold){
			Fa = Ka + (1 - Ka ) * Math.pow(time/step_point, 1D/e);
			p = minUtil + (1-Fa) * (maxUtil-minUtil);
		}
		else if(time <= step_point){
			tempE = e / step_point;
			Fa = Ka + (1 - Ka ) * Math.pow(time/step_point, 1D/tempE);
			tempMin += Math.abs(tempMax - tempMin)*step_point;
			p = tempMin + (1-Fa) * (tempMax-tempMin);
		}
		else{
			//Ka = (maxUtil - (tempMax - tempMin*step_point))/(maxUtil-minUtil);
			tempE = 30D;
			Fa = ( Ka + (1-Ka) * Math.pow((time-step_point)/(1-step_point), 1D/tempE));
			tempMax = tempMin + Math.abs(tempMax - tempMin)*step_point;
			p = tempMin + (1-Fa) * (tempMax-tempMin);
		}
		//System.out.println("P value: " + p);
		return p;
	}
	
	
	/**
	 * This is the main strategy of that determines the behavior of the agent. 
	 * It uses a concession function that in accord with remaining time decides which bids should be offered. 
	 * Also using the learned opponent utility, it tries to offer more acceptable bids.
	 * 
	 * @return {@link Action} that contains agents decision
	 */
	public Action chooseAction()
	{
		Entry<Double, Bid> newBid = null;
		Action newAction = null;

		double p = get_p();

		try
		{
			if (firstRound)
			{
				firstRound = !firstRound;
				newBid = BSelector.BidList.lastEntry();
				offerQueue.add(newBid);
			} 
			
			// if the offers queue has yet bids to be offered, skip this.
			// otherwise select some new bids to be offered
			else if (offerQueue.isEmpty() || offerQueue == null)
			{
				// calculations of concession step according to time

				TreeMap<Double, Bid> newBids = new TreeMap<Double,Bid>();
				newBid = BSelector.BidList.lowerEntry(bidHistory.getMyLastBid().getKey());
				newBids.put(newBid.getKey(),newBid.getValue());
				
				if (newBid.getKey()<p)
				{
					int indexer=bidHistory.getMyBidCount();
					indexer=indexer*(int)Math.floor(Math.random());
					newBids.remove(newBid.getKey());
					newBids.put(bidHistory.getMyBid(indexer).getKey(),bidHistory.getMyBid(indexer).getValue());
				}

				double firstUtil = newBid.getKey();

				Entry<Double, Bid> addBid = BSelector.BidList.lowerEntry(firstUtil);
				double addUtil = addBid.getKey();
				int count = 0;
				
				while((firstUtil-addUtil) < UTILITY_TOLORANCE && addUtil >= p)
				{
					newBids.put(addUtil,addBid.getValue());
					addBid = BSelector.BidList.lowerEntry(addUtil);
					addUtil = addBid.getKey();
					count=count+1;
				}
				if(newBids == null || newBids.isEmpty()){
					//System.out.println("NEW LIST IS EMPTY OR NULL.");
				}
				
				// adding selected bids to offering queue
				if(newBids.size() <= TOP_SELECTED_BIDS){
					offerQueue.addAll(newBids.entrySet());
				}
				else{
					int addedSofar = 0;
					Entry<Double, Bid> bestBid = null;

					while (addedSofar <= TOP_SELECTED_BIDS)
					{
						bestBid = newBids.lastEntry();
						// selecting the one bid with the most utility for the opponent.
						for(Entry<Double, Bid> e : newBids.entrySet()){
							if(oppUtility.getUtility(e.getValue()) > oppUtility.getUtility(bestBid.getValue())){
									bestBid = e;
							}
						}
						offerQueue.add(bestBid);
						newBids.remove(bestBid.getKey());
						addedSofar ++;
					}					
				}
				//if opponentbest entry is better for us then the offer que then replace the top entry			
				if(offerQueue.getFirst().getKey()<opponentbestentry.getKey()){
					offerQueue.addFirst(opponentbestentry);
				}
			}

			// if no bids are selected there must be a problem
			if (offerQueue.isEmpty() || offerQueue==null)
			{	
				//System.out.println("OFFER QUEUE IS EMPTY OR NULL.");
				//System.out.println("Damn, no bid generated");
				Bid bestBid1 = domain.getRandomBid();
				if(opponentLastBid != null &&  utilitySpace.getUtility(bestBid1) <= utilitySpace.getUtility(opponentLastBid))
					{//System.out.println("accept");
					newAction = new Accept(getAgentID());
					}
				else if(bestBid1 == null)
					{newAction = new Accept(getAgentID());
					//System.out.println("null, accepted");
					}
				else
					{
					//System.out.println("Offer");
					newAction = new Offer(getAgentID(), bestBid1);
					if(utilitySpace.getUtility(bestBid1) < lowestYetUtility)
						lowestYetUtility = utilitySpace.getUtility(bestBid1); 
					}
			}
			
			// if opponent's suggested bid is better than the one we just selected, then accept it
			if(opponentLastBid != null && 
					( utilitySpace.getUtility(opponentLastBid) > lowestYetUtility ||
						utilitySpace.getUtility(offerQueue.getFirst().getValue()) <= utilitySpace.getUtility(opponentLastBid)) )
			{
				//System.out.println("LYU: " + lowestYetUtility);
				//System.out.println("No better bid");
				newAction = new Accept(getAgentID());
			}
			// else offer a new bid
			else
			{
				//System.out.println("Better bid");
				if(offerQueue == null || offerQueue.isEmpty()){
					//System.out.println("OFFER QUEUE IS EMPTY OR NULL (2). IT SHOULD NOT.");
				}
				Entry<Double, Bid> offer = offerQueue.remove();
				bidHistory.addMyBid(offer);
				if(offer.getKey() < lowestYetUtility)
					lowestYetUtility = offer.getKey(); 
				newAction = new Offer(getAgentID(), offer.getValue());
				//System.out.println("sent");
			}
		}
		catch (Exception e)
		{	
			System.out.println("");
			System.out.println("Error: " + e);
		}
		
		return newAction;
	}
}
