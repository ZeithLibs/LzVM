package dev.zeith.lzvm;

public class LzMath
{
	public static final double PI = Math.PI;
	public static final double DEG_TO_RAD = (PI / 180);
	public static final double RAD_TO_DEG = (180 / PI);
	
	private static final float[] SIN = new float[65536];
	
	static
	{
		for(int i = 0; i < SIN.length; ++i)
			SIN[i] = (float) Math.sin(i * PI * 2.0 / 65536.0);
	}
	
	public static float sin(float pValue)
	{
		return SIN[(int) (pValue * 10430.378F) & 0xFFFF];
	}
	
	public static float cos(float pValue)
	{
		return SIN[(int) (pValue * 10430.378F + 16384.0F) & 0xFFFF];
	}
	
	public static double random(double low, double high)
	{
		return low + Math.random() * (high - low);
	}
	
	public static int randomInt(int low, int high)
	{
		return (int) Math.round((double) low + Math.random() * (double) (high - low));
	}
	
	public static double dieRoll(int num, double low, double high)
	{
		int total = 0;
		for(int i = 0; i++ < num; total += (int) random(low, high)) ;
		return total;
	}
	
	public static int dieRollInt(int num, int low, int high)
	{
		int total = 0;
		for(int i = 0; i++ < num; total += randomInt(low, high)) ;
		return total;
	}
	
	public static int hermiteBlend(int value)
	{
		return 3 * value ^ 2 - 2 * value ^ 3;
	}
	
	public static double lerp(double start, double end, double amount)
	{
		amount = Math.max(0.0F, Math.min(1.0F, amount));
		return start + (end - start) * amount;
	}
	
	public static double lerpRotate(double start, double end, double amount)
	{
		start = radify(start);
		end = radify(end);
		
		if(start > end)
		{
			double tmp = start;
			start = end;
			end = tmp;
		}
		
		return end - start > 180.0 ? radify(end + amount * (360.0 - (end - start))) : start + amount * (end - start);
	}
	
	public static double radify(double num)
	{
		return ((num + 180.0) % 360.0 + 180.0) % 360.0;
	}
}