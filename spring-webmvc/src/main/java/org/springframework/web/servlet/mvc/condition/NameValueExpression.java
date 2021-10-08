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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 名称、值表达式
 * <p>
 * A contract for {@code "name!=value"} style expression used to specify request
 * parameters and request header conditions in {@code @RequestMapping}.
 *
 * @param <T> the value type
 * @author Rossen Stoyanchev
 * @see RequestMapping#params()
 * @see RequestMapping#headers()
 * @since 3.1
 */
public interface NameValueExpression<T> {

	/**
	 * 获取参数名
	 *
	 * @return
	 */
	String getName();

	/**
	 * 获取参数值
	 *
	 * @return
	 */
	@Nullable
	T getValue();

	/**
	 * 是否取反
	 *
	 * @return
	 */
	boolean isNegated();

}
