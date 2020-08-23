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
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

/**
 * 从MergedAnnotations集合返回的单个合并注解。
 * 显示注解上的视图，其中属性值可能已从不同的源值“合并”。
 * 可以使用各种get方法访问属性值。例如，要访问int属性，将使用getInt（String）方法。
 * 请注意，属性值在访问时不会转换。例如，如果基础属性是int，则无法调用getString（String）。
 * 此规则唯一的例外是Class和Class[]值，它们可以分别作为String和String[]访问，以防止可能的早期类初始化。
 * 如果需要，MergedAnnotation可以合成回实际的注解。
 *
 * A single merged annotation returned from a {@link MergedAnnotations}
 * collection. Presents a view onto an annotation where attribute values may
 * have been "merged" from different source values.
 *
 * <p>Attribute values may be accessed using the various {@code get} methods.
 * For example, to access an {@code int} attribute the {@link #getInt(String)}
 * method would be used.
 *
 * <p>Note that attribute values are <b>not</b> converted when accessed.
 * For example, it is not possible to call {@link #getString(String)} if the
 * underlying attribute is an {@code int}. The only exception to this rule is
 * {@code Class} and {@code Class[]} values which may be accessed as
 * {@code String} and {@code String[]} respectively to prevent potential early
 * class initialization.
 *
 * <p>If necessary, a {@code MergedAnnotation} can be {@linkplain #synthesize()
 * synthesized} back into an actual {@link java.lang.annotation.Annotation}.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see MergedAnnotations
 * @see MergedAnnotationPredicates
 * @since 5.2
 */
public interface MergedAnnotation<A extends Annotation> {

	/**
	 * The attribute name for annotations with a single element.
	 */
	String VALUE = "value";


	/**
	 * 获取实际注解类型的类引用。
	 * <p>
	 * Get the {@code Class} reference for the actual annotation type.
	 *
	 * @return the annotation type
	 */
	Class<A> getType();

	/**
	 * 确定注解是否在源上标注，包括直接标注或元标注。
	 * <p>
	 * Determine if the annotation is present on the source. Considers
	 * {@linkplain #isDirectlyPresent() directly present} and
	 * {@linkplain #isMetaPresent() meta-present} annotations within the context
	 * of the {@link SearchStrategy} used.
	 *
	 * @return {@code true} if the annotation is present
	 */
	boolean isPresent();

	/**
	 * 确定注解是否直接标注在源上
	 * <p>
	 * Determine if the annotation is directly present on the source.
	 * <p>A directly present annotation is one that the user has explicitly
	 * declared and not one that is {@linkplain #isMetaPresent() meta-present}
	 * or {@link Inherited @Inherited}.
	 *
	 * @return {@code true} if the annotation is directly present
	 */
	boolean isDirectlyPresent();

	/**
	 * 确定注解是否间接标注在源上(即注解是否为标注在源的注解上)
	 * Determine if the annotation is meta-present on the source.
	 * <p>A meta-present annotation is an annotation that the user hasn't
	 * explicitly declared, but has been used as a meta-annotation somewhere in
	 * the annotation hierarchy.
	 *
	 * @return {@code true} if the annotation is meta-present
	 */
	boolean isMetaPresent();

	/**
	 * 获取这个注解与它作为元注解使用的距离。
	 * 直接定义的注解的距离是0，元注解的距离是1，元注解的元注解的距离是2，以此类推。
	 * 没有注解的距离总是返回-1
	 * <p>
	 * Get the distance of this annotation related to its use as a
	 * meta-annotation.
	 * <p>A directly declared annotation has a distance of {@code 0}, a
	 * meta-annotation has a distance of {@code 1}, a meta-annotation on a
	 * meta-annotation has a distance of {@code 2}, etc. A {@linkplain #missing()
	 * missing} annotation will always return a distance of {@code -1}.
	 *
	 * @return the annotation distance or {@code -1} if the annotation is missing
	 */
	int getDistance();

	/**
	 * 获取包含当前注解的集合中的索引。
	 * 可以用来给注解流重新排序，例如给定义在父类或接口上的注解较高的优先级。
	 * 没有注解的索引总是返回-1
	 * <p>
	 * Get the index of the aggregate collection containing this annotation.
	 * <p>Can be used to reorder a stream of annotations, for example, to give a
	 * higher priority to annotations declared on a superclass or interface. A
	 * {@linkplain #missing() missing} annotation will always return an aggregate
	 * index of {@code -1}.
	 *
	 * @return the aggregate index (starting at {@code 0}) or {@code -1} if the
	 * annotation is missing
	 */
	int getAggregateIndex();

	/**
	 * 获取根注解最终定义的源，如果源未知则返回null。
	 * 如果合并注解是从AnnotatedElement创建，则源是相同的类型。
	 * 如果注解没有使用反射加载，源可能是任意类型，但是应该有一个明确的toString()方法。
	 * 元注解总是返回和根注解相同的源。
	 * <p>
	 * Get the source that ultimately declared the root annotation, or
	 * {@code null} if the source is not known.
	 * <p>If this merged annotation was created
	 * {@link MergedAnnotations#from(AnnotatedElement) from} an
	 * {@link AnnotatedElement} then this source will be an element of the same
	 * type. If the annotation was loaded without using reflection, the source
	 * can be of any type, but should have a sensible {@code toString()}.
	 * Meta-annotations will always return the same source as the
	 * {@link #getRoot() root}.
	 *
	 * @return the source, or {@code null}
	 */
	@Nullable
	Object getSource();

	/**
	 * 获取元注解的源，如果注解不是元注解则返回null。
	 * meta-source是被当前注解元标注的注解。
	 * <p>
	 * Get the source of the meta-annotation, or {@code null} if the
	 * annotation is not {@linkplain #isMetaPresent() meta-present}.
	 * <p>The meta-source is the annotation that was meta-annotated with this
	 * annotation.
	 *
	 * @return the meta-annotation source or {@code null}
	 * @see #getRoot()
	 */
	@Nullable
	MergedAnnotation<?> getMetaSource();

	/**
	 * 获取根注解。距离是0直接定义在源上的注解。
	 * <p>
	 * Get the root annotation, i.e. the {@link #getDistance() distance} {@code 0}
	 * annotation as directly declared on the source.
	 *
	 * @return the root annotation
	 * @see #getMetaSource()
	 */
	MergedAnnotation<?> getRoot();

	/**
	 * 获取注解层次中从当前注解到根注解的完整列表，
	 *
	 * 如类Obj标注了注解A,A注解标注了注解B，当前注解为B，则返回[A,B]
	 * <p>
	 * Get the complete list of annotation types within the annotation hierarchy
	 * from this annotation to the {@link #getRoot() root}.
	 * <p>Provides a useful way to uniquely identify a merged annotation instance.
	 *
	 * @return the meta types for the annotation
	 * @see MergedAnnotationPredicates#unique(Function)
	 * @see #getRoot()
	 * @see #getMetaSource()
	 */
	List<Class<? extends Annotation>> getMetaTypes();


	/**
	 * 和注解定义相比，给定的属性名称是否有非默认值
	 * <p>
	 * Determine if the specified attribute name has a non-default value when
	 * compared to the annotation declaration.
	 *
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute value is different from the default
	 * value
	 */
	boolean hasNonDefaultValue(String attributeName);

	/**
	 * 和注解定义相比，给定的属性名称是否有默认值
	 * <p>
	 * Determine if the specified attribute name has a default value when compared
	 * to the annotation declaration.
	 *
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute value is the same as the default
	 * value
	 */
	boolean hasDefaultValue(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required byte attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a byte
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	byte getByte(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required byte array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a byte array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	byte[] getByteArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required boolean attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a boolean
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	boolean getBoolean(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required boolean array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a boolean array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	boolean[] getBooleanArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required char attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a char
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	char getChar(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required char array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a char array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	char[] getCharArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required short attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a short
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	short getShort(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required short array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a short array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	short[] getShortArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required int attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as an int
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	int getInt(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required int array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as an int array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	int[] getIntArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required long attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a long
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	long getLong(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required long array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a long array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	long[] getLongArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required double attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a double
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	double getDouble(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required double array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a double array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	double[] getDoubleArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required float attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a float
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	float getFloat(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required float array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a float array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	float[] getFloatArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required string attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a string
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	String getString(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required string array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a string array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	String[] getStringArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required class attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a class
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	Class<?> getClass(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required class array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return the value as a class array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	Class<?>[] getClassArray(String attributeName) throws NoSuchElementException;

	/**
	 * Get a required enum attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @param type          the enum type
	 * @return the value as a enum
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<E extends Enum<E>> E getEnum(String attributeName, Class<E> type) throws NoSuchElementException;

	/**
	 * Get a required enum array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @param type          the enum type
	 * @return the value as a enum array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) throws NoSuchElementException;

	/**
	 * Get a required annotation attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @param type          the annotation type
	 * @return the value as a {@link MergedAnnotation}
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName, Class<T> type)
			throws NoSuchElementException;

	/**
	 * Get a required annotation array attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @param type          the annotation type
	 * @return the value as a {@link MergedAnnotation} array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(String attributeName, Class<T> type)
			throws NoSuchElementException;

	/**
	 * Get an optional attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @return an optional value or {@link Optional#empty()} if there is no
	 * matching attribute
	 */
	Optional<Object> getValue(String attributeName);

	/**
	 * Get an optional attribute value from the annotation.
	 *
	 * @param attributeName the attribute name
	 * @param type          the attribute type. Must be compatible with the underlying
	 *                      attribute type or {@code Object.class}.
	 * @return an optional value or {@link Optional#empty()} if there is no
	 * matching attribute
	 */
	<T> Optional<T> getValue(String attributeName, Class<T> type);

	/**
	 * 从注解定义中获取默认的属性值
	 * <p>
	 * Get the default attribute value from the annotation as specified in
	 * the annotation declaration.
	 *
	 * @param attributeName the attribute name
	 * @return an optional of the default value or {@link Optional#empty()} if
	 * there is no matching attribute or no defined default
	 */
	Optional<Object> getDefaultValue(String attributeName);

	/**
	 * 从注解定义中获取默认的属性值
	 * <p>
	 * Get the default attribute value from the annotation as specified in
	 * the annotation declaration.
	 *
	 * @param attributeName the attribute name
	 * @param type          the attribute type. Must be compatible with the underlying
	 *                      attribute type or {@code Object.class}.
	 * @return an optional of the default value or {@link Optional#empty()} if
	 * there is no matching attribute or no defined default
	 */
	<T> Optional<T> getDefaultValue(String attributeName, Class<T> type);

	/**
	 * 创建一个新的所有默认值都被移除的注解视图
	 * <p>
	 * Create a new view of the annotation with all attributes that have default
	 * values removed.
	 *
	 * @return a filtered view of the annotation without any attributes that
	 * have a default value
	 * @see #filterAttributes(Predicate)
	 */
	MergedAnnotation<A> filterDefaultValues();

	/**
	 * 创建一个只有属性匹配给定Predicate的注解视图
	 * Create a new view of the annotation with only attributes that match the
	 * given predicate.
	 *
	 * @param predicate a predicate used to filter attribute names
	 * @return a filtered view of the annotation
	 * @see #filterDefaultValues()
	 * @see MergedAnnotationPredicates
	 */
	MergedAnnotation<A> filterAttributes(Predicate<String> predicate);

	/**
	 * 创建一个暴露非合并属性值的注解视图。
	 * 这个视图的方法将返回只有别名镜像规则被应用的属性值。
	 * 到 meta-source 属性的别名不会被应用。
	 * <p>
	 * Create a new view of the annotation that exposes non-merged attribute values.
	 * <p>Methods from this view will return attribute values with only alias mirroring
	 * rules applied. Aliases to {@link #getMetaSource() meta-source} attributes will
	 * not be applied.
	 *
	 * @return a non-merged view of the annotation
	 */
	MergedAnnotation<A> withNonMergedAttributes();

	/**
	 * 从当前合并注解创建一个新的可变的AnnotationAttributes实例。
	 * adaptations可以被使用去改变值被添加的方式
	 * <p>
	 * Create a new mutable {@link AnnotationAttributes} instance from this
	 * merged annotation.
	 * <p>The {@link Adapt adaptations} may be used to change the way that values
	 * are added.
	 *
	 * @param adaptations the adaptations that should be applied to the annotation values
	 * @return an immutable map containing the attributes and values
	 */
	AnnotationAttributes asAnnotationAttributes(Adapt... adaptations);

	/**
	 * 获取包含所有注解属性的不可变的Map
	 * <p>
	 * Get an immutable {@link Map} that contains all the annotation attributes.
	 * <p>The {@link Adapt adaptations} may be used to change the way that values are added.
	 *
	 * @param adaptations the adaptations that should be applied to the annotation values
	 * @return an immutable map containing the attributes and values
	 */
	Map<String, Object> asMap(Adapt... adaptations);

	/**
	 * 创建一个给定类型的包含所有注解属性的新的Map实例。
	 * adaptations可以被用来改变值被添加的方式。
	 * <p>
	 * Create a new {@link Map} instance of the given type that contains all the annotation
	 * attributes.
	 * <p>The {@link Adapt adaptations} may be used to change the way that values are added.
	 *
	 * @param factory     a map factory
	 * @param adaptations the adaptations that should be applied to the annotation values
	 * @return a map containing the attributes and values
	 */
	<T extends Map<String, Object>> T asMap(Function<MergedAnnotation<?>, T> factory, Adapt... adaptations);

	/**
	 * 创建可以直接在代码中使用这个合并后的类型安全的合成版本注解。
	 * 其结果是使用JDK Proxy合成的 ，第一调用时结果可能会产生计算成本。
	 * 如果当前合并注解从注解实例创建，如果不是合成的将返回不可修改的注解。
	 * 如果是以下情况之一，则注解被认为是合成的：
	 * 注解定义的属性被@AliasFor注解。
	 * 注解是一个组合注解，它依赖于元注解中基于约定的注解属性覆盖。
	 * 注解声明的属性是注解或本身可合成的注解数组。
	 * <p>
	 * Create a type-safe synthesized version of this merged annotation that can
	 * be used directly in code.
	 * <p>The result is synthesized using a JDK {@link Proxy} and as a result may
	 * incur a computational cost when first invoked.
	 * <p>If this merged annotation was created {@linkplain #from(Annotation) from}
	 * an annotation instance, that annotation will be returned unmodified if it is
	 * not <em>synthesizable</em>. An annotation is considered synthesizable if
	 * one of the following is true.
	 * <ul>
	 * <li>The annotation declares attributes annotated with {@link AliasFor @AliasFor}.</li>
	 * <li>The annotation is a composed annotation that relies on convention-based
	 * annotation attribute overrides in meta-annotations.</li>
	 * <li>The annotation declares attributes that are annotations or arrays of
	 * annotations that are themselves synthesizable.</li>
	 * </ul>
	 *
	 * @return a synthesized version of the annotation or the original annotation
	 * unmodified
	 * @throws NoSuchElementException on a missing annotation
	 */
	A synthesize() throws NoSuchElementException;

	/**
	 * 基于Predicate创建当前注解的类型安全合成版本。
	 * 结果是使用JDK代理合成的，因此在第一次调用时可能会产生计算开销。
	 * 参阅synthesis（）的文档，以了解什么是可合成的。
	 * <p>
	 * Optionally create a type-safe synthesized version of this annotation based
	 * on a condition predicate.
	 * <p>The result is synthesized using a JDK {@link Proxy} and as a result may
	 * incur a computational cost when first invoked.
	 * <p>Consult the documentation for {@link #synthesize()} for an explanation
	 * of what is considered synthesizable.
	 *
	 * @param condition the test to determine if the annotation can be synthesized
	 * @return an optional containing the synthesized version of the annotation or
	 * an empty optional if the condition doesn't match
	 * @throws NoSuchElementException on a missing annotation
	 * @see MergedAnnotationPredicates
	 */
	Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition) throws NoSuchElementException;


	/**
	 * 创建一个MergedAnnotation，它表示缺少的注解（即不存在的注解）。
	 * <p>
	 * Create a {@link MergedAnnotation} that represents a missing annotation
	 * (i.e. one that is not present).
	 *
	 * @return an instance representing a missing annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> missing() {
		return MissingMergedAnnotation.getInstance();
	}

	/**
	 * 从指定的注解创建一个新的MergedAnnotation实例。
	 * <p>
	 * Create a new {@link MergedAnnotation} instance from the specified
	 * annotation.
	 *
	 * @param annotation the annotation to include
	 * @return a {@link MergedAnnotation} instance containing the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> from(A annotation) {
		return from(null, annotation);
	}

	/**
	 * 从指定的注解创建一个新的MergedAnnotation实例。
	 * <p>
	 * Create a new {@link MergedAnnotation} instance from the specified
	 * annotation.
	 *
	 * @param source     the source for the annotation. This source is used only for
	 *                   information and logging. It does not need to <em>actually</em> contain
	 *                   the specified annotations, and it will not be searched.
	 *                   注解的源。此源仅用于信息和日志记录。它不需要实际包含指定的注解，也不会对其进行搜索。
	 * @param annotation the annotation to include
	 *                   要包含的注解
	 * @return a {@link MergedAnnotation} instance for the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source, A annotation) {
		return TypeMappedAnnotation.from(source, annotation);
	}

	/**
	 * 创建指定注解类型的新MergedAnnotation实例。
	 * 生成的注解将没有任何属性值，但仍可以用于查询默认值。
	 *
	 * Create a new {@link MergedAnnotation} instance of the specified
	 * annotation type. The resulting annotation will not have any attribute
	 * values but may still be used to query default values.
	 *
	 * @param annotationType the annotation type
	 * @return a {@link MergedAnnotation} instance for the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> of(Class<A> annotationType) {
		return of(null, annotationType, null);
	}

	/**
	 * 使用map提供的属性值创建指定注解类型的新MergedAnnotation实例。
	 *
	 * Create a new {@link MergedAnnotation} instance of the specified
	 * annotation type with attribute values supplied by a map.
	 *
	 * @param annotationType the annotation type
	 * @param attributes     the annotation attributes or {@code null} if just default
	 *                       values should be used
	 * @return a {@link MergedAnnotation} instance for the annotation and attributes
	 * @see #of(AnnotatedElement, Class, Map)
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return of(null, annotationType, attributes);
	}

	/**
	 * 使用map提供的属性值创建指定注解类型的新MergedAnnotation实例。
	 *
	 * Create a new {@link MergedAnnotation} instance of the specified
	 * annotation type with attribute values supplied by a map.
	 *
	 * @param source         the source for the annotation. This source is used only for
	 *                       information and logging. It does not need to <em>actually</em> contain
	 *                       the specified annotations and it will not be searched.
	 * @param annotationType the annotation type
	 * @param attributes     the annotation attributes or {@code null} if just default
	 *                       values should be used
	 * @return a {@link MergedAnnotation} instance for the annotation and attributes
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			@Nullable AnnotatedElement source, Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return of(null, source, annotationType, attributes);
	}

	/**
	 * 使用map提供的属性值创建指定注解类型的新MergedAnnotation实例。
	 *
	 * Create a new {@link MergedAnnotation} instance of the specified
	 * annotation type with attribute values supplied by a map.
	 *
	 * @param classLoader    the class loader used to resolve class attributes
	 * @param source         the source for the annotation. This source is used only for
	 *                       information and logging. It does not need to <em>actually</em> contain
	 *                       the specified annotations and it will not be searched.
	 * @param annotationType the annotation type
	 * @param attributes     the annotation attributes or {@code null} if just default
	 *                       values should be used
	 * @return a {@link MergedAnnotation} instance for the annotation and attributes
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			@Nullable ClassLoader classLoader, @Nullable Object source,
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return TypeMappedAnnotation.of(classLoader, source, annotationType, attributes);
	}


	/**
	 * Adaptations that can be applied to attribute values when creating
	 * {@linkplain MergedAnnotation#asMap(Adapt...) Maps} or
	 * {@link MergedAnnotation#asAnnotationAttributes(Adapt...) AnnotationAttributes}.
	 */
	enum Adapt {

		/**
		 * Adapt class or class array attributes to strings.
		 */
		CLASS_TO_STRING,

		/**
		 * Adapt nested annotation or annotation arrays to maps rather
		 * than synthesizing the values.
		 */
		ANNOTATION_TO_MAP;

		protected final boolean isIn(Adapt... adaptations) {
			for (Adapt candidate : adaptations) {
				if (candidate == this) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Factory method to create an {@link Adapt} array from a set of boolean flags.
		 *
		 * @param classToString    if {@link Adapt#CLASS_TO_STRING} is included
		 * @param annotationsToMap if {@link Adapt#ANNOTATION_TO_MAP} is included
		 * @return a new {@link Adapt} array
		 */
		public static Adapt[] values(boolean classToString, boolean annotationsToMap) {
			EnumSet<Adapt> result = EnumSet.noneOf(Adapt.class);
			addIfTrue(result, Adapt.CLASS_TO_STRING, classToString);
			addIfTrue(result, Adapt.ANNOTATION_TO_MAP, annotationsToMap);
			return result.toArray(new Adapt[0]);
		}

		private static <T> void addIfTrue(Set<T> result, T value, boolean test) {
			if (test) {
				result.add(value);
			}
		}
	}

}
