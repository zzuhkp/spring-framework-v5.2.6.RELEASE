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

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * AspectJ Aspect 实例的工厂
 * <p>
 * Interface implemented to provide an instance of an AspectJ aspect.
 * Decouples from Spring's bean factory.
 *
 * <p>Extends the {@link org.springframework.core.Ordered} interface
 * to express an order value for the underlying aspect in a chain.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory#getBean
 * @since 2.0
 */
public interface AspectInstanceFactory extends Ordered {

	/**
	 * 创建 Aspect 实例
	 * <p>
	 * Create an instance of this factory's aspect.
	 *
	 * @return the aspect instance (never {@code null})
	 */
	Object getAspectInstance();

	/**
	 * 获取当前工厂使用的 ClassLoader
	 * <p>
	 * Expose the aspect class loader that this factory uses.
	 *
	 * @return the aspect class loader (or {@code null} for the bootstrap loader)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	@Nullable
	ClassLoader getAspectClassLoader();

}
