package motest.util;

import dev.zeith.lzvm.LzVariableStore;
import dev.zeith.lzvm.jvm.*;
import dev.zeith.lzvm.molang.compiler.MoLangCompiler;
import dev.zeith.lzvm.op.*;
import dev.zeith.lzvm.program.*;
import dev.zeith.lzvm.vm.LzVM;

import java.util.*;

import static dev.zeith.lzvm.jvm.LzMath.EPS;

public class LzTestRunner
{
	private static final LzJvmCompiler JVM_COMPILER = new LzJvmCompiler();
	private static final LzVM VM = new LzVM();
	private static final MoLangCompiler MLPP_COMPILER = new MoLangCompiler();
	private static final MoLangCompiler MLPP_COMPILER_UNOPT = new MoLangCompiler();
	
	static
	{
		MLPP_COMPILER_UNOPT.optimize = false;
		VM.setUseSineLookupTable(false);
		JVM_COMPILER.setUseSineLookupTable(false);
	}
	
	public static void runTrue(String expression)
	{
		DummyVariableStore vars = new DummyVariableStore();
		double res = run(vars, expression);
		if(!LzMath.isOne(res))
			throw new AssertionError(expression + " expected to be true(1), but got " + res + ";\nVariables: " + vars);
	}
	
	public static void runLog(String expression)
	{
		DummyVariableStore vars = new DummyVariableStore();
		double res = run(vars, expression);
		System.out.println(expression + " = " + String.format("%0,2f", res));
		if(!vars.vars.isEmpty())
			System.out.println(" Variables: " + vars);
	}
	
	public static double run(String expression)
	{
		return run(new DummyVariableStore(), expression);
	}
	
	public static double run(LzVariableStore vars, String expression)
	{
		LzProgramBody[] programs = new LzProgramBody[2];
		
		programs[0] = MLPP_COMPILER.parseAndCompile(expression);
		programs[1] = MLPP_COMPILER_UNOPT.parseAndCompile(expression);
		
		double[][] runValues = new double[2][2];
		
		for(int i = 0, len = programs.length; i < len; i++)
		{
			LzProgramBody prog = programs[i];
			double jvm = JVM_COMPILER.expression(prog).instantiate(vars).get();
			double interp = VM.expression(prog).instantiate(vars).get();
			runValues[i][0] = jvm;
			runValues[i][1] = interp;
		}
		
		double jvm = runValues[0][0];
		double interp = runValues[0][1];
		if(Math.abs(jvm - interp) > EPS) throw new RuntimeException("[Optimized] JVM(" + jvm + ") and Interpreted(" + interp + ") values do not match: " + expression + "\nVariables: " + vars);
		
		jvm = runValues[1][0];
		interp = runValues[1][1];
		if(Math.abs(jvm - interp) > EPS) throw new RuntimeException("[UnOptimized] JVM(" + jvm + ") and Interpreted(" + interp + ") values do not match: " + expression + "\nVariables: " + vars);
		
		jvm = runValues[0][0];
		double jvm2 = runValues[1][0];
		if(Math.abs(jvm - jvm2) > EPS) throw new RuntimeException("Optimized(" + jvm + ") and UnOptimized(" + jvm2 + ") values do not match: " + expression + "\nVariables: " + vars);
		
		return jvm;
	}
	
	public static class DummyVariableStore
			implements LzVariableStore
	{
		protected final Map<String, LzVarOp> vars = new HashMap<String, LzVarOp>();
		
		@Override
		public LzCallOp findCall(String name, String descriptor)
		{
			return LzCallOp.NO_OP;
		}
		
		@Override
		public LzVarOp findVar(String name)
		{
			return vars.computeIfAbsent(name, s -> LzVarOp.readWrite());
		}
		
		@Override
		public LzVarOp tempVar(String name)
		{
			return findVar(name);
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for(Map.Entry<String, LzVarOp> var : vars.entrySet())
				sb.append("\n\t").append(var.getKey()).append(" = ").append(var.getValue());
			return sb.toString();
		}
	}
}