package savedExamples;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.HeapStatement;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.util.warnings.Warnings;
import com.ibm.wala.viz.NodeDecorator;

public class MyCallGraphGen {
	static int debugLevel = 1;

	/**
	 * Usage: MyCallGraphGen -scopeFile file_path [-entryClass class_name |
	 * -mainClass class_name]
	 * 
	 * If given -mainClass, uses main() method of class_name as entrypoint. If
	 * given -entryClass, uses all public methods of class_name.
	 * 
	 * @throws IOException
	 * @throws ClassHierarchyException
	 * @throws CallGraphBuilderCancelException
	 * @throws IllegalArgumentException
	 */

	public static void main(String[] args) throws IOException,
			ClassHierarchyException, IllegalArgumentException,
			CallGraphBuilderCancelException {
		long start = System.currentTimeMillis();

		// String scopeFile =
		// "C:\\Users\\StuartSiroky\\workspace_luna_wala\\WalaTest\\dat\\scopeList.txt";
		String scopeFile = "C:\\Users\\StuartSiroky\\workspace_luna_wala\\WalaTest\\dat\\CalcNoSwingScopeList.txt";
		String entryClass = null;
		String mainClass = "Lcalc/view/CalculatorView";
		// String mainClass = "LexPkg/MyTest";

		// use exclusions to eliminate certain library packages
		String exFile = "C:\\Users\\StuartSiroky\\workspace_luna_wala\\WalaTest\\dat\\MyExclusions.txt";
		File exclusionsFile = new File(exFile);
		AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile,
				exclusionsFile, MyCallGraphGen.class.getClassLoader());
		IClassHierarchy cha = ClassHierarchy.make(scope);
		if (debugLevel >= 1) {
			System.out.println(cha.getNumberOfClasses() + " classes");
		}
		if (debugLevel >= 6) {
			System.out.println(Warnings.asString());
		}
		Warnings.clear();

		printScopeClasses(scope, cha);

		AnalysisOptions options = new AnalysisOptions();
		Iterable<Entrypoint> entrypoints = entryClass != null ? makePublicEntrypoints(
				scope, cha, entryClass) : Util.makeMainEntrypoints(scope, cha,
				mainClass);
		options.setEntrypoints(entrypoints);
		// you can dial down reflection handling if you like
		// options.setReflectionOptions(ReflectionOptions.NONE);
		AnalysisCache cache = new AnalysisCache();
		// other builders can be constructed with different Util methods
		CallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options,
				cache, cha, scope);
		// CallGraphBuilder builder = Util.makeNCFABuilder(4, options, cache,
		// cha, scope);
		// CallGraphBuilder builder = Util.makeVanillaNCFABuilder(2, options,
		// cache, cha, scope);
		// CallGraphBuilder builder = Util.makeRTABuilder(options, cache, cha,
		// scope);
		System.out.println("building call graph...");
		CallGraph cg = builder.makeCallGraph(options, null);

		long end = System.currentTimeMillis();
		System.out.println("done");
		System.out.println("took " + (end - start) + "ms");
		System.out.println(CallGraphStats.getStats(cg));
		for (int i = 0; i < cg.getNumberOfNodes(); i++) {
			CGNode n = cg.getNode(i);
			System.out.println("NODE SIG " + n.getMethod().getSignature());
		}
		if (debugLevel >= 4) {
			System.out.println(cg.toString());
		}
		ArrayList<String> keepMethodList = calcSlice(builder, cg);

		ListRemoveScopeClasses(scope, cha, keepMethodList);
	}

	private static ArrayList<String> calcSlice(CallGraphBuilder builder,
			CallGraph cg) throws IOException {
		ArrayList<String> finalMethodSlice = new ArrayList<String>();

		DataDependenceOptions dOptions = Slicer.DataDependenceOptions.FULL;
		// DataDependenceOptions dOptions =
		// Slicer.DataDependenceOptions.NO_BASE_PTRS;
		// DataDependenceOptions dOptions =
		// Slicer.DataDependenceOptions.NO_BASE_NO_HEAP;
		// DataDependenceOptions dOptions =
		// Slicer.DataDependenceOptions.NO_BASE_NO_EXCEPTIONS;
		// DataDependenceOptions dOptions =
		// Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS;
		// DataDependenceOptions dOptions =
		// Slicer.DataDependenceOptions.NO_HEAP;
		// DataDependenceOptions dOptions =
		// Slicer.DataDependenceOptions.NO_HEAP_NO_EXCEPTIONS;
		// DataDependenceOptions dOptions =
		// Slicer.DataDependenceOptions.NO_EXCEPTIONS;
		// DataDependenceOptions dOptions =
		// Slicer.DataDependenceOptions.REFLECTION;
		// DataDependenceOptions dOptions = Slicer.DataDependenceOptions.NONE;
		// ControlDependenceOptions cOptions =
		// Slicer.ControlDependenceOptions.NONE;
		ControlDependenceOptions cOptions = Slicer.ControlDependenceOptions.FULL;
		boolean goBackwards = true;
		String srcCaller = "";
		String srcCallee = "";

		try { // TODO

			// : calc.view.CalculatorView.main([Ljava/lang/String;)V
			// : calc.view.CalculatorView.start(Lcalc/view/CalculatorView;II)V
			// :
			// calc.view.CalculatorView.equalsMethod(Lcalc/view/CalculatorView;)V
			// : buttons.EqualsButton.pushed()V
			// :
			// calc.view.CalculatorView$EqHandler.actionPerformed(Lcalc/noSwing/ActionEvent;)V
			// : calc.controller.CalculatorController.equalsOperation()V
			// : calc.model.CalculatorModel.equalsOp()V
			// :
			// calc.model.CalculatorModel.notifyChanged(Lcalc/model/ModelEvent;)V

			srcCaller = "calc.model.CalculatorModel.equalsOp()V";
			srcCallee = "calc.model.CalculatorModel.notifyChanged(Lcalc/model/ModelEvent;)V";
			srcCaller = "calc.view.CalculatorView$EqHandler.actionPerformed(Lcalc/noSwing/ActionEvent;)V";
			srcCallee = "calc.controller.CalculatorController.equalsOperation()V";

			finalMethodSlice = makeSDG(cg, builder, srcCaller, srcCallee,
					goBackwards, dOptions, cOptions);
		} catch (CancelException e) {
			System.err
					.println("Failed to find slice backward for Final Method");
			e.printStackTrace();
		}

		return finalMethodSlice;
	}

	private static void printScopeClasses(AnalysisScope scope,
			IClassHierarchy cha) {
		for (IClass c : cha) {
			if (!scope.isApplicationLoader(c.getClassLoader()))
				continue;
			String cname = c.getName().toString();
			if (debugLevel >= 2) {
				System.out.println("Class:" + cname);
				if (debugLevel >= 4) {
					for (IMethod m : c.getAllMethods()) {

						String mname = m.getName().toString();
						System.out.println("  method:" + mname + " "
								+ m.getDescriptor().toString());
					} // for method
				}
				System.out.println();
			}
		} // for class
	}

	private static Iterable<Entrypoint> makePublicEntrypoints(
			AnalysisScope scope, IClassHierarchy cha, String entryClass) {
		Collection<Entrypoint> result = new ArrayList<Entrypoint>();
		IClass klass = cha.lookupClass(TypeReference.findOrCreate(
				ClassLoaderReference.Application,
				StringStuff.deployment2CanonicalTypeString(entryClass)));
		for (IMethod m : klass.getDeclaredMethods()) {
			if (m.isPublic()) {
				result.add(new DefaultEntrypoint(m, cha));
			}
		}
		return result;
	}

	public static ArrayList<String> makeSDG(CallGraph cg,
			CallGraphBuilder builder, String srcCaller, String srcCallee,
			boolean goBackward, DataDependenceOptions dOptions,
			ControlDependenceOptions cOptions) throws IllegalArgumentException,
			CancelException, IOException {
		ArrayList<String> MethodList = new ArrayList<String>();
		SDG sdg = new SDG(cg, builder.getPointerAnalysis(), dOptions, cOptions);

		// find the call statement of interest

		// CGNode callerNode = SlicerTest.findMethod(cg, srcCaller);
		// CGNode calleeNode = SlicerTest.findMethod(cg, srcCallee);
		CGNode callerNode = SlicerTest.findMethodBySignature(cg, srcCaller);
		CGNode calleeNode = SlicerTest.findMethodBySignature(cg, srcCallee);
		System.out.println("CallerNode :" + callerNode.getMethod().getName()
				+ " " + callerNode.getMethod().getSignature());
		System.out.println("CalleeNode :" + calleeNode.getMethod().getName()
				+ " " + calleeNode.getMethod().getSignature());

		// Statement s = SlicerTest.findCallTo(callerNode, srcCallee);
		Statement s = SlicerTest.findCallToBySignature(callerNode, srcCallee);
		// compute the slice as a collection of statements
		Collection<Statement> slice = null;
		if (goBackward) {
			slice = Slicer.computeBackwardSlice(s, cg,
					builder.getPointerAnalysis(), dOptions, cOptions);
		} else {
			// for forward slices ... we actually slice from the return
			// value of
			// calls.
			s = getReturnStatementForCall(s);
			slice = Slicer.computeForwardSlice(s, cg,
					builder.getPointerAnalysis(), dOptions, cOptions);
		}

		IMethod cm = callerNode.getMethod();
		String css = cm.getSignature();
		if (!MethodList.contains(css)) {
			// System.out.println("STUART adding caller method "+css);
			MethodList.add(css);
		}
		cm = calleeNode.getMethod();
		css = cm.getSignature();
		if (!MethodList.contains(css)) {
			// System.out.println("STUART adding callee method "+css);
			MethodList.add(css);
		}

		if (debugLevel >= 2) {
			System.out.println("DUMP SLICE:");
			SlicerTest.dumpSlice(slice);
		}
		// create a view of the SDG restricted to nodes in the slice
		Graph<Statement> g = pruneSDG(sdg, slice);
		// System.out.println("Graph"+g.toString());
		sanityCheck(slice, g);

		for (Statement o : g) {
			CGNode n = o.getNode();
			IMethod m = n.getMethod();
			String ss = m.getSignature();
			if (!ss.contains("com.ibm.wala.FakeRootClass")) {
				if (!MethodList.contains(ss)) {
					// System.out.println("STUART adding method "+ss);
					MethodList.add(ss);
				}
			}
		}
		return MethodList;

	} // makeSDG

//	private static void combineSlices(ArrayList<String> IMSF,
//			ArrayList<String> IMSB, ArrayList<String> FMSB) {
//		// create a final slice from beginning to end by combining the slices
//		// (IMSF Union IMSB) Intersect FMSB
//		ArrayList<String> methodList = new ArrayList<String>();
//		for (String s : FMSB) {
//			// if(IMSF.contains(s) || IMSB.contains(s)) {
//			methodList.add(s);
//			System.out.println("STUART adding Method : " + s);
//			// }
//		}
//
//	}

	/**
	 * If s is a call statement, return the statement representing the normal
	 * return from s
	 */
	public static Statement getReturnStatementForCall(Statement s) {
		if (s.getKind() == Kind.NORMAL) {
			NormalStatement n = (NormalStatement) s;
			SSAInstruction st = n.getInstruction();
			if (st instanceof SSAInvokeInstruction) {
				SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) st;
				if (call.getCallSite().getDeclaredTarget().getReturnType()
						.equals(TypeReference.Void)) {
					throw new IllegalArgumentException(
							"this driver computes forward slices from the return value of calls.\n"
									+ ""
									+ "Method "
									+ call.getCallSite().getDeclaredTarget()
											.getSignature() + " returns void.");
				}
				return new NormalReturnCaller(s.getNode(),
						n.getInstructionIndex());
			} else {
				return s;
			}
		} else {
			return s;
		}
	}

	/**
	 * check that g is a well-formed graph, and that it contains exactly the
	 * number of nodes in the slice
	 */
	private static void sanityCheck(Collection<Statement> slice,
			Graph<Statement> g) {
		try {
			GraphIntegrity.check(g);
		} catch (UnsoundGraphException e1) {
			e1.printStackTrace();
			Assertions.UNREACHABLE();
		}
		Assertions.productionAssertion(g.getNumberOfNodes() == slice.size(),
				"panic " + g.getNumberOfNodes() + " " + slice.size());
	}

	public static Graph<Statement> pruneSDG(SDG sdg,
			final Collection<Statement> slice) {
		Predicate<Statement> f = new Predicate<Statement>() {
			@Override
			public boolean test(Statement o) {
				return slice.contains(o);
			}
		};
		return GraphSlicer.prune(sdg, f);
	}

	/**
	 * @return a NodeDecorator that decorates statements in a slice for a
	 *         dot-ted representation
	 */
	public static NodeDecorator<Statement> makeNodeDecorator() {
		return new NodeDecorator<Statement>() {
			@Override
			public String getLabel(Statement s) throws WalaException {
				switch (s.getKind()) {
				case HEAP_PARAM_CALLEE:
				case HEAP_PARAM_CALLER:
				case HEAP_RET_CALLEE:
				case HEAP_RET_CALLER:
					HeapStatement h = (HeapStatement) s;
					return s.getKind() + "\\n" + h.getNode() + "\\n"
							+ h.getLocation();
				case NORMAL:
					NormalStatement n = (NormalStatement) s;
					return n.getInstruction() + "\\n"
							+ n.getNode().getMethod().getSignature();
				case PARAM_CALLEE:
					ParamCallee paramCallee = (ParamCallee) s;
					return s.getKind() + " " + paramCallee.getValueNumber()
							+ "\\n" + s.getNode().getMethod().getName();
				case PARAM_CALLER:
					ParamCaller paramCaller = (ParamCaller) s;
					return s.getKind()
							+ " "
							+ paramCaller.getValueNumber()
							+ "\\n"
							+ s.getNode().getMethod().getName()
							+ "\\n"
							+ paramCaller.getInstruction().getCallSite()
									.getDeclaredTarget().getName();
				case EXC_RET_CALLEE:
				case EXC_RET_CALLER:
				case NORMAL_RET_CALLEE:
				case NORMAL_RET_CALLER:
				case PHI:
				default:
					return s.toString();
				}
			}

		};
	}

	private static void ListRemoveScopeClasses(AnalysisScope scope,
			IClassHierarchy cha, ArrayList<String> keepList) {
		ArrayList<String> removeList = new ArrayList<String>();
		for (IClass c : cha) {
			if (!scope.isApplicationLoader(c.getClassLoader()))
				continue;
			String cname = c.getName().toString();
			System.out.println("Class:" + cname);

			for (IMethod m : c.getAllMethods()) {
				String sig = m.getSignature();
				if (!keepList.contains(sig)) {
					removeList.add(sig);// TODO write to file
					if (debugLevel >= 2) {
						System.out.println("Remove " + sig);
					}
				} else {
					if (debugLevel >= 1) {
						System.out.println("Keep " + sig);
					}
				}
			} // method
		} // class
	}

} // end MyCallGraphGen
