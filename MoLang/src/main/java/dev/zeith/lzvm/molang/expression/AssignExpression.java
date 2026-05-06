package dev.zeith.lzvm.molang.expression;

import dev.zeith.lzvm.molang.compiler.MoLangCompiler;
import dev.zeith.lzvm.program.*;

public class AssignExpression
		extends MLExpression
{
	protected final NameExpression name;
	
	public AssignExpression(NameExpression name, MLExpression value)
	{
		super(1);
		this.name = name;
		this.children[0] = value;
	}
	
	@Override
	protected Object evalStatic()
	{
		return null;
	}
	
	@Override
	public void toLz(MoLangCompiler compiler, LzProgramBuilder builder, ExpressionScope scope)
	{
		// Push the value onto the stack
		this.children[0].toLz(compiler, builder, scope);
		
		// Write value onto the stack
		builder.addWrite(name.name);
	}
	
	@Override
	public ArgType getExpectedLzType()
	{
		return null;
	}
}