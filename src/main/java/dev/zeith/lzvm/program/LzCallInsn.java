package dev.zeith.lzvm.program;

import lombok.*;

@EqualsAndHashCode
@AllArgsConstructor
public class LzCallInsn
{
	public final String name;
	public final ArgType[] stackTypes; // These are reverse, used for JVM compilation.
	public final int argCount;
	
	public final String descriptor;
	
	public LzCallInsn(String name, ArgType... argTypes)
	{
		this.name = name;
		int argCount = argTypes.length;
		this.argCount = argCount;
		this.stackTypes = new ArgType[argCount];
		for(int i = 0; i < argCount; i++) this.stackTypes[i] = argTypes[argCount - 1 - i];
		this.descriptor = ArgType.descriptor(argTypes);
	}
}