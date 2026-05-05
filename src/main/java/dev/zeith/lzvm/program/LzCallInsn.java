package dev.zeith.lzvm.program;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class LzCallInsn
{
	public final String name;
	public final ArgType[] argTypes; // These are reverse, used for JVM compilation.
	public final int argCount;
	
	public final String descriptor;
	public final String jvmDescriptor;
	
	public final ArgType returnType;
	
	public LzCallInsn(String name, ArgType returnType, ArgType... argTypes)
	{
		this.name = name;
		this.returnType = returnType;
		this.argTypes = argTypes;
		this.argCount = argTypes.length;
		this.descriptor = ArgType.descriptor(returnType, argTypes);
		this.jvmDescriptor = ArgType.jvmDescriptor(returnType, argTypes);
	}
	
	public static LzCallInsn ofDbl(String name, ArgType... argTypes)
	{
		return of(name, ArgType.DOUBLE, argTypes);
	}
	
	public static LzCallInsn ofStr(String name, ArgType... argTypes)
	{
		return of(name, ArgType.STRING, argTypes);
	}
	
	public static LzCallInsn of(String name, ArgType returnType, ArgType... argTypes)
	{
		return new LzCallInsn(name, returnType, argTypes);
	}
}