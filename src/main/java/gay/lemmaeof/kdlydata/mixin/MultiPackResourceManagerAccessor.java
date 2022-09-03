package gay.lemmaeof.kdlydata.mixin;

import net.minecraft.resource.MultiPackResourceManager;
import net.minecraft.resource.NamespaceResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(MultiPackResourceManager.class)
public interface MultiPackResourceManagerAccessor {
	@Accessor
	Map<String, NamespaceResourceManager> getNamespaceManagers();
}
