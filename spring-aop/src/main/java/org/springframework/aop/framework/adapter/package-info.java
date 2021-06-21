/**
 * 允许 Spring AOP 框架处理任意 Advice 类型的 SPI 包（将 Advice 适配为 MethodInterceptor）
 * <p>
 * SPI package allowing Spring AOP framework to handle arbitrary advice types.
 *
 * <p>Users who want merely to <i>use</i> the Spring AOP framework, rather than extend
 * its capabilities, don't need to concern themselves with this package.
 *
 * <p>You may wish to use these adapters to wrap Spring-specific advices, such as MethodBeforeAdvice,
 * in MethodInterceptor, to allow their use in another AOP framework supporting the AOP Alliance interfaces.
 *
 * <p>These adapters do not depend on any other Spring framework classes to allow such usage.
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.framework.adapter;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
