package dev.zeith.lzvm.jvm;

import dev.zeith.lzvm.program.LzCallInsn;

public interface LzJcallShutter
{
	LzJcallShutter ALLOW_EVERYTHING = (owner, call) -> true;
	
	boolean permits(String className, LzCallInsn call);
	
	default LzJcallShutter and(LzJcallShutter other)
	{
		LzJcallShutter dis = this;
		return (className, call) -> dis.permits(className, call) && other.permits(className, call);
	}
}