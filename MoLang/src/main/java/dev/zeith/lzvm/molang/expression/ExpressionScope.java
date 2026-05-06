package dev.zeith.lzvm.molang.expression;

import dev.zeith.lzvm.program.LzLabel;
import lombok.*;

@Value
@With
public class ExpressionScope
{
	public static final ExpressionScope EMPTY = new ExpressionScope(null);
	
	LzLabel loopExit;
}