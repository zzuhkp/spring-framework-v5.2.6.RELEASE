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

package org.springframework.beans.factory;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * ObjectFactory的一个变体，专门为注入点设计，允许编程的可选性和宽松而非唯一的处理。
 * 从5.1开始，这个接口扩展了Iterable并提供了流支持。
 * 因此，它可以在for循环中使用，提供forEach迭代，并允许集合样式的流访问。
 * <p>
 * A variant of {@link ObjectFactory} designed specifically for injection points,
 * allowing for programmatic optionality and lenient not-unique handling.
 *
 * <p>As of 5.1, this interface extends {@link Iterable} and provides {@link Stream}
 * support. It can be therefore be used in {@code for} loops, provides {@link #forEach}
 * iteration and allows for collection-style {@link #stream} access.
 *
 * @param <T> the object type
 * @author Juergen Hoeller
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 * @since 4.3
 */
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	 * 允许指定明确的构造器参数
	 * <p>
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * <p>Allows for specifying explicit construction arguments, along the
	 * lines of {@link BeanFactory#getBean(String, Object...)}.
	 *
	 * @param args arguments to use when creating a corresponding instance
	 * @return an instance of the bean
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	T getObject(Object... args) throws BeansException;

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	 * <p>
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 *
	 * @return an instance of the bean, or {@code null} if not available
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	@Nullable
	T getIfAvailable() throws BeansException;

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	 * <p>
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 *
	 * @param defaultSupplier a callback for supplying a default object
	 *                        if none is present in the factory
	 * @return an instance of the bean, or the supplied default object
	 * if no such bean is available
	 * @throws BeansException in case of creation errors
	 * @see #getIfAvailable()
	 * @since 5.0
	 */
	default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 使用此工厂管理的对象的实例（可能是共享的或独立的）（如果可用）。
	 * <p>
	 * Consume an instance (possibly shared or independent) of the object
	 * managed by this factory, if available.
	 *
	 * @param dependencyConsumer a callback for processing the target object
	 *                           if available (not called otherwise)
	 * @throws BeansException in case of creation errors
	 * @see #getIfAvailable()
	 * @since 5.0
	 */
	default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfAvailable();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	 * <p>
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 *
	 * @return an instance of the bean, or {@code null} if not available or
	 * not unique (i.e. multiple candidates found with none marked as primary)
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	@Nullable
	T getIfUnique() throws BeansException;

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	 * <p>
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 *
	 * @param defaultSupplier a callback for supplying a default object
	 *                        if no unique candidate is present in the factory
	 * @return an instance of the bean, or the supplied default object
	 * if no such bean is available or if it is not unique in the factory
	 * (i.e. multiple candidates found with none marked as primary)
	 * @throws BeansException in case of creation errors
	 * @see #getIfUnique()
	 * @since 5.0
	 */
	default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfUnique();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * 如果唯一，则使用此工厂管理的对象的实例（可能是共享的或独立的）。
	 * <p>
	 * Consume an instance (possibly shared or independent) of the object
	 * managed by this factory, if unique.
	 *
	 * @param dependencyConsumer a callback for processing the target object
	 *                           if unique (not called otherwise)
	 * @throws BeansException in case of creation errors
	 * @see #getIfAvailable()
	 * @since 5.0
	 */
	default void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfUnique();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * 返回所有匹配对象实例的迭代器，无需特定的顺序保证（但通常按注册顺序）。
	 * <p>
	 * Return an {@link Iterator} over all matching object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 *
	 * @see #stream()
	 * @since 5.1
	 */
	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	/**
	 * 在所有匹配的对象实例上返回一个连续的流，没有特定的顺序保证（但通常是按注册顺序）。
	 * <p>
	 * Return a sequential {@link Stream} over all matching object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 *
	 * @see #iterator()
	 * @see #orderedStream()
	 * @since 5.1
	 */
	default Stream<T> stream() {
		throw new UnsupportedOperationException("Multi element access not supported");
	}

	/**
	 * 返回所有匹配对象实例的序列流，根据工厂的公共顺序比较器预先排序。
	 * 在标准Spring应用程序上下文中，将根据Ordered约定，如果是基于注解的配置，还应考虑Order注解，类似于列表/数组类型的多元素注入点。
	 * <p>
	 * Return a sequential {@link Stream} over all matching object instances,
	 * pre-ordered according to the factory's common order comparator.
	 * <p>In a standard Spring application context, this will be ordered
	 * according to {@link org.springframework.core.Ordered} conventions,
	 * and in case of annotation-based configuration also considering the
	 * {@link org.springframework.core.annotation.Order} annotation,
	 * analogous to multi-element injection points of list/array type.
	 *
	 * @see #stream()
	 * @see org.springframework.core.OrderComparator
	 * @since 5.1
	 */
	default Stream<T> orderedStream() {
		throw new UnsupportedOperationException("Ordered element access not supported");
	}

}
