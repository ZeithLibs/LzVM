package dev.zeith.lzvm.jvm;

import dev.zeith.lzvm.LzVariableStore;

public interface LzFactory
{
	LzExpression instantiate(LzVariableStore store);
}