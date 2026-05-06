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
	
	private static LzProgramBody gen1()
	{
		return LzProgramBuilder
				.of()
				.addConstD(0)
				.addLoad(0)
				.addConstD(2)
				.addInsn(LzOpcodes.MUL)
				.addStore(1)
				.addLoad(1)
				.addConstD(2)
				.addInsn(LzOpcodes.DIV)
				.addStore(2)
				.addConstD(360)
				.addLoad(2)
				.addInsn(LzOpcodes.DIV)
				.addRead("q.input")
				.addInsn(LzOpcodes.MUL)
				.addConstD(360)
				.addConstS("test")
				.addCall(MATH_ATAN3)
				.addWrite("q.output")
				.addRead("q.output")
				.addInsn(LzOpcodes.FSIN)
				.addInsn(LzOpcodes.FCOS)
				.addConstS("TestStringValue")
				.addConstS("test")
				.addJCall("TestLzVM", CONCAT_CALL)
				.addJCall("TestLzVM", TEST_CALL)
				.addInsn(LzOpcodes.COALESCE)
				.addInsn(LzOpcodes.RETURN)
				.build();
	}
	
	private static LzProgramBody conditionalGen()
	{
		LzLabel jumpTarget = new LzLabel();
		return LzProgramBuilder
				.of()
				
				// simple min(x, 5) implementation
				
				.addLoad(0).addConstD(5).addInsn(LzOpcodes.GREATER_THAN)
				.addInsn(LzOpcodes.NOT)
				.addJumpIfFalse(jumpTarget)
				// if x > 5 then execute this block
				.addConstD(5)
				.addInsn(LzOpcodes.RETURN)
				.addLabel(jumpTarget)
				//
				
				.addLoad(0)
				.addInsn(LzOpcodes.RETURN)
				.build();
	}
	
	public static void main(String[] args)
	{
		LzVM vm = createMoLangVM();
		
		double x = 8;
		
		LzProgramBody genProg = conditionalGen();
		
		try
		{
			Files.write(
					new File("run", "TestExpression.class").toPath(),
					LzJvmCompiler.compile(LzExpression.class.getName() + "/TestExpression", genProg, 1)
			);
		} catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		LzFactory fac = LzJVM.compile(genProg, 1);
		System.out.println(fac);
		
		LzExpression expr = fac.instantiate(vm);
		System.out.println(expr);
		
		LzProgram prog = new LzProgram(genProg);
		LzProgramStack pStack = prog.info
				.mallocStack(1)
				.fillArgs(x);
		
		System.out.println("Coldboot Eval: " + vm.eval(prog, pStack));
		System.out.println("Coldboot Java: " + expr.get(x));
		
		for(int i = 0; i < 8192; i++) vm.eval(prog, pStack);
		for(int i = 0; i < 8192; i++) expr.get(x);
		
		System.out.println(prog.body.disassemble(true));
		
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