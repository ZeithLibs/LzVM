package dev.zeith.lzvm.op;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.*;

public interface LzOpcodes
{
	int RETURN = 0; // Complete execution and return current value.
	int CONST = 1; // Load value from const table onto the stack.
	int LOAD = 2; // Load value from locals table onto the stack.
	int STORE = 3; // Store current value into locals table.
	int CALL = 4; // Calls
	int ADD = 5; // Pops two doubles from the stack and pushes their sum onto the stack
	int SUB = 6; // Pops two doubles from the stack and pushes their sub onto the stack
	int MUL = 7; // Pops two doubles from the stack and pushes their mul onto the stack
	int DIV = 8; // Pops two doubles from the stack and pushes their div onto the stack
	int READ = 9; // Read variable by const (next insn points to a string name in varTable) and push onto the stack
	int WRITE = 10; // Pop the double from stack and write it into LzVariableStore (next insn points to a string name in varTable)
	int LABEL = 11; // Marks a label that the GOTO insn can jump to. All labels get their starting at 0.
	int JUMP = 12; // Jumps to the label by the next insn index. (the index 0 always points to the first label)
	int SCONST = 13; // Load string value from const table onto the stack.
	
	int I_LAST = SCONST;
	int I_COUNT = I_LAST + 1;
	
	int[] EXTRA_SHIFTS = OpCodeIndexer.computeExtraShifts();
	int[] STACK_SHIFT = OpCodeIndexer.computeStackShift();
	
	Map<String, Integer> BY_NAME = OpCodeIndexer.computeByName();
	Map<Integer, String> NAME_OF = OpCodeIndexer.computeNameOf();
	
	@SuppressWarnings("DuplicatedCode")
	class OpCodeIndexer
	{
		private static int[] computeExtraShifts()
		{
			int[] c = new int[I_COUNT];
			c[CONST] = 1;
			c[LOAD] = 1;
			c[STORE] = 1;
			c[CALL] = 1;
			c[READ] = 1;
			c[WRITE] = 1;
			c[JUMP] = 1;
			c[SCONST] = 1;
			return c;
		}
		
		private static int[] computeStackShift()
		{
			int[] c = new int[I_COUNT];
			
			c[RETURN] = -1;
			c[CONST] = 1;
			c[LOAD] = 1;
			c[STORE] = -1;
			// calls are dynamic in nature and the argument count is always followed by the instruction
			c[CALL] = -1;
			c[ADD] = -2 + 1;
			c[SUB] = -2 + 1;
			c[MUL] = -2 + 1;
			c[DIV] = -2 + 1;
			c[READ] = 1;
			c[WRITE] = -1;
			c[SCONST] = 1;
			
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