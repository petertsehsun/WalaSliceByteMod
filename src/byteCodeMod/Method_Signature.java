package byteCodeMod;

public class Method_Signature {
	private String mName;
	private String mSig;

	public Method_Signature(String mName, String mSig) {
		this.mName = mName;
		this.mSig = mSig;
	}

	public String getMethodName() {
		return mName;
	}

	public void setMethodName(String mName) {
		this.mName = mName;
	}

	public String getMethodSig() {
		return mSig;
	}

	public void setMethodSig(String mSig) {
		this.mSig = mSig;
	}
	public String toString() {
		return mName+mSig;
	}
}
