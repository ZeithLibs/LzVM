package geo;

public abstract class ValueCurve
{
	public abstract Object getRaw();
	
	public static class NumConstant
			extends ValueCurve
	{
		public final Number x;
		
		public NumConstant(Number x)
		{
			this.x = x;
		}
		
		@Override
		public Object getRaw()
		{
			return x;
		}
	}
	
	public static class StringConstant
			extends ValueCurve
	{
		public final String x;
		
		public StringConstant(String x)
		{
			this.x = x;
		}
		
		@Override
		public Object getRaw()
		{
			return x;
		}
	}
	
	public static class UnknownCurve
			extends ValueCurve
	{
		public final Object x;
		
		public UnknownCurve(Object x)
		{
			this.x = x;
		}
		
		@Override
		public Object getRaw()
		{
			return x;
		}
	}
}