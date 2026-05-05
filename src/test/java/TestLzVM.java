import dev.zeith.lzvm.LzVM;
import dev.zeith.lzvm.jvm.*;
import dev.zeith.lzvm.op.*;
import dev.zeith.lzvm.program.*;

import java.io.File;
import java.nio.file.Files;

public class TestLzVM
{
	public static final LzCallInsn MATH_SIN = LzCallInsn.ofDbl("math.sin", ArgType.DOUBLE);
	public static final LzCallInsn MATH_ATAN2 = LzCallInsn.ofDbl("math.atan2", ArgType.DOUBLE, ArgType.DOUBLE);
	public static final LzCallInsn MATH_ATAN3 = LzCallInsn.ofDbl("math.atan3", ArgType.DOUBLE, ArgType.DOUBLE, ArgType.STRING);
	public static final LzCallInsn TEST_CALL = LzCallInsn.ofDbl("doCall", ArgType.DOUBLE, ArgType.STRING);
	public static final LzCallInsn CONCAT_CALL = LzCallInsn.ofStr("concat", ArgType.STRING, ArgType.STRING);
	
	public static String concat(String a, String b)
	{
		return a + b;
	}
	
	public static double doCall(double val, String str)
	{
		return val + str.length();
	}
	
	public static void main(String[] args)
	{
		String[] sConsts = {"test", "TestLzVM", "TestStringValue"};
		
		LzVM vm = createMoLangVM();
		
		double[] dConsts = {2, 360, 0};
		String[] varTable = {"q.output", "q.input"};
		LzCallInsn[] callTable = {
				MATH_SIN, // 0
				MATH_ATAN2, // 1
				MATH_ATAN3, // 2
				CONCAT_CALL, // 3
				TEST_CALL // 4
		};
		
		double x = 5;
		
		double realValue = LzMath.cosd(LzMath.sind(Math.atan2(360, 360 / (x * 2) * 15)));
		int[] program = new int[] {
				LzOpcodes.CONST, 2, // load dConsts[2]
				
				LzOpcodes.LOAD, 0,
				LzOpcodes.CONST, 0,
				LzOpcodes.MUL,
				LzOpcodes.STORE, 1,
				
				LzOpcodes.LOAD, 1,
				LzOpcodes.CONST, 0,
				LzOpcodes.DIV,
				LzOpcodes.STORE, 2,
				
				LzOpcodes.CONST, 1,
				LzOpcodes.LOAD, 2,
				LzOpcodes.DIV,
				LzOpcodes.READ, 1, // q.input (15)
				LzOpcodes.MUL,
				
				LzOpcodes.CONST, 1,
				LzOpcodes.SCONST, 0,
				LzOpcodes.CALL, 2, // math.atan2
				LzOpcodes.WRITE, 0,
				
				LzOpcodes.READ, 0,
				LzOpcodes.FSIN,
				LzOpcodes.FCOS,
				LzOpcodes.SCONST, 0,
				LzOpcodes.SCONST, 2,
				LzOpcodes.JCALL, 1, 3,
				LzOpcodes.JCALL, 1, 4,
				
				LzOpcodes.COALESCE,
				LzOpcodes.RETURN
		};
		
		program = new int[] {
			LzOpcodes.CONST, 0,
			LzOpcodes.CONST, 1,
			LzOpcodes.ADD,
		};
		
		LzProgram prog = new LzProgram(new LzProgramBody(program, dConsts, sConsts, callTable, varTable));
		
		try
		{
			Files.write(
					new File("run", "TestExpression.class").toPath(),
					LzJvmCompiler.compile(LzExpression.class.getName() + "/TestExpression", prog, 1)
			);
		} catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		LzFactory fac = LzJVM.compile(prog, 1);
		System.out.println(fac);
		
		LzExpression expr = fac.instantiate(vm);
		System.out.println(expr);
		
		LzProgramStack pStack = prog.info
				.mallocStack(1)
				.fillArgs(x);
		
		System.out.println("Coldboot Eval: " + vm.eval(prog, pStack));
		System.out.println("Coldboot Java: " + expr.get(x));
		
		for(int i = 0; i < 8192; i++) vm.eval(prog, pStack);
		for(int i = 0; i < 8192; i++) expr.get(x);
		
		System.out.println(prog.body.disassemble(true));
		
		System.out.println();
		System.out.println("Real: " + realValue);
		System.out.println();
		
		System.out.println();
		System.out.println("Eval: " + vm.eval(prog, pStack));
		System.out.println();
		
		benchmark(() -> vm.eval(prog, pStack));
		
		System.out.println();
		System.out.println("Java: " + expr.get(x));
		System.out.println();
		
		benchmark(() -> expr.get(x));
	}
	
	private static void benchmark(Runnable task)
	{
		int runs = 1000000;
		long avg = 0;
		long max = Long.MIN_VALUE;
		long min = Long.MAX_VALUE;
		
		long start;
		int instantComputes = 0;
		
		for(int i = 0; i < runs; i++)
		{
			start = System.nanoTime();
			task.run();
			long dur = System.nanoTime() - start;
			
			if(dur == 0L) ++instantComputes;
			min = Math.min(min, dur);
			
			max = Math.max(max, dur);
			avg += dur;
		}
		
		long nanos = avg / runs;
		System.out.println("AVERAGE: " + nanos + " ns");
		System.out.println("MIN: " + min + " ns");
		System.out.println("MAX: " + max + " ns");
		System.out.println("INSTANT (0 ns): " + instantComputes + " / " + runs + " RUNS (" + ((instantComputes * 1000L / runs) / 10D) + "%)");
	}
	
	private static LzVM createMoLangVM()
	{
		LzVM vm = new LzVM();
		
		vm.registerCall(MATH_SIN,
				moArgs -> Math.sin(Math.toRadians((double) moArgs[0]))
		);
		
		vm.registerCall(MATH_ATAN2,
				moArgs -> Math.atan2((double) moArgs[0], (double) moArgs[1])
		);
		
		vm.registerCall(MATH_ATAN3,
				moArgs -> Math.atan2((double) moArgs[0], (double) moArgs[1])
		);
		
		vm.registerVar("q.input", LzVarOp.readOnly(() -> 15));
		vm.registerVar("q.output", LzVarOp.readWrite());
		
		return vm;
	}
}