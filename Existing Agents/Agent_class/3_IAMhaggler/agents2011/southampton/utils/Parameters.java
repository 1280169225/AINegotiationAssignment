package agents2011.southampton.utils;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Parameters implements Serializable {

	private static final long serialVersionUID = 7515067999039123041L;
	public double a;
	public double b;
	public double maxima;
	public double breakoff;
	public double beta;
	public int ourTime;
	public int opponentTime;
	public double discounting;
	transient public ArrayList<Pair<Double, Double>> opponentBestHistory;
	public Pair<Double, Double> lastOpponentBest;
	public Exception exception;
	public double utility0;
	public double timeScaledDiscounting;
	public double time;

	private void writeObject(ObjectOutputStream out) throws IOException {
		if (opponentBestHistory == null || opponentBestHistory.size() == 0)
			lastOpponentBest = null;
		else
			lastOpponentBest = opponentBestHistory.get(opponentBestHistory.size() - 1);
		out.defaultWriteObject();
	}
}
