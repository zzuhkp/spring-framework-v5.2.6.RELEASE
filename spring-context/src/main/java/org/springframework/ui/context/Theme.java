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

package org.springframework.ui.context;

import org.springframework.context.MessageSource;

/**
 * 主题
 * <p>
 * A Theme can resolve theme-specific messages, codes, file paths, etcetera
 * (e&#46;g&#46; CSS and image files in a web environment).
 * The exposed {@link org.springframework.context.MessageSource} supports
 * theme-specific parameterization and internationalization.
 *
 * @author Juergen Hoeller
 * @see ThemeSource
 * @see org.springframework.web.servlet.ThemeResolver
 * @since 17.06.2003
 */
public interface Theme {

	/**
	 * 获取主题名称
	 * <p>
	 * Return the name of the theme.
	 *
	 * @return the name of the theme (never {@code null})
	 */
	String getName();

	/**
	 * 获取消息源
	 * <p>
	 * Return the specific MessageSource that resolves messages
	 * with respect to this theme.
	 *
	 * @return the theme-specific MessageSource (never {@code null})
	 */
	MessageSource getMessageSource();

}
