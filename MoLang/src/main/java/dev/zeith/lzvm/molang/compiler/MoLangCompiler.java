package dev.zeith.lzvm.molang.compiler;

import dev.zeith.lzvm.jvm.*;
import dev.zeith.lzvm.molang.compiler.libs.*;
import dev.zeith.lzvm.molang.expression.*;
import dev.zeith.lzvm.molang.parser.MoParser;
import dev.zeith.lzvm.molang.tokenizer.Tokenizer;
import dev.zeith.lzvm.op.LzOpcodes;
import dev.zeith.lzvm.program.*;

import java.util.*;

public class MoLangCompiler
{
	private static final ThreadLocal<Tokenizer> TOKENIZER = ThreadLocal.withInitial(Tokenizer::new);
	
	protected final Map<LzCallInsn, IMoFunctionCallTransformer> transformers = new HashMap<>();
	protected final Map<String, String> aliases = new HashMap<>();
	
	public MoLangCompiler()
	{
		linkLibrary(MoMathLibrary.INSTANCE);
		registerAlias("q", "query");
		registerAlias("v", "variable");
		registerAlias("t", "temp");
		registerAlias("c", "context");
	}
	
	public static LzCallInsn doubleUnaryOperator(String name)
	{
		return new LzCallInsn(name, ArgType.DOUBLE, ArgType.DOUBLE);
	}
	
	public static LzCallInsn doubleBinaryOperator(String name)
	{
		return new LzCallInsn(name, ArgType.DOUBLE, ArgType.DOUBLE, ArgType.DOUBLE);
	}
	
	public static LzCallInsn doubleTernaryOperator(String name)
	{
		return new LzCallInsn(name, ArgType.DOUBLE, ArgType.DOUBLE, ArgType.DOUBLE);
	}
	
	public MoLangCompiler registerAlias(String alias, String origin)
	{
		this.aliases.put(alias, origin);
		return this;
	}
	
	public MoLangCompiler linkLibrary(ICompilerLibrary library)
	{
		library.register(this);
		return this;
	}
	
	public MoLangCompiler registerTransformer(LzCallInsn call, IMoFunctionCallTransformer transformer)
	{
		transformers.put(call, transformer);
		return this;
	}
	
	public IMoFunctionCallTransformer findTransformer(LzCallInsn call)
	{
		return transformers.get(call);
	}
	
	public LzFactory parseFactory(LzJvmCompiler compiler, String expression, int inArgs, IClassDefiner definer)
	{
		ArrayList<MLExpression> parsed = parse(expression, true);
		if(parsed.size() == 1)
		{
			OptionalDouble exp = parsed.get(0).asOptimizedDouble();
			if(exp.isPresent())
				return new LzExpression.ConstantExpression(exp.getAsDouble());
		}
		return LzJVM.compile(compiler, compile(inArgs, parsed), inArgs, definer);
	}
	
	public LzProgramBody compile(int inArgs, ArrayList<MLExpression> expression)
	{
		LzProgramBuilder pb = LzProgramBuilder.of(inArgs);
		for(MLExpression expr : expression) expr.toLz(this, pb, ExpressionScope.EMPTY);
		pb.addInsn(LzOpcodes.RETURN);
		return pb.build();
	}
	
	public ArrayList<MLExpression> parse(String expression, boolean optimize)
	{
		Tokenizer tkn = TOKENIZER.get();
		tkn.init(expression);
		MoParser p = new MoParser(this.aliases, tkn);
		ArrayList<MLExpression> exprs = p.parse();
		
		if(optimize)
			for(int i = 0, len = exprs.size(); i < len; i++)
			{
				MLExpression expr = exprs.get(i);
				MLExpression opt = expr.optimizeStatic(this);
				if(opt != expr) exprs.set(i, opt);
			}
		
		return exprs;
	}
}