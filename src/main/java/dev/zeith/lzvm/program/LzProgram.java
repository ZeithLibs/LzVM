package dev.zeith.lzvm.program;

public class LzProgram
{
	public final LzProgramInfo info;
	public final LzProgramBody body;
	
	public LzProgram(LzProgramBody body)
	{
		this(body.computeInfo(), body);
	}
	
	public LzProgram(LzProgramInfo info, LzProgramBody body)
	{
		this.info = info;
		this.body = body;
	}
}