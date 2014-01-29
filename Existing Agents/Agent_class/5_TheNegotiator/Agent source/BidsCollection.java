
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import negotiator.Bid;

/**
 * The BidsCollection class stores the bids of the partner and all possible bids.
 * 
 * @author Alex Dirkzwager, Mark Hendrikx, Julian de Ruiter
 */
public class BidsCollection {

	// bids done by partner
	private ArrayList<UTBid> partnerBids;
	// all possible bids (for us) which do not violate the constraints
	private ArrayList<UTBid> possibleBids;

	/**
	 * Creates a BidsCollection-object which stores the partner bids and all possible
	 * bids.
	 */
	public BidsCollection() {
		partnerBids = new ArrayList<UTBid>();
		possibleBids  = new ArrayList<UTBid>();
	}

	/**
	 * @return the partnerBids
	 */
	public ArrayList<UTBid> getPartnerBids() {
		return partnerBids;
	}

	/**
	 * @return the possibleBids
	 */
	public ArrayList<UTBid> getPossibleBids() {
		return possibleBids;
	}

	/**
	 * Add a partner bid to the history. Bids are stored at the front
	 * to preserve the timeline.
	 * 
	 * @param bid made by partner in the current turn
	 * @param utility of the bid
	 */
	public void addPartnerBid(Bid bid, double utility) {
		UTBid utbid = new UTBid(bid, utility);
		partnerBids.add(0, utbid);
	}
	
	/**
	 * Add a possible bid to the list of possible bids. The given bid 
	 * should not violate the constraints of the negotiation.
	 * 
	 * @param bid which is possible
	 * @param utility of the bid
	 */
	public void addPossibleBid(Bid bid, double utility) {
		UTBid utbid = new UTBid(bid, utility);
		possibleBids.add(utbid);
	}
	
	/**
	 * Sorts all possible bids in reverse natural order.
	 */
	public void sortPossibleBids() {
		Collections.sort(possibleBids);
	}
		
	/**
	 * Get a partner bid.
	 * 
	 * @param i 
	 * @return the i'th bid in the timeline
	 */
	public Bid getPartnerBid(int i) {
		Bid bid = null;
		
		if (i < partnerBids.size()) {
			bid = partnerBids.get(i).getBid();
		} else {
			ErrorLogger.log("BIDSCOLLECTION: Out of bounds");
		}
		return bid;
	}

	/**
	 * Get a partner bid which has a utility of at least a certain
	 * value. Null is returned if no such bid exists.
	 * 
	 * @param threshold
	 * @return bid with utility > threshold if exists
	 */
	public Bid getBestPartnerBids(double threshold) {
		ArrayList<UTBid> temp = partnerBids;
		Collections.sort(temp);
		Bid bid = null;

		int count = 0;
		while (count < temp.size() && temp.get(count).getUtility() >= threshold) {
			count++;
		}
		
		if (count > 0) {
			Random randomnr = new Random();
			bid = temp.get(randomnr.nextInt(count)).getBid();
		}
		return bid;
	}

	public Bid getOwnBidBetween(double lowerThres, double upperThres) {
		return getOwnBidBetween(lowerThres, upperThres, 0);
	}
	/**
	 * Get a random bid between two given thresholds.
	 * 
	 * @param lowerThres lowerbound threshold
	 * @param upperThres upperbound threshold
	 * @return random bid between thresholds
	 */
	public Bid getOwnBidBetween(double lowerThres, double upperThres, int counter) {
		int lB = 0;
		int uB = 0;
		Bid bid = null;

		// determine upperbound and lowerbound by visiting all points
		for (int i = 0; i < possibleBids.size(); i++) {
			double util = possibleBids.get(i).getUtility();
			if (util > upperThres) {
				uB++;
			}
			if (util >= lowerThres) {
				lB++;
			}
		}
		// if there are no points between the bounds
		if (lB == uB) {
			if (counter == 1) {
				return possibleBids.get(0).getBid(); // safe fallback value
			}
			// ignore upper threshold
			bid = getOwnBidBetween(lowerThres, 1.1, 1);
		} else {
			// decrement upper- and lowerbound to get the correct index
			// (count counts from 1, while arrays are indexed from 0)
			if (lB > 0) {
				lB--;
			}
			if ((uB + 1) <= lB) {
				uB++;
			}
			// calculate a random bid index
			int result = uB + (int) ( Math.random() * (lB - uB) + 0.5);
			bid = possibleBids.get(result).getBid();
			if (possibleBids.get(result).getUtility() < lowerThres || possibleBids.get(result).getUtility() > upperThres) {
				System.out.println("BIDSCOLLECTION: bugs in getOwnBidBetween");
			}
		}
		return bid;
	}

	/**
	 * Calculate the upperthreshold based on the lowerthreshold and a given percentage.
	 * @param threshold
	 * @param percentage
	 * @return
	 */
	public double getUpperThreshold(double threshold, double percentage) {
		int boundary = 0;
		while (boundary < possibleBids.size() && possibleBids.get(boundary).getUtility() >= threshold) {
			boundary++;
		}
		if (boundary > 0)
			boundary--;
		int index = boundary - (int) Math.ceil(percentage * boundary);
	
		double utility = possibleBids.get(index).getUtility();
		return utility;
	}
}