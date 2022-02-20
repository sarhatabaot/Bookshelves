package org.inventivetalent.bookshelves.utils;

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.inventivetalent.bookshelves.Bookshelves;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public class MetaHelper {

	private MetaHelper() {
		throw new UnsupportedOperationException();
	}
	@Contract("_, _, _ -> param3")
	public static <T> T setMetaValue(@NotNull Metadatable meta, String key, T value) {
		meta.setMetadata(key, new FixedMetadataValue(Bookshelves.instance, value));
		return value;
	}

	@SuppressWarnings("unchecked")
	public static <T> @Nullable T getMetaValue(@NotNull Metadatable metadatable, String key, Class<T> clazz) {
		List<MetadataValue> meta = metadatable.getMetadata(key);
		for (MetadataValue m : meta) {
			if (clazz.isInstance(m.value()) || m.value().getClass().isAssignableFrom(clazz)) {
				return (T) m.value();
			}
		}
		return null;
	}

}
