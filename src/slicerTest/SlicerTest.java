package slicerTest;

import java.io.IOException;
import java.util.ArrayList;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import slicerWala.CallGraphSlicer;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

public class SlicerTest {
	public static void main(String[] args) throws IOException,
			ClassHierarchyException, IllegalArgumentException,
			CallGraphBuilderCancelException, NotFoundException,
			CannotCompileException {
		long start = System.currentTimeMillis();
		
		String classPath = "C:\\Users\\StuartSiroky\\git\\NoSwingCalc\\bin";
		String scopeFile = "C:\\Users\\StuartSiroky\\workspace_luna_wala\\WalaTest\\dat\\CalcNoSwingScopeList.txt";
		String excluFile = "C:\\Users\\StuartSiroky\\workspace_luna_wala\\WalaTest\\dat\\MyExclusions.txt";
		String mainClass = "Lcalc/view/CalculatorView";
		String caller = "";
		String callee = "";
		caller = "calc.controller.CalculatorController.equalsOperation()V";
		callee = "calc.model.CalculatorModel.equalsOp()V";
		ArrayList<String> keepMList = new ArrayList<String>();
		keepMList.add("calc.view.CalculatorView.getContentPane()Lcalc/noSwing/Container;");
		keepMList.add("calc.noSwing.MyJFrame.getContentPane()Lcalc/noSwing/Container;");

		CallGraphSlicer cgs = new CallGraphSlicer();
		cgs.calcSlice(scopeFile, excluFile, mainClass, caller, callee,
				classPath, keepMList);
		long end = System.currentTimeMillis();
		long totalTime = end - start;
		System.out.println("Total time to Slice and fix Byte Code : "+totalTime+"ms");
	}
}
// : calc.view.CalculatorView.main([Ljava/lang/String;)V
// : calc.view.CalculatorView.start(Lcalc/view/CalculatorView;II)V
// : calc.view.CalculatorView.equalsMethod(Lcalc/view/CalculatorView;)V
// : buttons.EqualsButton.pushed()V
// :
// calc.view.CalculatorView$EqHandler.actionPerformed(Lcalc/noSwing/ActionEvent;)V
// : calc.controller.CalculatorController.equalsOperation()V
// : calc.model.CalculatorModel.equalsOp()V
// : calc.model.CalculatorModel.notifyChanged(Lcalc/model/ModelEvent;)V
