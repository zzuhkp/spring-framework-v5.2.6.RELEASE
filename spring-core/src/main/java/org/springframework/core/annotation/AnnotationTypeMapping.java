/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 在根注解类型的上下文中提供单个注解（或元注解）的映射信息。
 * <p>
 * Provides mapping information for a single annotation (or meta-annotation) in the context of a root annotation type.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see AnnotationTypeMappings
 * @since 5.2
 */
final class AnnotationTypeMapping {

	private static final MirrorSet[] EMPTY_MIRROR_SETS = new MirrorSet[0];

	/**
	 * 源注解，即当前注解为元注解时，当前注解标注的注解
	 */
	@Nullable
	private final AnnotationTypeMapping source;

	/**
	 * 根注解，即当前注解为元注解时，在注解层次结构中查找到的标注的非元注解
	 */
	private final AnnotationTypeMapping root;

	/**
	 * 注解距离根注解的距离，如果当前注解为根注解则为0
	 */
	private final int distance;

	/**
	 * 当前注解的类型
	 */
	private final Class<? extends Annotation> annotationType;

	/**
	 * 根注解到当前注解的列表
	 */
	private final List<Class<? extends Annotation>> metaTypes;

	/**
	 * 注解实例
	 */
	@Nullable
	private final Annotation annotation;

	/**
	 * 注解的属性方法
	 */
	private final AttributeMethods attributes;

	private final MirrorSets mirrorSets;

	/**
	 * 当前注解attributes中作为别名的属性方法索引 -> 根注解attributes中直接或间接标注别名的属性方法索引
	 */
	private final int[] aliasMappings;

	private final int[] conventionMappings;

	/**
	 * attributes 的属性方法下标 -> 对应的最后一个镜像方法在 attributes 的下标
	 */
	private final int[] annotationValueMappings;

	/**
	 * attributes 的属性方法下标 -> AnnotationTypeMapping
	 */
	private final AnnotationTypeMapping[] annotationValueSource;

	/**
	 * 不考虑递归，注解属性方法(可能是当前注解或当前注解的元注解的属性) -> 使用@AliasFor标注了该注解属性方法名称的注解属性列表
	 */
	private final Map<Method, List<Method>> aliasedBy;

	private final boolean synthesizable;

	/**
	 * 当前注解所有属性方法及其别名方法
	 */
	private final Set<Method> claimedAliases = new HashSet<>();


	AnnotationTypeMapping(@Nullable AnnotationTypeMapping source,
			Class<? extends Annotation> annotationType, @Nullable Annotation annotation) {

		this.source = source;
		this.root = (source != null ? source.getRoot() : this);
		this.distance = (source == null ? 0 : source.getDistance() + 1);
		this.annotationType = annotationType;
		this.metaTypes = merge(
				source != null ? source.getMetaTypes() : null,
				annotationType);
		this.annotation = annotation;
		this.attributes = AttributeMethods.forAnnotationType(annotationType);
		this.mirrorSets = new MirrorSets();
		this.aliasMappings = filledIntArray(this.attributes.size());
		this.conventionMappings = filledIntArray(this.attributes.size());
		this.annotationValueMappings = filledIntArray(this.attributes.size());
		this.annotationValueSource = new AnnotationTypeMapping[this.attributes.size()];
		this.aliasedBy = resolveAliasedForTargets();
		processAliases();
		addConventionMappings();
		addConventionAnnotationValues();
		this.synthesizable = computeSynthesizableFlag();
	}

	/**
	 * 合并元注解
	 *
	 * @param existing
	 * @param element
	 * @param <T>
	 * @return
	 */
	private static <T> List<T> merge(@Nullable List<T> existing, T element) {
		if (existing == null) {
			return Collections.singletonList(element);
		}
		List<T> merged = new ArrayList<>(existing.size() + 1);
		merged.addAll(existing);
		merged.add(element);
		return Collections.unmodifiableList(merged);
	}

	/**
	 * 解析注解属性列表的别名
	 *
	 * @return
	 */
	private Map<Method, List<Method>> resolveAliasedForTargets() {
		//别名->标注了该别名的注解属性列表
		Map<Method, List<Method>> aliasedBy = new HashMap<>();
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
			if (aliasFor != null) {
				//解析注解属性的别名方法
				Method target = resolveAliasTarget(attribute, aliasFor);
				aliasedBy.computeIfAbsent(target, key -> new ArrayList<>()).add(attribute);
			}
		}
		return Collections.unmodifiableMap(aliasedBy);
	}

	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor) {
		return resolveAliasTarget(attribute, aliasFor, true);
	}

	/**
	 * 解析给定注解属性的别名方法
	 *
	 * @param attribute      注解属性
	 * @param aliasFor       注解属性上的@AliasFor注解实例
	 * @param checkAliasPair 是否在同一个注解中两个标注了@AliasFor的注解必须互为别名
	 * @return
	 */
	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor, boolean checkAliasPair) {
		if (StringUtils.hasText(aliasFor.value()) && StringUtils.hasText(aliasFor.attribute())) {
			//@AliasFor value() 和 attribute() 只能有一个取值
			throw new AnnotationConfigurationException(String.format(
					"In @AliasFor declared on %s, attribute 'attribute' and its alias 'value' " +
							"are present with values of '%s' and '%s', but only one is permitted.",
					AttributeMethods.describe(attribute), aliasFor.attribute(),
					aliasFor.value()));
		}
		Class<? extends Annotation> targetAnnotation = aliasFor.annotation();
		if (targetAnnotation == Annotation.class) {
			// 如果@AliasFor的annotation()没有赋值，则取目标注解则为当前注解类型
			targetAnnotation = this.annotationType;
		}
		String targetAttributeName = aliasFor.attribute();
		if (!StringUtils.hasLength(targetAttributeName)) {
			targetAttributeName = aliasFor.value();
		}
		if (!StringUtils.hasLength(targetAttributeName)) {
			//没有设置别名，目标属性名称为当前属性名称
			targetAttributeName = attribute.getName();
		}
		Method target = AttributeMethods.forAnnotationType(targetAnnotation).get(targetAttributeName);
		if (target == null) {
			if (targetAnnotation == this.annotationType) {
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an alias for '%s' which is not present.",
						AttributeMethods.describe(attribute), targetAttributeName));
			}
			throw new AnnotationConfigurationException(String.format(
					"%s is declared as an @AliasFor nonexistent %s.",
					StringUtils.capitalize(AttributeMethods.describe(attribute)),
					AttributeMethods.describe(targetAnnotation, targetAttributeName)));
		}
		if (target.equals(attribute)) {
			throw new AnnotationConfigurationException(String.format(
					"@AliasFor declaration on %s points to itself. " +
							"Specify 'annotation' to point to a same-named attribute on a meta-annotation.",
					AttributeMethods.describe(attribute)));
		}
		if (!isCompatibleReturnType(attribute.getReturnType(), target.getReturnType())) {
			// 别名注解属性方法和当前注解属性方法返回值类型不一致
			throw new AnnotationConfigurationException(String.format(
					"Misconfigured aliases: %s and %s must declare the same return type.",
					AttributeMethods.describe(attribute),
					AttributeMethods.describe(target)));
		}
		if (isAliasPair(target) && checkAliasPair) {
			AliasFor targetAliasFor = target.getAnnotation(AliasFor.class);
			if (targetAliasFor != null) {
				Method mirror = resolveAliasTarget(target, targetAliasFor, false);
				if (!mirror.equals(attribute)) {
					//同一个注解中的两个属性方法必须互为别名
					throw new AnnotationConfigurationException(String.format(
							"%s must be declared as an @AliasFor %s, not %s.",
							StringUtils.capitalize(AttributeMethods.describe(target)),
							AttributeMethods.describe(attribute), AttributeMethods.describe(mirror)));
				}
			}
		}
		return target;
	}

	private boolean isAliasPair(Method target) {
		return (this.annotationType == target.getDeclaringClass());
	}

	private boolean isCompatibleReturnType(Class<?> attributeType, Class<?> targetType) {
		return (attributeType == targetType || attributeType == targetType.getComponentType());
	}

	private void processAliases() {

		// 别名列表，第一个为属性方法，后面的为使用@AliasFor直接或间接使用该属性名称的注解属性
		List<Method> aliases = new ArrayList<>();
		for (int i = 0; i < this.attributes.size(); i++) {
			aliases.clear();
			aliases.add(this.attributes.get(i));
			collectAliases(aliases);
			if (aliases.size() > 1) {
				processAliases(i, aliases);
			}
		}
	}

	/**
	 * 从当前 AnnotationTypeMapping 递归到 source ，添加别名属性到 aliases
	 *
	 * @param aliases
	 */
	private void collectAliases(List<Method> aliases) {
		AnnotationTypeMapping mapping = this;
		while (mapping != null) {
			int size = aliases.size();
			for (int j = 0; j < size; j++) {
				List<Method> additional = mapping.aliasedBy.get(aliases.get(j));
				if (additional != null) {
					aliases.addAll(additional);
				}
			}
			mapping = mapping.source;
		}
	}

	private void processAliases(int attributeIndex, List<Method> aliases) {
		//aliases列表中第一个根注解属性的索引
		int rootAttributeIndex = getFirstRootAttributeIndex(aliases);
		AnnotationTypeMapping mapping = this;
		while (mapping != null) {
			if (rootAttributeIndex != -1 && mapping != this.root) {
				// 存在根注解属性并且当前注解不是根注解
				for (int i = 0; i < mapping.attributes.size(); i++) {
					// 别名列表中同时也包含当前注解属性的方法
					if (aliases.contains(mapping.attributes.get(i))) {
						mapping.aliasMappings[i] = rootAttributeIndex;
					}
				}
			}
			mapping.mirrorSets.updateFrom(aliases);
			mapping.claimedAliases.addAll(aliases);
			if (mapping.annotation != null) {
				int[] resolvedMirrors = mapping.mirrorSets.resolve(null,
						mapping.annotation, ReflectionUtils::invokeMethod);
				for (int i = 0; i < mapping.attributes.size(); i++) {
					if (aliases.contains(mapping.attributes.get(i))) {
						this.annotationValueMappings[attributeIndex] = resolvedMirrors[i];
						this.annotationValueSource[attributeIndex] = mapping;
					}
				}
			}
			mapping = mapping.source;
		}
	}

	/**
	 * 获取别名列表中第一个根注解属性的索引
	 *
	 * @param aliases
	 * @return
	 */
	private int getFirstRootAttributeIndex(Collection<Method> aliases) {
		AttributeMethods rootAttributes = this.root.getAttributes();
		for (int i = 0; i < rootAttributes.size(); i++) {
			if (aliases.contains(rootAttributes.get(i))) {
				return i;
			}
		}
		return -1;
	}

	private void addConventionMappings() {
		if (this.distance == 0) {
			return;
		}
		AttributeMethods rootAttributes = this.root.getAttributes();
		int[] mappings = this.conventionMappings;
		for (int i = 0; i < mappings.length; i++) {
			String name = this.attributes.get(i).getName();
			MirrorSet mirrors = getMirrorSets().getAssigned(i);
			int mapped = rootAttributes.indexOf(name);
			if (!MergedAnnotation.VALUE.equals(name) && mapped != -1) {
				mappings[i] = mapped;
				if (mirrors != null) {
					for (int j = 0; j < mirrors.size(); j++) {
						mappings[mirrors.getAttributeIndex(j)] = mapped;
					}
				}
			}
		}
	}

	private void addConventionAnnotationValues() {
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			boolean isValueAttribute = MergedAnnotation.VALUE.equals(attribute.getName());
			AnnotationTypeMapping mapping = this;
			while (mapping != null && mapping.distance > 0) {
				int mapped = mapping.getAttributes().indexOf(attribute.getName());
				if (mapped != -1 && isBetterConventionAnnotationValue(i, isValueAttribute, mapping)) {
					this.annotationValueMappings[i] = mapped;
					this.annotationValueSource[i] = mapping;
				}
				mapping = mapping.source;
			}
		}
	}

	private boolean isBetterConventionAnnotationValue(int index, boolean isValueAttribute,
			AnnotationTypeMapping mapping) {

		if (this.annotationValueMappings[index] == -1) {
			return true;
		}
		int existingDistance = this.annotationValueSource[index].distance;
		return !isValueAttribute && existingDistance > mapping.distance;
	}

	@SuppressWarnings("unchecked")
	private boolean computeSynthesizableFlag() {
		// Uses @AliasFor for local aliases?
		for (int index : this.aliasMappings) {
			if (index != -1) {
				return true;
			}
		}

		// Uses @AliasFor for attribute overrides in meta-annotations?
		if (!this.aliasedBy.isEmpty()) {
			return true;
		}

		// Uses convention-based attribute overrides in meta-annotations?
		for (int index : this.conventionMappings) {
			if (index != -1) {
				return true;
			}
		}

		// Has nested annotations or arrays of annotations that are synthesizable?
		if (getAttributes().hasNestedAnnotation()) {
			AttributeMethods attributeMethods = getAttributes();
			for (int i = 0; i < attributeMethods.size(); i++) {
				Method method = attributeMethods.get(i);
				Class<?> type = method.getReturnType();
				if (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation())) {
					Class<? extends Annotation> annotationType =
							(Class<? extends Annotation>) (type.isAnnotation() ? type : type.getComponentType());
					AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType).get(0);
					if (mapping.isSynthesizable()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Method called after all mappings have been set. At this point no further lookups from child mappings will occur.
	 */
	void afterAllMappingsSet() {
		validateAllAliasesClaimed();
		for (int i = 0; i < this.mirrorSets.size(); i++) {
			validateMirrorSet(this.mirrorSets.get(i));
		}
		this.claimedAliases.clear();
	}

	private void validateAllAliasesClaimed() {
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
			if (aliasFor != null && !this.claimedAliases.contains(attribute)) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an alias for %s which is not meta-present.",
						AttributeMethods.describe(attribute), AttributeMethods.describe(target)));
			}
		}
	}

	private void validateMirrorSet(MirrorSet mirrorSet) {
		Method firstAttribute = mirrorSet.get(0);
		Object firstDefaultValue = firstAttribute.getDefaultValue();
		for (int i = 1; i <= mirrorSet.size() - 1; i++) {
			Method mirrorAttribute = mirrorSet.get(i);
			Object mirrorDefaultValue = mirrorAttribute.getDefaultValue();
			if (firstDefaultValue == null || mirrorDefaultValue == null) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare default values.",
						AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
			}
			if (!ObjectUtils.nullSafeEquals(firstDefaultValue, mirrorDefaultValue)) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare the same default value.",
						AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
			}
		}
	}

	/**
	 * Get the root mapping.
	 *
	 * @return the root mapping
	 */
	AnnotationTypeMapping getRoot() {
		return this.root;
	}

	/**
	 * Get the source of the mapping or {@code null}.
	 *
	 * @return the source of the mapping
	 */
	@Nullable
	AnnotationTypeMapping getSource() {
		return this.source;
	}

	/**
	 * Get the distance of this mapping.
	 *
	 * @return the distance of the mapping
	 */
	int getDistance() {
		return this.distance;
	}

	/**
	 * Get the type of the mapped annotation.
	 *
	 * @return the annotation type
	 */
	Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	List<Class<? extends Annotation>> getMetaTypes() {
		return this.metaTypes;
	}

	/**
	 * Get the source annotation for this mapping. This will be the meta-annotation, or {@code null} if this is the root
	 * mapping.
	 *
	 * @return the source annotation of the mapping
	 */
	@Nullable
	Annotation getAnnotation() {
		return this.annotation;
	}

	/**
	 * Get the annotation attributes for the mapping annotation type.
	 *
	 * @return the attribute methods
	 */
	AttributeMethods getAttributes() {
		return this.attributes;
	}

	/**
	 * Get the related index of an alias mapped attribute, or {@code -1} if there is no mapping. The resulting value is
	 * the index of the attribute on the root annotation that can be invoked in order to obtain the actual value.
	 *
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped attribute index or {@code -1}
	 */
	int getAliasMapping(int attributeIndex) {
		return this.aliasMappings[attributeIndex];
	}

	/**
	 * Get the related index of a convention mapped attribute, or {@code -1} if there is no mapping. The resulting value
	 * is the index of the attribute on the root annotation that can be invoked in order to obtain the actual value.
	 *
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped attribute index or {@code -1}
	 */
	int getConventionMapping(int attributeIndex) {
		return this.conventionMappings[attributeIndex];
	}

	/**
	 * Get a mapped attribute value from the most suitable {@link #getAnnotation() meta-annotation}.
	 * <p>The resulting value is obtained from the closest meta-annotation,
	 * taking into consideration both convention and alias based mapping rules. For root mappings, this method will
	 * always return {@code null}.
	 *
	 * @param attributeIndex      the attribute index of the source attribute
	 * @param metaAnnotationsOnly if only meta annotations should be considered. If this parameter is {@code false} then
	 *                            aliases within the annotation will also be considered.
	 * @return the mapped annotation value, or {@code null}
	 */
	@Nullable
	Object getMappedAnnotationValue(int attributeIndex, boolean metaAnnotationsOnly) {
		int mappedIndex = this.annotationValueMappings[attributeIndex];
		if (mappedIndex == -1) {
			return null;
		}
		AnnotationTypeMapping source = this.annotationValueSource[attributeIndex];
		if (source == this && metaAnnotationsOnly) {
			return null;
		}
		return ReflectionUtils.invokeMethod(source.attributes.get(mappedIndex), source.annotation);
	}

	/**
	 * Determine if the specified value is equivalent to the default value of the attribute at the given index.
	 *
	 * @param attributeIndex the attribute index of the source attribute
	 * @param value          the value to check
	 * @param valueExtractor the value extractor used to extract values from any nested annotations
	 * @return {@code true} if the value is equivalent to the default value
	 */
	boolean isEquivalentToDefaultValue(int attributeIndex, Object value, ValueExtractor valueExtractor) {

		Method attribute = this.attributes.get(attributeIndex);
		return isEquivalentToDefaultValue(attribute, value, valueExtractor);
	}

	/**
	 * Get the mirror sets for this type mapping.
	 *
	 * @return the attribute mirror sets
	 */
	MirrorSets getMirrorSets() {
		return this.mirrorSets;
	}

	/**
	 * Determine if the mapped annotation is <em>synthesizable</em>.
	 * <p>Consult the documentation for {@link MergedAnnotation#synthesize()}
	 * for an explanation of what is considered synthesizable.
	 *
	 * @return {@code true} if the mapped annotation is synthesizable
	 * @since 5.2.6
	 */
	boolean isSynthesizable() {
		return this.synthesizable;
	}


	private static int[] filledIntArray(int size) {
		int[] array = new int[size];
		Arrays.fill(array, -1);
		return array;
	}

	/**
	 * 注解属性的值是否和默认值相同
	 *
	 * @param attribute
	 * @param value
	 * @param valueExtractor
	 * @return
	 */
	private static boolean isEquivalentToDefaultValue(Method attribute, Object value,
			ValueExtractor valueExtractor) {

		return areEquivalent(attribute.getDefaultValue(), value, valueExtractor);
	}

	/**
	 * 判断注解属性的默认值是否和实际的值相同
	 *
	 * @param value          注解属性默认值
	 * @param extractedValue 注解属性反射获取到的值
	 * @param valueExtractor 调用注解属性方法
	 * @return
	 */
	private static boolean areEquivalent(@Nullable Object value, @Nullable Object extractedValue,
			ValueExtractor valueExtractor) {

		if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
			return true;
		}
		if (value instanceof Class && extractedValue instanceof String) {
			return areEquivalent((Class<?>) value, (String) extractedValue);
		}
		if (value instanceof Class[] && extractedValue instanceof String[]) {
			return areEquivalent((Class[]) value, (String[]) extractedValue);
		}
		if (value instanceof Annotation) {
			return areEquivalent((Annotation) value, extractedValue, valueExtractor);
		}
		return false;
	}

	private static boolean areEquivalent(Class<?>[] value, String[] extractedValue) {
		if (value.length != extractedValue.length) {
			return false;
		}
		for (int i = 0; i < value.length; i++) {
			if (!areEquivalent(value[i], extractedValue[i])) {
				return false;
			}
		}
		return true;
	}

	private static boolean areEquivalent(Class<?> value, String extractedValue) {
		return value.getName().equals(extractedValue);
	}

	/**
	 * 判断类型为注解的注解属性的默认值是否和实际的值相同
	 *
	 * @param annotation     注解属性默认值
	 * @param extractedValue 注解属性反射获取到的值
	 * @param valueExtractor 调用注解属性方法
	 * @return
	 */
	private static boolean areEquivalent(Annotation annotation, @Nullable Object extractedValue,
			ValueExtractor valueExtractor) {

		AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
		for (int i = 0; i < attributes.size(); i++) {
			Method attribute = attributes.get(i);
			Object value1 = ReflectionUtils.invokeMethod(attribute, annotation);
			Object value2;
			if (extractedValue instanceof TypeMappedAnnotation) {
				value2 = ((TypeMappedAnnotation<?>) extractedValue).getValue(attribute.getName()).orElse(null);
			} else {
				value2 = valueExtractor.extract(attribute, extractedValue);
			}
			//分别用注解属性的默认值和实际值调用值的属性方法，判断这两者值的属性是否相同
			if (!areEquivalent(value1, value2, valueExtractor)) {
				return false;
			}
		}
		return true;
	}


	/**
	 * A collection of {@link MirrorSet} instances that provides details of all defined mirrors.
	 */
	class MirrorSets {

		/**
		 * assigned的去重版本
		 */
		private MirrorSet[] mirrorSets;

		/**
		 * attributes下标 -> 属性对应的镜像 如注解A中，属性a和属性b互为别名，属性c和属性d互为别名，属性e和属性f无别名， 属性a和属性b的镜像都为mirrorSet1,属性c和属性d的镜像都为mirrorSet2，
		 * 则assigned的值为[mirrorSet1,mirrorSet1,mirrorSet2,mirrorSet2,null,null]
		 */
		private final MirrorSet[] assigned;

		MirrorSets() {
			this.assigned = new MirrorSet[attributes.size()];
			this.mirrorSets = EMPTY_MIRROR_SETS;
		}

		/**
		 * @param aliases 注解中某个属性方法对应的一组别名
		 */
		void updateFrom(Collection<Method> aliases) {
			MirrorSet mirrorSet = null;
			// 别名列表中包含当前注解的属性方法的数量
			int size = 0;
			// 别名列表中最后一个当前注解属性方法的索引
			int last = -1;
			for (int i = 0; i < attributes.size(); i++) {
				Method attribute = attributes.get(i);
				if (aliases.contains(attribute)) {
					size++;
					if (size > 1) {//注解中存在两个及以上的属性互为别名时才赋值
						if (mirrorSet == null) {
							mirrorSet = new MirrorSet();
							this.assigned[last] = mirrorSet;
						}
						this.assigned[i] = mirrorSet;
					}
					last = i;
				}
			}
			if (mirrorSet != null) {
				mirrorSet.update();
				Set<MirrorSet> unique = new LinkedHashSet<>(Arrays.asList(this.assigned));
				unique.remove(null);
				this.mirrorSets = unique.toArray(EMPTY_MIRROR_SETS);
			}
		}

		int size() {
			return this.mirrorSets.length;
		}

		MirrorSet get(int index) {
			return this.mirrorSets[index];
		}

		@Nullable
		MirrorSet getAssigned(int attributeIndex) {
			return this.assigned[attributeIndex];
		}

		/**
		 * @param source
		 * @param annotation
		 * @param valueExtractor
		 * @return attributes 索引下标 -> 对应的最后一个镜像属性索引
		 */
		int[] resolve(@Nullable Object source, @Nullable Object annotation, ValueExtractor valueExtractor) {
			int[] result = new int[attributes.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = i;
			}
			for (int i = 0; i < size(); i++) {
				MirrorSet mirrorSet = get(i);
				int resolved = mirrorSet.resolve(source, annotation, valueExtractor);
				for (int j = 0; j < mirrorSet.size; j++) {
					result[mirrorSet.indexes[j]] = resolved;
				}
			}
			return result;
		}


		/**
		 * 镜像属性的集合
		 * <p>
		 * A single set of mirror attributes.
		 */
		class MirrorSet {

			/**
			 * 镜像的属性数量
			 */
			private int size;

			/**
			 * 注解属性互为别名的索引列表 注解A中，属性a和属性b互为别名，属性c和属性d互为别名，属性e和属性f无别名，
			 * <p>
			 * 当前MirrorSet可表示属性a和属性b的镜像mirrorSet1，取值为[0,1,-1,-1,-1]
			 */
			private final int[] indexes = new int[attributes.size()];

			void update() {
				this.size = 0;
				Arrays.fill(this.indexes, -1);
				for (int i = 0; i < MirrorSets.this.assigned.length; i++) {
					if (MirrorSets.this.assigned[i] == this) {
						this.indexes[this.size] = i;
						this.size++;
					}
				}
			}

			/**
			 * 获取该镜像中注解实例最后一个镜像方法的在 attributes 的索引位置
			 *
			 * @param source
			 * @param annotation
			 * @param valueExtractor
			 * @param <A>
			 * @return
			 */
			<A> int resolve(@Nullable Object source, @Nullable A annotation, ValueExtractor valueExtractor) {
				int result = -1;
				// 最后一个注解属性的值
				Object lastValue = null;
				for (int i = 0; i < this.size; i++) {
					// 镜像方法
					Method attribute = attributes.get(this.indexes[i]);
					Object value = valueExtractor.extract(attribute, annotation);
					boolean isDefaultValue = (value == null ||
							isEquivalentToDefaultValue(attribute, value, valueExtractor));
					if (isDefaultValue || ObjectUtils.nullSafeEquals(lastValue, value)) {
						if (result == -1) {
							result = this.indexes[i];
						}
						continue;
					}
					// 镜像属性方法的值不同，抛出异常
					if (lastValue != null && !ObjectUtils.nullSafeEquals(lastValue, value)) {
						String on = (source != null) ? " declared on " + source : "";
						throw new AnnotationConfigurationException(String.format(
								"Different @AliasFor mirror values for annotation [%s]%s; attribute '%s' " +
										"and its alias '%s' are declared with values of [%s] and [%s].",
								getAnnotationType().getName(), on,
								attributes.get(result).getName(),
								attribute.getName(),
								ObjectUtils.nullSafeToString(lastValue),
								ObjectUtils.nullSafeToString(value)));
					}
					result = this.indexes[i];
					lastValue = value;
				}
				return result;
			}

			int size() {
				return this.size;
			}

			Method get(int index) {
				int attributeIndex = this.indexes[index];
				return attributes.get(attributeIndex);
			}

			int getAttributeIndex(int index) {
				return this.indexes[index];
			}
		}
	}

}
