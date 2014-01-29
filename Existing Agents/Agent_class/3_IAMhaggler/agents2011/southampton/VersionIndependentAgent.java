package agents2011.southampton;

import agents2011.southampton.VersionIndependentAgentInterface;
import negotiator.Agent;

public abstract class VersionIndependentAgent extends Agent implements VersionIndependentAgentInterface {
	
	/* (non-Javadoc)
	 * @see agents2011.southampton.VersionIndependentAgentInterface#setOpponentTime(double)
	 */
	@Override
	public void setOpponentTime(long time) {
	}

	/* (non-Javadoc)
	 * @see agents2011.southampton.VersionIndependentAgentInterface#setOurTime(double)
	 */
	@Override
	public void setOurTime(long time) {
	}

	@Override
	public double getTime() {
		return timeline.getTime();
	}

	@Override
	public double adjustDiscountFactor(double discountFactor) {
		return discountFactor;
	}
}
