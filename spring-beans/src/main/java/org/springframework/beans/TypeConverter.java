/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.lang.reflect.Field;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * 定义类型转换方法的接口。
 * 通常（但不一定）与PropertyEditorRegistry接口一起实现。
 * 注意：由于TypeConverter实现通常基于不安全于线程的PropertyEditor，因此TypeConverter本身也不被视为线程安全的。
 * <p>
 * Interface that defines type conversion methods. Typically (but not necessarily)
 * implemented in conjunction with the {@link PropertyEditorRegistry} interface.
 *
 * <p><b>Note:</b> Since TypeConverter implementations are typically based on
 * {@link java.beans.PropertyEditor PropertyEditors} which aren't thread-safe,
 * TypeConverters themselves are <em>not</em> to be considered as thread-safe either.
 *
 * @author Juergen Hoeller
 * @see SimpleTypeConverter
 * @see BeanWrapperImpl
 * @since 2.0
 */
public interface TypeConverter {

	/**
	 * 将值转换为所需的类型（如果需要，可从字符串转换）。
	 * 从字符串到任何类型的转换通常使用PropertyEditor类的setAsText方法，或者在ConversionService中使用Spring转换器。
	 * <p>
	 * Convert the value to the required type (if necessary from a String).
	 * <p>Conversions from String to any type will typically use the {@code setAsText}
	 * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
	 *
	 * @param value        the value to convert
	 * @param requiredType the type we must convert to
	 *                     (or {@code null} if not known, for example in case of a collection element)
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;

	/**
	 * 将值转换为所需的类型（如果需要，可从字符串转换）。
	 * 从字符串到任何类型的转换通常使用PropertyEditor类的setAsText方法，或者在ConversionService中使用Spring转换器。
	 * <p>
	 * Convert the value to the required type (if necessary from a String).
	 * <p>Conversions from String to any type will typically use the {@code setAsText}
	 * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
	 *
	 * @param value        the value to convert
	 * @param requiredType the type we must convert to
	 *                     (or {@code null} if not known, for example in case of a collection element)
	 * @param methodParam  转换的目标的方法参数(用于分析泛型类型)
	 *                     the method parameter that is the target of the conversion
	 *                     (for analysis of generic types; may be {@code null})
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
							 @Nullable MethodParameter methodParam) throws TypeMismatchException;

	/**
	 * 将值转换为所需的类型（如果需要，可从字符串转换）。
	 * 从字符串到任何类型的转换通常使用PropertyEditor类的setAsText方法，或者在ConversionService中使用Spring转换器。
	 * <p>
	 * Convert the value to the required type (if necessary from a String).
	 * <p>Conversions from String to any type will typically use the {@code setAsText}
	 * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
	 *
	 * @param value        the value to convert
	 * @param requiredType the type we must convert to
	 *                     (or {@code null} if not known, for example in case of a collection element)
	 * @param field        转换的目标的字段(用于分析泛型类型)
	 *                     the reflective field that is the target of the conversion
	 *                     (for analysis of generic types; may be {@code null})
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException;

	/**
	 * 将值转换为所需的类型（如果需要，可从字符串转换）。
	 * 从字符串到任何类型的转换通常使用PropertyEditor类的setAsText方法，或者在ConversionService中使用Spring转换器。
	 * <p>
	 * Convert the value to the required type (if necessary from a String).
	 * <p>Conversions from String to any type will typically use the {@code setAsText}
	 * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
	 *
	 * @param value          the value to convert
	 * @param requiredType   the type we must convert to
	 *                       (or {@code null} if not known, for example in case of a collection element)
	 * @param typeDescriptor the type descriptor to use (may be {@code null}))
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 * @since 5.1.4
	 */
	@Nullable
	default <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
									 @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
	}

}
