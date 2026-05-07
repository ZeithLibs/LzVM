package geo;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Consumer;

public class Vec3Animation
{
	public final ValueCurve[] value = new ValueCurve[3];
	public final boolean isSeparate;
	
	public Vec3Animation(ValueCurve value)
	{
		Arrays.fill(this.value, value);
		this.isSeparate = false;
	}
	
	public Vec3Animation(ValueCurve[] values)
	{
		System.arraycopy(values, 0, this.value, 0, values.length);
		this.isSeparate = true;
	}
	
	public void visitMolang(Consumer<String> molangVisitor)
	{
		for(int i = 0, len = isSeparate ? value.length : 1; i < len; i++)
		{
			if(value[i] instanceof ValueCurve.StringConstant)
			{
				molangVisitor.accept(((ValueCurve.StringConstant) value[i]).x);
			}
		}
	}
	
	public static class Vec3AnimationDeserializer
			implements JsonDeserializer<Vec3Animation>
	{
		@Override
		public Vec3Animation deserialize(JsonElement e, Type type, JsonDeserializationContext ctx)
				throws JsonParseException
		{
			if(e.isJsonPrimitive())
			{
				JsonPrimitive prim = e.getAsJsonPrimitive();
				if(prim.isNumber()) return new Vec3Animation(new ValueCurve.NumConstant(prim.getAsNumber()));
				return new Vec3Animation(new ValueCurve.StringConstant(prim.getAsString()));
			} else if(e.isJsonArray())
			{
				JsonArray arr = e.getAsJsonArray();
				ValueCurve[] curves = new ValueCurve[arr.size()];
				for(int i = 0; i < arr.size(); i++)
				{
					Vec3Animation a = ctx.deserialize(arr.get(i), Vec3Animation.class);
					if(a.isSeparate) throw new JsonParseException("Vec3 animation fields may not animate separate axis inside a specific axis.");
					curves[i] = a.value[0];
				}
				return new Vec3Animation(curves);
			}
			return new Vec3Animation(new ValueCurve.UnknownCurve(e));
		}
	}
}
