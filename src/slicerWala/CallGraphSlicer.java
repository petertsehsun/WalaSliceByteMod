package slicerWala;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import savedExamples.MyCallGraphGen;
import byteCodeMod.ClassFileModifier;
import byteCodeMod.Method_Signature;

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
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.warnings.Warnings;

public class CallGraphSlicer {
	static int debugLevel = 6;
	long start; 
	long end ; 

	public void calcSlice(String scopeFile, String excluFile, String mainClass,
			String caller, String callee, String cPath, ArrayList<String> keepMList) throws IOException,
			ClassHierarchyException, IllegalArgumentException,
			CallGraphBuilderCancelException, NotFoundException,
			CannotCompileException {
		start = System.currentTimeMillis();

		File exclusionsFile = new File(excluFile);
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
		Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha,
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

		end = System.currentTimeMillis();
		System.out.println("done");
		System.out.println("Time to build Call Graph " + (end - start) + "ms");
		System.out.println(CallGraphStats.getStats(cg));
		if (debugLevel >= 4) {
			for (int i = 0; i < cg.getNumberOfNodes(); i++) {
				CGNode n = cg.getNode(i);
				System.out.println("NODE SIG " + n.getMethod().getSignature());
			}
		
			System.out.println(cg.toString());
		}
		start = System.currentTimeMillis();
		ArrayList<String> keepMethodList = calcSlice(builder, cg, caller,
				callee);
		// add to the keep list
		for(String s: keepMList) {
			keepMethodList.add(s);
		}
		if(debugLevel >= 1) {
		for(String s: keepMethodList) {
			System.out.println("KEEP: "+s.toString());
		}
		}
		end = System.currentTimeMillis();
		System.out.println("Time to Calc Slice " + (end - start) + "ms");

		start = System.currentTimeMillis();
		ListRemoveScopeClasses(scope, cha, keepMethodList, cPath);
		end = System.currentTimeMillis();
		System.out.println("Time to Modifiy Byte Code " + (end - start) + "ms");
	}

	private ArrayList<String> calcSlice(CallGraphBuilder builder, CallGraph cg,
			String srcCaller, String srcCallee) throws IOException {
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
		// String srcCaller = "";
		// String srcCallee = "";
		long l_start = System.currentTimeMillis();
	

		try {
			finalMethodSlice = makeSDG(cg, builder, srcCaller, srcCallee,
					goBackwards, dOptions, cOptions);
		} catch (CancelException e) {
			System.err
					.println("Failed to find slice backward for Final Method");
			e.printStackTrace();
		}
		long l_end = System.currentTimeMillis();
		System.out.println("Time to create SDG " + (l_end - l_start) + "ms");

		return finalMethodSlice;
	}

	private void printScopeClasses(AnalysisScope scope, IClassHierarchy cha) {
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

	public ArrayList<String> makeSDG(CallGraph cg, CallGraphBuilder builder,
			String srcCaller, String srcCallee, boolean goBackward,
			DataDependenceOptions dOptions, ControlDependenceOptions cOptions)
			throws IllegalArgumentException, CancelException, IOException {
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
			//n.getDU();
			IMethod m = n.getMethod();
			String ss = m.getSignature();
			if (!ss.contains("com.ibm.wala.FakeRootClass")) {
				if (!MethodList.contains(ss)) {
					if (debugLevel >= 4) {
						System.out.println("STUART adding method "+ss);
					}
					MethodList.add(ss);
				}
			}
		}
		return MethodList;

	} // makeSDG

	/**
	 * If s is a call statement, return the statement representing the normal
	 * return from s
	 */
	private Statement getReturnStatementForCall(Statement s) {
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
	private void sanityCheck(Collection<Statement> slice, Graph<Statement> g) {
		try {
			GraphIntegrity.check(g);
		} catch (UnsoundGraphException e1) {
			e1.printStackTrace();
			Assertions.UNREACHABLE();
		}
		Assertions.productionAssertion(g.getNumberOfNodes() == slice.size(),
				"panic " + g.getNumberOfNodes() + " " + slice.size());
	}

	private Graph<Statement> pruneSDG(SDG sdg, final Collection<Statement> slice) {
		Predicate<Statement> f = new Predicate<Statement>() {
			@Override
			public boolean test(Statement o) {
				return slice.contains(o);
			}
		};
		return GraphSlicer.prune(sdg, f);
	}

	private void ListRemoveScopeClasses(AnalysisScope scope,
			IClassHierarchy cha, ArrayList<String> keepList, String cPath)
			throws NotFoundException, CannotCompileException, IOException {
		ArrayList<String> removeList = new ArrayList<String>();
		ClassFileModifier cfm = new ClassFileModifier();

		for (IClass c : cha) {
			ArrayList<Method_Signature> MethodSig = new ArrayList<Method_Signature>();

			if (!scope.isApplicationLoader(c.getClassLoader()))
				continue;
			String cname = c.getName().toString();
			//System.out.println("Class:" + cname);
			String cc = cname.replaceFirst("L", "");
			String fixCName = cc.replace("/", ".");
//STUART
			for (IMethod m : c.getAllMethods()) {
				String sig = m.getSignature();
				String desc = m.getDescriptor().toString();
				String name = m.getName().toString();

				if (!keepList.contains(sig) && sig.startsWith(fixCName)) {
					// don't add java library stuff
					if (!(cname.startsWith("Ljava") || name.contains("<init>") || name
							.contains("<clinit>") || sig.startsWith("java.lang.Object"))) {
						removeList.add(sig);
						MethodSig.add(new Method_Signature(name, desc));
						// System.out.println("mclas = " + fixCName +
						// " mName = " + name
						// + " mSig = " + desc);

						if (debugLevel >= 2) {
							System.out.println("Remove " + sig);
						}
					}
				} else {
					if (debugLevel >= 1) {
						// System.out.println("Keep " + sig);
					}
				}
			} // method
			if (!MethodSig.isEmpty()) {
				cfm.findModMethod(cPath, fixCName, MethodSig);
			}
		} // class
	}

}
