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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Extended variant of the standard {@link ApplicationListener} interface,
 * exposing further metadata such as the supported event and source type.
 *
 * <p>As of Spring Framework 4.2, this interface supersedes the Class-based
 * {@link SmartApplicationListener} with full handling of generic event types.
 *
 * @author Stephane Nicoll
 * @see SmartApplicationListener
 * @see GenericApplicationListenerAdapter
 * @since 4.2
 */
public interface GenericApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

	/**
	 * 是否支持给定的事件类型
	 * <p>
	 * Determine whether this listener actually supports the given event type.
	 *
	 * @param eventType the event type (never {@code null})
	 */
	boolean supportsEventType(ResolvableType eventType);

	/**
	 * 是否支持给定的事件源类型
	 * <p>
	 * Determine whether this listener actually supports the given source type.
	 * <p>The default implementation always returns {@code true}.
	 *
	 * @param sourceType the source type, or {@code null} if no source
	 */
	default boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return true;
	}

	/**
	 * 确定支持同一事件的监听器的执行顺序
	 * <p>
	 * Determine this listener's order in a set of listeners for the same event.
	 * <p>The default implementation returns {@link #LOWEST_PRECEDENCE}.
	 */
	@Override
	default int getOrder() {
		return LOWEST_PRECEDENCE;
	}

}
