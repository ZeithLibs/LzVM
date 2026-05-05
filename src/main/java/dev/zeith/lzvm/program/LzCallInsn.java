package dev.zeith.lzvm.program;

import lombok.*;

@EqualsAndHashCode
@AllArgsConstructor
public class LzCallInsn
{
	public final String name;
	public final ArgType[] argTypes;
	public final int argCount;
	
	public LzCallInsn(String name, ArgType... argTypes)
	{
		this.name = name;
		this.argTypes = argTypes;
		this.argCount = argTypes.length;
	}
	
	public String descriptor()
	{
		StringBuilder sb = new StringBuilder("(");
		for(ArgType at : argTypes) sb.append("L").append(at.desc).append(";");
		return sb.append(")").append(ArgType.DOUBLE.desc).toString();
	}
}