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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

/**
 * 用于在两个MergedAnnotation实例之间进行选择的策略接口。
 * <p>
 * Strategy interface used to select between two {@link MergedAnnotation}
 * instances.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @see MergedAnnotationSelectors
 * @since 5.2
 */
@FunctionalInterface
public interface MergedAnnotationSelector<A extends Annotation> {

	/**
	 * 确定现有注解是否已知为最佳候选，并且可以跳过任何后续选择。
	 * <p>
	 * Determine if the existing annotation is known to be the best
	 * candidate and any subsequent selections may be skipped.
	 *
	 * @param annotation the annotation to check
	 * @return {@code true} if the annotation is known to be the best candidate
	 */
	default boolean isBestCandidate(MergedAnnotation<A> annotation) {
		return false;
	}

	/**
	 * 选择应使用的注解。
	 *
	 * Select the annotation that should be used.
	 *
	 * @param existing  an existing annotation returned from an earlier result
	 * @param candidate a candidate annotation that may be better suited
	 * @return the most appropriate annotation from the {@code existing} or
	 * {@code candidate}
	 */
	MergedAnnotation<A> select(MergedAnnotation<A> existing, MergedAnnotation<A> candidate);

}
