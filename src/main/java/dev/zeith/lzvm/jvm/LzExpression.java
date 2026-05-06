package dev.zeith.lzvm.jvm;

import dev.zeith.lzvm.LzVariableStore;

public interface LzExpression
		extends LzFactory
{
	double[] EMPTY_DOUBLE_ARRAY = new double[0];
	
	default double get()
	{
		return get(EMPTY_DOUBLE_ARRAY);
	}
	
	double get(double... args);
	
	class ConstantExpression
			implements LzExpression
	{
		protected final double value;
		
		public ConstantExpression(double value)
		{
			this.value = value;
		}
		
		@Override
		public double get(double... args)
		{
			return value;
		}
		
		@Override
		public LzExpression instantiate(LzVariableStore store)
		{
			return this;
		}
	}
}