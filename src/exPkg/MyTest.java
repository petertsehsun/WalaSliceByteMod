package exPkg;

public class MyTest {

	public void start() {
		ABase a = new ABase();
		Base b = new Base();
		System.out.println("Start");
		int i = 5;

		b.m2();
		i = a.m3();
		i++;
		a.m4();
		if (i > 5) {
			// a.m2();
			a.m1();
		}
	}

	public static void main(String[] args) {
		MyTest t = new MyTest();
		t.start();
	}

}
