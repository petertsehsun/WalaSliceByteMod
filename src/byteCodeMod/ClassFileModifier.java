package byteCodeMod;

import java.io.IOException;
import java.util.ArrayList;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.Modifier;
import javassist.NotFoundException;

public class ClassFileModifier {
	int debug_level = 0;
	public void findModMethod(String cPath, String cName,
			ArrayList<Method_Signature> mSig) throws NotFoundException,
			CannotCompileException, IOException {

		ClassPool.getDefault().insertClassPath(cPath);
		CtClass clas = ClassPool.getDefault().get(cName);
		if (!Modifier.isAbstract(clas.getModifiers())) {
			if(debug_level > 0) {
				System.out.println("Modifying Class " + cName);
			}
			clearMethod(clas, mSig);
			if(debug_level > 0) {
				System.out.println("Writing ClassFile " + cName);
			}
			// make sure the change compiles.
			clas.rebuildClassFile();
			// clas.toClass();
			clas.writeFile(cPath);
		}
	}

	private void clearMethod(CtClass clas, ArrayList<Method_Signature> mSig)
			throws NotFoundException, CannotCompileException {
		for (Method_Signature ms : mSig) {
			// if (!ms.getMethodName().contains("addActionListener")) {
			if(debug_level>0) {
				System.out.println("\tRemoving Method " + ms.getMethodName()
					+ ms.getMethodSig());
			}
			CtMethod mChange = clas.getMethod(ms.getMethodName(),
					ms.getMethodSig());
			if (!Modifier.isAbstract(mChange.getModifiers())) {
				// CtMethod mChange =
				// clas.getDeclaredMethod(ms.getMethodName());
				// clear the method body
				mChange.setBody(null);

				CtClass mtype = mChange.getReturnType();
				String type = mtype.getName();
		
				StringBuffer body = new StringBuffer();
				// if the method is not return void return null
				if (!"void".equals(type)) {
					body.append("{\n");
					if (mtype instanceof CtPrimitiveType) {
						body.append("return 0;\n");
					} else if(type.equals("java.lang.String")) {
						body.append("return \"\";\n");
					} else if(type.contains("java.lang")) {
						body.append("return null;\n");
					} else {
						//System.out.println("return new "+type+"();");
						//body.append("return new "+type+"();\n");
						body.append("return null;\n");
					}
					body.append("}");
					mChange.setBody(body.toString());
				}
			} // not abstract method
			// }//! addActionListener
		} // for methods
	}

}
