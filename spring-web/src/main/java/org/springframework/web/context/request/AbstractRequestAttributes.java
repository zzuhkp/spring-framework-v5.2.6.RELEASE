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

package org.springframework.web.context.request;

import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 抽象的请求参数
 * <p>
 * Abstract support class for RequestAttributes implementations,
 * offering a request completion mechanism for request-specific destruction
 * callbacks and for updating accessed session attributes.
 *
 * @author Juergen Hoeller
 * @see #requestCompleted()
 * @since 2.0
 */
public abstract class AbstractRequestAttributes implements RequestAttributes {

	/**
	 * 销毁回调：bean name -> 回调
	 * <p>
	 * Map from attribute name String to destruction callback Runnable.
	 */
	protected final Map<String, Runnable> requestDestructionCallbacks = new LinkedHashMap<>(8);

	/**
	 * 请求是否未结束
	 */
	private volatile boolean requestActive = true;


	/**
	 * 请求完成回调
	 * <p>
	 * Signal that the request has been completed.
	 * <p>Executes all request destruction callbacks and updates the
	 * session attributes that have been accessed during request processing.
	 */
	public void requestCompleted() {
		executeRequestDestructionCallbacks();
		updateAccessedSessionAttributes();
		this.requestActive = false;
	}

	/**
	 * Determine whether the original request is still active.
	 *
	 * @see #requestCompleted()
	 */
	protected final boolean isRequestActive() {
		return this.requestActive;
	}

	/**
	 * 注册销毁回调
	 * <p>
	 * Register the given callback as to be executed after request completion.
	 *
	 * @param name     the name of the attribute to register the callback for
	 * @param callback the callback to be executed for destruction
	 */
	protected final void registerRequestDestructionCallback(String name, Runnable callback) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(callback, "Callback must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.put(name, callback);
		}
	}

	/**
	 * Remove the request destruction callback for the specified attribute, if any.
	 *
	 * @param name the name of the attribute to remove the callback for
	 */
	protected final void removeRequestDestructionCallback(String name) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.remove(name);
		}
	}

	/**
	 * 执行回调
	 * <p>
	 * Execute all callbacks that have been registered for execution
	 * after request completion.
	 */
	private void executeRequestDestructionCallbacks() {
		synchronized (this.requestDestructionCallbacks) {
			for (Runnable runnable : this.requestDestructionCallbacks.values()) {
				runnable.run();
			}
			this.requestDestructionCallbacks.clear();
		}
	}

	/**
	 * 更新参数
	 * <p>
	 * Update all session attributes that have been accessed during request processing,
	 * to expose their potentially updated state to the underlying session manager.
	 */
	protected abstract void updateAccessedSessionAttributes();

}
