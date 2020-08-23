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

package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

/**
 * BeanFactory中bean对象实现的接口，FactoryBean本身是单个对象的工厂。如果一个bean实现了这个接口，它将被作为一个公开对象的工厂，而不是直接作为一个将要公开的bean实例。
 * <p>
 * 注意：实现此接口的bean不能用作普通bean。FactoryBean是在bean样式中定义的，但是为bean引用公开的对象（getObject（））始终是它创建的对象。
 * <p>
 * FactoryBeans可以支持单例和原型，并且可以根据需要延迟创建对象，或者在启动时实时创建对象。SmartFactoryBean接口允许公开更细粒度的行为元数据。
 * <p>
 * 这个接口在框架本身中大量使用，例如AOP ProxyFactoryBean或者JndiObjectFactoryBean。它也可以用于自定义组件；但是，这只在基础结构代码中常见。
 * <p>
 * FactoryBean是一个程序化的契约。实现不应该依赖注解驱动的注入或其他反射工具。getObjectType（）getObject（）调用可能在引导过程的早期到达，甚至在任何后处理器设置之前。如果需要访问其他bean，请实现BeanFactoryAware并以编程方式获取它们。
 * <p>
 * 容器只负责管理FactoryBean实例的生命周期，而不负责由FactoryBean创建的对象的生命周期。因此，对公开的bean对象（Closeable#close()）不会自动调用。相反，FactoryBean应该实现DisposableBean并将任何这样的close调用委托给底层对象。
 * <p>
 * 最后，FactoryBean对象参与包含BeanFactory的bean创建同步。通常不需要内部同步，除了为了在FactoryBean本身（或类似的）中进行延迟初始化。
 * <p>
 * Interface to be implemented by objects used within a {@link BeanFactory} which
 * are themselves factories for individual objects. If a bean implements this
 * interface, it is used as a factory for an object to expose, not directly as a
 * bean instance that will be exposed itself.
 *
 * <p><b>NB: A bean that implements this interface cannot be used as a normal bean.</b>
 * A FactoryBean is defined in a bean style, but the object exposed for bean
 * references ({@link #getObject()}) is always the object that it creates.
 *
 * <p>FactoryBeans can support singletons and prototypes, and can either create
 * objects lazily on demand or eagerly on startup. The {@link SmartFactoryBean}
 * interface allows for exposing more fine-grained behavioral metadata.
 *
 * <p>This interface is heavily used within the framework itself, for example for
 * the AOP {@link org.springframework.aop.framework.ProxyFactoryBean} or the
 * {@link org.springframework.jndi.JndiObjectFactoryBean}. It can be used for
 * custom components as well; however, this is only common for infrastructure code.
 *
 * <p><b>{@code FactoryBean} is a programmatic contract. Implementations are not
 * supposed to rely on annotation-driven injection or other reflective facilities.</b>
 * {@link #getObjectType()} {@link #getObject()} invocations may arrive early in the
 * bootstrap process, even ahead of any post-processor setup. If you need access to
 * other beans, implement {@link BeanFactoryAware} and obtain them programmatically.
 *
 * <p><b>The container is only responsible for managing the lifecycle of the FactoryBean
 * instance, not the lifecycle of the objects created by the FactoryBean.</b> Therefore,
 * a destroy method on an exposed bean object (such as {@link java.io.Closeable#close()}
 * will <i>not</i> be called automatically. Instead, a FactoryBean should implement
 * {@link DisposableBean} and delegate any such close call to the underlying object.
 *
 * <p>Finally, FactoryBean objects participate in the containing BeanFactory's
 * synchronization of bean creation. There is usually no need for internal
 * synchronization other than for purposes of lazy initialization within the
 * FactoryBean itself (or the like).
 *
 * @param <T> the bean type
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 * @since 08.03.2003
 */
public interface FactoryBean<T> {

	/**
	 * BeanDefinition可以使用AttributeAccessor#setAttribute方法设置的属性的名称，以便可以推断bean的类型
	 * <p>
	 * The name of an attribute that can be
	 * {@link org.springframework.core.AttributeAccessor#setAttribute set} on a
	 * {@link org.springframework.beans.factory.config.BeanDefinition} so that
	 * factory beans can signal their object type when it can't be deduced from
	 * the factory bean class.
	 *
	 * @since 5.2
	 */
	String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";


	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	 * 与BeanFactory一样，这允许同时支持Singleton和Prototype设计模式。
	 * 如果调用时此FactoryBean尚未完全初始化（例如，因为它涉及循环引用），则抛出相应的FactoryBeanNotInitializedException。
	 * 从Spring2.0开始，允许FactoryBeans返回空对象。工厂将把它视为要使用的正常值；
	 * 在这种情况下，它不会再抛出FactoryBeanNotInitializedException。
	 * 现在鼓励FactoryBean实现根据需要抛出FactoryBeanNotInitializedException。
	 * <p>
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * <p>As with a {@link BeanFactory}, this allows support for both the
	 * Singleton and Prototype design pattern.
	 * <p>If this FactoryBean is not fully initialized yet at the time of
	 * the call (for example because it is involved in a circular reference),
	 * throw a corresponding {@link FactoryBeanNotInitializedException}.
	 * <p>As of Spring 2.0, FactoryBeans are allowed to return {@code null}
	 * objects. The factory will consider this as normal value to be used; it
	 * will not throw a FactoryBeanNotInitializedException in this case anymore.
	 * FactoryBean implementations are encouraged to throw
	 * FactoryBeanNotInitializedException themselves now, as appropriate.
	 *
	 * @return an instance of the bean (can be {@code null})
	 * @throws Exception in case of creation errors
	 * @see FactoryBeanNotInitializedException
	 */
	@Nullable
	T getObject() throws Exception;

	/**
	 * 返回此FactoryBean创建的对象类型，如果事先不知道，则返回null。
	 * 这允许用户在不实例化对象的情况下检查特定类型的bean，例如在autowiring上。
	 * 对于正在创建单例对象的实现，此方法应尽量避免创建单例对象；它应该预先估计类型。
	 * 对于原型，在这里返回一个有意义的类型也是可取的。
	 * 在完全初始化此FactoryBean之前可以调用此方法。它不能依赖于初始化期间创建的状态；当然，如果可用，它仍然可以使用这种状态。
	 * 注意：Autowiring只会忽略在这里返回null的factorybean。因此，强烈建议使用FactoryBean的当前状态正确地实现此方法。
	 * <p>
	 * Return the type of object that this FactoryBean creates,
	 * or {@code null} if not known in advance.
	 * <p>This allows one to check for specific types of beans without
	 * instantiating objects, for example on autowiring.
	 * <p>In the case of implementations that are creating a singleton object,
	 * this method should try to avoid singleton creation as far as possible;
	 * it should rather estimate the type in advance.
	 * For prototypes, returning a meaningful type here is advisable too.
	 * <p>This method can be called <i>before</i> this FactoryBean has
	 * been fully initialized. It must not rely on state created during
	 * initialization; of course, it can still use such state if available.
	 * <p><b>NOTE:</b> Autowiring will simply ignore FactoryBeans that return
	 * {@code null} here. Therefore it is highly recommended to implement
	 * this method properly, using the current state of the FactoryBean.
	 *
	 * @return the type of object that this FactoryBean creates,
	 * or {@code null} if not known at the time of the call
	 * @see ListableBeanFactory#getBeansOfType
	 */
	@Nullable
	Class<?> getObjectType();

	/**
	 * 这个工厂管理的对象是单例的吗？也就是说，getObject（）是否总是返回相同的对象（可以缓存的引用）？
	 * <p>
	 * 注意：如果FactoryBean指示保存一个singleton对象，那么从getObject（）返回的对象可能会被拥有的BeanFactory缓存。
	 * 因此，除非FactoryBean始终公开相同的引用，否则不要返回true。
	 * <p>
	 * FactoryBean本身的singleton状态通常由拥有它的BeanFactory提供；通常，它必须在那里定义为singleton。
	 * <p>
	 * 注意：这个返回false的方法不一定表示返回的对象是独立的实例。
	 * 扩展SmartFactoryBean接口的实现可以通过SmartFactoryBean.isPrototype（）方法。
	 * 如果isSingleton（）实现返回false，那么不实现此扩展接口的普通FactoryBean实现将始终返回独立实例。
	 * 默认实现返回true，因为FactoryBean通常管理一个singleton实例。
	 * <p>
	 * Is the object managed by this factory a singleton? That is,
	 * will {@link #getObject()} always return the same object
	 * (a reference that can be cached)?
	 * <p><b>NOTE:</b> If a FactoryBean indicates to hold a singleton object,
	 * the object returned from {@code getObject()} might get cached
	 * by the owning BeanFactory. Hence, do not return {@code true}
	 * unless the FactoryBean always exposes the same reference.
	 * <p>The singleton status of the FactoryBean itself will generally
	 * be provided by the owning BeanFactory; usually, it has to be
	 * defined as singleton there.
	 * <p><b>NOTE:</b> This method returning {@code false} does not
	 * necessarily indicate that returned objects are independent instances.
	 * An implementation of the extended {@link SmartFactoryBean} interface
	 * may explicitly indicate independent instances through its
	 * {@link SmartFactoryBean#isPrototype()} method. Plain {@link FactoryBean}
	 * implementations which do not implement this extended interface are
	 * simply assumed to always return independent instances if the
	 * {@code isSingleton()} implementation returns {@code false}.
	 * <p>The default implementation returns {@code true}, since a
	 * {@code FactoryBean} typically manages a singleton instance.
	 *
	 * @return whether the exposed object is a singleton
	 * @see #getObject()
	 * @see SmartFactoryBean#isPrototype()
	 */
	default boolean isSingleton() {
		return true;
	}

}
