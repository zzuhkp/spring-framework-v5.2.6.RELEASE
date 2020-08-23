/*
 * Copyright 2002-2019 the original author or authors.
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

import java.beans.PropertyEditor;
import java.security.AccessControlContext;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

/**
 *
 * Configuration interface to be implemented by most bean factories. Provides
 * facilities to configure a bean factory, in addition to the bean factory
 * client methods in the {@link org.springframework.beans.factory.BeanFactory}
 * interface.
 *
 * <p>This bean factory interface is not meant to be used in normal application
 * code: Stick to {@link org.springframework.beans.factory.BeanFactory} or
 * {@link org.springframework.beans.factory.ListableBeanFactory} for typical
 * needs. This extended interface is just meant to allow for framework-internal
 * plug'n'play and for special access to bean factory configuration methods.
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.ListableBeanFactory
 * @see ConfigurableListableBeanFactory
 * @since 03.11.2003
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

	/**
	 * Scope identifier for the standard singleton scope: {@value}.
	 * <p>Custom scopes can be added via {@code registerScope}.
	 *
	 * @see #registerScope
	 */
	String SCOPE_SINGLETON = "singleton";

	/**
	 * Scope identifier for the standard prototype scope: {@value}.
	 * <p>Custom scopes can be added via {@code registerScope}.
	 *
	 * @see #registerScope
	 */
	String SCOPE_PROTOTYPE = "prototype";


	/**
	 * 不能更改父级：只有在工厂实例化时父级不可用时，才应在构造函数外部设置父级。
	 * <p>
	 * Set the parent of this bean factory.
	 * <p>Note that the parent cannot be changed: It should only be set outside
	 * a constructor if it isn't available at the time of factory instantiation.
	 *
	 * @param parentBeanFactory the parent BeanFactory
	 * @throws IllegalStateException if this factory is already associated with
	 *                               a parent BeanFactory
	 * @see #getParentBeanFactory()
	 */
	void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

	/**
	 * 这个类加载器只应用于没有已解析的bean class的BeanDefinition
	 * <p>
	 * Set the class loader to use for loading bean classes.
	 * Default is the thread context class loader.
	 * <p>Note that this class loader will only apply to bean definitions
	 * that do not carry a resolved bean class yet. This is the case as of
	 * Spring 2.0 by default: Bean definitions only carry bean class names,
	 * to be resolved once the factory processes the bean definition.
	 *
	 * @param beanClassLoader the class loader to use,
	 *                        or {@code null} to suggest the default class loader
	 */
	void setBeanClassLoader(@Nullable ClassLoader beanClassLoader);

	/**
	 * Return this factory's class loader for loading bean classes
	 * (only {@code null} if even the system ClassLoader isn't accessible).
	 *
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * Specify a temporary ClassLoader to use for type matching purposes.
	 * Default is none, simply using the standard bean ClassLoader.
	 * <p>A temporary ClassLoader is usually just specified if
	 * <i>load-time weaving</i> is involved, to make sure that actual bean
	 * classes are loaded as lazily as possible. The temporary loader is
	 * then removed once the BeanFactory completes its bootstrap phase.
	 *
	 * @since 2.5
	 */
	void setTempClassLoader(@Nullable ClassLoader tempClassLoader);

	/**
	 * Return the temporary ClassLoader to use for type matching purposes,
	 * if any.
	 *
	 * @since 2.5
	 */
	@Nullable
	ClassLoader getTempClassLoader();

	/**
	 * Set whether to cache bean metadata such as given bean definitions
	 * (in merged fashion) and resolved bean classes. Default is on.
	 * <p>Turn this flag off to enable hot-refreshing of bean definition objects
	 * and in particular bean classes. If this flag is off, any creation of a bean
	 * instance will re-query the bean class loader for newly resolved classes.
	 */
	void setCacheBeanMetadata(boolean cacheBeanMetadata);

	/**
	 * Return whether to cache bean metadata such as given bean definitions
	 * (in merged fashion) and resolved bean classes.
	 */
	boolean isCacheBeanMetadata();

	/**
	 * Specify the resolution strategy for expressions in bean definition values.
	 * <p>There is no expression support active in a BeanFactory by default.
	 * An ApplicationContext will typically set a standard expression strategy
	 * here, supporting "#{...}" expressions in a Unified EL compatible style.
	 *
	 * @since 3.0
	 */
	void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver);

	/**
	 * Return the resolution strategy for expressions in bean definition values.
	 *
	 * @since 3.0
	 */
	@Nullable
	BeanExpressionResolver getBeanExpressionResolver();

	/**
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 *
	 * @since 3.0
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * Return the associated ConversionService, if any.
	 *
	 * @since 3.0
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * 添加要应用于所有bean创建过程的PropertyEditorRegistrar。
	 * PropertyEditorRegistrar创建新的PropertyEditor实例，
	 * 并在给定的PropertyEditorRegistry注册它们，每次创建bean时都是新的。
	 * 避免了在自定义PropertyEditor上进行同步的需要；因此，通常最好使用此方法而不是registerCustomEditor。
	 * <p>
	 * Add a PropertyEditorRegistrar to be applied to all bean creation processes.
	 * <p>Such a registrar creates new PropertyEditor instances and registers them
	 * on the given registry, fresh for each bean creation attempt. This avoids
	 * the need for synchronization on custom editors; hence, it is generally
	 * preferable to use this method instead of {@link #registerCustomEditor}.
	 *
	 * @param registrar the PropertyEditorRegistrar to register
	 */
	void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

	/**
	 * 为给定类型的所有属性注册给定的自定义属性编辑器。在工厂配置期间调用。
	 * 此方法将注册共享的自定义PropertyEditor实例；为了线程安全，将同步对该实例的访问。
	 * 通常最好使用addPropertyEditorRegistrar而不是此方法，以避免在自定义PropertyEditor上进行同步。
	 * <p>
	 * Register the given custom property editor for all properties of the
	 * given type. To be invoked during factory configuration.
	 * <p>Note that this method will register a shared custom editor instance;
	 * access to that instance will be synchronized for thread-safety. It is
	 * generally preferable to use {@link #addPropertyEditorRegistrar} instead
	 * of this method, to avoid for the need for synchronization on custom editors.
	 *
	 * @param requiredType        type of the property
	 * @param propertyEditorClass the {@link PropertyEditor} class to register
	 */
	void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

	/**
	 * 使用已在此BeanFactory中注册的自定义编辑器初始化给定的PropertyEditorRegistry。
	 * <p>
	 * Initialize the given PropertyEditorRegistry with the custom editors
	 * that have been registered with this BeanFactory.
	 *
	 * @param registry the PropertyEditorRegistry to initialize
	 */
	void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

	/**
	 * 设置此BeanFactory用于转换bean属性值、构造函数参数值等的自定义类型转换器。
	 * 这将覆盖默认的PropertyEditor机制，
	 * 从而使任何自定义PropertyEditor或自定义PropertyEditorRegistrar都不相关。
	 * <p>
	 * Set a custom type converter that this BeanFactory should use for converting
	 * bean property values, constructor argument values, etc.
	 * <p>This will override the default PropertyEditor mechanism and hence make
	 * any custom editors or custom editor registrars irrelevant.
	 *
	 * @see #addPropertyEditorRegistrar
	 * @see #registerCustomEditor
	 * @since 2.5
	 */
	void setTypeConverter(TypeConverter typeConverter);

	/**
	 * 获取此BeanFactory使用的类型转换器。每次调用可能返回一个新实例，因为类型转换器通常不是线程安全的。
	 * 如果默认的PropertyEditor机制处于活动状态，则返回的TypeConverter将知道所有已注册的自定义PropertyEditor。
	 * <p>
	 * Obtain a type converter as used by this BeanFactory. This may be a fresh
	 * instance for each call, since TypeConverters are usually <i>not</i> thread-safe.
	 * <p>If the default PropertyEditor mechanism is active, the returned
	 * TypeConverter will be aware of all custom editors that have been registered.
	 *
	 * @since 2.5
	 */
	TypeConverter getTypeConverter();

	/**
	 * 为嵌入值（如注解属性）添加字符串解析器。
	 * <p>
	 * Add a String resolver for embedded values such as annotation attributes.
	 *
	 * @param valueResolver the String resolver to apply to embedded values
	 * @since 3.0
	 */
	void addEmbeddedValueResolver(StringValueResolver valueResolver);

	/**
	 * 确定嵌入值解析器是否注册到此BeanFactory，以便通过{@link #resolveEmbeddedValue(String)}使用。
	 * <p>
	 * Determine whether an embedded value resolver has been registered with this
	 * bean factory, to be applied through {@link #resolveEmbeddedValue(String)}.
	 *
	 * @since 4.3
	 */
	boolean hasEmbeddedValueResolver();

	/**
	 * 解析给定的嵌入值，例如注解属性。
	 * <p>
	 * Resolve the given embedded value, e.g. an annotation attribute.
	 *
	 * @param value the value to resolve
	 * @return the resolved value (may be the original value as-is)
	 * @since 3.0
	 */
	@Nullable
	String resolveEmbeddedValue(String value);

	/**
	 * 添加一个新的应用于此工厂创建的bean的BeanPostProcessor。在工厂配置期间调用。
	 * 注意：这里提交的BeanPostProcessor将按注册顺序应用；
	 * 通过实现{@link org.springframework.core.Ordered}将忽略已排序的接口。
	 * 请注意，自动检测到的后处理器（例如，作为ApplicationContext中的bean）
	 * 将始终在以编程方式注册后应用。
	 * <p>
	 * Add a new BeanPostProcessor that will get applied to beans created
	 * by this factory. To be invoked during factory configuration.
	 * <p>Note: Post-processors submitted here will be applied in the order of
	 * registration; any ordering semantics expressed through implementing the
	 * {@link org.springframework.core.Ordered} interface will be ignored. Note
	 * that autodetected post-processors (e.g. as beans in an ApplicationContext)
	 * will always be applied after programmatically registered ones.
	 *
	 * @param beanPostProcessor the post-processor to register
	 */
	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	/**
	 * 返回当前已注册的BeanPostProcessor数量（如果有）。
	 * Return the current number of registered BeanPostProcessors, if any.
	 */
	int getBeanPostProcessorCount();

	/**
	 * 注册给定的作用域，由给定的作用域实现支持。
	 * <p>
	 * Register the given scope, backed by the given Scope implementation.
	 *
	 * @param scopeName the scope identifier
	 * @param scope     the backing Scope implementation
	 */
	void registerScope(String scopeName, Scope scope);

	/**
	 * 返回当前注册的所有作用域的名称。
	 * 只返回显式注册作用域的名称。诸如“singleton”和“prototype”之类的内置作用域不会公开。
	 * <p>
	 * Return the names of all currently registered scopes.
	 * <p>This will only return the names of explicitly registered scopes.
	 * Built-in scopes such as "singleton" and "prototype" won't be exposed.
	 *
	 * @return the array of scope names, or an empty array if none
	 * @see #registerScope
	 */
	String[] getRegisteredScopeNames();

	/**
	 * 返回给定作用域名称的作用域实现（如果有）。
	 * 只返回显式注册的作用域。诸如“singleton”和“prototype”之类的内置作用域不会公开。
	 * <p>
	 * Return the Scope implementation for the given scope name, if any.
	 * <p>This will only return explicitly registered scopes.
	 * Built-in scopes such as "singleton" and "prototype" won't be exposed.
	 *
	 * @param scopeName the name of the scope
	 * @return the registered Scope implementation, or {@code null} if none
	 * @see #registerScope
	 */
	@Nullable
	Scope getRegisteredScope(String scopeName);

	/**
	 * 提供与此工厂相关的安全访问控制上下文。
	 * <p>
	 * Provides a security access control context relevant to this factory.
	 *
	 * @return the applicable AccessControlContext (never {@code null})
	 * @since 3.0
	 */
	AccessControlContext getAccessControlContext();

	/**
	 * 从给定的其他工厂复制所有相关配置。
	 * 应包括所有标准配置设置以及BeanPostProcessors、作用域和工厂特定的内部设置。
	 * 不应包含实际bean定义的任何元数据，例如BeanDefinition对象和bean名称别名。
	 * <p>
	 * Copy all relevant configuration from the given other factory.
	 * <p>Should include all standard configuration settings as well as
	 * BeanPostProcessors, Scopes, and factory-specific internal settings.
	 * Should not include any metadata of actual bean definitions,
	 * such as BeanDefinition objects and bean name aliases.
	 *
	 * @param otherFactory the other BeanFactory to copy from
	 */
	void copyConfigurationFrom(ConfigurableBeanFactory otherFactory);

	/**
	 * 给定bean名称，创建别名。
	 * 我们通常使用此方法来支持XML id中非法的名称（用于bean名称）。
	 * 通常在工厂配置期间调用，但也可以用于别名的运行时注册。因此，工厂实现应该同步别名访问。
	 * <p>
	 * Given a bean name, create an alias. We typically use this method to
	 * support names that are illegal within XML ids (used for bean names).
	 * <p>Typically invoked during factory configuration, but can also be
	 * used for runtime registration of aliases. Therefore, a factory
	 * implementation should synchronize alias access.
	 *
	 * @param beanName the canonical name of the target bean
	 * @param alias    the alias to be registered for the bean
	 * @throws BeanDefinitionStoreException if the alias is already in use
	 */
	void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException;

	/**
	 * 解析此工厂中注册的所有别名目标名称和别名，并将给定的StringValueResolver应用于它们。
	 * 例如，值解析器可以解析目标bean名称甚至别名中的占位符。
	 * <p>
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 *
	 * @param valueResolver the StringValueResolver to apply
	 * @since 2.5
	 */
	void resolveAliases(StringValueResolver valueResolver);

	/**
	 * 返回给定bean名称合并后的BeanDefinition，必要时将子BeanDefinition与其父BeanDefinition合并。
	 * 同时考虑祖先工厂中的BeanDefinition。
	 * <p>
	 * Return a merged BeanDefinition for the given bean name,
	 * merging a child bean definition with its parent if necessary.
	 * Considers bean definitions in ancestor factories as well.
	 *
	 * @param beanName the name of the bean to retrieve the merged definition for
	 * @return a (potentially merged) BeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean definition with the given name
	 * @since 2.5
	 */
	BeanDefinition getMergedBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的bean是否为FactoryBean。
	 * <p>
	 * Determine whether the bean with the given name is a FactoryBean.
	 *
	 * @param name the name of the bean to check
	 * @return whether the bean is a FactoryBean
	 * ({@code false} means the bean exists but is not a FactoryBean)
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 2.5
	 */
	boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 显式控制指定bean的当前创建状态。仅供容器内部使用。
	 * <p>
	 * Explicitly control the current in-creation status of the specified bean.
	 * For container-internal use only.
	 *
	 * @param beanName   the name of the bean
	 * @param inCreation whether the bean is currently in creation
	 * @since 3.1
	 */
	void setCurrentlyInCreation(String beanName, boolean inCreation);

	/**
	 * 确定指定的bean当前是否正在创建中。
	 * <p>
	 * Determine whether the specified bean is currently in creation.
	 *
	 * @param beanName the name of the bean
	 * @return whether the bean is currently in creation
	 * @since 2.5
	 */
	boolean isCurrentlyInCreation(String beanName);

	/**
	 * 注册一个依赖指定bean的bean，以便在指定bean被销毁之前销毁。
	 * <p>
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 *
	 * @param beanName          the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 * @since 2.5
	 */
	void registerDependentBean(String beanName, String dependentBeanName);

	/**
	 * 返回依赖于指定bean的所有bean的名称（如果有）。
	 * <p>
	 * Return the names of all beans which depend on the specified bean, if any.
	 *
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 * @since 2.5
	 */
	String[] getDependentBeans(String beanName);

	/**
	 * 返回指定bean所依赖的所有bean的名称（如果有）。
	 * <p>
	 * Return the names of all beans that the specified bean depends on, if any.
	 *
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 * @since 2.5
	 */
	String[] getDependenciesForBean(String beanName);

	/**
	 * 根据bean定义销毁给定的bean实例（通常是从这个工厂获得的原型实例）。
	 * 在销毁过程中出现的任何异常都应该被捕获并记录，而不是传播给此方法的调用方。
	 * <p>
	 * <p>
	 * Destroy the given bean instance (usually a prototype instance
	 * obtained from this factory) according to its bean definition.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 *
	 * @param beanName     the name of the bean definition
	 * @param beanInstance the bean instance to destroy
	 */
	void destroyBean(String beanName, Object beanInstance);

	/**
	 * 销毁当前目标作用域中的指定作用域bean（如果有）。
	 * 在销毁过程中出现的任何异常都应该被捕获并记录，而不是传播给此方法的调用方。
	 * <p>
	 * Destroy the specified scoped bean in the current target scope, if any.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 *
	 * @param beanName the name of the scoped bean
	 */
	void destroyScopedBean(String beanName);

	/**
	 * 销毁此工厂中的所有单例bean，包括已注册为一次性的内部bean。工厂被关闭时调用。
	 * 在销毁过程中出现的任何异常都应该被捕获并记录，而不是传播给此方法的调用方。
	 *
	 * Destroy all singleton beans in this factory, including inner beans that have
	 * been registered as disposable. To be called on shutdown of a factory.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 */
	void destroySingletons();

}
