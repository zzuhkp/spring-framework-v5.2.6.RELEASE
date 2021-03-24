/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.framework;

/**
 * 在 ProxyCreatorSupport 注册的监听器，允许在激活和更改 Advice 时接收回调
 * <p>
 * Listener to be registered on {@link ProxyCreatorSupport} objects
 * Allows for receiving callbacks on activation and change of advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ProxyCreatorSupport#addListener
 */
public interface AdvisedSupportListener {

	/**
	 * 创建第一个代理时调用
	 * <p>
	 * Invoked when the first proxy is created.
	 *
	 * @param advised the AdvisedSupport object
	 */
	void activated(AdvisedSupport advised);

	/**
	 * 创建代理后更改 Advice 时调用
	 * <p>
	 * Invoked when advice is changed after a proxy is created.
	 *
	 * @param advised the AdvisedSupport object
	 */
	void adviceChanged(AdvisedSupport advised);

}
