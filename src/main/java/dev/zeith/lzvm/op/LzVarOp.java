package dev.zeith.lzvm.op;

import dev.zeith.lzvm.exception.LzVMOperationNotSupportedException;

import java.util.function.*;

public interface LzVarOp
{
	LzVarOp ZERO = readOnly(() -> 0);
	
	static LzVarOp readOnly(ReadonlyLzVarOp getter)
	{
		return getter;
	}
	
	static LzVarOp tempVar()
	{
		return readWrite();
	}
	
	static LzVarOp readWrite(DoubleSupplier get, DoubleConsumer set)
	{
		return new LzVarOp()
		{
			@Override
			public double get()
					throws LzVMOperationNotSupportedException
			{
				return get.getAsDouble();
			}
			
			@Override
			public void set(double value)
					throws LzVMOperationNotSupportedException
			{
				set.accept(value);
			}
		};
	}
	
	static LzVarOp readWrite()
	{
		return new LzVarOp()
		{
			double v;
			
			@Override
			public double get()
					throws LzVMOperationNotSupportedException
			{
				return v;
			}
			
			@Override
			public void set(double value)
					throws LzVMOperationNotSupportedException
			{
				v = value;
			}
		};
	}
	
	double get()
			throws LzVMOperationNotSupportedException;
	
	void set(double value)
			throws LzVMOperationNotSupportedException;
	
	default double get(double index)
	{
		throw new LzVMOperationNotSupportedException();
	}
	
	default void set(double index, double value)
	{
		throw new LzVMOperationNotSupportedException();
	}
	
	interface ReadonlyLzVarOp
			extends LzVarOp
	{
		@Override
		default void set(double value)
				throws LzVMOperationNotSupportedException
		{
			throw new LzVMOperationNotSupportedException();
		}
	}
}