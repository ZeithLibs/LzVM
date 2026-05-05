package dev.zeith.lzvm;

import dev.zeith.lzvm.exception.*;
import dev.zeith.lzvm.jvm.LzFMath;
import dev.zeith.lzvm.op.*;
import dev.zeith.lzvm.program.*;

import java.util.*;

public class LzVM
		implements LzVariableStore
{
	protected final Map<String, LzCallOp> callRegister = new HashMap<>();
	protected final Map<String, LzVarOp> varRegister = new HashMap<>();
	
	public LzVM registerCall(LzCallInsn name, LzCallOp op)
	{
		callRegister.put(name.name + name.descriptor, op);
		return this;
	}
	
	public LzVM registerVar(String name, LzVarOp op)
	{
		varRegister.put(name, op);
		return this;
	}
	
	public double eval(LzProgram program, LzProgramStack stack)
			throws LzVMException
	{
		return eval(program.body, stack);
	}
	
	public double eval(LzProgramBody program, LzProgramStack pStack)
			throws LzVMException
	{
		int[] insn = program.insnList;
		LzCallInsn[] callTable = program.callTable;
		String[] varTable = program.varTable;
		double[] consts = program.dConstTable;
		String[] sConsts = program.sConstTable;
		
		Object[] stack = pStack.stack;
		Object[] locals = pStack.locals;
		
		int ptr = -1;
		
		String vinsn = null;
		LzCallInsn cinsn = null;
		int i = 0, state = -1, expect = 0;
		try
		{
			for(; i < insn.length; i++)
			{
				state = insn[i];
				switch(state)
				{
					case LzOpcodes.RETURN:
						return ptr < 0 ? 0.0 : coerce(stack[ptr]);
					case LzOpcodes.CONST:
						stack[++ptr] = consts[insn[++i]]; break;
					case LzOpcodes.LOAD:
						stack[++ptr] = locals[insn[++i]]; break;
					case LzOpcodes.STORE:
						locals[insn[++i]] = stack[ptr--]; break;
					case LzOpcodes.CALL:
					{
						int callIdx = insn[++i];
						cinsn = callTable[callIdx];
						LzCallOp call = findCallByName(cinsn);
						Object[] capturedArgs = new Object[cinsn.argCount];
						expect = capturedArgs.length;
						for(int j = 0; j < capturedArgs.length; j++) capturedArgs[j] = stack[ptr--];
						stack[++ptr] = call.call(capturedArgs);
					}
					break;
					case LzOpcodes.ADD:
					case LzOpcodes.SUB:
					case LzOpcodes.MUL:
					case LzOpcodes.DIV:
					{
						expect = 2;
						double right = coerce(stack[ptr--]);
						double left = coerce(stack[ptr--]);
						stack[++ptr] = LzBinaryOp.byOpcode(state).operate(left, right);
					}
					break;
					case LzOpcodes.READ:
					{
						int varIdx = insn[++i];
						vinsn = varTable[varIdx];
						stack[++ptr] = findVar(vinsn).get();
					}
					break;
					case LzOpcodes.WRITE:
					{
						expect = 1;
						int varIdx = insn[++i];
						vinsn = varTable[varIdx];
						findVar(vinsn).set(coerce(stack[ptr--]));
					}
					break;
					case LzOpcodes.SCONST:
						stack[++ptr] = sConsts[insn[++i]]; break;
					case LzOpcodes.FSIN:
					{
						double onStack = coerce(stack[ptr--]);
						stack[++ptr] = LzFMath.sind(onStack);
						break;
					}
					case LzOpcodes.FCOS:
					{
						double onStack = coerce(stack[ptr--]);
						stack[++ptr] = LzFMath.cosd(onStack);
						break;
					}
					default:
						break;
				}
			}
		} catch(ArrayIndexOutOfBoundsException e)
		{
			if(ptr < 0)
				throw new LzVMStackUnderflowException("Op[" + i + "](" + LzOpcodes.NAME_OF.get(state) + ") expected " + expect + " arguments on the stack.", e);
			throw e;
		} catch(NullPointerException e)
		{
			if(cinsn != null && state == LzOpcodes.CALL)
				throw new LzVMCallNotFoundException("Could not find call " + cinsn.name + cinsn.descriptor + " with " + cinsn.argCount + " arguments.", e);
			if(vinsn != null && (state == LzOpcodes.READ || state == LzOpcodes.WRITE))
				throw new LzVMCallNotFoundException("Could not find var " + vinsn, e);
			throw e;
		}
		
		return 0.0;
	}
	
	public static double coerce(Object obj)
	{
		return obj instanceof Number ? ((Number) obj).doubleValue() : (obj != null ? 1.0 : 0.0);
	}
	
	public LzCallOp findCallByName(LzCallInsn insn)
	{
		return findCall(insn.name, insn.descriptor);
	}
	
	@Override
	public LzCallOp findCall(String name, String descriptor)
	{
		return callRegister.getOrDefault(name + descriptor, LzCallOp.NO_OP);
	}
	
	public LzVarOp findVar(String insn)
	{
		return varRegister.get(insn);
	}
}