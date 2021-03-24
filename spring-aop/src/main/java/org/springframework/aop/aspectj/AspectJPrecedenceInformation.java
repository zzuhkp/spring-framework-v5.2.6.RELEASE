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

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;

/**
 * AspectJ 优先级信息，实现提供按照 AspectJ 的优先级规则排序 Advice/Advisor 的信息
 * <p>
 * Interface to be implemented by types that can supply the information
 * needed to sort advice/advisors by AspectJ's precedence rules.
 *
 * @author Adrian Colyer
 * @see org.springframework.aop.aspectj.autoproxy.AspectJPrecedenceComparator
 * @since 2.0
 */
public interface AspectJPrecedenceInformation extends Ordered {

	// Implementation note:
	// We need the level of indirection this interface provides as otherwise the
	// AspectJPrecedenceComparator must ask an Advisor for its Advice in all cases
	// in order to sort advisors. This causes problems with the
	// InstantiationModelAwarePointcutAdvisor which needs to delay creating
	// its advice for aspects with non-singleton instantiation models.

	/**
	 * 返回 Advice 定义所在的 Aspect Bean 的名称
	 * <p>
	 * Return the name of the aspect (bean) in which the advice was declared.
	 */
	String getAspectName();

	/**
	 * 返回 Advice 在 Aspect 的定义顺序
	 * <p>
	 * Return the declaration order of the advice member within the aspect.
	 */
	int getDeclarationOrder();

	/**
	 * 当前实例是否是一个 Before Advice
	 * <p>
	 * Return whether this is a before advice.
	 */
	boolean isBeforeAdvice();

	/**
	 * 当前实例是否是一个 After Advice
	 * <p>
	 * Return whether this is an after advice.
	 */
	boolean isAfterAdvice();

}
