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

package org.springframework.beans.factory.config;

import java.util.Set;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.ConstructorResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DisposableBeanAdapter;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * <p>This is just a minimal interface: The main intention is to allow a
 * {@link BeanFactoryPostProcessor} to introspect and modify property values
 * and other bean metadata.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see ConfigurableListableBeanFactory#getBeanDefinition
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 * @since 19.03.2004
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * Scope identifier for the standard singleton scope: {@value}.
	 * <p>Note that extended bean factories might support further scopes.
	 *
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * Scope identifier for the standard prototype scope: {@value}.
	 * <p>Note that extended bean factories might support further scopes.
	 *
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * Role hint indicating that a {@code BeanDefinition} is a major part
	 * of the application. Typically corresponds to a user-defined bean.
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * Role hint indicating that a {@code BeanDefinition} is a supporting
	 * part of some larger configuration, typically an outer
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 * {@code SUPPORT} beans are considered important enough to be aware
	 * of when looking more closely at a particular
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition},
	 * but not when looking at the overall configuration of an application.
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * Role hint indicating that a {@code BeanDefinition} is providing an
	 * entirely background role and has no relevance to the end-user. This hint is
	 * used when registering beans that are completely part of the internal workings
	 * of a {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 */
	int ROLE_INFRASTRUCTURE = 2;

	// Modifiable attributes

	/**
	 * Set the name of the parent definition of this bean definition, if any.
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * 返回父 BeanDefinition，合并 BeanDefinition 时使用，子 BeanDefinition 将覆盖父 BeanDefinition
	 *
	 * @see AbstractBeanFactory#getMergedBeanDefinition(String, BeanDefinition, BeanDefinition)
	 *
	 * Return the name of the parent definition of this bean definition, if any.
	 */
	@Nullable
	String getParentName();

	/**
	 * Specify the bean class name of this bean definition.
	 * <p>The class name can be modified during bean factory post-processing,
	 * typically replacing the original class name with a parsed variant of it.
	 *
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * 当前 BeanDefinition 的 bean 的类名，不一定是运行时真实的类名，可以包含占位符，可能会被修改
	 *
	 * @see AbstractAutowireCapableBeanFactory#createBean(String, RootBeanDefinition, Object[])
	 * @see AbstractBeanFactory#doResolveBeanClass(RootBeanDefinition, Class[])
	 * @see org.springframework.context.annotation.ConfigurationClassParser#parse(Set)
	 *
	 * Return the current bean class name of this bean definition.
	 * <p>Note that this does not have to be the actual class name used at runtime, in
	 * case of a child definition overriding/inheriting the class name from its parent.
	 * Also, this may just be the class that a factory method is called on, or it may
	 * even be empty in case of a factory bean reference that a method is called on.
	 * Hence, do <i>not</i> consider this to be the definitive bean type at runtime but
	 * rather only use it for parsing purposes at the individual bean definition level.
	 * @see #getParentName()
	 * @see #getFactoryBeanName()
	 * @see #getFactoryMethodName()
	 */
	@Nullable
	String getBeanClassName();

	/**
	 * Override the target scope of this bean, specifying a new scope name.
	 *
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * 当前 bean 的作用域，如 singleton/prototype/request/session/application 等，会影响 bean 的获取
	 *
	 * @see AbstractBeanFactory#doGetBean
	 *
	 * Return the name of the current target scope for this bean,
	 * or {@code null} if not known yet.
	 */
	@Nullable
	String getScope();

	/**
	 * Set whether this bean should be lazily initialized.
	 * <p>If {@code false}, the bean will get instantiated on startup by bean
	 * factories that perform eager initialization of singletons.
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * 当前 bean 是否延迟初始化，容器启动后会初始化所有非延迟初始化的单例 bean
	 *
	 * @see ConfigurableListableBeanFactory#preInstantiateSingletons
	 *
	 * Return whether this bean should be lazily initialized, i.e. not
	 * eagerly instantiated on startup. Only applicable to a singleton bean.
	 */
	boolean isLazyInit();

	/**
	 * 设置当前 bean 在初始化时依赖的其他 bean
	 *
	 * Set the names of the beans that this bean depends on being initialized.
	 * The bean factory will guarantee that these beans get initialized first.
	 */
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * 当前 bean 依赖的其他 bean，必须保证依赖的 bean 初始化，当前 bean 才能初始化
	 *
	 * @see AbstractBeanFactory#doGetBean(String, Class, Object[], boolean)
	 *
	 * Return the bean names that this bean depends on.
	 */
	@Nullable
	String[] getDependsOn();

	/**
	 * 设置当前 bean 是否为 autowire 的候选对象，只影响按类型装配，不影响按名称装配
	 *
	 * Set whether this bean is a candidate for getting autowired into some other bean.
	 * <p>Note that this flag is designed to only affect type-based autowiring.
	 * It does not affect explicit references by name, which will get resolved even
	 * if the specified bean is not marked as an autowire candidate. As a consequence,
	 * autowiring by name will nevertheless inject a bean if the name matches.
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * 当前 bean 是否为其他 bean autowire 的 候选对象
	 *
	 * @see AutowireCandidateResolver#isAutowireCandidate(BeanDefinitionHolder, DependencyDescriptor)
	 * @see DefaultListableBeanFactory#resolveNamedBean(ResolvableType, Object[], boolean)
	 *
	 * Return whether this bean is a candidate for getting autowired into some other bean.
	 */
	boolean isAutowireCandidate();

	/**
	 * 设置当前 bean 是否为主要的 autowire 候选对象
	 *
	 * Set whether this bean is a primary autowire candidate.
	 * <p>If this value is {@code true} for exactly one bean among multiple
	 * matching candidates, it will serve as a tie-breaker.
	 */
	void setPrimary(boolean primary);

	/**
	 * 是否为首要的 autowire 候选对象，相同类型的 bean 不能有多个 primary
	 *
	 * @see DefaultListableBeanFactory#isPrimary(String, Object)
	 *
	 * Return whether this bean is a primary autowire candidate.
	 */
	boolean isPrimary();

	/**
	 * Specify the factory bean to use, if any.
	 * This the name of the bean to call the specified factory method on.
	 *
	 * @see #setFactoryMethodName
	 */
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * 工厂 bean 的名称，不为空则当前 bean 由工厂 bean 进行创建
	 * 用于创建bean，获取 bean 的类型
	 *
	 * @see AbstractAutowireCapableBeanFactory#getTypeForFactoryMethod(String, RootBeanDefinition, Class[])
	 * @see AbstractAutowireCapableBeanFactory#getTypeForFactoryBean(String, RootBeanDefinition, boolean)
	 * @see ConstructorResolver#instantiateUsingFactoryMethod(String, RootBeanDefinition, Object[])
	 *
	 * Return the factory bean name, if any.
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * Specify a factory method, if any. This method will be invoked with
	 * constructor arguments, or with no arguments if none are specified.
	 * The method will be invoked on the specified factory bean, if any,
	 * or otherwise as a static method on the local bean class.
	 *
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * 工厂方法名称，不为空则使用工厂 bean 的工厂方法创建 bean
	 * 用于获取 bean 的类型，实例化 bean
	 *
	 * @see AbstractAutowireCapableBeanFactory#determineTargetType(String, RootBeanDefinition, Class[])
	 * @see AbstractAutowireCapableBeanFactory#getTypeForFactoryBean(String, RootBeanDefinition, boolean)
	 * @see AbstractAutowireCapableBeanFactory#createBeanInstance(String, RootBeanDefinition, Object[])
	 *
	 *
	 * Return a factory method, if any.
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * 实例化 bean 的构造器/工厂方法参数
	 * 用于从工厂方法中获取 bean 的类型，确定构造方法/工厂方法来实例化 bean
	 *
	 * Return the constructor argument values for this bean.
	 * <p>The returned instance can be modified during bean factory post-processing.
	 *
	 * @return the ConstructorArgumentValues object (never {@code null})
	 * @see ConstructorResolver#autowireConstructor(String, RootBeanDefinition, Constructor[], Object[])
	 * @see AbstractAutowireCapableBeanFactory#getTypeForFactoryMethod(String, RootBeanDefinition, Class[])
	 * @see ConstructorResolver#instantiateUsingFactoryMethod(String, RootBeanDefinition, Object[])
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * Return if there are constructor argument values defined for this bean.
	 *
	 * @since 5.0.2
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * bean 的属性值信息，可用于填充属性值
	 *
	 * @return the MutablePropertyValues object (never {@code null})
	 * @see AbstractAutowireCapableBeanFactory#populateBean(String, RootBeanDefinition, BeanWrapper)
	 *
	 * Return the property values to be applied to a new instance of the bean.
	 * <p>The returned instance can be modified during bean factory post-processing.
	 */
	MutablePropertyValues getPropertyValues();

	/**
	 * Return if there are property values defined for this bean.
	 *
	 * @since 5.0.2
	 */
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}

	/**
	 * Set the name of the initializer method.
	 *
	 * @since 5.1
	 */
	void setInitMethodName(@Nullable String initMethodName);

	/**
	 * bean 的初始化方法，方法不能携带参数
	 *
	 * @see AbstractAutowireCapableBeanFactory#invokeInitMethods(String, Object, RootBeanDefinition)
	 *
	 * Return the name of the initializer method.
	 * @since 5.1
	 */
	@Nullable
	String getInitMethodName();

	/**
	 * Set the name of the destroy method.
	 *
	 * @since 5.1
	 */
	void setDestroyMethodName(@Nullable String destroyMethodName);

	/**
	 * bean 的销毁方法，最多携带一个 boolean 类型的参数
	 *
	 * @see DisposableBeanAdapter#hasDestroyMethod(Object, RootBeanDefinition)
	 *
	 * Return the name of the destroy method.
	 * @since 5.1
	 */
	@Nullable
	String getDestroyMethodName();

	/**
	 * Set the role hint for this {@code BeanDefinition}. The role hint
	 * provides the frameworks as well as tools an indication of
	 * the role and importance of a particular {@code BeanDefinition}.
	 *
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 * @since 5.1
	 */
	void setRole(int role);

	/**
	 * 指明当前 bean 的角色
	 *
	 * Get the role hint for this {@code BeanDefinition}. The role hint
	 * provides the frameworks as well as tools an indication of
	 * the role and importance of a particular {@code BeanDefinition}.
	 *
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * Set a human-readable description of this bean definition.
	 *
	 * @since 5.1
	 */
	void setDescription(@Nullable String description);

	/**
	 * 获取 bean 的描述
	 *
	 * Return a human-readable description of this bean definition.
	 */
	@Nullable
	String getDescription();

	// Read-only attributes

	/**
	 * 获取运行时 bean 的类型
	 *
	 * Return a resolvable type for this bean definition,
	 * based on the bean class or other specific metadata.
	 * <p>This is typically fully resolved on a runtime-merged bean definition
	 * but not necessarily on a configuration-time definition instance.
	 *
	 * @return the resolvable type (potentially {@link ResolvableType#NONE})
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition
	 * @since 5.2
	 */
	ResolvableType getResolvableType();

	/**
	 * bean 是否为单例
	 *
	 * Return whether this a <b>Singleton</b>, with a single, shared instance
	 * returned on all calls.
	 *
	 * @see #SCOPE_SINGLETON
	 */
	boolean isSingleton();

	/**
	 * bean 是否为原型
	 *
	 * Return whether this a <b>Prototype</b>, with an independent instance
	 * returned for each call.
	 *
	 * @see #SCOPE_PROTOTYPE
	 * @since 3.0
	 */
	boolean isPrototype();

	/**
	 * bean 是否为抽象的
	 *
	 * Return whether this bean is "abstract", that is, not meant to be instantiated.
	 */
	boolean isAbstract();

	/**
	 * 获取 bean 的资源描述信息
	 * Return a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 */
	@Nullable
	String getResourceDescription();

	/**
	 * 获取原始的 BeanDefinition
	 *
	 * Return the originating BeanDefinition, or {@code null} if none.
	 * <p>Allows for retrieving the decorated bean definition, if any.
	 * <p>Note that this method returns the immediate originator. Iterate through the
	 * originator chain to find the original BeanDefinition as defined by the user.
	 */
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
