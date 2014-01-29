
import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Action;
import negotiator.actions.Offer;

/**
 * The Decider class is used to decide each turn which action the agent should perform.
 * 
 * @author Alex Dirkzwager, Mark Hendrikx, Julian de Ruiter
 */
public class Decider {
	
	// storageobject which contains all possible bids, bids already used, and opponent bids.
	private BidsCollection bidsCollection;
	// reference to the timemanager (manages time related function)
	private TimeManager timeManager;
	// reference to the bidsgenerator (generates a (counter)offer)
	private BidGenerator bidGenerator;
	// reference to the acceptor (generates an action to accept)
	private Acceptor acceptor;
	// reference to the negotiation agent
	private Agent agent;
	
	/**
	 * Creates a Decider-object which determines which offers should be made
	 * during the negotiation.
	 * 
	 * @param agent in negotiation
	 */
	public Decider(Agent agent) {
		this.agent = agent;
		bidsCollection = new BidsCollection();
		bidGenerator = new BidGenerator(agent, bidsCollection);
		acceptor = new Acceptor(agent.utilitySpace, bidsCollection);
		timeManager = new TimeManager(agent.timeline, agent.utilitySpace.getDiscountFactor(), bidsCollection);
	}
	
	/**
	 * Stores the bids of the partner in the history with the corresponding
	 * utility.
	 * 
	 * @param action action made by partner
	 */
	public void setPartnerMove(Action action) {
		if(action instanceof Offer) {
			Bid bid = ((Offer)action).getBid();
			try {
				bidsCollection.addPartnerBid(bid, agent.utilitySpace.getUtility(bid));
			} catch (Exception e) {
				
			}
		}
	}

	/**
	 * Method which returns the action to be performed by the agent.
	 */
	public Action makeDecision() {

		int phase = timeManager.getPhase(timeManager.getTime());
		int movesLeft = 0;
		
		if (phase == 3) {
			movesLeft = timeManager.getMovesLeft();
		}
		// if the negotiation is still going on and a bid has already been made
		Action action = acceptor.determineAccept(phase, timeManager.getThreshold(timeManager.getTime()), agent.timeline.getTime(), movesLeft);
		
		// if we didn't accept, generate an offer (end of negotiation is never played
		if (action == null) {
			action = bidGenerator.determineOffer(agent.getAgentID(), phase, timeManager.getThreshold(timeManager.getTime()));
		}
		return action;
	}
}