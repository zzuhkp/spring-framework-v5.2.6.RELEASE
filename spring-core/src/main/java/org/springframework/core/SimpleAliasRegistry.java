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

package org.springframework.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Simple implementation of the {@link AliasRegistry} interface.
 * <p>Serves as base class for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * implementations.
 *
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @since 2.5.2
 */
public class SimpleAliasRegistry implements AliasRegistry {

	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 别名 -> 规范化的名称
	 * Map from alias to canonical name.
	 */
	private final Map<String, String> aliasMap = new ConcurrentHashMap<>(16);


	@Override
	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		synchronized (this.aliasMap) {
			if (alias.equals(name)) {
				//名称和别名相同时从别名集合中移除别名
				this.aliasMap.remove(alias);
				if (logger.isDebugEnabled()) {
					logger.debug("Alias definition '" + alias + "' ignored since it points to same name");
				}
			} else {
				String registeredName = this.aliasMap.get(alias);
				if (registeredName != null) {
					if (registeredName.equals(name)) {
						// 旧名称和新注册的名称相同，无需继续处理
						return;
					}
					if (!allowAliasOverriding()) {
						//如果不允许重新注册已存在的别名则抛出异常
						throw new IllegalStateException("Cannot define alias '" + alias + "' for name '" +
								name + "': It is already registered for name '" + registeredName + "'.");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Overriding alias '" + alias + "' definition for registered name '" +
								registeredName + "' with new target name '" + name + "'");
					}
				}
				//检查别名和名称之间是否存在循环，例如别名a-->名称b，则不允许别名b-->名称a
				checkForAliasCircle(name, alias);
				this.aliasMap.put(alias, name);
				if (logger.isTraceEnabled()) {
					logger.trace("Alias definition '" + alias + "' registered for name '" + name + "'");
				}
			}
		}
	}

	/**
	 * 是否别名允许覆盖，即注册过别名后是否允许重复注册
	 * Determine whether alias overriding is allowed.
	 * <p>Default is {@code true}.
	 */
	protected boolean allowAliasOverriding() {
		return true;
	}

	/**
	 * 确定是否给定的名称已经注册别名
	 * Determine whether the given name has the given alias registered.
	 *
	 * @param name  the name to check
	 * @param alias the alias to look for
	 * @since 4.2.1
	 */
	public boolean hasAlias(String name, String alias) {
		//a->b->c 前一项是别名，后一项是名称
		//如果alias为a,name为c >> a->b,b不等于name >> b作为alias,b->c >> c等于name,则c具有别名a
		String registeredName = this.aliasMap.get(alias);
		return ObjectUtils.nullSafeEquals(registeredName, name) || (registeredName != null
				&& hasAlias(name, registeredName));
	}

	@Override
	public void removeAlias(String alias) {
		synchronized (this.aliasMap) {
			String name = this.aliasMap.remove(alias);
			if (name == null) {
				//别名不存在时直接抛出异常
				throw new IllegalStateException("No alias '" + alias + "' registered");
			}
		}
	}

	@Override
	public boolean isAlias(String name) {
		return this.aliasMap.containsKey(name);
	}

	@Override
	public String[] getAliases(String name) {
		List<String> result = new ArrayList<>();
		synchronized (this.aliasMap) {
			retrieveAliases(name, result);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * 检索给定名称的所有别名，将结果存储在result中
	 * Transitively retrieve all aliases for the given name.
	 *
	 * @param name   the target name to find aliases for
	 * @param result the resulting aliases list
	 */
	private void retrieveAliases(String name, List<String> result) {
		this.aliasMap.forEach((alias, registeredName) -> {
			if (registeredName.equals(name)) {
				//a->b->-c->d 前一项是后一项的别名，则name为d时可返回结果[c,b,a]
				result.add(alias);
				retrieveAliases(alias, result);
			}
		});
	}

	/**
	 * 使用给定的StringValueResolver解析注册在当前注册中心的所有的别名和对应的名称。
	 * 值解析器可以解析目标bean甚至别名中的占位符。
	 * <p>
	 * Resolve all alias target names and aliases registered in this
	 * registry, applying the given {@link StringValueResolver} to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 *
	 * @param valueResolver the StringValueResolver to apply
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			Map<String, String> aliasCopy = new HashMap<>(this.aliasMap);
			//循环处理别名
			aliasCopy.forEach((alias, registeredName) -> {
				String resolvedAlias = valueResolver.resolveStringValue(alias);
				String resolvedName = valueResolver.resolveStringValue(registeredName);
				if (resolvedAlias == null || resolvedName == null || resolvedAlias.equals(resolvedName)) {
					//解析出的别名或名称为null或者相同则直接从该注册中心中移除
					this.aliasMap.remove(alias);
				} else if (!resolvedAlias.equals(alias)) {
					String existingName = this.aliasMap.get(resolvedAlias);
					if (existingName != null) {
						if (existingName.equals(resolvedName)) {
							//解析出的别名和存在的别名不同，并且根据解析出别名获取的名称和解析出的名称相同，从注册中心移除别名
							//保证了解析后的别名和注册中心已存在的相同别名对应的名称相同时，不会存在相同数据
							//例：a -> b ，解析的结果为 a' -> b' ,注册中心已存在 a' -> b'，移除 a ->b
							// Pointing to existing alias - just remove placeholder
							this.aliasMap.remove(alias);
							return;
						}
						//解析后的别名在注册中心已存在，但是注册中心中解析后的别名对应的名称和解析的名称不同，直接抛出异常
						//此时相当于一个键对应两个不同的值，无法处理
						throw new IllegalStateException(
								"Cannot register resolved alias '" + resolvedAlias + "' (original: '" + alias +
										"') for name '" + resolvedName + "': It is already registered for name '" +
										registeredName + "'.");
					}
					//不存在循环依赖时移除旧的别名，注册解析后的别名
					checkForAliasCircle(resolvedName, resolvedAlias);
					this.aliasMap.remove(alias);
					this.aliasMap.put(resolvedAlias, resolvedName);
				} else if (!registeredName.equals(resolvedName)) {
					//解析后别名未发生变化，名称发生变化，则直接覆盖旧值
					this.aliasMap.put(alias, resolvedName);
				}
			});
		}
	}

	/**
	 * Check whether the given name points back to the given alias as an alias
	 * in the other direction already, catching a circular reference upfront
	 * and throwing a corresponding IllegalStateException.
	 *
	 * @param name  the candidate name
	 * @param alias the candidate alias
	 * @see #registerAlias
	 * @see #hasAlias
	 */
	protected void checkForAliasCircle(String name, String alias) {
		if (hasAlias(alias, name)) {
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Circular reference - '" +
					name + "' is a direct or indirect alias for '" + alias + "' already");
		}
	}

	/**
	 * 解析别名为规范化名称
	 * a->b->c->d 前一项是后一项的别名，name为a，则最终返回的值为d
	 * <p>
	 * Determine the raw name, resolving aliases to canonical names.
	 *
	 * @param name the user-specified name
	 * @return the transformed name
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing...
		String resolvedName;
		do {
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				canonicalName = resolvedName;
			}
		}
		while (resolvedName != null);
		return canonicalName;
	}

}
