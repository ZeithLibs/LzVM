import dev.zeith.lzvm.jvm.*;
import dev.zeith.lzvm.molang.compiler.MoLangCompiler;
import dev.zeith.lzvm.program.LzProgramBody;
import dev.zeith.lzvm.vm.SimpleLzVariableStore;

public class SimpleMoLangTest
{
	public static void main(String[] args)
	{
		final MoLangCompiler molang = new MoLangCompiler();
		final LzJvmCompiler jvm = new LzJvmCompiler();
		
		// Step 1
		LzProgramBody body = molang.parseAndCompile("math.sin(q.anim_time * 90) * 10");
		
		// Step 2
		LzFactory fac = jvm.expression(body);
		
		long start = System.currentTimeMillis();
		
		// Step 3
		SimpleLzVariableStore vars = new SimpleLzVariableStore();
		vars.registerReadVar("query.anim_time", () -> (System.currentTimeMillis() - start) % 100000L / 1000D);
		
		// Step 4
		LzExpression expr = fac.instantiate(vars);
		
		// Step 5
		for(int i = 0; i < 100; i++)
		{
			System.out.println(expr.get());
			try {Thread.sleep(50L);} catch(InterruptedException ignored) {}
		}
	}
}