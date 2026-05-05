package dev.zeith.lzvm.jvm;

public class LzFMath
{
	private static final float[] SIN = new float[65536];
	
	static
	{
		for(int i = 0; i < SIN.length; ++i)
			SIN[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
	}
	
	public static float sin(float pValue)
	{
		return SIN[(int) (pValue * 10430.378F) & 0xFFFF];
	}
	
	public static float cos(float pValue)
	{
		return SIN[(int) (pValue * 10430.378F + 16384F) & 0xFFFF];
	}
	
	public static double sind(double pValue)
	{
		return SIN[(int) (pValue * 10430.378F) & 0xFFFF];
	}
	
	public static double cosd(double pValue)
	{
		return SIN[(int) (pValue * 10430.378F + 16384F) & 0xFFFF];
	}
}