package dev.zeith.lzvm.jvm;

import dev.zeith.lzvm.op.LzOpcodes;
import dev.zeith.lzvm.program.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.*;

import static org.objectweb.asm.Opcodes.*;

public class LzJvmCompiler
{
	public static final String LzExpression = dev.zeith.lzvm.jvm.LzExpression.class.getName().replace('.', '/');
	public static final String LzVariableStore = dev.zeith.lzvm.LzVariableStore.class.getName().replace('.', '/');
	public static final String LzVarOp = dev.zeith.lzvm.op.LzVarOp.class.getName().replace('.', '/');
	public static final String LzCallOp = dev.zeith.lzvm.op.LzCallOp.class.getName().replace('.', '/');
	public static final String LzGenerated = Generated.class.getName().replace('.', '/');
	public static final String LzFMath = LzMath.class.getName().replace('.', '/');
	
	public static final String L_LzExpression = "L" + LzExpression + ";";
	public static final String L_LzVariableStore = "L" + LzVariableStore + ";";
	public static final String L_LzVarOp = "L" + LzVarOp + ";";
	public static final String L_LzCallOp = "L" + LzCallOp + ";";
	public static final String L_LzGenerated = "L" + LzGenerated + ";";
	
	static boolean useArray = false;
	
	@SuppressWarnings("deprecation") // methodCall is not actually deprecated,
	private static MethodInsnNode mCall(final int opcode, final String owner, final String name, final String descriptor)
	{
		return new MethodInsnNode(opcode, owner, name, descriptor);
	}
	
	public static byte[] compile(String name, LzProgram program, int argCount)
	{
		final LzProgramBody body = program.body;
		
		final ClassNode cn = new ClassNode();
		cn.version = V1_8;
		cn.access = ACC_PUBLIC;
		cn.name = name.replace('.', '/');
		cn.superName = "java/lang/Object";
		cn.interfaces = Collections.singletonList(LzExpression);
		
		final Set<String> usedFieldNames = new HashSet<>();
		final Set<String> usedMethodNames = new HashSet<>();
		
		usedFieldNames.add("isFactory");
		
		usedMethodNames.add("toString");
		usedMethodNames.add("wait");
		usedMethodNames.add("notify");
		usedMethodNames.add("notifyAll");
		usedMethodNames.add("hashCode");
		usedMethodNames.add("getClass");
		usedMethodNames.add("equals");
		usedMethodNames.add("clone");
		usedMethodNames.add("finalize");
		
		usedMethodNames.add("instantiate");
		usedMethodNames.add("get");
		
		final int startingFieldCount = usedFieldNames.size();
		
		final FieldNode isFactory = new FieldNode(ACC_PRIVATE | ACC_FINAL, "isFactory", "Z", null, null);
		cn.fields.add(isFactory);
		
		//<editor-fold desc="expression ctor">
		final MethodNode ctor = new MethodNode(ACC_PUBLIC, "<init>", "(" + L_LzVariableStore + ")V", null, null);
		InsnList cinsn = ctor.instructions;
		{
			cinsn.add(new VarInsnNode(ALOAD, 0));
			cinsn.add(mCall(INVOKESPECIAL, cn.superName, "<init>", "()V"));
			cinsn.add(new VarInsnNode(ALOAD, 0));
			cinsn.add(new InsnNode(ICONST_0));
			cinsn.add(new FieldInsnNode(PUTFIELD, cn.name, isFactory.name, isFactory.desc));
			cinsn.add(new InsnNode(RETURN));
		}
		cn.methods.add(ctor);
		//</editor-fold>
		
		//<editor-fold desc="factory ctor">
		final MethodNode fctor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
		cinsn = fctor.instructions;
		{
			cinsn.add(new VarInsnNode(ALOAD, 0));
			cinsn.add(mCall(INVOKESPECIAL, cn.superName, "<init>", "()V"));
			cinsn.add(new VarInsnNode(ALOAD, 0));
			cinsn.add(new InsnNode(ICONST_1));
			cinsn.add(new FieldInsnNode(PUTFIELD, cn.name, isFactory.name, isFactory.desc));
			cinsn.add(new InsnNode(RETURN));
		}
		cn.methods.add(fctor);
		//</editor-fold>
		
		final Function<String, FieldNode> varGetter = getVarFieldHelper(cn, usedFieldNames, ctor);
		final Function<LzCallInsn, FieldNode> callGetter = getCallFieldHelper(cn, usedFieldNames, ctor);
		final Map<LzCallInsn, MethodNode> registeredCalls = new HashMap<>(body.callTable.length); // preallocate all calls
		final String disassembly = body.disassemble(true);
		cn.visibleAnnotations = generated(disassembly, Collections.emptyMap());
		
		//<editor-fold desc="toString()">
		final MethodNode toString = new MethodNode(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
		cinsn = toString.instructions;
		{
			LabelNode notFactory = new LabelNode();
			LabelNode end = new LabelNode();
			String exprStr = dev.zeith.lzvm.jvm.LzExpression.class.getSimpleName() + "{" + body.disassemble(false) + "}";
			String factoryStr = dev.zeith.lzvm.jvm.LzFactory.class.getSimpleName() + "{" + body.disassemble(false) + "}";
			cinsn.add(new VarInsnNode(ALOAD, 0));
			cinsn.add(new FieldInsnNode(GETFIELD, cn.name, isFactory.name, isFactory.desc));
			cinsn.add(new JumpInsnNode(IFEQ, notFactory));
			cinsn.add(new LdcInsnNode(factoryStr));
			cinsn.add(new JumpInsnNode(GOTO, end));
			cinsn.add(notFactory);
			cinsn.add(new LdcInsnNode(exprStr));
			cinsn.add(end);
			cinsn.add(new InsnNode(ARETURN));
		}
		cn.methods.add(toString);
		//</editor-fold>
		
		//<editor-fold desc="get([D)D">
		MethodNode m = new MethodNode(ACC_PUBLIC, "get", "([D)D", null, null);
		cn.methods.add(m);
		
		InsnList insn = m.instructions;
		
		// create locals array
		if(useArray)
		{
			pushInt(insn, program.info.maxLocals); // some temp space
			insn.add(new IntInsnNode(NEWARRAY, T_DOUBLE));
			insn.add(new VarInsnNode(ASTORE, 2)); // locals at slot 2
		}
		
		int[] code = body.insnList;
		
		Map<Integer, LabelNode> labelMap = new HashMap<>();
		
		int labelIndex = 0;
		for(int i = 0, insnLen = code.length; i < insnLen; i++)
		{
			int instr = code[i];
			i += LzOpcodes.EXTRA_SHIFTS[instr];
			if(instr == LzOpcodes.LABEL) labelMap.put(labelIndex++, new LabelNode());
		}
		labelIndex = 0;
		
		int stackPos = 0;
		
		// --- Emit instructions ---
		for(int i = 0; i < code.length; i++)
		{
			int op = code[i];
			
			switch(op)
			{
				case LzOpcodes.RETURN:
				{
					// If nothing is on the stack, replicate LzVM's behavior of returning zero.
					if(stackPos <= 0) insn.add(new LdcInsnNode(0.0));
					insn.add(new InsnNode(DRETURN));
				}
				break;
				
				case LzOpcodes.CONST:
				{
					int idx = code[++i];
					double val = body.dConstTable[idx];
					insn.add(new LdcInsnNode(val));
					++stackPos;
				}
				break;
				
				case LzOpcodes.SCONST:
				{
					int constIdx = code[++i];
					insn.add(new LdcInsnNode(body.sConstTable[constIdx]));
					++stackPos;
				}
				break;
				
				case LzOpcodes.LOAD:
				{
					int idx = code[++i];
					
					if(useArray || idx < argCount)
					{
						insn.add(new VarInsnNode(ALOAD, idx < argCount ? 1 : 2));
						pushInt(insn, idx);
						insn.add(new InsnNode(DALOAD));
					} else
					{
						insn.add(new VarInsnNode(DLOAD, 2 + idx));
					}
					
					++stackPos;
				}
				break;
				
				case LzOpcodes.STORE:
				{
					int idx = code[++i];
					if(useArray || idx < argCount)
					{
						insn.add(new VarInsnNode(DSTORE, 3));
						insn.add(new VarInsnNode(ALOAD, idx < argCount ? 1 : 2));
						pushInt(insn, idx);
						insn.add(new VarInsnNode(DLOAD, 3));
						insn.add(new InsnNode(DASTORE));
					} else
					{
						insn.add(new VarInsnNode(DSTORE, 2 + idx));
					}
					--stackPos;
				}
				break;
				
				case LzOpcodes.JCALL:
				{
					String owner = body.sConstTable[code[++i]];
					LzCallInsn call = body.callTable[code[++i]];
					
					insn.add(mCall(
							INVOKESTATIC,
							owner.replace('.', '/'),
							call.name,
							call.jvmDescriptor
					));
					
					stackPos -= call.argCount;
					++stackPos;
				}
				break;
				
				case LzOpcodes.CALL:
				{
					int callIdx = code[++i];
					LzCallInsn call = body.callTable[callIdx];
					
					MethodNode method;
					if((method = registeredCalls.get(call)) == null)
					{
						FieldNode callField = callGetter.apply(call);
						registeredCalls.put(call, method = createCallMethod(call, cn, usedMethodNames, callField));
					}
					
					// Add the lookup into the stack
					insn.add(new VarInsnNode(ALOAD, 0));
					
					// Call the method
					insn.add(mCall(
							INVOKESTATIC,
							cn.name,
							method.name,
							method.desc
					));
					
					stackPos -= call.argCount;
					++stackPos;
				}
				break;
				
				case LzOpcodes.ADD:
					insn.add(new InsnNode(DADD)); --stackPos; break;
				case LzOpcodes.SUB:
					insn.add(new InsnNode(DSUB)); --stackPos; break;
				case LzOpcodes.MUL:
					insn.add(new InsnNode(DMUL)); --stackPos; break;
				case LzOpcodes.DIV:
					insn.add(new InsnNode(DDIV)); --stackPos; break;
				case LzOpcodes.EQUALS:
					insn.add(mCall(INVOKESTATIC, LzFMath, "eqd", "(DD)D")); --stackPos; break;
				case LzOpcodes.NOT_EQUALS:
					insn.add(mCall(INVOKESTATIC, LzFMath, "neqd", "(DD)D")); --stackPos; break;
				case LzOpcodes.GREATER_THAN:
					insn.add(mCall(INVOKESTATIC, LzFMath, "gtd", "(DD)D")); --stackPos; break;
				case LzOpcodes.GREATER_EQ_THAN:
					insn.add(mCall(INVOKESTATIC, LzFMath, "getd", "(DD)D")); --stackPos; break;
				case LzOpcodes.LESS_THAN:
					insn.add(mCall(INVOKESTATIC, LzFMath, "ltd", "(DD)D")); --stackPos; break;
				case LzOpcodes.LESS_EQ_THAN:
					insn.add(mCall(INVOKESTATIC, LzFMath, "letd", "(DD)D")); --stackPos; break;
				case LzOpcodes.COALESCE:
					insn.add(mCall(INVOKESTATIC, LzFMath, "coalesce", "(DD)D")); --stackPos; break;
				case LzOpcodes.FSIN:
					insn.add(mCall(INVOKESTATIC, LzFMath, "sind", "(D)D")); break;
				case LzOpcodes.FCOS:
					insn.add(mCall(INVOKESTATIC, LzFMath, "cosd", "(D)D")); break;
				
				case LzOpcodes.READ:
				{
					int idx = code[++i];
					String nameStr = body.varTable[idx];
					FieldNode varHolder = varGetter.apply(nameStr);
					
					insn.add(new VarInsnNode(ALOAD, 0));
					insn.add(new FieldInsnNode(GETFIELD, cn.name, varHolder.name, varHolder.desc));
					insn.add(mCall(
							INVOKEINTERFACE,
							LzVarOp,
							"get",
							"()D"
					));
					
					++stackPos;
				}
				break;
				
				case LzOpcodes.WRITE:
				{
					int idx = code[++i];
					String nameStr = body.varTable[idx];
					FieldNode varHolder = varGetter.apply(nameStr);
					
					// Store into temp variable
					insn.add(new VarInsnNode(DSTORE, 3));
					
					insn.add(new VarInsnNode(ALOAD, 0));
					insn.add(new FieldInsnNode(GETFIELD, cn.name, varHolder.name, varHolder.desc));
					insn.add(new VarInsnNode(DLOAD, 3));
					
					insn.add(mCall(
							INVOKEINTERFACE,
							LzVarOp,
							"set",
							"(D)V"
					));
					
					--stackPos;
				}
				break;
				
				case LzOpcodes.LABEL:
				{
					insn.add(labelMap.get(labelIndex++));
				}
				break;
				
				case LzOpcodes.JUMP:
				{
					int target = code[++i];
					insn.add(new JumpInsnNode(GOTO, labelMap.get(target)));
				}
				break;
				
				default:
					throw new IllegalStateException("Unknown opcode: " + op);
			}
		}
		
		// At the end, always return 0
		insn.add(new LdcInsnNode(0.0));
		insn.add(new InsnNode(DRETURN));
		//</editor-fold>
		
		//<editor-fold desc="instantiate(LzVariableStore)LzExpression">
		final MethodNode instantiate = new MethodNode(ACC_PUBLIC, "instantiate", "(" + L_LzVariableStore + ")" + L_LzExpression, null, null);
		cinsn = instantiate.instructions;
		if(usedFieldNames.size() == startingFieldCount)
		{
			FieldNode instanceNode = new FieldNode(ACC_PRIVATE, "instance", L_LzExpression, null, null);
			cn.fields.add(instanceNode);
			
			// no fields were added during generation, return a cached instance
			cinsn.add(new VarInsnNode(ALOAD, 0));
			cinsn.add(new FieldInsnNode(GETFIELD, cn.name, instanceNode.name, instanceNode.desc));
			cinsn.add(new InsnNode(ARETURN));
			
			// Modify factory constructor to initialize instance field
			{
				cinsn = new InsnList();
				cinsn.add(new VarInsnNode(ALOAD, 0));
				cinsn.add(new TypeInsnNode(NEW, cn.name));
				cinsn.add(new InsnNode(DUP));
				cinsn.add(new InsnNode(ACONST_NULL));
				cinsn.add(mCall(
						INVOKESPECIAL,
						cn.name,
						ctor.name,
						ctor.desc
				));
				cinsn.add(new FieldInsnNode(PUTFIELD, cn.name, instanceNode.name, instanceNode.desc));
				AbstractInsnNode ret = findNode(fctor.instructions, insnNode -> insnNode.getOpcode() == RETURN);
				fctor.instructions.insertBefore(ret, cinsn);
			}
			
			// Modify instance constructor to reference to itself
			{
				cinsn = new InsnList();
				cinsn.add(new VarInsnNode(ALOAD, 0));
				cinsn.add(new InsnNode(DUP));
				cinsn.add(new FieldInsnNode(PUTFIELD, cn.name, instanceNode.name, instanceNode.desc));
				AbstractInsnNode ret = findNode(ctor.instructions, insnNode -> insnNode.getOpcode() == RETURN);
				ctor.instructions.insertBefore(ret, cinsn);
			}
		} else
		{
			cinsn.add(new TypeInsnNode(NEW, cn.name)); // new expression instance
			cinsn.add(new InsnNode(DUP));
			cinsn.add(new VarInsnNode(ALOAD, 1)); // load LzVariableStore
			cinsn.add(mCall(
					INVOKESPECIAL,
					cn.name,
					ctor.name,
					ctor.desc
			));
			cinsn.add(new InsnNode(ARETURN));
		}
		cn.methods.add(instantiate);
		//</editor-fold>
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}
	
	protected static AbstractInsnNode findNode(InsnList list, Predicate<AbstractInsnNode> filter)
	{
		// Iterating over the InsnList isn't a thing in 6.2. Use older ListIterator approach.
		ListIterator<AbstractInsnNode> itr = list.iterator();
		while(itr.hasNext())
		{
			AbstractInsnNode i = itr.next();
			if(filter.test(i))
				return i;
		}
		return null;
	}
	
	protected static Function<String, FieldNode> getVarFieldHelper(ClassNode owner, Set<String> usedFieldNames, MethodNode ctor)
	{
		Map<String, FieldNode> registeredVarFields = new HashMap<>();
		return var -> registeredVarFields.computeIfAbsent(var, fName ->
				{
					FieldNode fn = new FieldNode(
							ACC_PRIVATE | ACC_FINAL,
							JavaNamingConventions.processUniqueMethodName(fName, usedFieldNames),
							L_LzVarOp,
							null,
							null
					);
					fn.visibleAnnotations = generated(fName, Collections.emptyMap());
					owner.fields.add(fn);
					
					InsnList inject = new InsnList();
					
					inject.add(new VarInsnNode(ALOAD, 0)); // this
					inject.add(new VarInsnNode(ALOAD, 1)); // store
					inject.add(new LdcInsnNode(fName)); // string
					inject.add(mCall( // call
							INVOKEINTERFACE,
							LzVariableStore,
							"findVar",
							"(Ljava/lang/String;)" + L_LzVarOp
					));
					inject.add(new FieldInsnNode(PUTFIELD, owner.name, fn.name, fn.desc)); // store to field
					
					AbstractInsnNode ret = findNode(ctor.instructions, insnNode -> insnNode.getOpcode() == RETURN);
					ctor.instructions.insertBefore(ret, inject);
					
					return fn;
				}
		);
	}
	
	protected static Function<LzCallInsn, FieldNode> getCallFieldHelper(ClassNode owner, Set<String> usedFieldNames, MethodNode ctor)
	{
		Map<LzCallInsn, FieldNode> registeredCallFields = new HashMap<>();
		return callId -> registeredCallFields.computeIfAbsent(callId, call ->
				{
					FieldNode fn = new FieldNode(
							ACC_PRIVATE | ACC_FINAL,
							JavaNamingConventions.processUniqueMethodName(call.name, usedFieldNames),
							L_LzCallOp,
							null,
							null
					);
					fn.visibleAnnotations = generated(call.name + call.descriptor, Collections.emptyMap());
					owner.fields.add(fn);
					
					InsnList inject = new InsnList();
					
					inject.add(new VarInsnNode(ALOAD, 0)); // this
					inject.add(new VarInsnNode(ALOAD, 1)); // store
					inject.add(new LdcInsnNode(call.name)); // string
					inject.add(new LdcInsnNode(call.descriptor)); // string
					inject.add(mCall( // call
							INVOKEINTERFACE,
							LzVariableStore,
							"findCall",
							"(Ljava/lang/String;Ljava/lang/String;)" + L_LzCallOp
					));
					inject.add(new FieldInsnNode(PUTFIELD, owner.name, fn.name, fn.desc)); // store to field
					
					AbstractInsnNode ret = findNode(ctor.instructions, insnNode -> insnNode.getOpcode() == RETURN);
					ctor.instructions.insertBefore(ret, inject);
					
					return fn;
				}
		);
	}
	
	private static MethodNode createJCallMethod(LzCallInsn call, ClassNode owner, Set<String> usedMethodNames, String jCall)
	{
		final int argc = call.argCount;
		final int RETURN_INSTRUCT = call.returnType == ArgType.DOUBLE ? DRETURN : ARETURN;
		
		StringBuilder desc = new StringBuilder("(");
		ArgType[] argTypes = call.argTypes;
		for(ArgType argType : argTypes) desc.append(argType.jvmDesc);
		desc.append(")").append(call.returnType.jvmDesc);
		
		MethodNode m = new MethodNode(
				ACC_PRIVATE | ACC_STATIC,
				JavaNamingConventions.processUniqueMethodName("jcall$" + call.name, usedMethodNames),
				desc.toString(),
				null,
				null
		);
		
		Map<String, Object> props = new HashMap<>();
		props.put("argCount", argc);
		m.visibleAnnotations = generated(jCall.replace('/', '.') + "." + call.name, props);
		
		InsnList insn = m.instructions;
		
		int[] slots = new int[argc];
		int slotCursor = 0;
		for(int i = 0; i < argc; i++)
		{
			slots[i] = slotCursor;
			if(call.argTypes[i] == ArgType.DOUBLE)
				slotCursor += 2;
			else
				slotCursor += 1;
		}
		
		for(int i = 0; i < argc; ++i)
		{
			int slot = slots[i];
			if(call.argTypes[i] == ArgType.DOUBLE)
				insn.add(new VarInsnNode(DLOAD, slot));
			else
				insn.add(new VarInsnNode(ALOAD, slot));
		}
		
		insn.add(mCall(
				INVOKESTATIC,
				jCall.replace('.', '/'),
				call.name,
				call.jvmDescriptor
		));
		
		insn.add(new InsnNode(RETURN_INSTRUCT));
		owner.methods.add(m);
		return m;
	}
	
	private static MethodNode createCallMethod(LzCallInsn call, ClassNode owner, Set<String> usedMethodNames, FieldNode callField)
	{
		StringBuilder desc = new StringBuilder("(");
		ArgType[] argTypes = call.argTypes;
		for(ArgType t : argTypes)
			desc.append(t.jvmDesc);
		desc.append("L").append(owner.name).append(";)").append(call.returnType.jvmDesc);
		
		MethodNode m = new MethodNode(
				ACC_PRIVATE | ACC_STATIC,
				JavaNamingConventions.processUniqueMethodName(call.name, usedMethodNames),
				desc.toString(),
				null,
				null
		);
		
		final int argc = call.argCount;
		
		Map<String, Object> props = new HashMap<>();
		props.put("argCount", argc);
		m.visibleAnnotations = generated(call.name, props);
		
		InsnList insn = m.instructions;
		
		int RETURN_INSTRUCT = call.returnType == ArgType.DOUBLE ? DRETURN : ARETURN;
		
		// -----------------------------
		// 1. Compute JVM slot layout properly
		// -----------------------------
		int[] slots = new int[argc];
		int slotCursor = 0;
		for(int i = 0; i < argc; i++)
		{
			slots[i] = slotCursor;
			if(call.argTypes[i] == ArgType.DOUBLE)
				slotCursor += 2;
			else
				slotCursor += 1;
		}
		
		int storeSlot = slotCursor;
		
		// --- pack args into Object[] ---
		insn.add(new LdcInsnNode(argc));
		insn.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
		
		for(int i = 0; i < argc; ++i)
		{
			insn.add(new InsnNode(DUP));
			pushInt(insn, i);
			
			int slot = slots[i];
			
			if(call.argTypes[i] == ArgType.DOUBLE)
			{
				insn.add(new VarInsnNode(DLOAD, slot));
				insn.add(mCall(
						INVOKESTATIC,
						"java/lang/Double",
						"valueOf",
						"(D)Ljava/lang/Double;"
				));
			} else
			{
				insn.add(new VarInsnNode(ALOAD, slot));
			}
			
			insn.add(new InsnNode(AASTORE));
		}
		
		// --- DUP Object[] because we still need it after call lookup ---
		insn.add(new InsnNode(DUP));
		
		// --- resolve call ---
		insn.add(new VarInsnNode(ALOAD, storeSlot));
		insn.add(new FieldInsnNode(GETFIELD,
				owner.name,
				callField.name,
				callField.desc
		));
		
		// --- swap to correct order for call ---
		insn.add(new InsnNode(SWAP));
		
		// --- call(Object[]) ---
		insn.add(mCall(
				INVOKEINTERFACE,
				LzCallOp,
				"call",
				"([Ljava/lang/Object;)D"
		));
		
		insn.add(new InsnNode(RETURN_INSTRUCT));
		owner.methods.add(m);
		return m;
	}
	
	private static List<AnnotationNode> generated(String value, Map<String, Object> rest)
	{
		AnnotationNode an = new AnnotationNode(L_LzGenerated);
		an.values = new ArrayList<>();
		an.values.add("value");
		an.values.add(value);
		for(Map.Entry<String, Object> e : rest.entrySet())
		{
			an.values.add(e.getKey());
			an.values.add(e.getValue());
		}
		List<AnnotationNode> nodes = new ArrayList<>();
		nodes.add(an);
		return nodes;
	}
	
	private static void pushInt(InsnList insn, int v)
	{
		if(v >= -1 && v <= 5)
			insn.add(new InsnNode(ICONST_0 + v));
		else if(v <= Byte.MAX_VALUE)
			insn.add(new IntInsnNode(BIPUSH, v));
		else if(v <= Short.MAX_VALUE)
			insn.add(new IntInsnNode(SIPUSH, v));
		else
			insn.add(new LdcInsnNode(v));
	}
}