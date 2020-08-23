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
 * 接口由希望在销毁时释放资源的bean实现。
 * BeanFactory将对作用域bean的单个销毁调用destroy方法。
 * ApplicationContext应该在关闭时释放其所有单例，由应用程序生命周期驱动。
 * Spring管理的bean也可以出于同样的目的实现Java的自动关闭接口。
 * 实现接口的另一种方法是指定自定义销毁方法，例如在XML bean定义中。
 * 有关所有bean生命周期方法的列表，请参阅BeanFactory javadocs。
 * <p>
 * Interface to be implemented by beans that want to release resources on destruction.
 * A {@link BeanFactory} will invoke the destroy method on individual destruction of a
 * scoped bean. An {@link org.springframework.context.ApplicationContext} is supposed
 * to dispose all of its singletons on shutdown, driven by the application lifecycle.
 *
 * <p>A Spring-managed bean may also implement Java's {@link AutoCloseable} interface
 * for the same purpose. An alternative to implementing an interface is specifying a
 * custom destroy method, for example in an XML bean definition. For a list of all
 * bean lifecycle methods, see the {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Juergen Hoeller
 * @see InitializingBean
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName()
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
 * @see org.springframework.context.ConfigurableApplicationContext#close()
 * @since 12.08.2003
 */
public interface DisposableBean {

	/**
	 * BeanFactory销毁bean时调用。
	 * <p>
	 * Invoked by the containing {@code BeanFactory} on destruction of a bean.
	 *
	 * @throws Exception in case of shutdown errors. Exceptions will get logged
	 *                   but not rethrown to allow other beans to release their resources as well.
	 */
	void destroy() throws Exception;

}
