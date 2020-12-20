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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * CandidateComponentsMetadata 收集器
 * <p>
 * Used by {@link CandidateComponentsIndexer} to collect {@link CandidateComponentsMetadata}.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class MetadataCollector {

	/**
	 * 本轮处理的元数据
	 */
	private final List<ItemMetadata> metadataItems = new ArrayList<>();

	private final ProcessingEnvironment processingEnvironment;

	/**
	 * 上次生成的元数据
	 */
	private final CandidateComponentsMetadata previousMetadata;

	private final TypeHelper typeHelper;

	/**
	 * 注解处理器处理的所有类型
	 */
	private final Set<String> processedSourceTypes = new HashSet<>();


	/**
	 * Create a new {@code MetadataProcessor} instance.
	 *
	 * @param processingEnvironment the processing environment of the build
	 * @param previousMetadata      之前生成的元数据
	 *                              any previous metadata or {@code null}
	 */
	public MetadataCollector(ProcessingEnvironment processingEnvironment,
			CandidateComponentsMetadata previousMetadata) {

		this.processingEnvironment = processingEnvironment;
		this.previousMetadata = previousMetadata;
		this.typeHelper = new TypeHelper(processingEnvironment);
	}


	public void processing(RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getRootElements()) {
			markAsProcessed(element);
		}
	}

	private void markAsProcessed(Element element) {
		if (element instanceof TypeElement) {
			this.processedSourceTypes.add(this.typeHelper.getType(element));
		}
	}

	public void add(ItemMetadata metadata) {
		this.metadataItems.add(metadata);
	}

	public CandidateComponentsMetadata getMetadata() {
		CandidateComponentsMetadata metadata = new CandidateComponentsMetadata();
		for (ItemMetadata item : this.metadataItems) {
			metadata.add(item);
		}
		if (this.previousMetadata != null) {
			List<ItemMetadata> items = this.previousMetadata.getItems();
			for (ItemMetadata item : items) {
				if (shouldBeMerged(item)) {
					metadata.add(item);
				}
			}
		}
		return metadata;
	}

	/**
	 * 元数据是否应该被合并
	 *
	 * @param itemMetadata
	 * @return 之前的类型在当前处理未被删除并且当前未处理之前的类型则应该被合并
	 */
	private boolean shouldBeMerged(ItemMetadata itemMetadata) {
		String sourceType = itemMetadata.getType();
		return (sourceType != null && !deletedInCurrentBuild(sourceType)
				&& !processedInCurrentBuild(sourceType));
	}

	/**
	 * 之前的类型在这次注解处理器处理时是否已经被删除
	 *
	 * @param sourceType 上次注解处理器处理的类型
	 * @return
	 */
	private boolean deletedInCurrentBuild(String sourceType) {
		return this.processingEnvironment.getElementUtils()
				.getTypeElement(sourceType) == null;
	}

	/**
	 * 本次注解处理器处理的类型中是否包含给定的类型
	 *
	 * @param sourceType
	 * @return
	 */
	private boolean processedInCurrentBuild(String sourceType) {
		return this.processedSourceTypes.contains(sourceType);
	}

}
