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

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * 允许自定义修改新bean实例的工厂钩子，例如，检查标记接口或用代理包装bean。
 * <p>
 * 通常，通过标记接口等填充bean的BeanPostProcessor将实现{@link #postProcessBeforeInitialization}，
 * 而使用代理包装bean的BeanPostProcessor通常实现{@link #postProcessAfterInitialization}。
 * <p>
 * ApplicationContext可以在其BeanDefinition中自动检测BeanPostProcessor bean，
 * 并将这些BeanPostProcessor应用于随后创建的任何bean。
 * 一个普通的BeanFactory允许对BeanPostProcessor进行编程注册，将它们应用于通过bean工厂创建的所有bean。
 * <p>
 * 在ApplicationContext中自动检测到的BeanPostProcessor bean将根据
 * {@link org.springframework.core.PriorityOrdered}和{@link org.springframework.core.Ordered}
 * 语义执行。相反，以编程方式向BeanFactory注册的BeanPostProcessor将按注册顺序应用；
 * 通过实现PriorityOrdered或Ordered接口表示的任何排序语义对于以编程方式注册的BeanPostProcessor都将被忽略。
 * 此外，BeanPostProcessor bean不考虑@Order注释。
 * <p>
 * <p>
 * Factory hook that allows for custom modification of new bean instances &mdash;
 * for example, checking for marker interfaces or wrapping beans with proxies.
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} can autodetect {@code BeanPostProcessor} beans
 * in its bean definitions and apply those post-processors to any beans subsequently
 * created. A plain {@code BeanFactory} allows for programmatic registration of
 * post-processors, applying them to all beans created through the bean factory.
 *
 * <h3>Ordering</h3>
 * <p>{@code BeanPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link org.springframework.core.PriorityOrdered} and
 * {@link org.springframework.core.Ordered} semantics. In contrast,
 * {@code BeanPostProcessor} beans that are registered programmatically with a
 * {@code BeanFactory} will be applied in the order of registration; any ordering
 * semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link org.springframework.core.annotation.Order @Order} annotation is not
 * taken into account for {@code BeanPostProcessor} beans.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 * @since 10.10.2003
 */
public interface BeanPostProcessor {

	/**
	 * 在任何bean初始化回调（如InitializingBean的afterPropertiesSet或自定义init方法）之前，
	 * 将此BeanPostProcessor应用于给定的新bean实例。
	 * bean已经填充了属性值。返回的bean实例可以是原始实例的包装器。 默认实现按原样返回给定的bean。
	 * <p>
	 * <p>
	 * Apply this {@code BeanPostProcessor} to the given new bean instance <i>before</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>The default implementation returns the given {@code bean} as-is.
	 *
	 * @param bean     the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * 在任何bean初始化回调（如InitializingBean的afterPropertiesSet或自定义init方法）之后，
	 * 将此BeanPostProcessor应用于给定的新bean实例。
	 * bean已经填充了属性值。返回的bean实例可以是原始实例的包装器。
	 * 对于FactoryBean，将为FactoryBean实例和由FactoryBean创建的对象（从Spring 2.0开始）调用此回调。
	 * BeanPostProcessor可以通过FactoryBean检查的相应bean实例来决定是应用于FactoryBean还是创建的对象，
	 * 或者两者都应用。
	 * 此回调也将在实例化操作触发短路后调用实例化前的
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation}方法，
	 * 与所有其他BeanPostProcessor回调相反。
	 * 默认实现按原样返回给定的bean。
	 * <p>
	 * Apply this {@code BeanPostProcessor} to the given new bean instance <i>after</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>In case of a FactoryBean, this callback will be invoked for both the FactoryBean
	 * instance and the objects created by the FactoryBean (as of Spring 2.0). The
	 * post-processor can decide whether to apply to either the FactoryBean or created
	 * objects or both through corresponding {@code bean instanceof FactoryBean} checks.
	 * <p>This callback will also be invoked after a short-circuiting triggered by a
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} method,
	 * in contrast to all other {@code BeanPostProcessor} callbacks.
	 * <p>The default implementation returns the given {@code bean} as-is.
	 *
	 * @param bean     the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
