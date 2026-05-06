package dev.zeith.lzvm;

import dev.zeith.lzvm.op.*;

public interface LzVariableStore
{
	LzCallOp findCall(String name, String descriptor);
	
	LzVarOp findVar(String name);
	
	LzVarOp tempVar(String name);
}