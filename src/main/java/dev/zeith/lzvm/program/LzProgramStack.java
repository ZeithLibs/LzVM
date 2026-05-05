package dev.zeith.lzvm.program;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LzProgramStack
{
	public final double[] stack;
	public final double[] locals;
	public final int inputArgCount;
	
	public LzProgramStack fillArgs(double... args)
	{
		System.arraycopy(args, 0, locals, 0, args.length);
		return this;
	}
}