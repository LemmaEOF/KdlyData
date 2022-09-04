package gay.lemmaeof.kdlydata;

import com.google.gson.JsonElement;
import dev.hbeck.kdl.objects.KDLDocument;
import dev.hbeck.kdl.parse.KDLParser;
import gay.lemmaeof.kdlydata.mixin.MixinNamespaceResourceManager;
import gay.lemmaeof.kdlydata.mixin.NamespaceResourceManagerAccessor;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceMetadata;
import net.minecraft.resource.pack.ResourcePack;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Hooks {
	private static final KDLParser PARSER = new KDLParser();
	public static final ThreadLocal<Boolean> predicateIsJson = new ThreadLocal<>();
	public static final ThreadLocal<NamespaceResourceManagerAccessor> thisManager = new ThreadLocal<>();

	public static Resource.InputSupplier<InputStream> getKdlyInputStreamSupplier(NamespaceResourceManagerAccessor manager, Identifier path, ResourcePack pack) throws IOException, IllegalArgumentException {
		InputStream input = pack.open(manager.getType(), path);
		KDLDocument doc = PARSER.parse(input);
		JsonElement elem = KdlProcessor.parseKdl(doc);
		return () -> new ByteArrayInputStream(elem.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static Resource entryAsKdlyResource(NamespaceResourceManagerAccessor manager, NamespaceResourceManager.ResourceEntry entry) {
		MixinNamespaceResourceManager.ResourceEntryAccessor accessor = (MixinNamespaceResourceManager.ResourceEntryAccessor) entry;
		String string = accessor.getSource().getName();
		try {
			return accessor.getHasMetadata() ? new Resource(string, getKdlyInputStreamSupplier(manager, accessor.getId(), accessor.getSource()), () -> {
				if (accessor.getSource().contains(manager.getType(), accessor.getMetadataId())) {
					InputStream inputStream = accessor.getSource().open(manager.getType(), accessor.getMetadataId());

					ResourceMetadata metadata;
					try {
						metadata = ResourceMetadata.fromInputStream(inputStream);
					} catch (Throwable t) {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (Throwable t2) {
								t.addSuppressed(t2);
							}
						}

						throw t;
					}

					inputStream.close();

					return metadata;
				} else {
					return ResourceMetadata.EMPTY;
				}
			}) : new Resource(string, getKdlyInputStreamSupplier(manager, accessor.getId(), accessor.getSource()));
		} catch (IOException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}
}
