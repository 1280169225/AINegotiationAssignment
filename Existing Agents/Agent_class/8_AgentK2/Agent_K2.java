import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;

public class Agent_K2 extends Agent {

    private Action partner = null;
    private HashMap<Bid, Double> offeredBidMap;
    private double target; // 効用値獲得目標
    private double bidTarget; // 相手に提示する手に使用する基準値
    private double sum; // 相手から提示された効用値の話
    private double sum2; // 提示された効用値の二乗和
    private int rounds; // 交渉回数
    private double tremor; // 揺らぎ幅

    public void init() {
        // System.out.println("debug : ----- Initialize -----");

        offeredBidMap = new HashMap<Bid, Double>();
        target = 1.0;
        bidTarget = 1.0;
        sum = 0.0;
        sum2 = 0.0;
        rounds = 0;
        tremor = 2.0;
    }

    public static String getVersion() {
        return "0.31415_discountFix";
    }

    public void ReceiveMessage(Action opponentAction) {
        // System.out.println("debug : ----- ReceiveMessage -----");
        partner = opponentAction;
    }

    public Action chooseAction() {
        Action action = null;
        try {
            if (partner == null) {
                action = selectBid();
            }
            if (partner instanceof Offer) {
                Bid offeredBid = ((Offer) partner).getBid();

                // 受諾確率計算
                double p = acceptProbability(offeredBid);

                if (p > Math.random()) {
                    // System.out.println("debug : Choose Action => Accept");
                    action = new Accept(getAgentID());
                } else {
                    // System.out.println("debug : Choose Action => Select Bid");
                    action = selectBid();
                }
            }
        } catch (Exception e) {
            // System.out.println("Exception in ChooseAction:" +
            // e.getMessage());
            action = new Accept(getAgentID());
        }
        return action;
    }

    private Action selectBid() {
        // System.out.println("debug : ----- Select Bid -----");
        Bid nextBid = null;

        ArrayList<Bid> bidTemp = new ArrayList<Bid>();

        for (Bid bid : offeredBidMap.keySet()) {
            if (offeredBidMap.get(bid) > target) {
                bidTemp.add(bid);
            }
        }

        int size = bidTemp.size();
        if (size > 0) {
            // System.out.println("debug : hit effective bid = " + size);
            int sindex = (int) Math.floor(Math.random() * size);
            // System.out.println("debug : select index " + sindex);
            nextBid = bidTemp.get(sindex);
        } else {
            double searchUtil = 0.0;
            // System.out.println("debug : no hit ");
            try {
                int loop = 0;
                while (searchUtil < bidTarget) {
                    if (loop > 500) {
                        bidTarget -= 0.01;
                        loop = 0;
                        // System.out.println("debug : challenge fail, targetUtility reset = "
                        // + targetUtility);
                    }
                    nextBid = searchBid();
                    searchUtil = utilitySpace.getUtility(nextBid);
                    loop++;
                }
            } catch (Exception e) {
                // System.out.println("Problem with received bid:" +
                // e.getMessage() + ". cancelling bidding");
            }
        }

        if (nextBid == null) {
            // System.out.println("debug : emergency accept");
            return (new Accept(getAgentID()));
        }
        return (new Offer(getAgentID(), nextBid));
    }

    private Bid searchBid() throws Exception {
        HashMap<Integer, Value> values = new HashMap<Integer, Value>();
        ArrayList<Issue> issues = utilitySpace.getDomain().getIssues();
        Random randomnr = new Random();

        Bid bid = null;

        for (Issue lIssue : issues) {
            switch (lIssue.getType()) {
            case DISCRETE:
                IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
                int optionIndex = randomnr.nextInt(lIssueDiscrete
                        .getNumberOfValues());
                values.put(lIssue.getNumber(),
                        lIssueDiscrete.getValue(optionIndex));
                break;
            case REAL:
                IssueReal lIssueReal = (IssueReal) lIssue;
                int optionInd = randomnr.nextInt(lIssueReal
                        .getNumberOfDiscretizationSteps() - 1);
                values.put(
                        lIssueReal.getNumber(),
                        new ValueReal(lIssueReal.getLowerBound()
                                + (lIssueReal.getUpperBound() - lIssueReal
                                        .getLowerBound())
                                * (double) (optionInd)
                                / (double) (lIssueReal
                                        .getNumberOfDiscretizationSteps())));
                break;
            case INTEGER:
                IssueInteger lIssueInteger = (IssueInteger) lIssue;
                int optionIndex2 = lIssueInteger.getLowerBound()
                        + randomnr.nextInt(lIssueInteger.getUpperBound()
                                - lIssueInteger.getLowerBound());
                values.put(lIssueInteger.getNumber(), new ValueInteger(
                        optionIndex2));
                break;
            default:
                throw new Exception("issue type " + lIssue.getType()
                        + " not supported by SimpleAgent2");
            }
        }
        bid = new Bid(utilitySpace.getDomain(), values);
        return bid;
    }

    double acceptProbability(Bid offeredBid) throws Exception {

        double offeredUtility = utilitySpace.getUtility(offeredBid);
        offeredBidMap.put(offeredBid, offeredUtility);

        sum += offeredUtility;
        sum2 += offeredUtility * offeredUtility;
        rounds++;

        // 平均値
        double mean = sum / rounds;

        // 分散
        double variance = (sum2 / rounds) - (mean * mean);

        // 相手の推定行動幅
        double deviation = Math.sqrt(variance * 12);
        if (Double.isNaN(deviation)) {
            deviation = 0.0;
        }

        double time = timeline.getTime();

        double t = time * time * time;

        // 効用値例外処理
        if (offeredUtility < 0 || offeredUtility > 1.05) {
            throw new Exception("utility " + offeredUtility + " outside [0,1]");
        }

        // 時間例外処理
        if (t < 0 || t > 1) {
            throw new Exception("time " + t + " outside [0,1]");
        }

        // 効用値上限処理
        if (offeredUtility > 1.) {
            offeredUtility = 1;
        }

        // 現在推定される相手から引き出せる最大の効用値
        double estimateMax = mean + ((1 - mean) * deviation);

        // 接近係数 betaは揺らぎあり
        double alpha = 1 + tremor + (10 * mean) - (2 * tremor * mean);
        double beta = alpha + (Math.random() * tremor) - (tremor / 2);

        double preTarget = 1 - (Math.pow(time, alpha) * (1 - estimateMax));
        double preTarget2 = 1 - (Math.pow(time, beta) * (1 - estimateMax));

        // 相手との譲歩比率
        double ratio = (deviation + 0.1) / (1 - preTarget);
        if (Double.isNaN(ratio) || ratio > 2.0) {
            ratio = 2.0;
        }

        double ratio2 = (deviation + 0.1) / (1 - preTarget2);
        if (Double.isNaN(ratio2) || ratio2 > 2.0) {
            ratio2 = 2.0;
        }

        target = ratio * preTarget + 1 - ratio;
        bidTarget = ratio2 * preTarget2 + 1 - ratio2;

        // 最終補正
        double m = t * (-300) + 400;
        if (target > estimateMax) {
            double r = target - estimateMax;
            double f = 1 / (r * r);
            if (f > m || Double.isNaN(f))
                f = m;
            double app = r * f / m;
            target = target - app;
        } else {
            target = estimateMax;
        }

        if (bidTarget > estimateMax) {
            double r = bidTarget - estimateMax;
            double f = 1 / (r * r);
            if (f > m || Double.isNaN(f))
                f = m;
            double app = r * f / m;
            bidTarget = bidTarget - app;
        } else {
            bidTarget = estimateMax;
        }

        // test code for Discount Factor
        double discount_utility = utilitySpace.getUtilityWithDiscount(
                offeredBid, time);
        double discount_ratio = discount_utility / offeredUtility;
        if (!Double.isNaN(discount_utility)) {
            target *= discount_ratio;
            bidTarget *= discount_ratio;
        }
        System.out.printf("%f, %f, %f, %f, %f, %f %n", time, estimateMax,
                target, offeredUtility, discount_utility, discount_ratio);
        // test code for Discount Factor

        double utilityEvaluation = offeredUtility - estimateMax;
        double satisfy = offeredUtility - target;

        double p = (Math.pow(time, alpha) / 5) + utilityEvaluation + satisfy;
        if (p < 0.1) {
            p = 0.0;
        }
        // System.out.println("debug : n = " + n);
        // System.out.println("debug : Mean = " + mean);
        // System.out.println("debug : Variance = " + variance);
        // System.out.println("debug : Deviation = " + deviation);
        // System.out.println("debug : Time = " + time);
        // System.out.println("debug : Estimate Max = " + estimateMax);
        // System.out.println("debug : Bid Target = " + bidTarget);
        // System.out.println("debug : Eval Target = " + target);
        // System.out.println("debug : Offered Utility = " + offeredUtility);
        // System.out.println("debug : Accept Probability= " + p);
        // System.out.println("debug : Utility Evaluation = " +
        // utilityEvaluation);
        // System.out.println("debug : Ssatisfy = " + satisfy);

        return p;
    }
}
