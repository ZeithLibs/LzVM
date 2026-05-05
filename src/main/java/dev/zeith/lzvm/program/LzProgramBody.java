package dev.zeith.lzvm.program;

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