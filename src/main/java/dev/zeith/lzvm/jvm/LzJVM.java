package dev.zeith.lzvm.jvm;

import dev.zeith.lzvm.LzVariableStore;
import dev.zeith.lzvm.program.LzProgram;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static java.lang.ClassLoader.getSystemClassLoader;

public class LzJVM
{
	public static final LzClassLoader LZ_CLASS_LOADER = new LzClassLoader(getSystemClassLoader());
	
	public static LzFactory compile(LzProgram program, int argCount)
	{
		return compile(program, argCount, LZ_CLASS_LOADER);
	}
	
	@SneakyThrows
	public static LzFactory compile(LzProgram program, int argCount, IClassDefiner definer)
	{
		byte[] bytecode = LzJvmCompiler.compile(LzExpression.class.getName() + "_" + UUID.randomUUID().toString().replace('-', '_'), program, argCount);
		
		Constructor<? extends LzExpression> ctor = definer
				.defineClass(bytecode)
				.asSubclass(LzExpression.class)
				.getDeclaredConstructor(LzVariableStore.class);
		
		return a ->
		{
			try
			{
				return ctor.newInstance(a);
			} catch(ReflectiveOperationException e)
			{
				throw new RuntimeException(e);
			}
		};
	}
	
	public static class LzClassLoader
			extends ClassLoader
			implements IClassDefiner
	{
		public LzClassLoader(ClassLoader parent)
		{
			super(parent);
		}
		
		// @Override
		public String getName()
		{
			return "LzClassLoader";
		}
		
		@Override
		public Class<?> defineClass(byte[] bytecode)
		{
			return defineClass(null, bytecode, 0, bytecode.length);
		}
	}
}