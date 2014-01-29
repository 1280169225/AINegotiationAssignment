package agents2011.southampton;

public class IAMhaggler2011c3_0 extends IAMhaggler2011 {

	public IAMhaggler2011c3_0() {
		RISK_PARAMETER = 3.0;
	}

	@Override
	public String getName() {
		return super.getName() + " newr=" + RISK_PARAMETER;
	}
}
