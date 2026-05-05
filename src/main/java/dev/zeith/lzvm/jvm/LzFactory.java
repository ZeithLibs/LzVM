package dev.zeith.lzvm.jvm;

import dev.zeith.lzvm.LzVariableStore;

public interface LzFactory
{
	LzExpression instantiate(LzVariableStore store);
	
	default boolean isGenerated()
	{
		return getClass().isAnnotationPresent(Generated.class);
	}
	
	default String getGeneratedInstructionSet()
	{
		Generated gen = getClass().getDeclaredAnnotation(Generated.class);
		return gen != null ? gen.value() : null;
	}
}