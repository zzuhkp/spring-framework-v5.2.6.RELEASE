/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;

/**
 * 封装 PropertyAccessor 配置方法的接口
 * <p>
 * Interface that encapsulates configuration methods for a PropertyAccessor.
 * Also extends the PropertyEditorRegistry interface, which defines methods
 * for PropertyEditor management.
 *
 * <p>Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see BeanWrapper
 * @since 2.0
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

	/**
	 * 设置 ConversionService 转换属性，以替换 PropertyEditor
	 * <p>
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * 获取 ConversionService
	 * <p>
	 * Return the associated ConversionService, if any.
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * 设置将属性编辑器应用于属性的新值时是否提取旧属性值。
	 * <p>
	 * Set whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 */
	void setExtractOldValueForEditor(boolean extractOldValueForEditor);

	/**
	 * 返回将属性编辑器应用于属性的新值时是否提取旧属性值。
	 * <p>
	 * Return whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 */
	boolean isExtractOldValueForEditor();

	/**
	 * 设置此实例是否应尝试“auto-grow”包含空值的嵌套路径。
	 * <p>
	 * Set whether this instance should attempt to "auto-grow" a
	 * nested path that contains a {@code null} value.
	 * <p>If {@code true}, a {@code null} path location will be populated
	 * with a default object value and traversed instead of resulting in a
	 * {@link NullValueInNestedPathException}.
	 * <p>Default is {@code false} on a plain PropertyAccessor instance.
	 */
	void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

	/**
	 * 返回嵌套路径的“auto-growing”是否已激活。
	 * <p>
	 * Return whether "auto-growing" of nested paths has been activated.
	 */
	boolean isAutoGrowNestedPaths();

}
