package geo;

import com.google.gson.*;
import dev.zeith.lzvm.LzVariableStore;
import dev.zeith.lzvm.jvm.*;
import dev.zeith.lzvm.molang.compiler.MoLangCompiler;
import dev.zeith.lzvm.op.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class TestBulkBedrockParsing
{
	static final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(Vec3Animation.class, new Vec3Animation.Vec3AnimationDeserializer())
			.create();
	
	public static void main(String[] args)
			throws InterruptedException, ExecutionException
	{
		ExecutorService exec = Executors.newWorkStealingPool();
		
		class ExecDuration
		{
			final String name;
			final long duration;
			final int expressionCount;
			
			ExecDuration(String name, long duration, int expressionCount)
			{
				this.name = name;
				this.duration = duration;
				this.expressionCount = expressionCount;
			}
		}
		
		List<Future<ExecDuration>> latency = new ArrayList<>();
		
		try(Stream<Path> itr = Files.walk(Paths.get(args[0])).filter(Files::isRegularFile).filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")))
		{
			Iterator<Path> it = itr.iterator();
			while(it.hasNext())
			{
				Path path = it.next();
				latency.add(exec.submit(() ->
				{
					String str = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
					long start = System.nanoTime();
					int expressionCount = 0;
					try
					{
						expressionCount = parse(str);
					} catch(Exception e)
					{
						e.printStackTrace();
					}
					return new ExecDuration(path.getFileName().toString(), System.nanoTime() - start, expressionCount);
				}));
			}
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		} finally
		{
			exec.shutdown();
			exec.awaitTermination(1L, TimeUnit.MINUTES);
		}
		
		long totalDuration = 0;
		int totalExpressionCount = 0;
		for(Future<ExecDuration> f : latency)
		{
			ExecDuration ed = f.get();
			totalDuration += ed.duration;
			totalExpressionCount += ed.expressionCount;
		}
		
		System.out.println("Total CPU time: " + TimeUnit.NANOSECONDS.toMillis(totalDuration) + " ms");
		System.out.println("Parsed MoLang expressions (uncached): " + totalExpressionCount + " in " + latency.size() + " files.");
		System.out.println("Average time per file: " + TimeUnit.NANOSECONDS.toMicros(totalDuration / latency.size()) + " micros");
		System.out.println("Average time per expression: " + TimeUnit.NANOSECONDS.toMicros(totalDuration / totalExpressionCount) + " micros");
	}
	
	private static int parse(String str)
			throws IOException
	{
		AnimationFile af = gson.fromJson(str, AnimationFile.class);
		if(Double.isNaN(af.animation_length)) return 0;
		
		MoLangCompiler compiler = new MoLangCompiler();
		LzJvmCompiler jvmCompiler = new LzJvmCompiler();
		
		AtomicInteger counter = new AtomicInteger();
		af.visitMolang(ml ->
		{
			counter.incrementAndGet();
			try
			{
//				compiler.compile(0, compiler.parse(ml));
				LzFactory lzf = compiler.parseFactory(jvmCompiler, ml,  new LzJVM.LzClassLoader());
				LzExpression expr = lzf.instantiate(STORE);
				boolean constant = false;
				if(expr instanceof ConstantExpression) constant = true;
				expr.get();
				System.out.println(ml + " = " + expr.get() + (constant ? " (CONST)" : ""));
			} catch(Exception e)
			{
				e.printStackTrace();
			}
		});
		
		return counter.get();
	}
	
	static final LzVariableStore STORE = new LzVariableStore()
	{
		@Override
		public LzCallOp findCall(String name, String descriptor)
		{
			return LzCallOp.NO_OP;
		}
		
		@Override
		public LzVarOp findVar(String name)
		{
			return LzVarOp.tempVar();
		}
		
		@Override
		public LzVarOp tempVar(String name)
		{
			return LzVarOp.tempVar();
		}
	};
}
