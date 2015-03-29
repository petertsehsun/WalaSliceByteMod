package slicerTest;

import java.io.IOException;
import java.util.ArrayList;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import slicerWala.CallGraphSlicer;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

public class SlicerTest_modelCheckCTL_noSwing {
	public static void main(String[] args) throws IOException,
			ClassHierarchyException, IllegalArgumentException,
			CallGraphBuilderCancelException, NotFoundException,
			CannotCompileException {
		long start = System.currentTimeMillis();
		
		String classPath = "C:\\Users\\StuartSiroky\\workspace_luna_wala\\modelCheckCTL_noSwing\\bin";
		String scopeFile = "C:\\Users\\StuartSiroky\\git\\WalaSliceByteMod\\dat\\modelCheckCTLScopeList_noSwing.txt";
		String excluFile = "C:\\Users\\StuartSiroky\\git\\WalaSliceByteMod\\dat\\modelCheckCTLExclusions_noSwing.txt";
		String mainClass = "LmodelCheckCTL/view/ModelCheckCTLView";
		String caller = "modelCheckCTL.model.ModelCheckCTLModel.parseFormula(Ljava/lang/String;)Z";
		String callee = "modelCheckCTL.model.ModelCheckCTLModel.modelViewFormulaNotify(Ljava/lang/String;)V";
//		caller = "";
//		callee = "";
		ArrayList<String> keepMList = new ArrayList<String>();
		keepMList.add("modelCheckCTL.view.ModelCheckCTLView.setLog(LnoSwing/JTextArea;)V");
		keepMList.add("modelCheckCTL.view.ModelCheckCTLView.getLog()LnoSwing/JTextArea;");

		CallGraphSlicer cgs = new CallGraphSlicer();
		cgs.calcSlice(scopeFile, excluFile, mainClass, caller, callee,
				classPath, keepMList);
		long end = System.currentTimeMillis();
		long totalTime = end - start;
		System.out.println("Total time to Slice and fix Byte Code : "+totalTime+"ms");
	}
}

