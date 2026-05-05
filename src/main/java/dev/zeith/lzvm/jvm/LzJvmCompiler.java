package dev.zeith.lzvm.jvm;

import dev.zeith.lzvm.op.LzOpcodes;
import dev.zeith.lzvm.program.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class LzJvmCompiler
{
	public static final String LzExpression = dev.zeith.lzvm.jvm.LzExpression.class.getName().replace('.', '/');
	public static final String LzVariableStore = dev.zeith.lzvm.LzVariableStore.class.getName().replace('.', '/');
	public static final String LzVarOp = dev.zeith.lzvm.op.LzVarOp.class.getName().replace('.', '/');
	public static final String LzCallOp = dev.zeith.lzvm.op.LzCallOp.class.getName().replace('.', '/');
	public static final String LzGenerated = Generated.class.getName().replace('.', '/');
	
	public static final String L_LzVariableStore = "L" + LzVariableStore + ";";
	public static final String L_LzVarOp = "L" + LzVarOp + ";";
	public static final String L_LzCallOp = "L" + LzCallOp + ";";
	public static final String L_LzGenerated = "L" + LzGenerated + ";";
	
	public static byte[] compile(String name, LzProgram program, int argCount)
	{
		LzProgramBody body = program.body;
		
		ClassNode cn = new ClassNode();
		cn.version = V1_8;
		cn.access = ACC_PUBLIC;
		cn.name = name.replace('.', '/');
		cn.superName = "java/lang/Object";
		cn.interfaces = Collections.singletonList(LzExpression);
		
		Set<String> usedFieldNames = new HashSet<>();
		Set<String> usedMethodNames = new HashSet<>();
		
		// ctor
		MethodNode ctor = new MethodNode(ACC_PUBLIC, "<init>", "(" + L_LzVariableStore + ")V", null, null);
		ctor.instructions.add(new VarInsnNode(ALOAD, 0));
		ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL, cn.superName, "<init>", "()V", false));
		ctor.instructions.add(new InsnNode(RETURN));
		cn.methods.add(ctor);
		
		Function<String, FieldNode> varGetter = getVarFieldHelper(cn, usedFieldNames, ctor);
		Function<LzCallInsn, FieldNode> callGetter = getCallFieldHelper(cn, usedFieldNames, ctor);
		
		// method: toString()
		MethodNode toString = new MethodNode(
				ACC_PUBLIC,
				"toString",
				"()Ljava/lang/String;",
				null,
				null
		);
		toString.instructions.add(new LdcInsnNode(dev.zeith.lzvm.jvm.LzExpression.class.getSimpleName() + "{gen: " + LzOpcodes.disassemble(body.insnList, false) + "}"));
		toString.instructions.add(new InsnNode(ARETURN));
		cn.methods.add(toString);
		
		// method: get(LzVariableStore, [D)D
		MethodNode m = new MethodNode(
				ACC_PUBLIC,
				"get",
				"([D)D",
				null,
				null
		);
		usedMethodNames.add("get");
		
		m.visibleAnnotations = generated(LzOpcodes.disassemble(body.insnList, true), Collections.emptyMap());
		
		InsnList insn = m.instructions;
		
		Map<LzCallInsn, MethodNode> registeredNodes = new HashMap<>();
		
		
		// create locals array
		pushInt(insn, program.info.maxLocals); // some extra temp space
		insn.add(new IntInsnNode(NEWARRAY, T_DOUBLE));
		insn.add(new VarInsnNode(ASTORE, 2)); // locals at slot 2
		
		int[] code = body.insnList;
		
		// --- Label mapping ---
		Map<Integer, LabelNode> labelMap = new HashMap<>();
		int labelIndex = 0;
		
		for(int j : code)
		{
			if(j == LzOpcodes.LABEL)
			{
				labelMap.put(labelIndex++, new LabelNode());
			}
		}
		
		labelIndex = 0;
		
		// --- Emit instructions ---
		for(int i = 0; i < code.length; i++)
		{
			int op = code[i];
			
			switch(op)
			{
				case LzOpcodes.RETURN:
				{
					insn.add(new InsnNode(DRETURN));
				}
				break;
				
				case LzOpcodes.CONST:
				{
					int idx = code[++i];
					double val = body.dConstTable[idx];
					insn.add(new LdcInsnNode(val));
				}
				break;
				
				case LzOpcodes.LOAD:
				{
					int idx = code[++i];
					
					insn.add(new VarInsnNode(ALOAD, idx < argCount ? 1 : 2));
					pushInt(insn, idx);
					insn.add(new InsnNode(DALOAD));
				}
				break;
				
				case LzOpcodes.STORE:
				{
					int idx = code[++i];
					insn.add(new VarInsnNode(DSTORE, 3));
					insn.add(new VarInsnNode(ALOAD, idx < argCount ? 1 : 2));
					pushInt(insn, idx);
					insn.add(new VarInsnNode(DLOAD, 3));
					insn.add(new InsnNode(DASTORE));
				}
				break;
				
				case LzOpcodes.ADD:
					insn.add(new InsnNode(DADD)); break;
				case LzOpcodes.SUB:
					insn.add(new InsnNode(DSUB)); break;
				case LzOpcodes.MUL:
					insn.add(new InsnNode(DMUL)); break;
				case LzOpcodes.DIV:
					insn.add(new InsnNode(DDIV)); break;
				
				case LzOpcodes.READ:
				{
					int idx = code[++i];
					String nameStr = body.varTable[idx];
					FieldNode varHolder = varGetter.apply(nameStr);
					
					insn.add(new VarInsnNode(ALOAD, 0));
					insn.add(new FieldInsnNode(GETFIELD, cn.name, varHolder.name, varHolder.desc));
					insn.add(new MethodInsnNode(
							INVOKEINTERFACE,
							LzVarOp,
							"get",
							"()D",
							true
					));
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
					
					insn.add(new MethodInsnNode(
							INVOKEINTERFACE,
							LzVarOp,
							"set",
							"(D)V",
							true
					));
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
				
				// FIXME
				case LzOpcodes.CALL:
				{
					int callIdx = code[++i];
					LzCallInsn call = body.callTable[callIdx];
					
					MethodNode method;
					if((method = registeredNodes.get(call)) == null)
					{
						FieldNode callField = callGetter.apply(call);
						registeredNodes.put(call, method = createCallMethod(call, cn, usedMethodNames, callField));
					}
					
					// Add the lookup into the stack
					insn.add(new VarInsnNode(ALOAD, 0));
					
					// Call the method
					insn.add(new MethodInsnNode(
							INVOKESTATIC,
							cn.name,
							method.name,
							method.desc,
							false
					));
				}
				break;
				
				default:
					throw new IllegalStateException("Unknown opcode: " + op);
			}
		}
		
		cn.methods.add(m);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
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
					
					AbstractInsnNode ret = null;
					
					for(AbstractInsnNode insnNode : ctor.instructions)
						if(insnNode.getOpcode() == RETURN)
						{
							ret = insnNode;
							break;
						}
					
					inject.add(new VarInsnNode(ALOAD, 0)); // this
					inject.add(new VarInsnNode(ALOAD, 1)); // store
					inject.add(new LdcInsnNode(fName)); // string
					inject.add(new MethodInsnNode( // call
							INVOKEINTERFACE,
							LzVariableStore,
							"findVar",
							"(Ljava/lang/String;)" + L_LzVarOp,
							true
					));
					inject.add(new FieldInsnNode(PUTFIELD, owner.name, fn.name, fn.desc)); // store to field
					
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
					fn.visibleAnnotations = generated(call.name + call.descriptor(), Collections.emptyMap());
					owner.fields.add(fn);
					
					InsnList inject = new InsnList();
					
					AbstractInsnNode ret = null;
					for(AbstractInsnNode insnNode : ctor.instructions)
						if(insnNode.getOpcode() == RETURN)
						{
							ret = insnNode;
							break;
						}
					
					inject.add(new VarInsnNode(ALOAD, 0)); // this
					inject.add(new VarInsnNode(ALOAD, 1)); // store
					inject.add(new LdcInsnNode(call.name)); // string
					inject.add(new LdcInsnNode(call.descriptor())); // string
					inject.add(new MethodInsnNode( // call
							INVOKEINTERFACE,
							LzVariableStore,
							"findCall",
							"(Ljava/lang/String;Ljava/lang/String;)" + L_LzCallOp,
							true
					));
					inject.add(new FieldInsnNode(PUTFIELD, owner.name, fn.name, fn.desc)); // store to field
					
					ctor.instructions.insertBefore(ret, inject);
					
					return fn;
				}
		);
	}
	
	private static MethodNode createCallMethod(LzCallInsn call, ClassNode owner, Set<String> usedMethodNames, FieldNode callField)
	{
		StringBuilder desc = new StringBuilder("(");
		
		for(ArgType t : call.argTypes)
		{
			if(t == ArgType.DOUBLE)
				desc.append("D");
			else
				desc.append("Ljava/lang/String;");
		}
		
		desc.append("L").append(owner.name).append(";)D");
		
		MethodNode m = new MethodNode(
				ACC_PRIVATE | ACC_STATIC,
				JavaNamingConventions.processUniqueMethodName(call.name, usedMethodNames),
				desc.toString(),
				null,
				null
		);
		
		Map<String, Object> props = new HashMap<>();
		props.put("argCount", call.argCount);
		m.visibleAnnotations = generated(call.name, props);
		
		InsnList insn = m.instructions;
		
		int argc = call.argTypes.length;
		
		// -----------------------------
		// 1. Compute JVM slot layout properly
		// -----------------------------
		int[] slots = new int[argc + 1];
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
			pushInt(insn, argc - 1 - i);
			
			int slot = slots[i];
			
			if(call.argTypes[i] == ArgType.DOUBLE)
			{
				insn.add(new VarInsnNode(DLOAD, slot));
				insn.add(new MethodInsnNode(
						INVOKESTATIC,
						"java/lang/Double",
						"valueOf",
						"(D)Ljava/lang/Double;",
						false
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
		insn.add(new MethodInsnNode(
				INVOKEINTERFACE,
				LzCallOp,
				"call",
				"([Ljava/lang/Object;)D",
				true
		));
		
		insn.add(new InsnNode(DRETURN));
		
		owner.methods.add(m);
		return m;
	}
	
	private static List<AnnotationNode> generated(String expression, Map<String, Object> rest)
	{
		AnnotationNode an = new AnnotationNode(L_LzGenerated);
		an.values = new ArrayList<>();
		an.values.add("expression");
		an.values.add(expression);
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