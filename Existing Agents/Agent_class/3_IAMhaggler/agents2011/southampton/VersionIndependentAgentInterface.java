package agents2011.southampton;

public interface VersionIndependentAgentInterface {

	public abstract double adjustDiscountFactor(double discountFactor);
	public abstract double getTime();
	public abstract void log(String message);
	public void setOurTime(long time);
	public void setOpponentTime(long time); 
}
