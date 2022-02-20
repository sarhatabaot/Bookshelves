package org.inventivetalent.bookshelves.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class AccessUtil {
	private AccessUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set a specified Field accessible
	 *
	 * @param field Field set accessible
	 */
	@Contract("_ -> param1")
	public static @NotNull Field setAccessible(@NotNull Field field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		field.setAccessible(true);
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEF);
		return field;
	}

	/**
	 * Set a specified Method accessible
	 *
	 * @param m Method set accessible
	 */
	@Contract("_ -> param1")
	public static @NotNull Method setAccessible(@NotNull Method m) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		m.setAccessible(true);
		return m;
	}

	@Contract("_ -> param1")
	public static @NotNull Constructor setAccessible(@NotNull Constructor c) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		c.setAccessible(true);
		return c;
	}

}