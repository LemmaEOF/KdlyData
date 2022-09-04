package gay.lemmaeof.kdlydata.mixin;

import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NamespaceResourceManager.class)
public interface NamespaceResourceManagerAccessor {
	@Accessor
	ResourceType getType();
}
