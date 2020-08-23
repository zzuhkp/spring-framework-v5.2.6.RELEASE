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

/**
 * 可用于筛选特定注解类型的回调接口。 请注意，MergedAnnotations模型（这个接口是为它设计的）总是根据普通过滤器忽略lang注释（出于效率原因）。
 * 任何附加的过滤器，甚至是定制的过滤器实现都会在这个范围内应用，并且可能只会进一步缩小范围。
 * <p>
 * Callback interface that can be used to filter specific annotation types.
 *
 * <p>Note that the {@link MergedAnnotations} model (which this interface has been
 * designed for) always ignores lang annotations according to the {@link #PLAIN} filter (for efficiency reasons). Any
 * additional filters and even custom filter implementations apply within this boundary and may only narrow further from
 * here.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @see MergedAnnotations
 * @since 5.2
 */
@FunctionalInterface
public interface AnnotationFilter {

	/**
	 * 匹配java.lang及org.springframework.lang包及其子包注解的注解过滤器。 这是MergedAnnotations模型中的默认过滤器。
	 * <p>
	 * {@link AnnotationFilter} that matches annotations in the {@code java.lang} and {@code org.springframework.lang}
	 * packages and their subpackages.
	 * <p>This is the default filter in the {@link MergedAnnotations} model.
	 */
	AnnotationFilter PLAIN = packages("java.lang", "org.springframework.lang");

	/**
	 * 匹配java及javax包及其子包注解的注解过滤器
	 * <p>
	 * {@link AnnotationFilter} that matches annotations in the {@code java} and {@code javax} packages and their
	 * subpackages.
	 */
	AnnotationFilter JAVA = packages("java", "javax");

	/**
	 * 始终匹配的AnnotationFilter，当根本不希望出现相关注解类型时可以使用该过滤器。
	 * <p>
	 * {@link AnnotationFilter} that always matches and can be used when no relevant annotation types are expected to be
	 * present at all.
	 */
	AnnotationFilter ALL = new AnnotationFilter() {
		@Override
		public boolean matches(Annotation annotation) {
			return true;
		}

		@Override
		public boolean matches(Class<?> type) {
			return true;
		}

		@Override
		public boolean matches(String typeName) {
			return true;
		}

		@Override
		public String toString() {
			return "All annotations filtered";
		}
	};

	/**
	 * {@link AnnotationFilter} that never matches and can be used when no filtering is needed (allowing for any
	 * annotation types to be present).
	 *
	 * @see #PLAIN
	 * @deprecated as of 5.2.6 since the {@link MergedAnnotations} model always ignores lang annotations according to
	 * the {@link #PLAIN} filter (for efficiency reasons)
	 */
	@Deprecated
	AnnotationFilter NONE = new AnnotationFilter() {
		@Override
		public boolean matches(Annotation annotation) {
			return false;
		}

		@Override
		public boolean matches(Class<?> type) {
			return false;
		}

		@Override
		public boolean matches(String typeName) {
			return false;
		}

		@Override
		public String toString() {
			return "No annotation filtering";
		}
	};


	/**
	 * Test if the given annotation matches the filter.
	 *
	 * @param annotation the annotation to test
	 * @return {@code true} if the annotation matches
	 */
	default boolean matches(Annotation annotation) {
		return matches(annotation.annotationType());
	}

	/**
	 * Test if the given type matches the filter.
	 *
	 * @param type the annotation type to test
	 * @return {@code true} if the annotation matches
	 */
	default boolean matches(Class<?> type) {
		return matches(type.getName());
	}

	/**
	 * 给定的注解类型名称是否匹配， 如果匹配将会跳过处理该注解
	 * <p>
	 * Test if the given type name matches the filter.
	 *
	 * @param typeName the fully qualified class name of the annotation type to test
	 * @return {@code true} if the annotation matches
	 */
	boolean matches(String typeName);


	/**
	 * Create a new {@link AnnotationFilter} that matches annotations in the specified packages.
	 *
	 * @param packages the annotation packages that should match
	 * @return a new {@link AnnotationFilter} instance
	 */
	static AnnotationFilter packages(String... packages) {
		return new PackagesAnnotationFilter(packages);
	}

}
