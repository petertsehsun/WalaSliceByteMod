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
//		caller = "rwgui.model.RackAndComponentSet.createRackTypeI(Ljava/lang/String;IDDZZLjava/lang/String;)V";
//		callee = "java.beans.PropertyChangeSupport.firePropertyChange(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V";
		caller = "rwgui.view.RWGuiView.initCreateDialog(Ljava/lang/String;Ljava/lang/Class;)Lrwgui/view/CreateDialog";
		callee = "rwgui.view.CreateDialog.\\<init\\>(Ljavax/swing/JFrame;Ljava/lang/String;Lrwgui/view/RWGuiView;Lrwgui/controller/RWGuiController;Ljava/lang/Class;)V";
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

//RWGuiView: private CreateDialog initCreateDialog(String title, Class nodeClass)
//CreateDialog:public CreateDialog(JFrame frame, String title, RWGuiView parent,RWGuiController controller, Class nodeClass) {


//rwgui.view.CreateDialog.createDialog propertyChange(PropertyChangeEvent)
//CreateRackTypeMethod: public void execute(Hashtable params
//RWGuiController:public void createRackType(Hashtable params
//RWGuiController:public void (String typeName, int height, double power,double price, boolean moreCanBePurchased, boolean hasUPS)
//caller = "rwgui.model.RackAndComponentSet.createRackTypeI(Ljava/lang/String;IDDZZLjava/lang/String;)V";
//callee = "java.beans.PropertyChangeSupport.firePropertyChange(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V";
