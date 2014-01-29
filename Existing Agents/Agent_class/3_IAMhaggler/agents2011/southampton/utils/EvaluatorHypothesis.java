package agents2011.southampton.utils;

import negotiator.utility.Evaluator;

/**
 * @author Colin Williams
 * 
 */
public class EvaluatorHypothesis extends Hypothesis {
	private String description;
	private Evaluator evaluator;

	public EvaluatorHypothesis(Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	public Evaluator getEvaluator() {
		return this.evaluator;
	}

	public String toString() {
		return this.description;
	}

	public void setDesc(String value) {
		this.description = value;
	}

	public String getDesc() {
		return this.description;
	}
}