package dev.zeith.lzvm.op;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.*;

public interface LzOpcodes
{
	int RETURN = 1; // Complete execution and return current value.
	int CONST = 2; // Load value from const table onto the stack.
	int SCONST = 3; // Load string value from const table onto the stack.
	int LOAD = 4; // Load value from locals table onto the stack.
	int STORE = 5; // Store current value into locals table.
	int JCALL = 6; // Call static java method
	int CALL = 7; // Calls
	int ADD = 8; // Pops two doubles from the stack and pushes their sum onto the stack
	int SUB = 9; // Pops two doubles from the stack and pushes their sub onto the stack
	int MUL = 10; // Pops two doubles from the stack and pushes their mul onto the stack
	int DIV = 11; // Pops two doubles from the stack and pushes their div onto the stack
	int EQUALS = 12;
	int NOT_EQUALS = 13;
	int GREATER_THAN = 14;
	int GREATER_EQ_THAN = 15;
	int LESS_THAN = 16;
	int LESS_EQ_THAN = 17;
	int COALESCE = 18;
	int AND = 19;
	int OR = 20;
	int NOT = 21;
	int FSIN = 22;
	int FCOS = 23;
	int READ = 24; // Read variable by const (next insn points to a string name in varTable) and push onto the stack
	int WRITE = 25; // Pop the double from stack and write it into LzVariableStore (next insn points to a string name in varTable)
	int LABEL = 26; // Marks a label that the GOTO insn can jump to. All labels get their starting at 0.
	int JUMP = 27; // Jumps to the label by the next insn index. (the index 0 always points to the first label)
	int JUMP_IF_TRUE = 28;
	int JUMP_IF_FALSE = 29;
	
	int I_LAST = JUMP_IF_FALSE;
	int I_COUNT = I_LAST + 1;
	
	int[] EXTRA_SHIFTS = OpCodeIndexer.computeExtraShifts();
	int[] STACK_SHIFT = OpCodeIndexer.computeStackShift();
	
	Map<String, Integer> BY_NAME = OpCodeIndexer.computeByName();
	Map<Integer, String> NAME_OF = OpCodeIndexer.computeNameOf();
	
	static String opName(int opcode)
	{
		return "Op(" + LzOpcodes.NAME_OF.get(opcode) + ")";
	}
	
	static String opNameIndexed(int idx, int opcode)
	{
		return "Op[" + idx + "](" + LzOpcodes.NAME_OF.get(opcode) + ")";
	}
	
	@SuppressWarnings("DuplicatedCode")
	class OpCodeIndexer
	{
		private static int[] computeExtraShifts()
		{
			int[] c = new int[I_COUNT];
			c[CONST] = 1;
			c[SCONST] = 1;
			c[LOAD] = 1;
			c[STORE] = 1;
			c[JCALL] = 2;
			c[CALL] = 1;
			c[READ] = 1;
			c[WRITE] = 1;
			c[JUMP] = 1;
			c[JUMP_IF_TRUE] = 1;
			c[JUMP_IF_FALSE] = 1;
			return c;
		}
		
		private static int[] computeStackShift()
		{
			int[] c = new int[I_COUNT];
			
			c[RETURN] = -1;
			c[CONST] = 1;
			c[SCONST] = 1;
			c[LOAD] = 1;
			c[STORE] = -1;
			c[ADD] = -1;
			c[SUB] = -1;
			c[MUL] = -1;
			c[DIV] = -1;
			c[EQUALS] = -1;
			c[NOT_EQUALS] = -1;
			c[GREATER_THAN] = -1;
			c[GREATER_EQ_THAN] = -1;
			c[LESS_THAN] = -1;
			c[LESS_EQ_THAN] = -1;
			c[COALESCE] = -1;
			c[AND] = -1;
			c[OR] = -1;
			c[READ] = 1;
			c[WRITE] = -1;
			c[JUMP_IF_TRUE] = -1;
			c[JUMP_IF_FALSE] = -1;
			
			// calls are dynamic in nature and the argument count is always followed by the instruction
			c[JCALL] = -1;
			c[CALL] = -1;
			
			return c;
		}
		
		@SneakyThrows
		private static Map<String, Integer> computeByName()
		{
			Map<String, Integer> m = new HashMap<>();
			for(Field f : LzOpcodes.class.getDeclaredFields())
			{
				if(f.getType().isPrimitive() && !f.getName().startsWith("I_"))
				{
					String name = f.getName();
					int insn = f.getInt(null);
					m.put(name, insn);
				}
			}
			return Collections.unmodifiableMap(m);
		}
		
		@SneakyThrows
		private static Map<Integer, String> computeNameOf()
		{
			Map<Integer, String> m = new HashMap<>();
			for(Field f : LzOpcodes.class.getDeclaredFields())
			{
				if(f.getType().isPrimitive() && !f.getName().startsWith("I_"))
				{
					String name = f.getName();
					int insn = f.getInt(null);
					m.put(insn, name);
				}
			}
			return Collections.unmodifiableMap(m);
		}
	}
}