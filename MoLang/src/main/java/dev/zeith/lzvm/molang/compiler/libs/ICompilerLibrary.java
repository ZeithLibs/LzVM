package dev.zeith.lzvm.molang.compiler.libs;

import dev.zeith.lzvm.molang.compiler.*;
import dev.zeith.lzvm.molang.expression.*;
import dev.zeith.lzvm.program.*;

import java.util.function.*;

public interface ICompilerLibrary
{
	void register(MoLangCompiler compiler);
	
	default IMoFunctionCallTransformer argsAndExtraPure(Function<FuncCallExpression, MLExpression> optimizer, Consumer<LzProgramBuilder> extra)
	{
		return argsAndExtra(true, extra, optimizer);
	}
	
	default IMoFunctionCallTransformer argsAndExtra(boolean pure, Consumer<LzProgramBuilder> extra, Function<FuncCallExpression, MLExpression> optimizer)
	{
		return new IMoFunctionCallTransformer()
		{
			@Override
			public boolean appendCall(MoLangCompiler compiler, LzProgramBuilder builder, FuncCallExpression expression, LzCallInsn call, ExpressionScope scope)
			{
				for(MLExpression child : expression.getChildren()) child.toLz(compiler, builder, scope);
				extra.accept(builder);
				return true;
			}
			
			@Override
			public MLExpression optimizeCall(MoLangCompiler compiler, FuncCallExpression expression, LzCallInsn call)
			{
				if(optimizer != null)
				{
					MLExpression apply = optimizer.apply(expression);
					if(apply != null) return apply;
				}
				return expression;
			}
			
			@Override
			public boolean isPure()
			{
				return pure;
			}
		};
	}
}