package dev.zeith.lzvm;

import dev.zeith.lzvm.exception.*;
import dev.zeith.lzvm.jvm.LzMath;
import dev.zeith.lzvm.op.*;
import dev.zeith.lzvm.program.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LzVM
		implements LzVariableStore
{
	protected final Map<String, LzCallOp> callRegister = new HashMap<>();
	protected final Map<String, LzVarOp> varRegister = new HashMap<>();
	
	protected final ClassLoader loader;
	protected final Map<String, Map<LzCallInsn, Optional<Method>>> jvmCache = new ConcurrentHashMap<>();
	
	public LzVM(ClassLoader loader)
	{
		this.loader = loader;
	}
	
	public LzVM()
	{
		this(Thread.currentThread().getContextClassLoader());
	}
	
	protected Method findMethod(String owner, LzCallInsn call)
	{
		Map<LzCallInsn, Optional<Method>> map = jvmCache.computeIfAbsent(owner, k -> new ConcurrentHashMap<>());
		return map.computeIfAbsent(call, c ->
				{
					try
					{
						Class<?> cls = loader.loadClass(owner.replace('/', '.'));
						Class<?>[] params = ArgType.toJavaArgs(call.argTypes);
						return Optional.of(cls.getDeclaredMethod(call.name, params));
					} catch(ReflectiveOperationException e)
					{
						return Optional.empty();
					}
				}
		).orElse(null);
	}
	
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
	
	public double interpret(LzProgram program, LzProgramStack stack)
			throws LzVMException
	{
		return interpret(program.body, stack);
	}
	
	public double interpret(LzProgramBody program, LzProgramStack pStack)
			throws LzVMException
	{
		int[] insn = program.insnList;
		LzCallInsn[] callTable = program.callTable;
		double[] consts = program.dConstTable;
		String[] sConsts = program.sConstTable;
		
		Object[] stack = pStack.stack;
		Object[] locals = pStack.locals;
		
		Map<String, LzVarOp> varCache = new HashMap<>();
		Function<String, LzVarOp> fetchVar = name ->
		{
			if(name.startsWith("temp.")) return tempVar(name);
			return findVar(name);
		};
		Function<String, LzVarOp> getVar = n -> varCache.computeIfAbsent(n, fetchVar);
		
		List<Integer> labelCords = new ArrayList<>();
		for(int i = 0; i < insn.length; i++)
		{
			int instr = insn[i];
			i += LzOpcodes.EXTRA_SHIFTS[instr];
			if(instr == LzOpcodes.LABEL) labelCords.add(i);
		}
		
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
					case LzOpcodes.SCONST:
						stack[++ptr] = sConsts[insn[++i]]; break;
					case LzOpcodes.LOAD:
						stack[++ptr] = locals[insn[++i]]; break;
					case LzOpcodes.STORE:
						locals[insn[++i]] = stack[ptr--]; break;
					
					case LzOpcodes.JCALL:
					{
						String owner = vinsn = sConsts[insn[++i]];
						cinsn = callTable[insn[++i]];
						Method method = findMethod(owner, cinsn);
						Object[] capturedArgs = new Object[cinsn.argCount];
						expect = capturedArgs.length;
						for(int j = capturedArgs.length - 1; j >= 0; --j) capturedArgs[j] = stack[ptr--];
						try
						{
							stack[++ptr] = method.invoke(null, capturedArgs);
						} catch(ReflectiveOperationException e)
						{
							throw new LzVMCallNotFoundException(e);
						}
					}
					break;
					
					case LzOpcodes.CALL:
					{
						int callIdx = insn[++i];
						cinsn = callTable[callIdx];
						LzCallOp call = findCallByName(cinsn);
						Object[] capturedArgs = new Object[cinsn.argCount];
						expect = capturedArgs.length;
						for(int j = capturedArgs.length - 1; j >= 0; --j) capturedArgs[j] = stack[ptr--];
						stack[++ptr] = call.call(capturedArgs);
					}
					break;
					
					case LzOpcodes.ADD:
					case LzOpcodes.SUB:
					case LzOpcodes.MUL:
					case LzOpcodes.DIV:
					case LzOpcodes.MOD:
					case LzOpcodes.EQUALS:
					case LzOpcodes.NOT_EQUALS:
					case LzOpcodes.GREATER_THAN:
					case LzOpcodes.GREATER_EQ_THAN:
					case LzOpcodes.LESS_THAN:
					case LzOpcodes.LESS_EQ_THAN:
					case LzOpcodes.COALESCE:
					case LzOpcodes.AND:
					case LzOpcodes.OR:
					{
						expect = 2;
						double right = coerce(stack[ptr--]);
						double left = coerce(stack[ptr--]);
						stack[++ptr] = LzBinaryOp.byOpcode(state).operate(left, right);
					}
					break;
					case LzOpcodes.NOT:
					{
						double onStack = coerce(stack[ptr]);
						stack[ptr] = LzMath.notd(onStack);
					}
					break;
					case LzOpcodes.FSIN:
					{
						double onStack = coerce(stack[ptr]);
						stack[ptr] = LzMath.sind(onStack);
					}
					break;
					case LzOpcodes.FCOS:
					{
						double onStack = coerce(stack[ptr]);
						stack[ptr] = LzMath.cosd(onStack);
					}
					break;
					
					case LzOpcodes.READ:
					{
						int varIdx = insn[++i];
						vinsn = sConsts[varIdx];
						stack[++ptr] = getVar.apply(vinsn).get();
					}
					break;
					
					case LzOpcodes.WRITE:
					{
						expect = 1;
						int varIdx = insn[++i];
						vinsn = sConsts[varIdx];
						getVar.apply(vinsn).set(coerce(stack[ptr--]));
					}
					break;
					
					case LzOpcodes.LABEL:
						break;
					
					case LzOpcodes.JUMP:
					{
						int lblIdx = insn[++i];
						i = labelCords.get(lblIdx);
					}
					break;
					
					case LzOpcodes.JUMP_IF_TRUE:
					{
						int lblIdx = insn[++i];
						if(LzMath.isZero(coerce(stack[ptr--])))
							i = labelCords.get(lblIdx);
					}
					break;
					
					case LzOpcodes.JUMP_IF_FALSE:
					{
						int lblIdx = insn[++i];
						if(LzMath.isNotZero(coerce(stack[ptr--])))
							i = labelCords.get(lblIdx);
					}
					break;
					
					case LzOpcodes.TO_STRING:
					{
						Object obj = stack[ptr--];
						stack[++ptr] = String.valueOf(obj);
					}
					break;
					
					case LzOpcodes.POP:
					{
						Object obj = stack[ptr--];
					}
					break;
					
					case LzOpcodes.READ_INDEXED:
					{
						double index = coerce(stack[ptr--]);
						int varIdx = insn[++i];
						vinsn = sConsts[varIdx];
						stack[++ptr] = getVar.apply(vinsn).get(index);
					}
					break;
					
					case LzOpcodes.WRITE_INDEXED:
					{
						double index = coerce(stack[ptr--]);
						expect = 1;
						int varIdx = insn[++i];
						vinsn = sConsts[varIdx];
						getVar.apply(vinsn).set(index, coerce(stack[ptr--]));
					}
					break;
					
					default:
						throw new LzVMOperationNotSupportedException("Unknown opcode: " + LzOpcodes.opNameIndexed(i, state));
				}
			}
		} catch(ArrayIndexOutOfBoundsException e)
		{
			if(ptr < 0)
				throw new LzVMStackUnderflowException(LzOpcodes.opNameIndexed(i, state) + " expected " + expect + " arguments on the stack.", e);
			throw e;
		} catch(NullPointerException e)
		{
			if(cinsn != null && state == LzOpcodes.CALL)
				throw new LzVMCallNotFoundException("Could not find call " + cinsn.name + cinsn.descriptor + " with " + cinsn.argCount + " arguments.", e);
			if(cinsn != null && state == LzOpcodes.JCALL)
				throw new LzVMCallNotFoundException("Could not find jvm call " + vinsn.replace('/', '.') + "." + cinsn.name + cinsn.descriptor + " with " + cinsn.argCount + " arguments.", e);
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
	
	@Override
	public LzVarOp findVar(String name)
	{
		return varRegister.getOrDefault(name, LzVarOp.ZERO);
	}
	
	@Override
	public LzVarOp tempVar(String name)
	{
		return LzVarOp.readWrite();
	}
}