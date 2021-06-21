/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.target;

/**
 * TargetSource 的池配置
 * <p>
 * Config interface for a pooling target source.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface PoolingConfig {

	/**
	 * 最大数量
	 * <p>
	 * Return the maximum size of the pool.
	 */
	int getMaxSize();

	/**
	 * 激活数量
	 * <p>
	 * Return the number of active objects in the pool.
	 *
	 * @throws UnsupportedOperationException if not supported by the pool
	 */
	int getActiveCount() throws UnsupportedOperationException;

	/**
	 * 空闲数量
	 * <p>
	 * Return the number of idle objects in the pool.
	 *
	 * @throws UnsupportedOperationException if not supported by the pool
	 */
	int getIdleCount() throws UnsupportedOperationException;

}
