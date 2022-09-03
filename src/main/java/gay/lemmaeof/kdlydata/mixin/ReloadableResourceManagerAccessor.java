package gay.lemmaeof.kdlydata.mixin;

import net.minecraft.resource.AutoCloseableResourceManager;
import net.minecraft.resource.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ReloadableResourceManager.class)
public interface ReloadableResourceManagerAccessor {
	@Accessor
	AutoCloseableResourceManager getResources();
}
