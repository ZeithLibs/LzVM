package dev.zeith.lzvm.program;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ArgType
{
	DOUBLE("d"),
	STRING("s"),
	;
	
	public final String desc;
	
	public static String descriptor(ArgType... argTypes)
	{
		StringBuilder sb = new StringBuilder("(");
		for(ArgType at : argTypes) sb.append("L").append(at.desc).append(";");
		return sb.append(")").append(ArgType.DOUBLE.desc).toString();
	}
}
