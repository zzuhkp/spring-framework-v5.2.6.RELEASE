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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * 提供对合并注解集合的访问，这些注解通常从类或方法等源获取。
 * 每个合并注解表示一个视图，其中属性值可以从不同的源值“合并”，通常：
 * 注解中一个或多个属性的显式和隐式@AliasFor声明
 * 元注解的显式@AliasFor声明
 * 元注解的基于约定的属性别名
 * 从元注解声明
 * <p>
 * 注意：MergedAnnotations API及其底层模型是为Spring的公共组件模型中的可组合注解而设计的，
 * 其重点是属性别名和元注解关系。不支持使用此API检索纯Java注释；
 * 请使用标准Java反射或Spring的AnnotationUtils进行简单的注解检索。
 * <p>
 * Provides access to a collection of merged annotations, usually obtained
 * from a source such as a {@link Class} or {@link Method}.
 *
 * <p>Each merged annotation represents a view where the attribute values may be
 * "merged" from different source values, typically:
 *
 * <ul>
 * <li>Explicit and Implicit {@link AliasFor @AliasFor} declarations on one or
 * more attributes within the annotation</li>
 * <li>Explicit {@link AliasFor @AliasFor} declarations for a meta-annotation</li>
 * <li>Convention based attribute aliases for a meta-annotation</li>
 * <li>From a meta-annotation declaration</li>
 * </ul>
 *
 * <p>For example, a {@code @PostMapping} annotation might be defined as follows:
 *
 * <pre class="code">
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;RequestMapping(method = RequestMethod.POST)
 * public &#064;interface PostMapping {
 *
 *     &#064;AliasFor(attribute = "path")
 *     String[] value() default {};
 *
 *     &#064;AliasFor(attribute = "value")
 *     String[] path() default {};
 * }
 * </pre>
 *
 * <p>If a method is annotated with {@code @PostMapping("/home")} it will contain
 * merged annotations for both {@code @PostMapping} and the meta-annotation
 * {@code @RequestMapping}. The merged view of the {@code @RequestMapping}
 * annotation will contain the following attributes:
 *
 * <p><table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Value</th>
 * <th>Source</th>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>"/home"</td>
 * <td>Declared in {@code @PostMapping}</td>
 * </tr>
 * <tr>
 * <td>path</td>
 * <td>"/home"</td>
 * <td>Explicit {@code @AliasFor}</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>RequestMethod.POST</td>
 * <td>Declared in meta-annotation</td>
 * </tr>
 * </table>
 *
 * <p>{@link MergedAnnotations} can be obtained {@linkplain #from(AnnotatedElement)
 * from} any Java {@link AnnotatedElement}. They may also be used for sources that
 * don't use reflection (such as those that directly parse bytecode).
 *
 * <p>Different {@linkplain SearchStrategy search strategies} can be used to locate
 * related source elements that contain the annotations to be aggregated. For
 * example, {@link SearchStrategy#TYPE_HIERARCHY} will search both superclasses and
 * implemented interfaces.
 *
 * <p>From a {@link MergedAnnotations} instance you can either
 * {@linkplain #get(String) get} a single annotation, or {@linkplain #stream()
 * stream all annotations} or just those that match {@linkplain #stream(String)
 * a specific type}. You can also quickly tell if an annotation
 * {@linkplain #isPresent(String) is present}.
 *
 * <p>Here are some typical examples:
 *
 * <pre class="code">
 * // is an annotation present or meta-present?
 * mergedAnnotations.isPresent(ExampleAnnotation.class);
 *
 * // get the merged "value" attribute of ExampleAnnotation (either directly or
 * // meta-present)
 * mergedAnnotations.get(ExampleAnnotation.class).getString("value");
 *
 * // get all meta-annotations but no directly present annotations
 * mergedAnnotations.stream().filter(MergedAnnotation::isMetaPresent);
 *
 * // get all ExampleAnnotation declarations (including any meta-annotations) and
 * // print the merged "value" attributes
 * mergedAnnotations.stream(ExampleAnnotation.class)
 *     .map(mergedAnnotation -&gt; mergedAnnotation.getString("value"))
 *     .forEach(System.out::println);
 * </pre>
 *
 * <p><b>NOTE: The {@code MergedAnnotations} API and its underlying model have
 * been designed for composable annotations in Spring's common component model,
 * with a focus on attribute aliasing and meta-annotation relationships.</b>
 * There is no support for retrieving plain Java annotations with this API;
 * please use standard Java reflection or Spring's {@link AnnotationUtils}
 * for simple annotation retrieval purposes.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see MergedAnnotation
 * @see MergedAnnotationCollectors
 * @see MergedAnnotationPredicates
 * @see MergedAnnotationSelectors
 * @since 5.2
 */
public interface MergedAnnotations extends Iterable<MergedAnnotation<Annotation>> {

	/**
	 * 确定指定的注解是否直接存在或元存在。
	 * 相当于调用get（annotationType）.isPresent（）。
	 * <p>
	 * Determine if the specified annotation is either directly present or
	 * meta-present.
	 * <p>Equivalent to calling {@code get(annotationType).isPresent()}.
	 *
	 * @param annotationType the annotation type to check
	 * @return {@code true} if the annotation is present
	 */
	<A extends Annotation> boolean isPresent(Class<A> annotationType);

	/**
	 * 确定指定的注解是否直接存在或元存在。
	 * 相当于调用get（annotationType）.isPresent（）。
	 * <p>
	 * Determine if the specified annotation is either directly present or
	 * meta-present.
	 * <p>Equivalent to calling {@code get(annotationType).isPresent()}.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to check
	 * @return {@code true} if the annotation is present
	 */
	boolean isPresent(String annotationType);

	/**
	 * 确定指定的注解是否直接存在。
	 * 相当于调用get（annotationType）.isDirectlyPresent（）。
	 * <p>
	 * Determine if the specified annotation is directly present.
	 * <p>Equivalent to calling {@code get(annotationType).isDirectlyPresent()}.
	 *
	 * @param annotationType the annotation type to check
	 * @return {@code true} if the annotation is directly present
	 */
	<A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType);

	/**
	 * 确定指定的注解是否直接存在。
	 * 相当于调用get（annotationType）.isDirectlyPresent（）。
	 * <p>
	 * Determine if the specified annotation is directly present.
	 * <p>Equivalent to calling {@code get(annotationType).isDirectlyPresent()}.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to check
	 * @return {@code true} if the annotation is directly present
	 */
	boolean isDirectlyPresent(String annotationType);

	/**
	 * 获取指定类型的最近匹配注解或元注解
	 * <p>
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
	 * annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the annotation type to get
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType);

	/**
	 * 获取指定类型的最近匹配的注解或元注解
	 * <p>
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
	 * annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the annotation type to get
	 * @param predicate      predicate必须匹配，如果为null将只匹配需要的类型
	 *                       a predicate that must match, or {@code null} if only
	 *                       type matching is required
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * 获取指定类型的匹配注解或元注解
	 * <p>
	 * Get a matching annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the annotation type to get
	 * @param predicate      a predicate that must match, or {@code null} if only
	 *                       type matching is required
	 * @param selector       选择器，用于选择聚合中最合适的注解，使用null选择最近的注解
	 *                       a selector used to choose the most appropriate annotation
	 *                       within an aggregate, or {@code null} to select the
	 *                       {@linkplain MergedAnnotationSelectors#nearest() nearest}
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 * @see MergedAnnotationSelectors
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector);

	/**
	 * 获取指定类型的最近匹配注解或元注解
	 * <p>
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
	 * annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to get
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType);

	/**
	 * 获取指定类型的最近匹配注解或元注解
	 * <p>
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
	 * annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to get
	 * @param predicate      a predicate that must match, or {@code null} if only
	 *                       type matching is required
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * 获取指定类型的最近匹配注解或元注解
	 * <p>
	 * Get a matching annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to get
	 * @param predicate      a predicate that must match, or {@code null} if only
	 *                       type matching is required
	 * @param selector       a selector used to choose the most appropriate annotation
	 *                       within an aggregate, or {@code null} to select the
	 *                       {@linkplain MergedAnnotationSelectors#nearest() nearest}
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 * @see MergedAnnotationSelectors
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector);

	/**
	 * 流式处理与指定类型匹配的所有注解和元注解。结果流遵循与stream（）相同的排序规则。
	 * <p>
	 * Stream all annotations and meta-annotations that match the specified
	 * type. The resulting stream follows the same ordering rules as
	 * {@link #stream()}.
	 *
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

	/**
	 * 流式处理与指定类型匹配的所有注解和元注解。结果流遵循与stream（）相同的排序规则。
	 * <p>
	 * Stream all annotations and meta-annotations that match the specified
	 * type. The resulting stream follows the same ordering rules as
	 * {@link #stream()}.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

	/**
	 * 流式处理此集合中包含的所有注解和元注解。
	 * 结果流首先按聚合索引排序，然后按注解距离排序（最接近的注解先排序）。
	 * 这种排序意味着，对于大多数用例，最合适的注解最早出现在流中。
	 * <p>
	 * Stream all annotations and meta-annotations contained in this collection.
	 * The resulting stream is ordered first by the
	 * {@linkplain MergedAnnotation#getAggregateIndex() aggregate index} and then
	 * by the annotation distance (with the closest annotations first). This ordering
	 * means that, for most use-cases, the most suitable annotations appear
	 * earliest in the stream.
	 *
	 * @return a stream of annotations
	 */
	Stream<MergedAnnotation<Annotation>> stream();


	/**
	 * 创建一个新的MergedAnnotations实例，该实例包含来自指定元素的所有注解和元注解。
	 * 结果实例不包含任何继承的注解。如果你想包含这些，
	 * 你应该使用from（AnnotatedElement，MergedAnnotations.SearchStrategy)
	 * 用适当的MergedAnnotations.SearchStrategy.
	 * <p>
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element. The
	 * resulting instance will not include any inherited annotations. If you
	 * want to include those as well you should use
	 * {@link #from(AnnotatedElement, SearchStrategy)} with an appropriate
	 * {@link SearchStrategy}.
	 *
	 * @param element the source element
	 * @return a {@link MergedAnnotations} instance containing the element's
	 * annotations
	 */
	static MergedAnnotations from(AnnotatedElement element) {
		return from(element, SearchStrategy.DIRECT);
	}

	/**
	 * 创建一个新的MergedAnnotations实例，该实例包含来自指定元素的所有注解和元注解，
	 * 具体取决于MergedAnnotations.SearchStrategy，相关的继承元素。
	 * <p>
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 *
	 * @param element        the source element
	 * @param searchStrategy the search strategy to use
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy) {
		return from(element, searchStrategy, RepeatableContainers.standardRepeatables());
	}

	/**
	 * 创建一个新的MergedAnnotations实例，该实例包含来自指定元素的所有注解和元注解，
	 * 具体取决于MergedAnnotations.SearchStrategy，相关的继承元素。
	 * <p>
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 *
	 * @param element              the source element
	 * @param searchStrategy       the search strategy to use
	 * @param repeatableContainers 元素注解或元注解可能使用的可重复容器
	 *                             the repeatable containers that may be used by
	 *                             the element annotations or the meta-annotations
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers) {

		return TypeMappedAnnotations.from(element, searchStrategy, repeatableContainers, AnnotationFilter.PLAIN);
	}

	/**
	 * 创建一个新的MergedAnnotations实例，该实例包含来自指定元素的所有注解和元注解，
	 * 具体取决于MergedAnnotations.SearchStrategy，相关的继承元素。
	 * <p>
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 *
	 * @param element              the source element
	 * @param searchStrategy       the search strategy to use
	 * @param repeatableContainers the repeatable containers that may be used by
	 *                             the element annotations or the meta-annotations
	 * @param annotationFilter     用于限制考虑的注解的注解过滤器
	 *                             an annotation filter used to restrict the
	 *                             annotations considered
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		return TypeMappedAnnotations.from(element, searchStrategy, repeatableContainers, annotationFilter);
	}

	/**
	 * 从指定的注解创建新的MergedAnnotations实例。
	 * <p>
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 *
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 * @see #from(Object, Annotation...)
	 */
	static MergedAnnotations from(Annotation... annotations) {
		return from(annotations, annotations);
	}

	/**
	 * 从指定的注解创建新的MergedAnnotations实例。
	 * <p>
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 *
	 * @param source      the source for the annotations. This source is used only
	 *                    for information and logging. It does not need to <em>actually</em>
	 *                    contain the specified annotations, and it will not be searched.
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 * @see #from(Annotation...)
	 * @see #from(AnnotatedElement)
	 */
	static MergedAnnotations from(Object source, Annotation... annotations) {
		return from(source, annotations, RepeatableContainers.standardRepeatables());
	}

	/**
	 * 从指定的注释创建新的MergedAnnotations实例。
	 * <p>
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 *
	 * @param source               注解的源。此源仅用于信息和日志记录。它不需要实际包含指定的注解，也不会对其进行搜索。
	 *                             the source for the annotations. This source is used only
	 *                             for information and logging. It does not need to <em>actually</em>
	 *                             contain the specified annotations, and it will not be searched.
	 * @param annotations          the annotations to include
	 * @param repeatableContainers the repeatable containers that may be used by
	 *                             meta-annotations
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 */
	static MergedAnnotations from(Object source, Annotation[] annotations, RepeatableContainers repeatableContainers) {
		return TypeMappedAnnotations.from(source, annotations, repeatableContainers, AnnotationFilter.PLAIN);
	}

	/**
	 * 从指定的注释创建新的MergedAnnotations实例。
	 * <p>
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 *
	 * @param source               the source for the annotations. This source is used only
	 *                             for information and logging. It does not need to <em>actually</em>
	 *                             contain the specified annotations, and it will not be searched.
	 * @param annotations          the annotations to include
	 * @param repeatableContainers the repeatable containers that may be used by
	 *                             meta-annotations
	 * @param annotationFilter     an annotation filter used to restrict the
	 *                             annotations considered
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 */
	static MergedAnnotations from(Object source, Annotation[] annotations,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		return TypeMappedAnnotations.from(source, annotations, repeatableContainers, annotationFilter);
	}

	/**
	 * 从直接注解的指定集合中创建一个新的 MergedAnnotations 实例。
	 * 这个方法允许 MergedAnnotations 实例从不需要使用反射的annotations集合中创建。
	 * 提供的注解必须全部直接标注，并且聚合索引必须为0。
	 * 生成的MergedAnnotations实例将同时包含指定的注解和可以使用反射读取的任何元注解。
	 * <p>
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * collection of directly present annotations. This method allows a
	 * {@link MergedAnnotations} instance to be created from annotations that
	 * are not necessarily loaded using reflection. The provided annotations
	 * must all be {@link MergedAnnotation#isDirectlyPresent() directly present}
	 * and must have a {@link MergedAnnotation#getAggregateIndex() aggregate
	 * index} of {@code 0}.
	 * <p>
	 * The resulting {@link MergedAnnotations} instance will contain both the
	 * specified annotations, and any meta-annotations that can be read using
	 * reflection.
	 *
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 * @see MergedAnnotation#of(ClassLoader, Object, Class, java.util.Map)
	 */
	static MergedAnnotations of(Collection<MergedAnnotation<?>> annotations) {
		return MergedAnnotationsCollection.of(annotations);
	}


	/**
	 * from方法支持的搜索策略
	 * 每种策略都创建一组不同的聚合，这些聚合将被组合起来以创建最终的MergedAnnotations。
	 * <p>
	 * Search strategies supported by
	 * {@link MergedAnnotations#from(AnnotatedElement, SearchStrategy)}.
	 *
	 * <p>Each strategy creates a different set of aggregates that will be
	 * combined to create the final {@link MergedAnnotations}.
	 */
	enum SearchStrategy {

		/**
		 * 只查找直接声明的注解，不考虑@Inherited注解，也不搜索超类或实现的接口。
		 * <p>
		 * Find only directly declared annotations, without considering
		 * {@link Inherited @Inherited} annotations and without searching
		 * superclasses or implemented interfaces.
		 */
		DIRECT,

		/**
		 * 查找所有直接声明的注解以及任何@Inherited超类注释。
		 * 这种策略只在与Class类型一起使用时才真正有用，因为对于所有其他带注解的元素，@Inherited注释被忽略。
		 * 此策略不搜索已实现的接口。
		 * <p>
		 * Find all directly declared annotations as well as any
		 * {@link Inherited @Inherited} superclass annotations. This strategy
		 * is only really useful when used with {@link Class} types since the
		 * {@link Inherited @Inherited} annotation is ignored for all other
		 * {@linkplain AnnotatedElement annotated elements}. This strategy does
		 * not search implemented interfaces.
		 */
		INHERITED_ANNOTATIONS,

		/**
		 * 查找所有直接声明的和超类注解。此策略类似于继承的注解，只是注解不需要用@Inherited进行元注释。
		 * 此策略不搜索已实现的接口。
		 * <p>
		 * Find all directly declared and superclass annotations. This strategy
		 * is similar to {@link #INHERITED_ANNOTATIONS} except the annotations
		 * do not need to be meta-annotated with {@link Inherited @Inherited}.
		 * This strategy does not search implemented interfaces.
		 */
		SUPERCLASS,

		/**
		 * 执行整个类型层次结构的完整搜索，包括超类和实现的接口。超类注解不需要用@Inherited进行元注解。
		 * <p>
		 * Perform a full search of the entire type hierarchy, including
		 * superclasses and implemented interfaces. Superclass annotations do
		 * not need to be meta-annotated with {@link Inherited @Inherited}.
		 */
		TYPE_HIERARCHY,

		/**
		 * 对源和任何封闭类执行整个类型层次结构的完整搜索。
		 * 此策略类似于TYPE_HIERARCHY，只是也搜索封闭类。
		 * 超类注释不需要用@Inherited进行元注释。
		 * 当搜索方法源时，此策略与类型TYPE_HIERARCHY相同。
		 * <p>
		 * Perform a full search of the entire type hierarchy on the source
		 * <em>and</em> any enclosing classes. This strategy is similar to
		 * {@link #TYPE_HIERARCHY} except that {@linkplain Class#getEnclosingClass()
		 * enclosing classes} are also searched. Superclass annotations do not
		 * need to be meta-annotated with {@link Inherited @Inherited}. When
		 * searching a {@link Method} source, this strategy is identical to
		 * {@link #TYPE_HIERARCHY}.
		 */
		TYPE_HIERARCHY_AND_ENCLOSING_CLASSES
	}

}
