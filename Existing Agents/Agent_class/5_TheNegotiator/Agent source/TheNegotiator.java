

import negotiator.Agent;
import negotiator.actions.Action;


/**
 * The TheNegotiator class specifies a negotiation agent in the GENIUS domain.
 * 
 * @author Alex Dirkzwager, Mark Hendrikx, Julian de Ruiter
 */
public class TheNegotiator extends Agent {

	// reference to the decider (governs which action should be performed each turn)
	private Decider decider;

	/**
	 * init is called when a next session starts with the same partner.
	 */
	public void init() {
		decider = new Decider(this);
	}

	public static String getVersion() { return "3.0"; }

	/**
	 * Store the actions made by a partner.
	 * 
	 * @param action by partner in current turn
	 */
	public void ReceiveMessage(Action partnerAction) {
		decider.setPartnerMove(partnerAction);
	}

	/**
	 * Choose an action to perform.
	 */
	public Action chooseAction() {
		return decider.makeDecision();
	}
}