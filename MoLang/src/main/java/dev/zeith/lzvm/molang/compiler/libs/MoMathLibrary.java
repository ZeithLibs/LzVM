package dev.zeith.lzvm.molang.compiler.libs;

import dev.zeith.lzvm.jvm.LzMath;
import dev.zeith.lzvm.molang.compiler.MoLangCompiler;
import dev.zeith.lzvm.molang.expression.*;
import dev.zeith.lzvm.op.LzOpcodes;

import java.util.OptionalDouble;
import java.util.function.*;

import static dev.zeith.lzvm.molang.compiler.MoLangCompiler.*;

public enum MoMathLibrary
		implements ICompilerLibrary
{
	INSTANCE;
	
	@Override
	public void register(MoLangCompiler c)
	{
		c.registerTransformer(doubleUnaryOperator("math.sin"),
				argsAndExtraPure(
						doubleOptimizer(val -> LzMath.sind(val * LzMath.DEG_TO_RAD)),
						builder -> builder.addConstD(LzMath.DEG_TO_RAD).addInsn(LzOpcodes.MUL).addInsn(LzOpcodes.FSIN)
				)
		);
		c.registerTransformer(doubleUnaryOperator("math.cos"),
				argsAndExtraPure(
						doubleOptimizer(val -> LzMath.sind(val * LzMath.DEG_TO_RAD)),
						builder -> builder.addConstD(LzMath.DEG_TO_RAD).addInsn(LzOpcodes.MUL).addInsn(LzOpcodes.FCOS)
				)
		);
		c.registerTransformer(doubleBinaryOperator("math.mod"),
				argsAndExtraPure(
						doubleBinaryOptimizer((left, right) -> left % right),
						builder -> builder.addInsn(LzOpcodes.MOD)
				)
		);
	}
	
	private Function<FuncCallExpression, MLExpression> doubleOptimizer(DoubleUnaryOperator operator)
	{
		return call ->
		{
			OptionalDouble opt = call.getChildren()[0].asOptimizedDouble();
			if(opt.isPresent()) return new NumberExpression(operator.applyAsDouble(opt.getAsDouble()));
			return null;
		};
	}
	
	private Function<FuncCallExpression, MLExpression> doubleBinaryOptimizer(DoubleBinaryOperator operator)
	{
		return call ->
		{
			OptionalDouble opt1 = call.getChildren()[0].asOptimizedDouble();
			OptionalDouble opt2 = call.getChildren()[1].asOptimizedDouble();
			if(opt1.isPresent() && opt2.isPresent()) return new NumberExpression(operator.applyAsDouble(opt1.getAsDouble(), opt2.getAsDouble()));
			return null;
		};
	}
}