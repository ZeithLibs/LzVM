package dev.zeith.lzvm.op;

public interface LzOpcodeVisitor
{
	void visitInstruction(int opcode, Object[] args);
}