package dev.zeith.lzvm.op;

import lombok.*;

@Getter
@RequiredArgsConstructor
public enum LzBinaryOp
{
	ADD(LzOpcodes.ADD)
			{
				@Override
				public double operate(double left, double right)
				{
					return left + right;
				}
			},
	SUB(LzOpcodes.SUB)
			{
				@Override
				public double operate(double left, double right)
				{
					return left - right;
				}
			},
	MUL(LzOpcodes.MUL)
			{
				@Override
				public double operate(double left, double right)
				{
					return left * right;
				}
			},
	DIV(LzOpcodes.DIV)
			{
				@Override
				public double operate(double left, double right)
				{
					return left / right;
				}
			},
	;
	
	private final int opcode;
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