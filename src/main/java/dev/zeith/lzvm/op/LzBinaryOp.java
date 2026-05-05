package dev.zeith.lzvm.op;

import lombok.*;

import static dev.zeith.lzvm.jvm.LzMath.EPS;

@Getter
@RequiredArgsConstructor
public enum LzBinaryOp
{
	ADD(LzOpcodes.ADD, "+")
			{
				@Override
				public double operate(double left, double right)
				{
					return left + right;
				}
			},
	SUB(LzOpcodes.SUB, "-")
			{
				@Override
				public double operate(double left, double right)
				{
					return left - right;
				}
			},
	MUL(LzOpcodes.MUL, "*")
			{
				@Override
				public double operate(double left, double right)
				{
					return left * right;
				}
			},
	DIV(LzOpcodes.DIV, "/")
			{
				@Override
				public double operate(double left, double right)
				{
					return left / right;
				}
			},
	EQUALS(LzOpcodes.EQUALS, "==")
			{
				@Override
				public double operate(double left, double right)
				{
					return Math.abs(left - right) < EPS ? 1.0 : 0.0;
				}
			},
	NOT_EQUALS(LzOpcodes.NOT_EQUALS, "!=")
			{
				@Override
				public double operate(double left, double right)
				{
					return Math.abs(left - right) > EPS ? 1.0 : 0.0;
				}
			},
	GREATER_THAN(LzOpcodes.GREATER_THAN, ">")
			{
				@Override
				public double operate(double left, double right)
				{
					return left > right ? 1.0 : 0.0;
				}
			},
	GREATER_EQ_THAN(LzOpcodes.GREATER_EQ_THAN, ">=")
			{
				@Override
				public double operate(double left, double right)
				{
					return left > right - EPS ? 1.0 : 0.0;
				}
			},
	LESS_THAN(LzOpcodes.LESS_THAN, "<")
			{
				@Override
				public double operate(double left, double right)
				{
					return left < right ? 1.0 : 0.0;
				}
			},
	LESS_EQ_THAN(LzOpcodes.LESS_EQ_THAN, "<=")
			{
				@Override
				public double operate(double left, double right)
				{
					return left <= right + EPS ? 1.0 : 0.0;
				}
			},
	COALESCE(LzOpcodes.COALESCE, "??")
			{
				@Override
				public double operate(double left, double right)
				{
					return Math.abs(left) > EPS ? left : right;
				}
			},
	;
	
	private final int opcode;
	private final String asString;
	private static final LzBinaryOp[] BY_OP = new LzBinaryOp[LzOpcodes.I_COUNT];
	
	public abstract double operate(double left, double right);
	
	static
	{
		for(LzBinaryOp value : values())
			BY_OP[value.opcode] = value;
	}
	
	public static LzBinaryOp byOpcode(int opcode)
	{
		return BY_OP[opcode];
	}
}