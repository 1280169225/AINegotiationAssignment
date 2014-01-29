package agents2011.southampton.utils;

import negotiator.Bid;

public class EBid implements Comparable<EBid> {
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "EBid(" + value + "\t" + bid + ")";
	}

	private Bid bid;
	private double value;

	public EBid(Bid bid, double value) {
		this.bid = bid;
		this.value = value;
	}

	@Override
	public int compareTo(EBid o) {
		if(this.value == o.value)
			return 0;
		return (this.value < o.value) ? -1 : 1;
	}

	public Bid getBid() {
		return bid;
	}

	public double getValue() {
		return value;
	}
}
