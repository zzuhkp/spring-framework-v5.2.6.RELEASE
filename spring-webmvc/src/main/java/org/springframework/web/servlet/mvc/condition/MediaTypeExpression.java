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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * MediaType 表达式
 * <p>
 * A contract for media type expressions (e.g. "text/plain", "!text/plain") as
 * defined in the {@code @RequestMapping} annotation for "consumes" and
 * "produces" conditions.
 *
 * @author Rossen Stoyanchev
 * @see RequestMapping#consumes()
 * @see RequestMapping#produces()
 * @since 3.1
 */
public interface MediaTypeExpression {

	/**
	 * 获取媒体类型
	 *
	 * @return
	 */
	MediaType getMediaType();

	/**
	 * 是否取反
	 *
	 * @return
	 */
	boolean isNegated();

}
