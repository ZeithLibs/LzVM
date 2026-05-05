package dev.zeith.lzvm.program;

import dev.zeith.lzvm.StringQuoter;
import dev.zeith.lzvm.op.LzOpcodes;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LzProgramBody
{
	public final int[] insnList;
	public final double[] dConstTable;
	public final String[] sConstTable;
	public final LzCallInsn[] callTable;
	public final String[] varTable;
	
	public String disassemble(boolean newlines)
	{
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < insnList.length; i++)
		{
			int ins = insnList[i];
			int extra = LzOpcodes.EXTRA_SHIFTS[ins];
			sb.append(LzOpcodes.NAME_OF.get(ins));
			for(int j = 0; j < extra; j++)
			{
				int v = insnList[++i];
				sb.append(", ").append(getConstantAsString(ins, j, v));
			}
			if(i + 1 < insnList.length) sb.append(newlines ? "\n" : "; ");
		}
		
		return sb.toString();
	}
	
	public String getConstantAsString(int insn, int ordinal, int value)
	{
		Object c = getConstant(insn, ordinal, value);
		if(c instanceof Number) return c.toString();
		if(c instanceof String) return StringQuoter.quote(c.toString());
		if(c instanceof LzCallInsn)
		{
			LzCallInsn call = (LzCallInsn) c;
			return call.name + call.descriptor;
		}
		return c == null ? "null" : c.toString();
	}
	
	public Object getConstant(int insn, int ordinal, int value)
	{
		switch(insn)
		{
			case LzOpcodes.CONST:
				return dConstTable[value];
			case LzOpcodes.CALL:
				return callTable[value];
			case LzOpcodes.READ:
			case LzOpcodes.WRITE:
				return varTable[value];
			case LzOpcodes.SCONST:
				return sConstTable[value];
		}
		return value;
	}
	
	public LzProgramInfo computeInfo()
	{
		int[] insn = this.insnList;
		
		int ptr = -1;
		int maxStackPos = -1;
		int maxLocal = -1;
		int labelCount = 0;
		
		for(int i = 0, len = insn.length; i < len; i++)
		{
			int ins = insn[i];
			i += LzOpcodes.EXTRA_SHIFTS[ins];
			int shift = LzOpcodes.STACK_SHIFT[ins];
			switch(ins)
			{
				case LzOpcodes.LABEL:
					++labelCount;
					break;
				case LzOpcodes.LOAD:
					maxLocal = Math.max(maxLocal, insn[i]);
					break;
				case LzOpcodes.STORE:
					maxLocal = Math.max(maxLocal, insn[i]);
					break;
				case LzOpcodes.CALL:
				{
					int callIdx = insn[i];
					LzCallInsn cinsn = callTable[callIdx];
					ptr -= cinsn.argCount;
					ptr++;
				}
				break;
				default:
					break;
			}
			ptr += shift;
			maxStackPos = Math.max(maxStackPos, ptr);
		}
		
		int maxStack = maxStackPos + 1;
		++maxLocal;
		
		return new LzProgramInfo(
				maxStack,
				maxLocal,
				labelCount
		);
	}
}