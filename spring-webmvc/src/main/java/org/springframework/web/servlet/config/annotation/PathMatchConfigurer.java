/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * 路径匹配配置
 * <p>
 * Helps with configuring HandlerMappings path matching options such as trailing
 * slash match, suffix registration, path matcher and path helper.
 *
 * <p>Configured path matcher and path helper instances are shared for:
 * <ul>
 * <li>RequestMappings</li>
 * <li>ViewControllerMappings</li>
 * <li>ResourcesMappings</li>
 * </ul>
 *
 * @author Brian Clozel
 * @see RequestMappingHandlerMapping
 * @see org.springframework.web.servlet.handler.SimpleUrlHandlerMapping
 * @since 4.0.3
 */
public class PathMatchConfigurer {

	/**
	 * 是否使用后缀模式匹配请求路径
	 */
	@Nullable
	private Boolean suffixPatternMatch;

	/**
	 * 是否在请求路径后添加后缀模式后再匹配
	 */
	@Nullable
	private Boolean registeredSuffixPatternMatch;

	/**
	 * 是否在请求路径后添加 / 再匹配
	 */
	@Nullable
	private Boolean trailingSlashMatch;

	@Nullable
	private UrlPathHelper urlPathHelper;

	@Nullable
	private PathMatcher pathMatcher;

	/**
	 * 路径前缀 -> 处理器测试，将会为满足条件的处理器在处理的路径前添加前缀再进行匹配
	 */
	@Nullable
	private Map<String, Predicate<Class<?>>> pathPrefixes;


	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a method mapped to "/users" also matches to "/users.*".
	 * <p>By default this is set to {@code true}.
	 *
	 * @see #registeredSuffixPatternMatch
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options. As there is no replacement for this method, for the time
	 * being it's necessary to set it to {@code false}. In 5.3 when {@code false}
	 * becomes the default, use of this property will no longer be necessary.
	 */
	@Deprecated
	public PathMatchConfigurer setUseSuffixPatternMatch(Boolean suffixPatternMatch) {
		this.suffixPatternMatch = suffixPatternMatch;
		return this;
	}

	/**
	 * Whether suffix pattern matching should work only against path extensions
	 * explicitly registered when you
	 * {@link WebMvcConfigurer#configureContentNegotiation configure content
	 * negotiation}. This is generally recommended to reduce ambiguity and to
	 * avoid issues such as when a "." appears in the path for other reasons.
	 * <p>By default this is set to "false".
	 *
	 * @see WebMvcConfigurer#configureContentNegotiation
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options.
	 */
	@Deprecated
	public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(Boolean registeredSuffixPatternMatch) {
		this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
		return this;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default value is {@code true}.
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
		this.trailingSlashMatch = trailingSlashMatch;
		return this;
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass,
	 * or to share common UrlPathHelper settings across multiple HandlerMappings
	 * and MethodNameResolvers.
	 */
	public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
		return this;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns. Default is AntPathMatcher.
	 *
	 * @see org.springframework.util.AntPathMatcher
	 */
	public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	/**
	 * Configure a path prefix to apply to matching controller methods.
	 * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
	 * method whose controller type is matched by the corresponding
	 * {@code Predicate}. The prefix for the first matching predicate is used.
	 * <p>Consider using {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} to group controllers.
	 *
	 * @param prefix    the prefix to apply
	 * @param predicate a predicate for matching controller types
	 * @since 5.1
	 */
	public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
		if (this.pathPrefixes == null) {
			this.pathPrefixes = new LinkedHashMap<>();
		}
		this.pathPrefixes.put(prefix, predicate);
		return this;
	}


	/**
	 * Whether to use registered suffixes for pattern matching.
	 *
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options.
	 */
	@Nullable
	@Deprecated
	public Boolean isUseSuffixPatternMatch() {
		return this.suffixPatternMatch;
	}

	/**
	 * Whether to use registered suffixes for pattern matching.
	 *
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options.
	 */
	@Nullable
	@Deprecated
	public Boolean isUseRegisteredSuffixPatternMatch() {
		return this.registeredSuffixPatternMatch;
	}

	@Nullable
	public Boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	@Nullable
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	@Nullable
	protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}
}
