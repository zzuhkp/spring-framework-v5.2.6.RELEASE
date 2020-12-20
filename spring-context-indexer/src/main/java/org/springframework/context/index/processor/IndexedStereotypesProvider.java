/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.index.processor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A {@link StereotypesProvider} implementation that extracts the stereotypes
 * flagged by the {@value INDEXED_ANNOTATION} annotation. This implementation
 * honors stereotypes defined this way on meta-annotations.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class IndexedStereotypesProvider implements StereotypesProvider {

	private static final String INDEXED_ANNOTATION = "org.springframework.stereotype.Indexed";

	private final TypeHelper typeHelper;


	public IndexedStereotypesProvider(TypeHelper typeHelper) {
		this.typeHelper = typeHelper;
	}

	/**
	 * 从元素上获取组合，
	 * 包括直接标注 @Indexed 的类型，
	 * 通过继承标注 @Indexed 的类型，
	 * 及父类和接口直接标注和继承 @Indexed 注解的类型
	 *
	 * @param element the element to handle
	 * @return
	 */
	@Override
	public Set<String> getStereotypes(Element element) {
		Set<String> stereotypes = new LinkedHashSet<>();
		ElementKind kind = element.getKind();
		if (kind != ElementKind.CLASS && kind != ElementKind.INTERFACE) {
			return stereotypes;
		}
		Set<Element> seen = new HashSet<>();
		collectStereotypesOnAnnotations(seen, stereotypes, element);
		seen = new HashSet<>();
		collectStereotypesOnTypes(seen, stereotypes, element);
		return stereotypes;
	}

	/**
	 * 收集 @Indexed 标注(或元标注)的组合
	 *
	 * @param seen
	 * @param stereotypes
	 * @param element
	 */
	private void collectStereotypesOnAnnotations(Set<Element> seen, Set<String> stereotypes, Element element) {
		for (AnnotationMirror annotation : this.typeHelper.getAllAnnotationMirrors(element)) {
			Element next = collectStereotypes(seen, stereotypes, element, annotation);
			if (next != null) {
				collectStereotypesOnAnnotations(seen, stereotypes, next);
			}
		}
	}

	/**
	 * 收集直接标注 @Indexed 注解的类型(包括父类上的和接口上的)到 stereotypes
	 *
	 * @param seen
	 * @param stereotypes
	 * @param type
	 */
	private void collectStereotypesOnTypes(Set<Element> seen, Set<String> stereotypes, Element type) {
		if (!seen.contains(type)) {
			seen.add(type);
			if (isAnnotatedWithIndexed(type)) {
				stereotypes.add(this.typeHelper.getType(type));
			}
			Element superClass = this.typeHelper.getSuperClass(type);
			if (superClass != null) {
				collectStereotypesOnTypes(seen, stereotypes, superClass);
			}
			this.typeHelper.getDirectInterfaces(type).forEach(
					i -> collectStereotypesOnTypes(seen, stereotypes, i));
		}
	}

	/**
	 * 从注解上收集组合
	 *
	 * @param seen
	 * @param stereotypes
	 * @param element
	 * @param annotation
	 * @return
	 */
	private Element collectStereotypes(Set<Element> seen, Set<String> stereotypes, Element element,
			AnnotationMirror annotation) {
		// 先根据元素上的 @Indexed 注解直接收集组合
		if (isIndexedAnnotation(annotation)) {
			stereotypes.add(this.typeHelper.getType(element));
		}
		// 再获取注解对应的元素(元注解可能是 @Indexed )
		return getCandidateAnnotationElement(seen, annotation);
	}

	/**
	 * 获取注解对应的元素
	 *
	 * @param seen
	 * @param annotation
	 * @return
	 */
	private Element getCandidateAnnotationElement(Set<Element> seen, AnnotationMirror annotation) {
		Element element = annotation.getAnnotationType().asElement();
		if (seen.contains(element)) {
			return null;
		}
		// We need to visit all indexed annotations.
		if (!isIndexedAnnotation(annotation)) {
			seen.add(element);
		}
		return (!element.toString().startsWith("java.lang") ? element : null);
	}

	/**
	 * 给定的类型上是否直接被 @Indexed 标注
	 *
	 * @param type
	 * @return
	 */
	private boolean isAnnotatedWithIndexed(Element type) {
		for (AnnotationMirror annotation : type.getAnnotationMirrors()) {
			if (isIndexedAnnotation(annotation)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 注解是否为 @Indexed
	 *
	 * @param annotation
	 * @return
	 */
	private boolean isIndexedAnnotation(AnnotationMirror annotation) {
		return INDEXED_ANNOTATION.equals(annotation.getAnnotationType().toString());
	}

}
