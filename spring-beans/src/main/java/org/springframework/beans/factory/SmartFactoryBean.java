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

/**
 * FactoryBean接口的扩展。实现可能会指示它们是否总是返回独立实例，因为它们的isSingleton（）实现返回false并不清楚地指示独立实例。
 * 如果isSingleton（）实现返回false，那么不实现此扩展接口的普通FactoryBean实现将始终返回独立的实例；只在需要时访问暴露的对象。
 * 注意：此接口是一个特殊用途的接口，主要用于框架内部和协作框架内。
 * 一般来说，应用程序提供的FactoryBeans应该简单地实现纯FactoryBean接口。
 * 新方法可能会被添加到这个扩展接口中，即使在小版本发布中也是如此。
 * <p>
 * Extension of the {@link FactoryBean} interface. Implementations may
 * indicate whether they always return independent instances, for the
 * case where their {@link #isSingleton()} implementation returning
 * {@code false} does not clearly indicate independent instances.
 *
 * <p>Plain {@link FactoryBean} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their {@link #isSingleton()} implementation returns
 * {@code false}; the exposed object is only accessed on demand.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryBeans should simply implement
 * the plain {@link FactoryBean} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 * @param <T> the bean type
 * @author Juergen Hoeller
 * @see #isPrototype()
 * @see #isSingleton()
 * @since 2.0.3
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * 这个工厂管理的对象是原型吗？也就是说，getObject（）是否总是返回一个独立的实例？
	 * FactoryBean本身的原型状态通常由拥有它的BeanFactory提供；通常，它必须被定义为singleton。
	 * 此方法应该严格检查独立实例；对于作用域对象或其他类型的非单例、非独立对象，它不应返回true。
	 * 因此，这不仅仅是isSingleton（）的倒置形式。 默认实现返回false。
	 * <p>
	 * Is the object managed by this factory a prototype? That is,
	 * will {@link #getObject()} always return an independent instance?
	 * <p>The prototype status of the FactoryBean itself will generally
	 * be provided by the owning {@link BeanFactory}; usually, it has to be
	 * defined as singleton there.
	 * <p>This method is supposed to strictly check for independent instances;
	 * it should not return {@code true} for scoped objects or other
	 * kinds of non-singleton, non-independent objects. For this reason,
	 * this is not simply the inverted form of {@link #isSingleton()}.
	 * <p>The default implementation returns {@code false}.
	 *
	 * @return whether the exposed object is a prototype
	 * @see #getObject()
	 * @see #isSingleton()
	 */
	default boolean isPrototype() {
		return false;
	}

	/**
	 * 这个FactoryBean是否期望立即初始化，也就是说，急切地初始化它自己以及期望它的singleton对象（如果有的话）的急切初始化？
	 * 标准的FactoryBean不需要急于初始化：它的getObject（）只会在实际访问时被调用，即使是在单例对象的情况下也是如此。
	 * 从这个方法返回true意味着应该急切地调用getObject（），也应该急切地应用后处理程序。
	 * 对于单例对象，这可能是有意义的，特别是如果后处理器希望在启动时应用。 默认实现返回false。
	 * <p>
	 * Does this FactoryBean expect eager initialization, that is,
	 * eagerly initialize itself as well as expect eager initialization
	 * of its singleton object (if any)?
	 * <p>A standard FactoryBean is not expected to initialize eagerly:
	 * Its {@link #getObject()} will only be called for actual access, even
	 * in case of a singleton object. Returning {@code true} from this
	 * method suggests that {@link #getObject()} should be called eagerly,
	 * also applying post-processors eagerly. This may make sense in case
	 * of a {@link #isSingleton() singleton} object, in particular if
	 * post-processors expect to be applied on startup.
	 * <p>The default implementation returns {@code false}.
	 *
	 * @return whether eager initialization applies
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	default boolean isEagerInit() {
		return false;
	}

}
