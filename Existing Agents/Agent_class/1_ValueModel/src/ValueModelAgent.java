
import java.util.HashMap;
import java.util.Map;


import negotiator.Agent;
import negotiator.Bid;
import negotiator.BidIterator;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.ISSUETYPE;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueReal;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.issue.ValueReal;
import negotiator.utility.Evaluator;
import negotiator.utility.UtilitySpace;

//This agent uses a very complex form of temporal difference reinforcement
//learning to learn opponent's utility space.
//The learning is focused on finding the amount of utility lost by
//opponent for each value.
//However, as the bid (expected) utilities represent the decrease in all
//issues, we needed away to decide which values should change the most.
//We use estimations of standard deviation and reliability to decide how
//to make the split.
//The reliability is also used to decide the learning factor of the individual
//learning.
//The agent then tries to slow down the compromise rate, until we have to be fair.

//This is the agent of Asaf, Dror and Gal.
public class ValueModelAgent extends Agent{
	private ValueModeler opponentUtilModel=null;
	private BidList allBids=null;
	private BidList approvedBids=null;
	private BidList iteratedBids=null;
	private Action actionOfPartner=null;
	private Action myLastAction=null;
	private Bid myLastBid = null;
	private int bidCount;
	public BidList opponentBids;
	public BidList ourBids;
	private OpponentModeler opponent;
	private double lowestAcceptable;
	private double lowestApproved;
	public double opponentStartbidUtil;
	public double opponentMaxBidUtil;
	private Bid opponentMaxBid;
	public double myMaximumUtility;
	private int amountOfApproved;
	public double noChangeCounter;
	private boolean retreatMode;
	private double lowestApprovedOriginal;
	private double concessionInOurUtility;
	private double concessionInTheirUtility;
	private double timeOfLastConcession;
	private ValueSeperatedBids seperatedBids;
	private int iter;

	private Action lAction = null;
	
	private double opponentUtil;
	public void init()
	{	
		opponentUtilModel=null;
		allBids=null;
		approvedBids=null;
		actionOfPartner=null;
		bidCount=0;
		opponentBids=new BidList();
		ourBids=new BidList();
		iteratedBids=new BidList();
		seperatedBids = new ValueSeperatedBids();
		lowestAcceptable=0.7;
		lowestApproved = 1;
		amountOfApproved = 0;
		opponentMaxBidUtil = 0;
		myMaximumUtility=1;
		noChangeCounter=0;
		retreatMode=false;
		lowestApprovedOriginal=0;
		concessionInOurUtility=0;
		concessionInTheirUtility=0;
		iter=0;
	}
	
	public void ReceiveMessage(Action opponentAction) 
	{
		actionOfPartner = opponentAction;
	}
	//remember our new bid
	private void bidSelected(BidWrapper bid){
		bid.sentByUs=true;
		ourBids.addIfNew(bid);
		myLastBid = bid.bid;
		if(opponentUtilModel!=null) seperatedBids.bidden(bid.bid, bidCount);
		//System.out.printf("new bid sent number %d, assumed %f\n", bidCount*2,bid.theirUtility);
	}
	//remember our new bid in all needed data structures
	private void bidSelectedByOpponent(Bid bid){
		BidWrapper opponentBid = new BidWrapper(bid,utilitySpace,myMaximumUtility);
		opponentBid.lastSentBid = bidCount;
		opponentBid.sentByThem=true;
		if(opponentBids.addIfNew(opponentBid)){
			noChangeCounter=0;
			//System.out.printf("completely new: number %d\n", opponentBids.bids.size());
		}
		else{
			noChangeCounter++;
			//System.out.printf("already sent %d\n", opponentBid.lastSentBid);
		}
		try{
			double opponentUtil = utilitySpace.getUtility(bid)/myMaximumUtility;
			if(opponentMaxBidUtil<opponentUtil){
				opponentMaxBidUtil = opponentUtil;
				opponentMaxBid = bid;
			}
			if(opponentUtilModel.initialized){
				double concession = opponentUtil - opponentStartbidUtil;
				if(concession>concessionInOurUtility) concessionInOurUtility=concession;
				//System.out.printf("concession1 %f,%f - %f\n", concession,opponentUtil,opponentStartbidUtil);
				//assumed utility he lost (should be lower
				//if our opponent is smart, but lets be honest
				//it is quite possible we missevaluated so lets
				//use our opponent utility instead
				ValueDecrease val = opponentUtilModel.utilityLoss(opponentBid.bid);
				concession = val.getDecrease();
				if(concession>concessionInTheirUtility) concessionInTheirUtility=concession;
				seperatedBids.bidden(bid, bidCount);
				//System.out.printf("concession2 %f,%f\n", concession,concessionInTheirUtility);
				
				
			}
		}
		catch(Exception ex){
			System.out.printf("error in bidSelectedByOpponent\n");
		}
	}
	
	private double timelineConcessionShield(){
		//0.38
		//double changeLeft = 0.98-lowestAcceptable;
		double curTime = timeline.getTime();
		double shield = 0.02;
		double dis=utilitySpace.getDiscountFactor();
	
		shield+=0.08*curTime;
		if(curTime>0.7){
			shield+=(curTime-0.7);
		}
		//if discount
		if(dis>0.05){
			double maxDiscountLoss=dis*(1-curTime)/2;
			if(maxDiscountLoss>shield) shield=maxDiscountLoss;
		}
		return shield;
	}
	
	private double calcApprovedLimit(){

		double minConcession = timelineConcessionShield();
		if(concessionInOurUtility<minConcession) minConcession=concessionInOurUtility;
		if(concessionInTheirUtility<minConcession) minConcession=concessionInTheirUtility;

		//we want to conceed less
		minConcession *= 3/4;
		if(lowestAcceptable>(1-minConcession)){
			return lowestAcceptable; 
		}
		else{
			//System.out.printf("we think he conceded %f %f %f %f\n",1-minConcession,concessionInOurUtility,concessionInTheirUtility,timelineConcessionShield());
			return 1-minConcession;
		}
		
	}
	private boolean setApprovedThreshold(double threshold,boolean clear){
		if(clear){
			approvedBids.bids.clear();
			seperatedBids.clear();
		}
		int i;
		if(clear) i=0;
		else i=amountOfApproved;
		for(;i<allBids.bids.size();i++){
			if(allBids.bids.get(i).ourUtility<threshold){
				break;
			}
			approvedBids.bids.add(allBids.bids.get(i));
			seperatedBids.addApproved(allBids.bids.get(i));
		}
		//System.out.printf("new threshold %f number bids %d %d %f\n",threshold,i,approvedBids.bids.size(),timeline.getTime());
		lowestApproved = threshold;
		boolean added = amountOfApproved!=i;
		amountOfApproved=i;
		return added;
	}
	
	private boolean lowerThreshold(){
		double newLow = allBids.bids.get(amountOfApproved).ourUtility;
		if(newLow>lowestApproved-0.005) newLow=lowestApproved-0.005;
		if(newLow>=calcApprovedLimit()){
			setApprovedThreshold(newLow,true);
			return true;
		}
		else return false;
	}
	private BidWrapper getNewBidForIteration() {
		BidWrapper prposedBid=null;
		lowerThreshold();
		approvedBids.sortByOpponentUtil(opponentUtilModel);
		//find the "best" bid for opponent
		//and choose it if we didn't send it to opponent
		for(int i=0;i<approvedBids.bids.size();i++){
			BidWrapper tempBid =  approvedBids.bids.get(i);
			if(!tempBid.sentByUs){
				return tempBid;
			}
		}
		int maxIndex =  approvedBids.bids.size()/2;
		int index = (int) (Math.random()*maxIndex);
		return approvedBids.bids.get(index);
	}
	
	public Action chooseAction()
	{
		Bid opponentBid = null;
		lAction=null;
		//System.out.printf("bid %d\n", bidCount);
		try	{
			//our first bid, initializing
			if(allBids == null){
				allBids = new BidList();
				approvedBids = new BidList();
				opponentUtilModel = new ValueModeler();
				seperatedBids.init(utilitySpace, opponentUtilModel);
				myMaximumUtility = utilitySpace.getUtility(utilitySpace.getMaxUtilityBid());
				BidIterator iter = new BidIterator(utilitySpace.getDomain());
				while(iter.hasNext()) {
					Bid tmpBid = iter.next();
					try{
						//if(utilitySpace.getCost(tmpBid)<=1200){
							BidWrapper wrap = new BidWrapper(tmpBid,utilitySpace,myMaximumUtility);
							allBids.bids.add(wrap);
						//}
					}
					catch(Exception ex){

						System.out.printf("got error allBids.bids.add(wrap)!");
						BidWrapper wrap = new BidWrapper(tmpBid,utilitySpace,myMaximumUtility);
						allBids.bids.add(wrap);
					}
				} //while
				allBids.sortByOurUtil();
				//System.out.printf("number of bids %d\n", allBids.bids.size());
				
				//amountOfApproved = (int) (0.05*allBids.bids.size());
				//if(amountOfApproved<2 && allBids.bids.size()>=2) amountOfApproved=2;
				//if(amountOfApproved>20) amountOfApproved=20;
				//System.out.print("got here\n");
				//TODO
				setApprovedThreshold(0.98,false);
				
				//if(opponentUtilModel!=null)
				//	approvedBids.sortByOpponentUtil(opponentUtilModel);
				iteratedBids.bids.add(allBids.bids.get(0));
			}
			//first bid is the highest bid
			if(bidCount == 0){
				
				lAction = new Offer(getAgentID(), allBids.bids.get(0).bid);
				bidSelected(allBids.bids.get(0));
			}
			
			//treat opponent's offer
			if (actionOfPartner instanceof Offer){
				opponentBid = ((Offer) actionOfPartner).getBid();
				opponentUtil = utilitySpace.getUtility(opponentBid)/myMaximumUtility;
				bidSelectedByOpponent(opponentBid);
				//System.out.printf("opponent bid's utility %f\n", opponentUtil);
				if(opponent==null){
					//lowestAcceptable = (1+utilitySpace.getUtility(opponentBid))/2;
					opponentStartbidUtil = opponentUtil;
					opponent = new OpponentModeler(bidCount,utilitySpace,timeline,ourBids,opponentBids,opponentUtilModel,allBids,this);
					opponentUtilModel.initialize(utilitySpace, opponentBid);
					approvedBids.sortByOpponentUtil(opponentUtilModel);
					//opponentBids = new BidList();
					//opponentBids.bids.add(new BidWrapper(opponentBid,utilitySpace));
					
				}
				else{
					opponent.tick();
					if(noChangeCounter==0){
						double opponentExpectedBidValue = opponent.guessCurrentBidUtil();
						opponentUtilModel.assumeBidWorth(opponentBid, 1-opponentExpectedBidValue, 0.02);
					}
				}
				//it seems like I should try to accept
				//his best bid (assuming its still on the table)
				//if its a good enough bid to be reasonable
				//(we don't accept 0.3,0.92 around here...)  
				//currently by reasonable we mean that we will
				//get most of what would be a "fair" distribution
				//of the utility
				
				
				
				
				if(timeline.getTime()>0.9 && timeline.getTime()<=0.96){
					chickenGame(0.039, 0, 0.7);
				}
				if(timeline.getTime()>0.96 && timeline.getTime()<=0.97){
					chickenGame(0.019, 0.5, 0.65);
					
				}
				if(timeline.getTime()>0.98 && timeline.getTime()<=0.99){

					if(opponentUtil>=lowestApproved-0.01){
						return new Accept(getAgentID());
					}
					if(bidCount%5 ==0){
						exploreScan();
					}
					else bestScan();
				}
				if(timeline.getTime()>0.99 && timeline.getTime()<=0.995){
					chickenGame(0.004, 0.8, 0.6);
					
				}
				//if(timeline.getTime()>0.99 && timeline.getTime()<=0.995){
				//	chickenGame(0.004, 0.5, 0.55);
					
				//}
				if(timeline.getTime()>0.995){
					
					if(opponentMaxBidUtil>0.55){
						
						//they might have a bug and not accept, so
						//if their offer is close enough accept
						if(opponentUtil>= opponentMaxBidUtil*0.99){
							return new Accept(getAgentID());
						}
						//System.out.print("0.99 give up\n");
						else return new Offer(getAgentID(), opponentMaxBid);
					}
					bestScan();
					//this will probably not work but what can we
					//do? he dosn't even give us 50%!
				}
					
				if(lAction!=null){
					myLastAction = lAction;
					bidCount++;
					return lAction;
				}
				//if our opponent settled enough for us we accept, and there is
				//a discount factor we accept
				//if(opponent.expectedDiscountRatioToConvergence()*opponentUtil > lowestApproved){
				if((opponentUtil> lowestApproved )//|| opponentUtil>0.98)
					&& (utilitySpace.getDiscountFactor()>0.02|| opponentUtil>0.975)){
						//){
					return new  Accept(getAgentID());
				}
				
				//otherwise we try to stretch it out
				if(opponentUtil> lowestApproved*0.99 && utilitySpace.getDiscountFactor()<0.02){
					if(!retreatMode)lowestApprovedOriginal=lowestApproved; 
					lowestApproved+=0.01;
					setApprovedThreshold(lowestApproved,true);
					retreatMode=true;
				}
				
				
				
				
				{
					//if there is discount we want the other player to get
					//input from us even if he explores without us
					/*
					int maxExtraBidDelay = 50;
					if(utilitySpace.getDiscountFactor()>0.05)
						maxExtraBidDelay=10;
					if(utilitySpace.getDiscountFactor()>0.2)
						maxExtraBidDelay=3;
					if(utilitySpace.getDiscountFactor()>0.5)
						maxExtraBidDelay = 1;
					
					if(bidCount>0){
						BidWrapper newestBid;
						if((noChangeCounter%2==0 && noChangeCounter!=0)||(bidCount%maxExtraBidDelay==0)){
							newestBid = getNewBidForIteration();
							iteratedBids.addIfNew(newestBid);
							
						}
						else{
							newestBid = allBids.bids.get(iter);
							iter++;
							if(iter==iteratedBids.bids.size()){
								//iteratedBids.sortByOpponentUtil(opponentUtilModel);
								iter=0;
							}
						}
						if(newestBid.ourUtility<opponentMaxBidUtil) {
							lAction = new Offer(getAgentID(), opponentMaxBid);
						}
						bidSelected(allBids.bids.get(bidCount));
					}
					*/
					if(bidCount>0 && bidCount<4){
						lAction = new Offer(getAgentID(), allBids.bids.get(0).bid);
						bidSelected(allBids.bids.get(bidCount));
					}
					if(bidCount>=4){
						//System.out.printf("got here\n");
						//utility he gave us 
						double concession = opponentUtil - opponentStartbidUtil;
						//assumed utility he lost (should be lower
						//if our opponent is smart, but lets be honest
						//it is quite possible we missevaluated so lets
						//use our opponent utility instead
						//double concession2 = opponentUtilModel.utilityLoss(opponentBid).getDecrease();
						double concession2 = 1-opponent.guessCurrentBidUtil();
						//System.out.printf("concession in our scale %f approximated theirs %f\n",concession,concession2);
						double minConcession = concession<concession2?concession:concession2;
						minConcession = minConcessionMaker(minConcession,1-opponentMaxBidUtil);
						if(minConcession>(1-lowestApproved)){
							if(lowestAcceptable>(1-minConcession)){
								lowestApproved=lowestAcceptable; 
							}
							else{
								lowestApproved=1-minConcession;
							}
							if(lowestApproved<opponentMaxBidUtil) lowestApproved=opponentMaxBidUtil+0.001;
							//System.out.printf("new lowest approved %f, because concession %f,concession2 %f,lowestAcceptable %f,ourUtil %f,start %f,assumption %f\n", lowestApproved,concession,concession2,lowestAcceptable,opponentUtil,opponentStartbidUtil,opponentUtilModel.utilityLoss(opponentBid).getDecrease());
							//System.out.printf("new lowest approved %f, because concession %f,concession2 %f,lowestAcceptable %f,ourUtil %f,start %f,\n", lowestApproved,concession,concession2,lowestAcceptable,opponentUtil,opponentStartbidUtil);
							if(setApprovedThreshold(lowestApproved,false)){
								approvedBids.sortByOpponentUtil(opponentUtilModel);
							}
							
							//int i;
							//for(i=amountOfApproved;i<allBids.bids.size() ;i++){
							//	if(allBids.bids.get(i).ourUtility<lowestApproved)
							//		break;
							//	approvedBids.bids.add(allBids.bids.get(i));
							//}
							//amountOfApproved=i-1;
							//System.out.printf("new lowest %f\n", lowestApproved);
							
							
						}
						if(bidCount%5 ==0){
							exploreScan();
						}
						else bestScan();
					}
					
					
				}
			}
			if(lAction == null){
				lAction = myLastAction;
			}
		}
		catch (Exception e)
		{ 
			System.out.printf("exception at main %s",e.getMessage());
			if(myLastBid==null){
				try{
					return new Offer(getAgentID(),utilitySpace.getMaxUtilityBid());
				}
				catch(Exception e2){
					return new Accept(getAgentID());
				}
			}
			lAction = new Offer(getAgentID(), myLastBid);
		}
		myLastAction = lAction;
		bidCount++;
		return lAction;
	}
	
	public void bestScan(){
		approvedBids.sortByOpponentUtil(opponentUtilModel);

		//find the "best" bid for opponent
		//and choose it if we didn't send it to opponent
		for(int i=0;i<approvedBids.bids.size();i++){
			BidWrapper tempBid =  approvedBids.bids.get(i);
			if(!tempBid.sentByUs){
				lAction = new Offer(getAgentID(), tempBid.bid);
				bidSelected(tempBid);
				break;
			}
		}
		if(lAction==null){
			int maxIndex = approvedBids.bids.size()/4;
			BidWrapper tempBid =  approvedBids.bids.get((int)(Math.random()*maxIndex));
			lAction = new Offer(getAgentID(), tempBid.bid);
			bidSelected(tempBid);
		}
	}
	public void exploreScan(){
		BidWrapper tempBid =  seperatedBids.explore(bidCount);
		if(tempBid!=null){
			lAction = new Offer(getAgentID(), tempBid.bid);
			bidSelected(tempBid);
		}
		else bestScan();
	}

	public void chickenGame(double timeToGive,double concessionPortion,double acceptableThresh){
		//set timeToGive to be 0.005 unless each turn is very
		//large
		//double timeToGive = 0.005>(opponent.delta*4)?0.005:(opponent.delta*4);
		double concessionLeft = lowestApproved-opponentMaxBidUtil;
		double planedThresh = lowestApproved-concessionPortion*concessionLeft;
		if(acceptableThresh>planedThresh) planedThresh=acceptableThresh;
		setApprovedThreshold(planedThresh, false);
		approvedBids.sortByOpponentUtil(opponentUtilModel);

		if(opponentUtil>=planedThresh-0.01){
			lAction=new Accept(getAgentID());
			return;
		}
		if(1.0-timeline.getTime()-timeToGive>0)
			sleep(1.0-timeline.getTime()-timeToGive);
		if(retreatMode || opponentMaxBidUtil>=planedThresh-0.01) {
			//System.out.printf("chicken! sending opponent's best bid, where approved %f\n",lowestApproved);
			lAction=new Offer(getAgentID(), opponentMaxBid);
		}
		//offer him the best bid for him amongst the
		//bids that are above our limit
		else{
			//System.out.printf("chicken! sending the best approved bid %f where max is %f\n",approvedBids.bids.get(0).ourUtility,lowestApproved);
			approvedBids.bids.get(0);
			//return new Offer(getAgentID(), approvedBids.bids.get(0).bid);
		}
	}
	
	double theirMaxUtilities[] = new double[21];
	double ourMinUtilities[] = new double[21];
	private double minConcessionMaker(double minConcession, double concessionLeft) {
		theirMaxUtilities[0]=opponentStartbidUtil;
		ourMinUtilities[0]=1;
		// TODO Auto-generated method stub
		double t = timeline.getTime();
		int tind = (int)(timeline.getTime()*20)+1;
		double segPortion=(t-(tind-1)*0.05)/0.05;
		if(ourMinUtilities[tind]==0)ourMinUtilities[tind]=1; 
		if(ourMinUtilities[tind-1]==0) ourMinUtilities[tind-1]=lowestApproved;
		if(lowestApproved<ourMinUtilities[tind]) 
			ourMinUtilities[tind]=lowestApproved;
		if(opponentMaxBidUtil>theirMaxUtilities[tind])
			theirMaxUtilities[tind]=opponentMaxBidUtil;
		double d=utilitySpace.getDiscountFactor();
		double defaultVal = 1-ourMinUtilities[tind-1];
		if(tind==1 || tind>=19) return defaultVal;
		if(ourMinUtilities[tind-2]==0) ourMinUtilities[tind-2]=lowestApproved;
		//System.out.printf("timeline %f %d %f %f %f\n",t,tind,d,defaultVal,minConcession);
		//if(defaultVal>minConcession) return minConcession;
		boolean theyMoved = theirMaxUtilities[tind]-theirMaxUtilities[tind-2]>0.01;
		boolean weMoved = ourMinUtilities[tind-2]-ourMinUtilities[tind-1]>0;
		double returnVal=defaultVal;
		//System.out.printf("their movement %f ours %f\n",theirMaxUtilities[tind]-theirMaxUtilities[tind-2],ourMinUtilities[tind-2]-ourMinUtilities[tind-1]);

		//System.out.printf("they moved %f in %f diff in our scale %f\n",theirMaxUtilities[tind-1]-theirMaxUtilities[tind-2],t,concessionLeft-defaultVal);
		if(d<0.05){
			//first 10% is reserved for 0.98...
			if(tind>2){
				//if we havn't compromised in the last session
				if(!weMoved){
					//we didn't move, they did
					if(theyMoved){
						//return defaultVal;
						//System.out.printf("they moved %f in %f\n",theirMaxUtilities[tind-1]-theirMaxUtilities[tind-2],t);
					}
					else if(tind<=16){
						returnVal = defaultVal+0.02;
						//give concession only if we think they conceded more!
						if(returnVal>minConcession*2/3) returnVal = minConcession*2/3;
						
					}
				}
			}

			//the negotiation is ending and they are not moving its time for compromize
			if(tind>16 && !theyMoved){
				//Compromise another portion every time...
				returnVal= (concessionLeft-defaultVal)/(21-tind)+defaultVal;
				if(returnVal>minConcession+0.05) returnVal = minConcession+0.05;
				//System.out.printf("game ending %f\n", returnVal);
			}
			//return defaultVal;
			
		}
		if(d>0.05){
			double discountEstimate=d*0.05;
			double expectedRoundGain=theirMaxUtilities[tind-1]-theirMaxUtilities[tind-2]-discountEstimate;
			
			if(tind<=16){
				returnVal = defaultVal+0.02;
				if(defaultVal-expectedRoundGain>returnVal) returnVal=defaultVal-expectedRoundGain;
				if(returnVal>minConcession) returnVal = minConcession;
				
			}
			else{
				//Compromise another portion every time...
				returnVal= (concessionLeft-defaultVal)/(21-tind)+defaultVal;
			}
		}
		//making a concession in steps. its slower but safer
		//System.out.printf("default %f return %f\n",defaultVal,returnVal); 
		returnVal=defaultVal+(returnVal-defaultVal)*segPortion;
		//System.out.printf("portion %f new return %f\n",segPortion,returnVal);
		return returnVal;
	}

	@Override
	public String getName()
	{
		return "value model";
	}
	
}
