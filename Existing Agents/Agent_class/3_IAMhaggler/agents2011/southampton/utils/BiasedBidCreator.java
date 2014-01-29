package agents2011.southampton.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.UtilitySpace;
import agents2011.southampton.utils.RandomBidCreator;

public class BiasedBidCreator extends RandomBidCreator {
	
	/* (non-Javadoc)
	 * @see agents2011.southampton.utils.RandomBidCreator#getBid(negotiator.utility.UtilitySpace, double, double)
	 */
	@Override
	public Bid getBid(UtilitySpace utilitySpace, double min, double max) {
		int samples = 100;
		Bid[] bids = new Bid[samples];
		double[] weights = new double[samples];
		EBid[] ebids = new EBid[samples];
		double sumWeights = 0;
		double minWeight = 1;
		for(int i=0; i<samples; i++)
		{
			bids[i] = getRandomBid(utilitySpace, min, max);
			weights[i] = evaluate(bids[i]);
			ebids[i] = new EBid(bids[i], weights[i]);
			minWeight = Math.min(minWeight, weights[i]);
			sumWeights += weights[i];
		}
		sumWeights -= samples * minWeight;
		
		double rand = random.nextDouble();
		int n = samples - 1 - (int)(Math.floor(Math.log(1 - rand + (rand * Math.pow(0.87, samples)))/Math.log(0.87)));
		
		return qnth(Arrays.asList(ebids), n).getBid();
	}

	public static EBid qnth(List<EBid> sample, int n) {
	    EBid pivot = sample.get(0);
	    List<EBid> below = new ArrayList<EBid>();
	    List<EBid> above = new ArrayList<EBid>();
	    for (EBid s : sample) {
	        if (s.compareTo(pivot) < 0)
	            below.add(s);
	        else if (s.compareTo(pivot) > 0)
	            above.add(s);
	    }
	    int i = below.size();
	    int j = sample.size() - above.size();
	    if (n < i)       return qnth(below, n);
	    else if (n >= j) return qnth(above, n-j);
	    else             return pivot;
	}

	
	protected double evaluate(Bid bid) {
		int i = 0;
		double evaluation = 0;
		for(int issueNumber : issueNumbers)
		{
			try {
				HashMap<String, Double> hm = issueWeights.get(i);
				double issueWeight = hm.get(bid.getValue(issueNumber));
				double issueWeightSum = issueWeightSums[i];
				evaluation += issueWeight/issueWeightSum;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
		}
		return evaluation;
	}

	protected ArrayList<HashMap<String, Double>> issueWeights;
	protected double[] issueWeightSums;
	protected int[] issueNumbers;

	public BiasedBidCreator(UtilitySpace utilitySpace) {
		ArrayList<Issue> issues = utilitySpace.getDomain().getIssues();
		
		issueWeights = new ArrayList<HashMap<String, Double>>();
		issueNumbers = new int[issues.size()];
		issueWeightSums = new double[issues.size()];
		
		int i=0;
		for (Issue issue : issues) {
			if(issue instanceof IssueDiscrete)
			{
				IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
				HashMap<String, Double> hm = new HashMap<String, Double>();
				for(ValueDiscrete valueDiscrete : issueDiscrete.getValues())
				{
					hm.put(valueDiscrete.getValue(), 1.0);
				}
				issueWeights.add(hm);
				issueWeightSums[i] = hm.size();
			}
			else
			{
				issueWeights.add(null);
			}
			issueNumbers[i] = issue.getNumber();
			i++;
		}
	}

	public void logBid(Bid bid, double time) {
		for(int i = 0; i<issueNumbers.length; i++)
		{
			int issueNumber = issueNumbers[i];
	        Value v;
			try {
				v = bid.getValue(issueNumber);
		        if(v instanceof ValueDiscrete)
		        {
		        	HashMap<String, Double> hm = issueWeights.get(i);
		        	
		        	String val = ((ValueDiscrete)v).getValue();
		        	hm.put(val, hm.get(val) + 1 - time);
		        	issueWeightSums[i]+=1 - time;
		        }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
