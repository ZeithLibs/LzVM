package dev.zeith.lzvm.molang.expression;

import dev.zeith.lzvm.molang.compiler.MoLangCompiler;
import dev.zeith.lzvm.program.*;
import lombok.ToString;

@ToString
public class NameExpression
		extends MLExpression
{
	public final String name;
	
	public NameExpression(String name)
	{
		super(0);
		this.name = name;
	}
	
	@Override
	protected boolean isOptimized()
	{
		return false;
	}
	
	@Override
	protected Object evalStatic()
	{
		return null;
	}
	
	@Override
	public void toLz(MoLangCompiler compiler, LzProgramBuilder builder, ExpressionScope scope)
	{
		builder.addRead(this.name);
	}
	
	@Override
	public ArgType getExpectedLzType()
	{
		return ArgType.DOUBLE;
	}
}