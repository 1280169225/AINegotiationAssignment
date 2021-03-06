package uk.ac.soton.ecs.gp4j.gp;

import Jama.Matrix;

public interface GaussianPrediction {

	public Matrix getVariance();

	public Matrix getMean();

	public Matrix getTestX();

	public Matrix getStandardDeviation();

	public int size();

}
