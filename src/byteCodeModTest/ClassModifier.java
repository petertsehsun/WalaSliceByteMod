package byteCodeModTest;

import java.io.IOException;
import java.util.ArrayList;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import byteCodeMod.ClassFileModifier;
import byteCodeMod.Method_Signature;

public class ClassModifier {

	public static void main(String[] argv) throws NotFoundException,
			CannotCompileException, IOException {
		String cPath = "C:\\Users\\StuartSiroky\\workspace_luna_wala\\exampleCode\\bin";
		String cName = "ex.MyApp2";
		ArrayList<Method_Signature> mSig = new ArrayList<Method_Signature>();
		mSig.add(new Method_Signature("foo", "()V"));
		// mSig.add(new Method_Signature("foo2", "(I)Lex/mytmp;"));

		ClassFileModifier cfm = new ClassFileModifier();

		cfm.findModMethod(cPath, cName, mSig);
	}
}
