package gay.lemmaeof.kdlydata.mixin;

import com.google.gson.*;
import dev.hbeck.kdl.objects.KDLDocument;
import dev.hbeck.kdl.objects.KDLNode;
import dev.hbeck.kdl.objects.KDLValue;
import dev.hbeck.kdl.parse.KDLParser;
import gay.lemmaeof.kdlydata.KdlType;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(JsonDataLoader.class)
public class MixinJsonDataLoader {
	private static final String kdlycontent$KDLY_FILE_SUFFIX = ".kdl";
	private static final int kdlycontent$KDLY_FILE_SUFFIX_LENGTH = kdlycontent$KDLY_FILE_SUFFIX.length();
	private static final KDLParser parser = new KDLParser();

	@Shadow @Final private String dataType;

	@Shadow @Final private static Logger LOGGER;

	@Inject(method = "prepare(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)Ljava/util/Map;", at = @At("RETURN"))
	private void hookKdlLoading(ResourceManager manager, Profiler profiler, CallbackInfoReturnable<Map<Identifier, JsonElement>> info) {
		int dataTypeLength = dataType.length() + 1;
		Map<Identifier, JsonElement> jsons = info.getReturnValue();
		for (Map.Entry<Identifier, Resource> entry : manager.findResources(dataType, path -> path.getPath().endsWith(kdlycontent$KDLY_FILE_SUFFIX)).entrySet()) {
			Identifier identifier = entry.getKey();
			String string = identifier.getPath();
			Identifier dataId = new Identifier(identifier.getNamespace(), string.substring(dataTypeLength, string.length() - kdlycontent$KDLY_FILE_SUFFIX_LENGTH));
			//check for duplicates - safely allows two-way overriding of both JSON and KDL
			if (jsons.containsKey(dataId)) {
				Optional<Resource> res = manager.getResource(new Identifier(dataId.getNamespace(), dataId.getPath() + ".json"));
				if (res.isPresent()) {
					String thisSource = entry.getValue().getSourceName();
					String otherSource = res.get().getSourceName();
					if (thisSource.equals(otherSource)) throw new IllegalStateException("A single resource pack has the same file as both a .json and a .kdl! Priority cannot be determined!");
					if (manager instanceof ReloadableResourceManagerAccessor reloadable) {
						AutoCloseableResourceManager closeable = reloadable.getResources();
						if (closeable instanceof MultiPackResourceManagerAccessor multiPack) {
							Map<String, NamespaceResourceManager> managers = multiPack.getNamespaceManagers();
							NamespaceResourceManager namespace = managers.get(dataId.getNamespace());
							if (namespace == null) throw new IllegalStateException("A resource was somehow registered without a namespace resource manager for it!");
							List<NamespaceResourceManager.PackEntry> entries = ((NamespaceResourceManagerAccessor)namespace).getPacks();
							int thisSourcePriority = -1;
							int otherSourcePriority = -1;
							for (int i = 0; i < entries.size(); i++) {
								if (entries.get(i).name().equals(thisSource)) thisSourcePriority = i;
								if (entries.get(i).name().equals(otherSource)) otherSourcePriority = i;
							}
							if (thisSourcePriority < otherSourcePriority) continue;
						}
					}
				}
			}
			//duplicates are resolved - let's go!
			try {
				KDLDocument doc = parser.parse(entry.getValue().open());
				jsons.put(dataId, parseKdl(doc));
			} catch (IOException | IllegalArgumentException e) {
				LOGGER.error("Coudln't parse data file {} from {}", dataId, identifier, e);
			}
		}
	}

	private JsonElement parseKdl(KDLDocument doc) {
		List<KDLNode> nodes = doc.getNodes();
		if (nodes.size() == 1) {
			KDLNode node = nodes.get(0);
			return switch(parseType(node)) {
				case LITERAL -> parsePrimitive(node.getArgs().get(0));
				case OBJECT -> parseObject(node);
				case ARRAY -> parseArray(node);
			};
		} else {
			return switch(parseType(doc.getNodes())) {
				case LITERAL -> throw new IllegalStateException("unreachable");
				case OBJECT -> parseObject(nodes);
				case ARRAY -> parseArray(nodes);
			};
		}
	}

	private JsonObject parseObject(KDLNode node) {
		JsonObject ret = new JsonObject();
		for (String key : node.getProps().keySet()) {
			ret.add(key, parsePrimitive(node.getProps().get(key)));
		}
		if (node.getChild().isPresent()) {
			JsonObject listObj = parseObject(node.getChild().get().getNodes());
			for (String key : listObj.keySet()) {
				ret.add(key, listObj.get(key));
			}
		}
		return ret;
	}

	private JsonObject parseObject(List<KDLNode> nodes) {
		JsonObject ret = new JsonObject();
		for (KDLNode node : nodes) {
			String key = node.getIdentifier();
			switch (parseType(node)) {
				case LITERAL -> ret.add(key, parsePrimitive(node.getArgs().get(0)));
				case OBJECT -> ret.add(key, parseObject(node));
				case ARRAY -> ret.add(key, parseArray(node));
			}
		}
		return ret;
	}

	private JsonArray parseArray(KDLNode node) {
		JsonArray ret = new JsonArray();
		for (KDLValue<?> val : node.getArgs()) {
			ret.add(parsePrimitive(val));
		}
		if (node.getChild().isPresent()) {
			JsonArray arrayObj = parseArray(node.getChild().get().getNodes());
			ret.addAll(arrayObj);
		}
		return ret;
	}

	private JsonArray parseArray(List<KDLNode> nodes) {
		JsonArray ret = new JsonArray();
		for (KDLNode node : nodes) {
			switch(parseType(node)) {
				case LITERAL -> ret.add(parsePrimitive(node.getArgs().get(0)));
				case OBJECT -> ret.add(parseObject(node));
				case ARRAY -> ret.add(parseArray(node));
			}
		}
		return ret;
	}

	private JsonElement parsePrimitive(KDLValue<?> value) {
		if (value.isNull()) return JsonNull.INSTANCE;
		if (value.isBoolean()) return new JsonPrimitive(value.getAsBooleanOrElse(false));
		if (value.isString()) return new JsonPrimitive(value.getAsString().getValue());
		if (value.isNumber()) return new JsonPrimitive(value.getAsNumberOrElse(0));
		throw new IllegalArgumentException("Unrecognized state for KDL value: " + value);
	}

	private KdlType parseType(KDLNode node) {
		//implementation of Bram Gotink's node type heuristic: https://github.com/kdl-org/kdl/issues/281#issuecomment-1215058690
		if (node.getType().isPresent()) {
			if (node.getType().get().equals("array")) return KdlType.ARRAY;
			if (node.getType().get().equals("object")) return KdlType.OBJECT;
			throw new IllegalArgumentException("Illegal node type hint `" + node.getType().get() + "` found: must be `array` or `object`");
		}
		if (!node.getProps().isEmpty()) return KdlType.OBJECT;
		if (node.getChild().isPresent()) {
			return parseType(node.getChild().get().getNodes());
		}
		if (node.getArgs().size() > 1) return KdlType.ARRAY;
		return KdlType.LITERAL;
	}

	private KdlType parseType(List<KDLNode> nodes) {
		for (KDLNode node : nodes) {
			if (!node.getIdentifier().equals("-")) return KdlType.OBJECT;
		}
		return KdlType.ARRAY;
	}
}
