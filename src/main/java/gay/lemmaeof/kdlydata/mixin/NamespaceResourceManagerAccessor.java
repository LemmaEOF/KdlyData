package gay.lemmaeof.kdlydata.mixin;

import net.minecraft.resource.NamespaceResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(NamespaceResourceManager.class)
public interface NamespaceResourceManagerAccessor {
	@Accessor
	List<NamespaceResourceManager.PackEntry> getPacks();
}
