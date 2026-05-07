package geo;

import java.util.function.Consumer;

public class BoneAnimation
{
	public Vec3Animation rotation;
	public Vec3Animation position;
	public Vec3Animation scale;
	
	public void visitMolang(Consumer<String> molangVisitor)
	{
		if(rotation != null) rotation.visitMolang(molangVisitor);
		if(position != null) position.visitMolang(molangVisitor);
		if(scale != null) scale.visitMolang(molangVisitor);
	}
}
