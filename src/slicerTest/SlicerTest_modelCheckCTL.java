package slicerTest;

import java.io.IOException;
import java.util.ArrayList;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import slicerWala.CallGraphSlicer;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

public class SlicerTest_modelCheckCTL {
	public static void main(String[] args) throws IOException,
			ClassHierarchyException, IllegalArgumentException,
			CallGraphBuilderCancelException, NotFoundException,
			CannotCompileException {
		long start = System.currentTimeMillis();
		
		String classPath = "C:\\Users\\StuartSiroky\\git\\modelCheckCTLcss\\bin";
		String scopeFile = "C:\\Users\\StuartSiroky\\git\\WalaSliceByteMod\\dat\\modelCheckCTLScopeList.txt";
		String excluFile = "C:\\Users\\StuartSiroky\\git\\WalaSliceByteMod\\dat\\modelCheckCTLExclusions.txt";
		String mainClass = "LmodelCheckCTL/view/ModelCheckCTLView";
		String caller = "modelCheckCTL.model.ModelCheckCTLModel.parseFormula(Ljava/lang/String;)Z";
		String callee = "modelCheckCTL.model.ModelCheckCTLModel.modelViewNotify(Ljava/lang/String;)V";
//		caller = "";
//		callee = "";
		ArrayList<String> keepMList = new ArrayList<String>();
		//keepMList.add("calc.view.CalculatorView.getContentPane()Lcalc/noSwing/Container;");
		//keepMList.add("calc.noSwing.MyJFrame.getContentPane()Lcalc/noSwing/Container;");

		CallGraphSlicer cgs = new CallGraphSlicer();
		cgs.calcSlice(scopeFile, excluFile, mainClass, caller, callee,
				classPath, keepMList);
		long end = System.currentTimeMillis();
		long totalTime = end - start;
		System.out.println("Total time to Slice and fix Byte Code : "+totalTime+"ms");
	}
}

