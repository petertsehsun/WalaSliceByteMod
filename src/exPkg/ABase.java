package exPkg;

public class ABase extends Base {
	public void m1() {
		super.m1();
		System.out.println("ABase.m1()");
		m2();
	}

	public int m3() {
		int i;
		System.out.println("ABase.m3()");
		i = 7;
		i++;
		return i;
	}

	public void m4() {
		System.out.println("ABase.m4()");
	}
}
