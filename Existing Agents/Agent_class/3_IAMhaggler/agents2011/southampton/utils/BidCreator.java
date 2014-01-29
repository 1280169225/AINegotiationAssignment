package agents2011.southampton.utils;

import negotiator.Bid;
import negotiator.utility.UtilitySpace;

public interface BidCreator {

	/**
	 * Get a bid.
	 * 
	 * @param utilitySpace
	 *            The utility space to generate the bid from.
	 * @return a bid.
	 */
	//public Bid getBid(UtilitySpace utilitySpace);

	/**
	 * Get a bid (above a minimum utility value if possible).
	 * 
	 * @param utilitySpace
	 *            The utility space to generate the bid from.
	 * @param min
	 *            The minimum utility value.
	 * @return a bid (above a minimum utility value if possible).
	 */
	//public Bid getBid(UtilitySpace utilitySpace, double min);

	/**
	 * Get a bid (within a utility range if possible).
	 * 
	 * @param utilitySpace
	 *            The utility space to generate the bid from.
	 * @param min
	 *            The minimum utility value.
	 * @param max
	 *            The maximum utility value.
	 * @return a bid (within a utility range if possible).
	 */
	public Bid getBid(UtilitySpace utilitySpace, double min, double max);
}
