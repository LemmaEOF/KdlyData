package gay.lemmaeof.kdlydata;

import com.google.gson.*;
import dev.hbeck.kdl.objects.KDLDocument;
import dev.hbeck.kdl.objects.KDLNode;
import dev.hbeck.kdl.objects.KDLValue;

import java.util.List;

public class KdlProcessor {
	public static JsonElement parseKdl(KDLDocument doc) {
		List<KDLNode> nodes = doc.getNodes();
		return switch(parseType(nodes)) {
			case LITERAL -> throw new IllegalStateException("unreachable");
			case OBJECT -> parseObject(nodes);
			case ARRAY -> parseArray(nodes);
		};
	}

	public static JsonObject parseObject(KDLNode node) {
		JsonObject ret = new JsonObject();
		for (String key : node.getProps().keySet()) {
			ret.add(key, parseLiteral(node.getProps().get(key)));
		}
		if (node.getChild().isPresent()) {
			JsonObject listObj = parseObject(node.getChild().get().getNodes());
			for (String key : listObj.keySet()) {
				ret.add(key, listObj.get(key));
			}
		}
		return ret;
	}

	public static JsonObject parseObject(List<KDLNode> nodes) {
		JsonObject ret = new JsonObject();
		for (KDLNode node : nodes) {
			String key = node.getIdentifier();
			switch (parseType(node)) {
				case LITERAL -> ret.add(key, parseLiteral(node.getArgs().get(0)));
				case OBJECT -> ret.add(key, parseObject(node));
				case ARRAY -> ret.add(key, parseArray(node));
			}
		}
		return ret;
	}

	public static JsonArray parseArray(KDLNode node) {
		JsonArray ret = new JsonArray();
		for (KDLValue<?> val : node.getArgs()) {
			ret.add(parseLiteral(val));
		}
		if (node.getChild().isPresent()) {
			JsonArray arrayObj = parseArray(node.getChild().get().getNodes());
			ret.addAll(arrayObj);
		}
		return ret;
	}

	public static JsonArray parseArray(List<KDLNode> nodes) {
		JsonArray ret = new JsonArray();
		for (KDLNode node : nodes) {
			switch(parseType(node)) {
				case LITERAL -> ret.add(parseLiteral(node.getArgs().get(0)));
				case OBJECT -> ret.add(parseObject(node));
				case ARRAY -> ret.add(parseArray(node));
			}
		}
		return ret;
	}

	public static JsonElement parseLiteral(KDLValue<?> value) {
		if (value.isNull()) return JsonNull.INSTANCE;
		if (value.isBoolean()) return new JsonPrimitive(value.getAsBooleanOrElse(false));
		if (value.isString()) return new JsonPrimitive(value.getAsString().getValue());
		if (value.isNumber()) return new JsonPrimitive(value.getAsNumberOrElse(0));
		throw new IllegalArgumentException("Unrecognized state for KDL value: " + value);
	}

	public static KdlType parseType(KDLNode node) {
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

	public static KdlType parseType(List<KDLNode> nodes) {
		for (KDLNode node : nodes) {
			if (!node.getIdentifier().equals("-")) return KdlType.OBJECT;
		}
		return KdlType.ARRAY;
	}

	public enum KdlType {
		OBJECT,
		ARRAY,
		LITERAL
	}

}
