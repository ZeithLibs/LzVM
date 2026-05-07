package geo;

import java.util.Map;
import java.util.function.Consumer;

public class AnimationFile
{
	public String loop;
	public double animation_length = Double.NaN;
	public Map<String, BoneAnimation> bones;
	
	public void visitMolang(Consumer<String> molangVisitor)
	{
		if(bones == null) return;
		for(BoneAnimation bone : bones.values())
		{
			bone.visitMolang(molangVisitor);
		}
	}
}