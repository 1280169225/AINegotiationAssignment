package agents2011.southampton.utils;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;

/**
 * This factory class has been created to allow Actions to be constructed.
 * 
 * @author Colin Williams
 * 
 */
public class ActionCreator {

	/**
	 * Create an offer message.
	 * 
	 * @param agent
	 *            Agent creating the offer message.
	 * @param bid
	 *            The bid made in the offer.
	 * @return an offer message.
	 */
	public static Action createOffer(Agent agent, Bid bid) {
		return new Offer(agent.getAgentID(), bid);
	}

	/**
	 * Create an accept message.
	 * 
	 * @param agent
	 *            Agent creating the accept message.
	 * @return an accept message.
	 */
	public static Action createAccept(Agent agent) {
		return new Accept(agent.getAgentID());
	}

}
