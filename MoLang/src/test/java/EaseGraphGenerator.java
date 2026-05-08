import dev.zeith.lzvm.jvm.*;
import dev.zeith.lzvm.molang.compiler.*;
import dev.zeith.lzvm.molang.compiler.libs.MoLangEasing;
import dev.zeith.lzvm.op.LzVarOp;
import dev.zeith.lzvm.program.LzCallInsn;
import motest.util.LzTestRunner;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class EaseGraphGenerator
{
	public static void main(String[] args)
	{
		Map<LzCallInsn, IMoFunctionCallTransformer> binds = new HashMap<>();
		MoLangEasing.bind(binds);
		
		MoLangCompiler mocomp = new MoLangCompiler();
		LzJvmCompiler comp = new LzJvmCompiler();
		
		IClassDefiner def = new LzJVM.LzClassLoader();
		
		File gdir = new File("run", "graphs");
		if(!gdir.isDirectory()) gdir.mkdirs();
		
		for(LzCallInsn insn : binds.keySet())
		{
			String expr = insn.getName();
			try
			{
				LzFactory fac = mocomp.parseFactory(comp, "math." + expr + "(0, 1, q.x)", def);
				ImageIO.write(generate(fac, 256), "png", new File(gdir, expr + ".png"));
			} catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	public static BufferedImage generate(LzFactory fac, int resolution)
	{
		LzTestRunner.DummyVariableStore vars = new LzTestRunner.DummyVariableStore();
		LzVarOp x = vars.findVar("query.x");
		
		LzExpression expression = fac.instantiate(vars);
		
		BufferedImage image = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_ARGB);
		
		int[] xPoints = new int[resolution];
		int[] yPoints = new int[resolution];
		
		for(int i = 0; i < resolution; i++)
		{
			xPoints[i] = i;
			x.set(i / (double) (resolution - 1));
			double y = 1 - expression.get();
			yPoints[i] = Math.max(0, Math.min(resolution - 1, (int) Math.round(y * resolution)));
		}
		
		Graphics2D gfx = image.createGraphics();
		gfx.setBackground(Color.WHITE);
		gfx.clearRect(0, 0, image.getWidth(), image.getHeight());
		gfx.setColor(Color.BLACK);
		gfx.drawPolyline(xPoints, yPoints, resolution);
		gfx.dispose();
		
		return image;
	}
}