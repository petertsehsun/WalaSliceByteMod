package slicerTest;

import java.io.IOException;
import java.util.ArrayList;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import slicerWala.CallGraphSlicer;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

public class SlicerTest_rwgui {
	public static void main(String[] args) throws IOException,
			ClassHierarchyException, IllegalArgumentException,
			CallGraphBuilderCancelException, NotFoundException,
			CannotCompileException {
		long start = System.currentTimeMillis();
		
		String classPath = "C:\\Users\\StuartSiroky\\workspace_luna_wala\\rwgui\\bin";
		String scopeFile = "C:\\Users\\StuartSiroky\\git\\WalaSliceByteMod\\dat\\rwguiScopeList.txt";
		String excluFile = "C:\\Users\\StuartSiroky\\git\\WalaSliceByteMod\\dat\\RWGuiExclusions.txt";
		String mainClass = "Lrwgui/view/RWGuiFrame";
		String caller = "";
		String callee = "";
		caller = "rwgui.model.RackAndComponentSet.createRackTypeI(Ljava/lang/String;IDDZZLjava/lang/String;)V";
		callee = "java.beans.PropertyChangeSupport.firePropertyChange(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V";
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
// : calc.view.CalculatorView.main([Ljava/lang/String;)V
// : calc.view.CalculatorView.start(Lcalc/view/CalculatorView;II)V
// : calc.view.CalculatorView.equalsMethod(Lcalc/view/CalculatorView;)V
// : buttons.EqualsButton.pushed()V
// :
// calc.view.CalculatorView$EqHandler.actionPerformed(Lcalc/noSwing/ActionEvent;)V
// : calc.controller.CalculatorController.equalsOperation()V
// : calc.model.CalculatorModel.equalsOp()V
// : calc.model.CalculatorModel.notifyChanged(Lcalc/model/ModelEvent;)V
