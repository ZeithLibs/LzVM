package dev.zeith.lzvm.molang.compiler.libs;

import dev.zeith.lzvm.exception.LzVMException;
import dev.zeith.lzvm.jvm.LzMath;
import dev.zeith.lzvm.molang.compiler.*;
import dev.zeith.lzvm.molang.expression.*;
import dev.zeith.lzvm.program.*;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import static dev.zeith.lzvm.jvm.LzMath.*;
import static dev.zeith.lzvm.molang.compiler.MoLangCompiler.*;
import static dev.zeith.lzvm.op.LzOpcodes.*;
import static dev.zeith.lzvm.op.LzOpcodes.MUL;
import static dev.zeith.lzvm.program.ArgType.DOUBLE;
import static dev.zeith.lzvm.program.LzCallInsn.ofDbl;

public enum MoMathLibrary
		implements ICompilerLibrary
{
	INSTANCE;
	
	public final Map<LzCallInsn, IMoFunctionCallTransformer> callTransformers;
	public final Map<String, Function<NameExpression, MLExpression>> nameTransformers;
	
	MoMathLibrary()
	{
		Map<LzCallInsn, IMoFunctionCallTransformer> c = new HashMap<>();
		Map<String, Function<NameExpression, MLExpression>> nt = new HashMap<>();
		
		final String JMath = "java/lang/Math";
		final String JMoMath = getClass().getName().replace('.', '/');
		
		// Constants
		nt.put("math.pi", e -> new NumberExpression(Math.PI));
		
		// Standard Java Math functions:
		c.put(dUnaryOperator("math.sin"), argsAndExtraPure(duOpt(val -> LzMath.sind(val * DEG_TO_RAD)), b -> b.addConstD(DEG_TO_RAD).addInsn(MUL).addInsn(FSIN)));
		c.put(dUnaryOperator("math.cos"), argsAndExtraPure(duOpt(val -> LzMath.cosd(val * DEG_TO_RAD)), b -> b.addConstD(DEG_TO_RAD).addInsn(MUL).addInsn(FCOS)));
		c.put(dBinaryOperator("math.mod"), argsAndExtraPure(dbOpt((left, right) -> left % right), b -> b.addInsn(MOD)));
		c.put(dUnaryOperator("math.acos"), argsAndExtraPure(duOpt(val -> Math.acos(val) * RAD_TO_DEG), b -> b.addJCall(JMath, dUnaryOperator("acos")).addConstD(RAD_TO_DEG).addInsn(MUL)));
		c.put(dUnaryOperator("math.asin"), argsAndExtraPure(duOpt(val -> Math.asin(val) * RAD_TO_DEG), b -> b.addJCall(JMath, dUnaryOperator("asin")).addConstD(RAD_TO_DEG).addInsn(MUL)));
		c.put(dUnaryOperator("math.atan"), argsAndExtraPure(duOpt(val -> Math.atan(val) * RAD_TO_DEG), b -> b.addJCall(JMath, dUnaryOperator("atan")).addConstD(RAD_TO_DEG).addInsn(MUL)));
		c.put(dBinaryOperator("math.atan2"), argsAndExtraPure(dbOpt((a, b) -> Math.atan2(a, b) * RAD_TO_DEG), b -> b.addJCall(JMath, dBinaryOperator("atan2")).addConstD(RAD_TO_DEG).addInsn(MUL)));
		c.put(dUnaryOperator("math.abs"), argsAndExtraPure(duOpt(Math::abs), b -> b.addJCall(JMath, dUnaryOperator("abs"))));
		c.put(dUnaryOperator("math.sqrt"), argsAndExtraPure(duOpt(Math::sqrt), b -> b.addJCall(JMath, dUnaryOperator("sqrt"))));
		c.put(dUnaryOperator("math.ceil"), argsAndExtraPure(duOpt(Math::ceil), b -> b.addJCall(JMath, dUnaryOperator("ceil"))));
		c.put(dUnaryOperator("math.ln"), argsAndExtraPure(duOpt(Math::log), b -> b.addJCall(JMath, dUnaryOperator("log"))));
		c.put(dUnaryOperator("math.exp"), argsAndExtraPure(duOpt(Math::exp), b -> b.addJCall(JMath, dUnaryOperator("exp"))));
		c.put(dBinaryOperator("math.max"), argsAndExtraPure(dbOpt(Math::max), b -> b.addJCall(JMath, dBinaryOperator("max"))));
		c.put(dBinaryOperator("math.min"), argsAndExtraPure(dbOpt(Math::min), b -> b.addJCall(JMath, dBinaryOperator("min"))));
		c.put(dUnaryOperator("math.floor"), argsAndExtraPure(duOpt(Math::floor), b -> b.addJCall(JMath, dUnaryOperator("floor"))));
		
		// MoMath functions
		c.put(dUnaryOperator("math.min_angle"), argsAndExtraPure(duOpt(MoMathLibrary::minAngle), b -> b.addJCall(JMoMath, dUnaryOperator("minAngle"))));
		c.put(dUnaryOperator("math.hermite_blend"), argsAndExtraPure(duOpt(MoMathLibrary::hermiteBlend), b -> b.addJCall(JMoMath, dUnaryOperator("hermiteBlend"))));
		c.put(dTernaryOperator("math.lerp"), argsAndExtraPure(dtOpt(MoMathLibrary::lerp), b -> b.addJCall(JMoMath, dTernaryOperator("lerp"))));
		c.put(dTernaryOperator("math.clamp"), argsAndExtraPure(dtOpt(MoMathLibrary::clamp), b -> b.addJCall(JMoMath, dTernaryOperator("clamp"))));
		c.put(dTernaryOperator("math.lerprotate"), argsAndExtraPure(dtOpt(MoMathLibrary::lerpRotate), b -> b.addJCall(JMoMath, dTernaryOperator("lerpRotate"))));
		c.put(dUnaryOperator("math.trunc"), argsAndExtraPure(duOpt(MoMathLibrary::trunc), b -> b.addJCall(JMoMath, dUnaryOperator("trunc"))));
		c.put(dUnaryOperator("math.round"), argsAndExtraPure(duOpt(MoMathLibrary::round), b -> b.addJCall(JMoMath, dUnaryOperator("round"))));
		
		// Random functions may never be optimized
		c.put(dBinaryOperator("math.random"), argsAndExtra(false, null, b -> b.addJCall(JMoMath, dBinaryOperator("random"))));
		c.put(dBinaryOperator("math.random_integer"), argsAndExtra(false, null, b -> b.addJCall(JMoMath, dBinaryOperator("randomInt"))));
		c.put(dTernaryOperator("math.die_roll"), argsAndExtra(false, null, b -> b.addJCall(JMoMath, dTernaryOperator("dieRoll"))));
		c.put(dTernaryOperator("math.die_roll_integer"), argsAndExtra(false, null, b -> b.addJCall(JMoMath, dTernaryOperator("dieRollInt"))));
		
		c.put(dBinaryOperator("math.pow"),
				argsAndExtraPure(
						call ->
						{
							OptionalDouble opt1 = call.getChildren()[0].asOptimizedDouble();
							OptionalDouble opt2 = call.getChildren()[1].asOptimizedDouble();
							if(opt1.isPresent() && opt2.isPresent()) return new NumberExpression(Math.pow(opt1.getAsDouble(), opt2.getAsDouble()));
							else if(opt2.isPresent() && LzMath.isOne(opt2.getAsDouble()))
								return call.getChildren()[0];
							return null;
						},
						b -> b.addJCall(JMath, ofDbl("pow", DOUBLE, DOUBLE))
				)
		);
		
		this.callTransformers = Collections.unmodifiableMap(c);
		this.nameTransformers = Collections.unmodifiableMap(nt);
		
		// https://wiki.bedrock.dev/concepts/molang
//		ensureTransformersExist(
//				"math.abs(x)	Absolute value of x",
//				"math.acos(x)	Arccosine (inverse cosine) of x",
//				"math.asin(x)	Arcsine (inverse sine) of x",
//				"math.atan(x)	Arctangent (inverse tangent) of x",
//				"math.atan2(y, x)	Arctangent of y / x — returns angle in degrees",
//				"math.ceil(x)	Round x up to the nearest integer",
//				"math.clamp(x, min, max)	Constrain x between min and max",
//				"math.cos(x)	Cosine of x degrees",
//				"math.die_roll(n, low, high)	Roll n floats between low and high and sum them",
//				"math.die_roll_integer(n, low, high)	Same as above but rolls integers",
//				"math.exp(x)	Exponential (e^x)",
//				"math.floor(x)	Round x down to the nearest integer",
//				"math.hermite_blend(t)	Smooth curve: 3t^2 - 2t^3, good for eased interpolation",
//				"math.lerp(a, b, t)	Linearly interpolate between a and b by t",
//				"math.lerprotate(a, b, t)	Rotational interpolation, shortest path around a circle",
//				"math.ln(x)	Natural logarithm of x",
//				"math.max(a, b)	Larger of a or b",
//				"math.min(a, b)	Smaller of a or b",
//				"math.min_angle(x)	Clamp angle x to the range -180° to 180°",
//				"math.mod(a, b)	Remainder of a / b",
//				"math.pi	Constant for π (approximately 3.14159)",
//				"math.pow(base, exponent)	Raise base to the exponent power",
//				"math.random(low, high)	Random float between low and high",
//				"math.random_integer(low, high)	Random integer between low and high",
//				"math.round(x)	Round x to the nearest integer",
//				"math.sin(x)	Sine of x degrees",
//				"math.sqrt(x)	Square root of x",
//				"math.trunc(x) Remove fractional part of x (round toward zero)"
//		);
	}
	
	@Override
	public void register(MoLangCompiler c)
	{
		nameTransformers.forEach(c::registerName);
		callTransformers.forEach(c::registerTransformer);
	}
	
	private void ensureTransformersExist(String... names)
	{
		final Pattern LINE_MAPPER = Pattern.compile("^(?<call>[\\w.]+)([(\\sx)]+.+)$");
		HashSet<String> missing = Arrays.stream(names).map(s ->
		{
			Matcher m = LINE_MAPPER.matcher(s);
			if(m.find()) return m.group("call");
			return s;
		}).collect(Collectors.toCollection(HashSet::new));
		missing.removeAll(nameTransformers.keySet());
		callTransformers.keySet().stream().map(LzCallInsn::getName).collect(Collectors.toList()).forEach(missing::remove);
		if(missing.isEmpty()) return;
		throw new LzVMException("MoMath is missing functions: " + String.join(", ", missing));
	}
	
	private Function<FuncCallExpression, MLExpression> duOpt(DoubleUnaryOperator operator)
	{
		return call ->
		{
			OptionalDouble opt = call.getChildren()[0].asOptimizedDouble();
			if(opt.isPresent()) return new NumberExpression(operator.applyAsDouble(opt.getAsDouble()));
			return null;
		};
	}
	
	private Function<FuncCallExpression, MLExpression> dbOpt(DoubleBinaryOperator operator)
	{
		return call ->
		{
			OptionalDouble opt1 = call.getChildren()[0].asOptimizedDouble();
			OptionalDouble opt2 = call.getChildren()[1].asOptimizedDouble();
			if(opt1.isPresent() && opt2.isPresent()) return new NumberExpression(operator.applyAsDouble(opt1.getAsDouble(), opt2.getAsDouble()));
			return null;
		};
	}
	
	private Function<FuncCallExpression, MLExpression> dtOpt(DoubleTernaryOperator operator)
	{
		return call ->
		{
			OptionalDouble opt1 = call.getChildren()[0].asOptimizedDouble();
			OptionalDouble opt2 = call.getChildren()[1].asOptimizedDouble();
			OptionalDouble opt3 = call.getChildren()[2].asOptimizedDouble();
			if(opt1.isPresent() && opt2.isPresent() && opt3.isPresent()) return new NumberExpression(operator.applyAsDouble(opt1.getAsDouble(), opt2.getAsDouble(), opt3.getAsDouble()));
			return null;
		};
	}
	
	@FunctionalInterface
	public interface DoubleTernaryOperator
	{
		double applyAsDouble(double a, double b, double c);
	}
	
	public static double hermiteBlend(double t)
	{
		return 3 * t * t - 2 * t * t * t;
	}
	
	public static double minAngle(double angle)
	{
		return ((angle + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
	}
	
	public static double random(double low, double high)
	{
		return low + Math.random() * (high - low);
	}
	
	public static double randomInt(double low, double high)
	{
		return Math.round((long) low + Math.random() * (long) (high - low));
	}
	
	public static double lerp(double start, double end, double amount)
	{
		amount = Math.max(0.0F, Math.min(1.0F, amount));
		return start + (end - start) * amount;
	}
	
	public static double clamp(double x, double min, double max)
	{
		return Math.max(min, Math.min(max, x));
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
	
	public static double dieRoll(double num, double low, double high)
	{
		int total = 0;
		for(int i = 0; i++ < num; total += (int) random(low, high)) ;
		return total;
	}
	
	public static double dieRollInt(double num, double low, double high)
	{
		double total = 0;
		for(int i = 0; i++ < num; total += randomInt(low, high)) ;
		return total;
	}
	
	public static double trunc(double x)
	{
		return (int) x;
	}
	
	public static double round(double x)
	{
		return Math.round(x);
	}
}