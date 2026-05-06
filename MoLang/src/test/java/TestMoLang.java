import dev.zeith.lzvm.LzVM;
import dev.zeith.lzvm.jvm.*;
import dev.zeith.lzvm.molang.compiler.MoLangCompiler;
import dev.zeith.lzvm.molang.compiler.libs.MoMathLibrary;
import dev.zeith.lzvm.op.LzVarOp;
import dev.zeith.lzvm.program.LzProgramBody;

import java.io.File;
import java.nio.file.Files;

public class TestMoLang
{
	public static void main(String[] args)
			throws InterruptedException
	{
		final String expression = "v.test = 0; loop(10, {\n\tv.test = v.test + 1;\n(v.test >= 5 ? break)\n}); v.test";
		
		LzVM vm = new LzVM();
		double[] time = new double[1];
		vm.registerVar("query.anim_time", LzVarOp.readOnly(() -> time[0]));
		vm.registerVar("variable.test", LzVarOp.readWrite());
		vm.registerVar("variable.test2", LzVarOp.readWrite());
		
		MoLangCompiler compiler = new MoLangCompiler();
		compiler.linkLibrary(MoMathLibrary.INSTANCE);
		
		LzProgramBody body = compiler.compile(0, compiler.parse(expression, true));
		
		LzJvmCompiler jvmc = new LzJvmCompiler();
		jvmc.generatedAnnotation = false;
		
		System.out.println(body.disassemble(true));
		try
		{
			Files.write(
					new File("run", "TestMoExpression.class").toPath(),
					jvmc.compile(LzExpression.class.getName() + "/TestMoExpression", body, 1)
			);
		} catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		jvmc.generatedAnnotation = false;
		
		LzFactory fact = compiler.parseFactory(jvmc, expression, 0, new LzJVM.LzClassLoader());
		LzExpression expr = fact.instantiate(vm);
		
		long start = System.currentTimeMillis();
		double lastLog = -10;
		
		while(true)
		{
			double elapsed = (System.currentTimeMillis() - start) / 1000D;
			if(elapsed > 5) break;
			double val = expr.get();
			time[0] = elapsed;
			if(elapsed - lastLog >= 0.25)
			{
				System.out.println("Time: " + String.format("%.04f", elapsed) + "\tValue: " + String.format("%.04f", val));
				lastLog = elapsed;
			}
		}
	}
}