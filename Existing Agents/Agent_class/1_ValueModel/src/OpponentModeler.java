

import negotiator.Timeline;
import negotiator.utility.UtilitySpace;

public class OpponentModeler {
	private boolean opponentFirst; 
	UtilitySpace utilitySpace;
	long lastTime;
	public double delta;
	double discount;
	Timeline timeline;
	BidList ourPastBids;
	BidList theirPastBids;
	BidList allBids;
	ValueModeler vmodel;
	ValueModelAgent agent;
	//reasonable to assume that this is the ratio between how much
	//they gave us and how much they lost.
	//i.e. if they gave us 9% than its reasonable to assume they lost at least 3%.
	//this is used for fail safe estimation
	double paretoRatioEstimation=5;
	
	public OpponentModeler(int bidCount,UtilitySpace space,Timeline timeline,BidList our,BidList their,ValueModeler vmodeler,BidList allBids,ValueModelAgent agent){
		ourPastBids = our;
		theirPastBids = their;
		opponentFirst = (bidCount==0);
		utilitySpace = space;
		lastTime = System.currentTimeMillis();
		discount = utilitySpace.getDiscountFactor();
		this.timeline = timeline;
		delta = 1.0/180000;
		vmodel = vmodeler;
		this.allBids = allBids;
		this.agent = agent;
	}
	public void tick(){
		long newTime = System.currentTimeMillis();
		delta = 0.8*delta+(newTime-lastTime)/5;
	}
	private int expectedBidsToConvergence(){
		return 10;
	}
	public int expectedBidsToTimeout(){
		if(delta>0)
			return (int) ((1-timeline.getTime())/delta);
		else 
			return (int) (1-timeline.getTime())*1000;
	}
	public double expectedDiscountRatioToConvergence(){
		double expectedPart = (double)(expectedBidsToConvergence()*delta);
		if(timeline.getTime()+expectedPart>1){
			return 1.1;
		}
		else{
			double div = 1-(discount*expectedPart); 
			if(div>0){
				return 1/div;
			}
			else return 1.1;
		}
	}
	private double paretoExpectredRatio(double ourMaxIncrease){
		if(ourMaxIncrease<0.2){
			return ourMaxIncrease/10;
		}
		else{
			return 0.02+(ourMaxIncrease-0.2)/paretoRatioEstimation;
		}
	}
	public double guessCurrentBidUtil(){
		int s2 = theirPastBids.bids.size();
		if(s2==0){
			return 1;
		}
		double sum = 0;
		double count=0;
		double symetricLowerBound = allBids.bids.get(s2).ourUtility;
		//trying to learn the average of the current bids
		for(int i=s2-2;i>=0 && i>s2-50;i--){
			theirPastBids.bids.get(i).update(vmodel);
			if(theirPastBids.bids.get(i).theirUtilityReliability>0.7){
				sum+=theirPastBids.bids.get(i).theirUtility;
				count++;
			}
		}
		double shield = timeline.getTime()*0.6;
		if(shield<0.03) shield =0.03;
		double minBound = symetricLowerBound;
		if(count>=5 && sum/count<minBound){
			minBound=sum/count;
		}
		if(minBound>(1-shield))
			return minBound;
		//it is very unsafe to assume our opponent conceded more than 15... 
		else return (1-shield);
		//if(s2<10){
		//	return allBids.bids.get(s2).ourUtility;
		//}
		
		
		//double maxProgress = agent.opponentMaxBidUtil-agent.opponentStartbidUtil;
		//double paretoUtilEstimation = 1-paretoExpectredRatio(maxProgress);
		//System.out.printf("pareto %f, ours %f", paretoUtilEstimation,allBids.bids.get(s2).ourUtility);
		//leniently choosing the lower amongst the two
		//if(paretoUtilEstimation>allBids.bids.get(s2).ourUtility)
		//if(symetricLowerBound<0.95) return 0.95;
		//else return symetricLowerBound;
		//else return paretoUtilEstimation;
		/*
		for(int i=s2-5;i<s2;i++){
			theirPastBids.bids.get(i).update(vmodel);
			sum+=theirPastBids.bids.get(i).theirUtility;
			System.out.printf("adding %f to average at %d\n",theirPastBids.bids.get(i).theirUtility,i);
		}
		
		return sum/5;
		*/
		//return 0.95;
	}
}
