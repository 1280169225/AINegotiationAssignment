package agents2011.southampton.utils.concession;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import uk.ac.soton.ecs.gp4j.bmc.BasicPrior;
import uk.ac.soton.ecs.gp4j.bmc.GaussianProcessMixture;
import uk.ac.soton.ecs.gp4j.bmc.GaussianProcessRegressionBMC;
import uk.ac.soton.ecs.gp4j.gp.GaussianProcessPrediction;
import uk.ac.soton.ecs.gp4j.gp.covariancefunctions.CovarianceFunction;
import uk.ac.soton.ecs.gp4j.gp.covariancefunctions.NoiseCovarianceFunction;
import uk.ac.soton.ecs.gp4j.gp.covariancefunctions.SquaredExponentialCovarianceFunction;
import uk.ac.soton.ecs.gp4j.gp.covariancefunctions.SumCovarianceFunction;
import uk.ac.soton.ecs.gp4j.util.MatrixUtils;

import agents2011.southampton.utils.Pair;
import agents2011.southampton.utils.Parameters;

public class ConcessionUtils {

	/**
	 * @param bestOpponentBidUtilityHistory The history of opponent bids.
	 * @param time The current time.
	 * @param discountFactor The discounting factor.
	 * @param utility0 Utility at time 0.
	 * @param minBeta Minimum value for beta.
	 * @param maxBeta Maximum value for beta.
	 * @param defaultBeta Default value for beta.
	 * @param breakoff The breakoff utility.
	 * @return
	 */
	public static Pair<Double, Parameters> getBeta(ArrayList<Pair<Double, Double>> bestOpponentBidUtilityHistory, double time, double discountFactor,
			double utility0, double minBeta, double maxBeta, double defaultBeta, double breakoff) {
		System.out.println("Discounting factor is " + discountFactor);
		Parameters parameters = new Parameters();
		parameters.utility0 = utility0;
		parameters.breakoff = breakoff;
		parameters.time = time;
		parameters.discounting = discountFactor;
		parameters.opponentBestHistory = bestOpponentBidUtilityHistory;
		try {
			// Estimate the values for a and b.
			Pair<Double, Double> params = getRegressionParameters(bestOpponentBidUtilityHistory, utility0);
			parameters.a = params.fst;
			parameters.b = params.snd;
			// Find the maxima.
			Pair<Double, Double> p = getMaxima(parameters.a, parameters.b, time, parameters.discounting, parameters.utility0);
			parameters.maxima = p.fst;
			parameters.timeScaledDiscounting = p.snd;
			// Find the current utility of our current concession strategy.
			double util = parameters.utility0 + (Math.exp(parameters.a) * Math.pow(parameters.maxima, parameters.b));
			util = Math.max(0, Math.min(1, util));
			// Calculate the beta value.
			// double beta = Math.max(0, 2.0 * Math.log(maxima) / Math.log((1 -
			// util) /* 0.5*/));
			if (util <= 0.5)
				parameters.beta = Double.MAX_VALUE;
			else
				parameters.beta = Math.max(0, Math.log(parameters.maxima) / Math.log((1 - util) / (1 - breakoff) /* 0.5 */));

			System.out.println("t=" + time + ", a=" + parameters.a + ", b=" + parameters.b + ", m=" + parameters.maxima + ", u=" + util + ", B="
					+ parameters.beta);

			/*
			 * if (time < 0.5) { double weightBeta = time / 0.5; //beta = (beta
			 * * weightBeta) + (defaultBeta * (1 - weightBeta)); beta =
			 * Math.exp(Math.log(beta) * weightBeta) +
			 * Math.exp(Math.log(defaultBeta) * (1 - weightBeta)); }
			 */

			parameters.beta = Math.min(maxBeta, Math.max(minBeta, parameters.beta));
			return new Pair<Double, Parameters>(parameters.beta, parameters);
		} catch (Exception ex) {
			parameters.exception = ex;
			parameters.beta = defaultBeta;
			return new Pair<Double, Parameters>(defaultBeta, parameters);
		}
	}
	
	private static Pair<Double, Double> getRegressionParameters(ArrayList<Pair<Double, Double>> bestOpponentBidUtilityHistory, double utility0) {
		double n = 1;
		double x = 0;// Math.log(1);
		double y = Math.log(1 - utility0);
		double sumlnxlny = 0;// x * y;
		double sumlnx = 0;// x;
		double sumlny = y;
		double sumlnxlnx = 0;// x * y;
		for (Pair<Double, Double> d : bestOpponentBidUtilityHistory) {
			x = Math.log(d.snd);
			y = Math.log(d.fst - utility0);

			if (Double.isNaN(x))
				throw new RuntimeException("Unable to perform regression using provided points (x).");
			if (Double.isNaN(y))
				throw new RuntimeException("Unable to perform regression using provided points (y).");
			if (Double.isInfinite(x) || Double.isInfinite(y))
				continue;

			sumlnxlny += x * y;
			sumlnx += x;
			sumlny += y;
			sumlnxlnx += x * x;
			n++;
		}

		double b = ((n * sumlnxlny) - (sumlnx * sumlny)) / ((n * sumlnxlnx) - (sumlnx * sumlnx));
		if (Double.isNaN(b))
			throw new RuntimeException("Unable to perform regression using provided points (b)." + (sumlnxlny) + ", " + (n * sumlnxlnx) + ", "
					+ (sumlnx * sumlnx));
		double a = (sumlny - (b * sumlnx)) / n;

		if (Double.isNaN(a))
			throw new RuntimeException("Unable to perform regression using provided points (a).");
		return new Pair<Double, Double>(a, b);
	}

	private static Pair<Double, Double> getMaxima(double a, double b, double time, double discounting, double utility0) {
		// double root = b / discounting;
		ArrayList<Double> maxima = new ArrayList<Double>();
		maxima.add(time);
		for (int i = (int) Math.floor(time * 1000); i <= 1000; i++) {
			double root = (double) i / 1000.0;
			// if (root >= 0 && root <= 1 && root > time) {
			maxima.add(root);
			// }
		}
		maxima.add(1.0);
		double maxUtil = 0;
		double result = 0;

		// We incorporate the imbalance between the two agents' time constraints
		// by affecting the discounting factor.
		// timeScaledDiscounting is therefore the discount factor at time 1 in
		// our world.
		double expA = Math.exp(a);

		System.out.println("-timeScaledDiscounting = " + discounting);

		for (double maximum : maxima) {
			//double util = (utility0 + (expA * Math.pow(maximum, b))) * Math.exp(-discounting * maximum);
			double util = (utility0 + (expA * Math.pow(maximum, b))) * Math.pow(discounting, maximum);
			if (util > maxUtil) {
				result = maximum;
				maxUtil = util;
			}
		}
		return new Pair<Double, Double>(result, -discounting);
	}

	/**
	 * @param bestOpponentBidUtilityHistory The history of opponent bids.
	 * @param time The current time.
	 * @param discountFactor The discounting factor.
	 * @param breakoff The breakoff utility.
	 * @return
	 */
	public static Pair<Double, Parameters> getLevel(ArrayList<Pair<Double, Double>> bestOpponentBidUtilityHistory, double time, double discountFactor,
			double breakoff) {

		double[] x = new double[bestOpponentBidUtilityHistory.size()];
		double[] y = new double[bestOpponentBidUtilityHistory.size()];
		int i = 0;
		for (Pair<Double, Double> d : bestOpponentBidUtilityHistory) {
			x[i] = Math.log(d.snd);
			y[i] = Math.log(d.fst);
		}
		
		process(x, y, time);
		// TODO Auto-generated method stub
		return null;
	}
	
	private static ArrayList<double[]> process(double[] x, double[] y, double time) {
		BasicPrior[] bps = {new BasicPrior(11, 1.0, 2.0), new BasicPrior(11, 1.0, 2.0), new BasicPrior(1, .01, 1.0)};
		//BasicPrior[] bps = {new BasicPrior(11, 1.0, 2.0), new BasicPrior(11,	1.0, 2.0)};
		
		CovarianceFunction cf;
		
		cf = new SumCovarianceFunction(SquaredExponentialCovarianceFunction.getInstance(), NoiseCovarianceFunction.getInstance());
		//cf = SquaredExponentialCovarianceFunction.getInstance();
		//cf = new SumCovarianceFunction(PeriodicCovarianceFunction.getInstance(), NoiseCovarianceFunction.getInstance());
		//cf = PeriodicCovarianceFunction.getInstance();
		//cf = new SumCovarianceFunction(Matern3CovarianceFunction.getInstance(), NoiseCovarianceFunction.getInstance());
		//cf = Matern3CovarianceFunction.getInstance();
		
		return process(x, y, cf, bps, time);
	}
	
	private static ArrayList<double[]> process(double[] x, double[] y, CovarianceFunction cf, BasicPrior[] bps, double time) {
		GaussianProcessRegressionBMC regression = new GaussianProcessRegressionBMC();
		regression.setCovarianceFunction(cf);

		regression.setPriors(bps);
		
		GaussianProcessMixture predictor = regression.calculateRegression(x, y);

		Map<Double[], Double> hyperParameterWeights = regression.getHyperParameterWeights();

		for (Double[] hyperparameterValue : hyperParameterWeights.keySet()) {
			System.out.println("hyperparameter value: "
					+ ArrayUtils.toString(hyperparameterValue) + ", weight: "
					+ hyperParameterWeights.get(hyperparameterValue));
		}
		
		double[] testX = getXValues(time);

		GaussianProcessPrediction prediction = predictor
				.calculatePrediction(MatrixUtils.createColumnVector(testX));

		ArrayList<double[]> result = new ArrayList<double[]>();
		result.add(testX);
		result.add(prediction.getMean().getColumnPackedCopy());
		result.add(prediction.getVariance().getColumnPackedCopy());
		return result;
	}
	
	private static double[] getXValues(double time) {
		int n = 100;
		double[] vals = new double[n+1];
		for(int i=0; i<=n; i++)
		{
			vals[i] = (double)i / (double)n;
		}
		return vals;
	}
}
