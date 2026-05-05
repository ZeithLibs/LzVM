import dev.zeith.lzvm.LzVM;
import dev.zeith.lzvm.jvm.*;
import dev.zeith.lzvm.op.*;
import dev.zeith.lzvm.program.*;

import java.io.File;
import java.nio.file.Files;

public class TestLzVM
{
	public static void main(String[] args)
	{
		String[] sConsts = { };
		
		LzVM vm = createMoLangVM();
		
		double[] dConsts = {2, 360};
		String[] varTable = {"q.output", "q.input"};
		LzCallInsn[] callTable = {
				new LzCallInsn("math.sin", ArgType.DOUBLE),
				new LzCallInsn("math.atan2", ArgType.DOUBLE, ArgType.DOUBLE)
		};
		
		double x = 5;
		double[] dArgs = new double[] {x};
		
		double realValue = Math.atan2(360, 360 / (x * 2) * 15);
		int[] program = new int[] {
				LzOpcodes.LOAD, 0,
				LzOpcodes.CONST, 0,
				LzOpcodes.MUL,
				LzOpcodes.STORE, 1,
				
				LzOpcodes.CONST, 1,
				LzOpcodes.LOAD, 1,
				LzOpcodes.DIV,
				LzOpcodes.READ, 1, // q.input (15)
				LzOpcodes.MUL,
				
				LzOpcodes.CONST, 1,
				LzOpcodes.CALL, 1, // math.atan2
				LzOpcodes.WRITE, 0,
				
				LzOpcodes.READ, 0,
				LzOpcodes.RETURN
		};
		
		LzProgram moprog = new LzProgram(new LzProgramBody(program, dConsts, sConsts, callTable, varTable));
		LzFactory fac = LzJVM.compile(moprog, 1);
		LzExpression expr = fac.instantiate(vm);
		
		try
		{
			Files.write(
					new File("run", "TestExpression.class").toPath(),
					LzJvmCompiler.compile(LzExpression.class.getName() + "/TestExpression", moprog, 1)
			);
		} catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		LzProgramStack pStack = moprog.info
				.mallocStack(1)
				.fillArgs(dArgs);
		
		for(int i = 0; i < 8192; i++) vm.eval(moprog, pStack);
		for(int i = 0; i < 8192; i++) expr.get(dArgs);
		
		System.out.println(LzOpcodes.disassemble(program, true));
		
		System.out.println();
		System.out.println("Real: " + realValue);
		System.out.println();
		
		System.out.println();
		System.out.println("Eval: " + vm.eval(moprog, pStack));
		System.out.println();
		
		benchmark(() -> vm.eval(moprog, pStack));
		
		System.out.println();
		System.out.println("Java: " + expr.get(dArgs));
		System.out.println();
		
		benchmark(() -> expr.get(dArgs));
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
		
		vm.registerCall(new LzCallInsn("math.sin", ArgType.DOUBLE),
				moArgs -> Math.sin(Math.toRadians((double) moArgs[0]))
		);
		
		vm.registerCall(new LzCallInsn("math.atan2", ArgType.DOUBLE, ArgType.DOUBLE),
				moArgs -> Math.atan2((double) moArgs[0], (double) moArgs[1])
		);
		
		vm.registerVar("q.input", LzVarOp.readOnly(() -> 15));
		vm.registerVar("q.output", LzVarOp.readWrite());
		
		return vm;
	}
}