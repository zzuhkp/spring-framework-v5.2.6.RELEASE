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

package org.springframework.core;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.core.SerializableTypeWrapper.FieldTypeProvider;
import org.springframework.core.SerializableTypeWrapper.MethodParameterTypeProvider;
import org.springframework.core.SerializableTypeWrapper.TypeProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Encapsulates a Java {@link java.lang.reflect.Type}, providing access to
 * {@link #getSuperType() supertypes}, {@link #getInterfaces() interfaces}, and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link java.lang.Class}.
 *
 * <p>{@code ResolvableTypes} may be obtained from {@link #forField(Field) fields},
 * {@link #forMethodParameter(Method, int) method parameters},
 * {@link #forMethodReturnType(Method) method returns} or
 * {@link #forClass(Class) classes}. Most methods on this class will themselves return
 * {@link ResolvableType ResolvableTypes}, allowing easy navigation. For example:
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #forField(Field)
 * @see #forMethodParameter(Method, int)
 * @see #forMethodReturnType(Method)
 * @see #forConstructorParameter(Constructor, int)
 * @see #forClass(Class)
 * @see #forType(Type)
 * @see #forInstance(Object)
 * @see ResolvableTypeProvider
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ResolvableType implements Serializable {

	/**
	 * {@code ResolvableType} returned when no value is available. {@code NONE} is used
	 * in preference to {@code null} so that multiple method calls can be safely chained.
	 */
	public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);

	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

	private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
			new ConcurrentReferenceHashMap<>(256);


	/**
	 * 正在管理的Java基础类型
	 * The underlying Java type being managed.
	 */
	private final Type type;

	/**
	 * 可选的类型提供者
	 * Optional provider for the type.
	 */
	@Nullable
	private final TypeProvider typeProvider;

	/**
	 * 类型变量解析器
	 * The {@code VariableResolver} to use or {@code null} if no resolver is available.
	 */
	@Nullable
	private final VariableResolver variableResolver;

	/**
	 * 数组的元素类型
	 * The component type for an array or {@code null} if the type should be deduced.
	 */
	@Nullable
	private final ResolvableType componentType;

	/**
	 * 缓存的哈希码
	 */
	@Nullable
	private final Integer hash;

	/**
	 * 解析后的类型
	 */
	@Nullable
	private Class<?> resolved;

	/**
	 * 父类
	 */
	@Nullable
	private volatile ResolvableType superType;

	/**
	 * 实现的接口
	 */
	@Nullable
	private volatile ResolvableType[] interfaces;

	/**
	 * 泛型参数
	 */
	@Nullable
	private volatile ResolvableType[] generics;


	/**
	 * Private constructor used to create a new {@link ResolvableType} for cache key purposes,
	 * with no upfront resolution.
	 */
	private ResolvableType(
			Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = null;
		this.hash = calculateHashCode();
		this.resolved = null;
	}

	/**
	 * Private constructor used to create a new {@link ResolvableType} for cache value purposes,
	 * with upfront resolution and a pre-calculated hash.
	 *
	 * @since 4.2
	 */
	private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
			@Nullable VariableResolver variableResolver, @Nullable Integer hash) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = null;
		this.hash = hash;
		this.resolved = resolveClass();
	}

	/**
	 * Private constructor used to create a new {@link ResolvableType} for uncached purposes,
	 * with upfront resolution but lazily calculated hash.
	 */
	private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
			@Nullable VariableResolver variableResolver, @Nullable ResolvableType componentType) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = componentType;
		this.hash = null;
		this.resolved = resolveClass();
	}

	/**
	 * Private constructor used to create a new {@link ResolvableType} on a {@link Class} basis.
	 * Avoids all {@code instanceof} checks in order to create a straight {@link Class} wrapper.
	 *
	 * @since 4.2
	 */
	private ResolvableType(@Nullable Class<?> clazz) {
		this.resolved = (clazz != null ? clazz : Object.class);
		this.type = this.resolved;
		this.typeProvider = null;
		this.variableResolver = null;
		this.componentType = null;
		this.hash = null;
	}


	/**
	 * 返回被管理的Java Type
	 * Return the underling Java {@link Type} being managed.
	 */
	public Type getType() {
		return SerializableTypeWrapper.unwrap(this.type);
	}

	/**
	 * 获取被管理的类型的原始类型
	 * Return the underlying Java {@link Class} being managed, if available;
	 * otherwise {@code null}.
	 */
	@Nullable
	public Class<?> getRawClass() {
		if (this.type == this.resolved) {
			return this.resolved;
		}
		Type rawType = this.type;
		if (rawType instanceof ParameterizedType) {
			rawType = ((ParameterizedType) rawType).getRawType();
		}
		return (rawType instanceof Class ? (Class<?>) rawType : null);
	}

	/**
	 * 返回ResolvableType的源
	 * Return the underlying source of the resolvable type. Will return a {@link Field},
	 * {@link MethodParameter} or {@link Type} depending on how the {@link ResolvableType}
	 * was constructed. With the exception of the {@link #NONE} constant, this method will
	 * never return {@code null}. This method is primarily to provide access to additional
	 * type information or meta-data that alternative JVM languages may provide.
	 */
	public Object getSource() {
		Object source = (this.typeProvider != null ? this.typeProvider.getSource() : null);
		return (source != null ? source : this.type);
	}

	/**
	 * 返回解析后的Class,如果解析失败则返回Object class
	 * Return this type as a resolved {@code Class}, falling back to
	 * {@link java.lang.Object} if no specific class can be resolved.
	 *
	 * @return the resolved {@link Class} or the {@code Object} fallback
	 * @see #getRawClass()
	 * @see #resolve(Class)
	 * @since 5.1
	 */
	public Class<?> toClass() {
		return resolve(Object.class);
	}

	/**
	 * 确定给定的对象是否为ResolvableType的实例
	 * Determine whether the given object is an instance of this {@code ResolvableType}.
	 *
	 * @param obj the object to check
	 * @see #isAssignableFrom(Class)
	 * @since 4.2
	 */
	public boolean isInstance(@Nullable Object obj) {
		return (obj != null && isAssignableFrom(obj.getClass()));
	}

	/**
	 * 确定给定的其他类型是否为当前ResolvableType的子类
	 * Determine whether this {@code ResolvableType} is assignable from the
	 * specified other type.
	 *
	 * @param other the type to be checked against (as a {@code Class})
	 * @see #isAssignableFrom(ResolvableType)
	 * @since 4.2
	 */
	public boolean isAssignableFrom(Class<?> other) {
		return isAssignableFrom(forClass(other), null);
	}

	/**
	 * 当前类型是否和other为相同的类型，或为other的父类,或实现的接口
	 * Determine whether this {@code ResolvableType} is assignable from the
	 * specified other type.
	 * <p>Attempts to follow the same rules as the Java compiler, considering
	 * whether both the {@link #resolve() resolved} {@code Class} is
	 * {@link Class#isAssignableFrom(Class) assignable from} the given type
	 * as well as whether all {@link #getGenerics() generics} are assignable.
	 *
	 * @param other the type to be checked against (as a {@code ResolvableType})
	 * @return {@code true} if the specified other type can be assigned to this
	 * {@code ResolvableType}; {@code false} otherwise
	 */
	public boolean isAssignableFrom(ResolvableType other) {
		return isAssignableFrom(other, null);
	}

	/**
	 * TODO 不完全理解
	 *
	 * @param other
	 * @param matchedBefore 上次匹配的类型
	 * @return
	 */
	private boolean isAssignableFrom(ResolvableType other, @Nullable Map<Type, Type> matchedBefore) {
		Assert.notNull(other, "ResolvableType must not be null");

		// 不能解析类型，返回false
		// If we cannot resolve types, we are not assignable
		if (this == NONE || other == NONE) {
			return false;
		}

		//如果是数组，则使用数组的元素类型进行判断
		// Deal with array by delegating to the component type
		if (isArray()) {
			return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
		}
		// 上次已经进行过匹配，直接返回
		if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
			return true;
		}

		//处理通配符类型
		// Deal with wildcard bounds
		WildcardBounds ourBounds = WildcardBounds.get(this);
		WildcardBounds typeBounds = WildcardBounds.get(other);

		// In the form X is assignable to <? extends Number>
		if (typeBounds != null) {
			//比较界限类型和边界类
			return (ourBounds != null && ourBounds.isSameKind(typeBounds) &&
					ourBounds.isAssignableFrom(typeBounds.getBounds()));
		}

		// In the form <? extends Number> is assignable to X...
		if (ourBounds != null) {
			//把other当做通配符类型的边界类比较
			return ourBounds.isAssignableFrom(other);
		}

		// Main assignability check about to follow
		//是否完全匹配，例如List<String>和List<Integer>不同
		boolean exactMatch = (matchedBefore != null);  // We're checking nested generic variables now...
		//是否比较泛型参数
		boolean checkGenerics = true;
		Class<?> ourResolved = null;
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			// Try default variable resolution
			if (this.variableResolver != null) {
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				if (resolved != null) {
					ourResolved = resolved.resolve();
				}
			}
			if (ourResolved == null) {
				// Try variable resolution against target type
				if (other.variableResolver != null) {
					ResolvableType resolved = other.variableResolver.resolveVariable(variable);
					if (resolved != null) {
						ourResolved = resolved.resolve();
						//认为泛型的类型相同，不再比较
						checkGenerics = false;
					}
				}
			}
			if (ourResolved == null) {
				// Unresolved type variable, potentially nested -> never insist on exact match
				exactMatch = false;
			}
		}
		if (ourResolved == null) {
			ourResolved = resolve(Object.class);
		}
		Class<?> otherResolved = other.toClass();

		// We need an exact type match for generics
		// List<CharSequence> is not assignable from List<String>
		if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved)) {
			return false;
		}

		if (checkGenerics) {
			// Recursively check each generic
			ResolvableType[] ourGenerics = getGenerics();
			ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
			if (ourGenerics.length != typeGenerics.length) {
				return false;
			}
			if (matchedBefore == null) {
				matchedBefore = new IdentityHashMap<>(1);
			}
			matchedBefore.put(this.type, other.type);
			for (int i = 0; i < ourGenerics.length; i++) {
				if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 如果此类型解析为表示数组的类，则返回true。
	 * Return {@code true} if this type resolves to a Class that represents an array.
	 *
	 * @see #getComponentType()
	 */
	public boolean isArray() {
		if (this == NONE) {
			return false;
		}
		return ((this.type instanceof Class && ((Class<?>) this.type).isArray()) ||
				this.type instanceof GenericArrayType || resolveType().isArray());
	}

	/**
	 * 返回表示数组元素类型的ResolvableType，如果当前类型不是数组，则返回NONE
	 * Return the ResolvableType representing the component type of the array or
	 * {@link #NONE} if this type does not represent an array.
	 *
	 * @see #isArray()
	 */
	public ResolvableType getComponentType() {
		if (this == NONE) {
			return NONE;
		}
		if (this.componentType != null) {
			return this.componentType;
		}
		if (this.type instanceof Class) {
			Class<?> componentType = ((Class<?>) this.type).getComponentType();
			return forType(componentType, this.variableResolver);
		}
		if (this.type instanceof GenericArrayType) {
			return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
		}
		return resolveType().getComponentType();
	}

	/**
	 * 将此类型转换为可解析的Collection类型，如果此类型没有继承或实现Collection返回NONE
	 * Convenience method to return this type as a resolvable {@link Collection} type.
	 * Returns {@link #NONE} if this type does not implement or extend
	 * {@link Collection}.
	 *
	 * @see #as(Class)
	 * @see #asMap()
	 */
	public ResolvableType asCollection() {
		return as(Collection.class);
	}

	/**
	 * 将此类型转换为可解析的Map类型，如果此类型没有继承或实现Map返回NONE
	 * Convenience method to return this type as a resolvable {@link Map} type.
	 * Returns {@link #NONE} if this type does not implement or extend
	 * {@link Map}.
	 *
	 * @see #as(Class)
	 * @see #asCollection()
	 */
	public ResolvableType asMap() {
		return as(Map.class);
	}

	/**
	 * 将此类型解析为指定class的ResolvableType。
	 * 搜索超类型和接口层次结构以查找匹配项，如果此类型未实现或扩展指定的类，则返回NONE。
	 * Return this type as a {@link ResolvableType} of the specified class. Searches
	 * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
	 * hierarchies to find a match, returning {@link #NONE} if this type does not
	 * implement or extend the specified class.
	 *
	 * @param type the required type (typically narrowed)
	 * @return a {@link ResolvableType} representing this object as the specified
	 * type, or {@link #NONE} if not resolvable as that type
	 * @see #asCollection()
	 * @see #asMap()
	 * @see #getSuperType()
	 * @see #getInterfaces()
	 */
	public ResolvableType as(Class<?> type) {
		if (this == NONE) {
			return NONE;
		}
		Class<?> resolved = resolve();
		if (resolved == null || resolved == type) {
			return this;
		}
		for (ResolvableType interfaceType : getInterfaces()) {
			ResolvableType interfaceAsType = interfaceType.as(type);
			if (interfaceAsType != NONE) {
				return interfaceAsType;
			}
		}
		return getSuperType().as(type);
	}

	/**
	 * 返回表示此类型的直接父类型的可解析类型。如果没有可用的父类型，则此方法返回NONE。
	 * Return a {@link ResolvableType} representing the direct supertype of this type.
	 * If no supertype is available this method returns {@link #NONE}.
	 * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
	 *
	 * @see #getInterfaces()
	 */
	public ResolvableType getSuperType() {
		Class<?> resolved = resolve();
		if (resolved == null || resolved.getGenericSuperclass() == null) {
			return NONE;
		}
		ResolvableType superType = this.superType;
		if (superType == null) {
			superType = forType(resolved.getGenericSuperclass(), this);
			this.superType = superType;
		}
		return superType;
	}

	/**
	 * 返回表示此类型实现的直接接口的ResolvableType数组。如果此类型未实现任何接口，则返回空数组。
	 * Return a {@link ResolvableType} array representing the direct interfaces
	 * implemented by this type. If this type does not implement any interfaces an
	 * empty array is returned.
	 * <p>Note: The resulting {@link ResolvableType} instances may not be {@link Serializable}.
	 *
	 * @see #getSuperType()
	 */
	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null) {
			return EMPTY_TYPES_ARRAY;
		}
		ResolvableType[] interfaces = this.interfaces;
		if (interfaces == null) {
			Type[] genericIfcs = resolved.getGenericInterfaces();
			interfaces = new ResolvableType[genericIfcs.length];
			for (int i = 0; i < genericIfcs.length; i++) {
				interfaces[i] = forType(genericIfcs[i], this);
			}
			this.interfaces = interfaces;
		}
		return interfaces;
	}

	/**
	 * 如果此类型包含泛型参数，则返回true。
	 * Return {@code true} if this type contains generic parameters.
	 *
	 * @see #getGeneric(int...)
	 * @see #getGenerics()
	 */
	public boolean hasGenerics() {
		return (getGenerics().length > 0);
	}

	/**
	 * 如果此类型仅包含不可解析的泛型，即不替换其任何声明的类型变量，则返回true。
	 * Return {@code true} if this type contains unresolvable generics only,
	 * that is, no substitute for any of its declared type variables.
	 */
	boolean isEntirelyUnresolvable() {
		if (this == NONE) {
			return false;
		}
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 确定基础类型是否有任何不可解析的泛型：
	 * 通过类型本身的不可解析类型变量，或通过以原始方式实现泛型接口，即不替换该接口的类型变量。
	 * 结果只有在这两种情况下才是正确的。
	 * <p>
	 * Determine whether the underlying type has any unresolvable generics:
	 * either through an unresolvable type variable on the type itself
	 * or through implementing a generic interface in a raw fashion,
	 * i.e. without substituting that interface's type variables.
	 * The result will be {@code true} only in those two scenarios.
	 */
	public boolean hasUnresolvableGenerics() {
		if (this == NONE) {
			return false;
		}
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds()) {
				return true;
			}
		}
		Class<?> resolved = resolve();
		if (resolved != null) {
			for (Type genericInterface : resolved.getGenericInterfaces()) {
				if (genericInterface instanceof Class) {
					if (forClass((Class<?>) genericInterface).hasGenerics()) {
						return true;
					}
				}
			}
			return getSuperType().hasUnresolvableGenerics();
		}
		return false;
	}

	/**
	 * 确定基础类型是否是无法通过关联的变量解析器解析的类型变量。
	 * Determine whether the underlying type is a type variable that
	 * cannot be resolved through the associated variable resolver.
	 */
	private boolean isUnresolvableTypeVariable() {
		if (this.type instanceof TypeVariable) {
			if (this.variableResolver == null) {
				//不存在variableResolver即为不可解析的类型变量
				return true;
			}
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			ResolvableType resolved = this.variableResolver.resolveVariable(variable);
			if (resolved == null || resolved.isUnresolvableTypeVariable()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否为没有边界的通配符类型
	 * Determine whether the underlying type represents a wildcard
	 * without specific bounds (i.e., equal to {@code ? extends Object}).
	 */
	private boolean isWildcardWithoutBounds() {
		if (this.type instanceof WildcardType) {
			WildcardType wt = (WildcardType) this.type;
			if (wt.getLowerBounds().length == 0) {
				Type[] upperBounds = wt.getUpperBounds();
				if (upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 返回指定嵌套级别的可解析类型。
	 * Return a {@link ResolvableType} for the specified nesting level.
	 * See {@link #getNested(int, Map)} for details.
	 *
	 * @param nestingLevel the nesting level
	 * @return the {@link ResolvableType} type, or {@code #NONE}
	 */
	public ResolvableType getNested(int nestingLevel) {
		return getNested(nestingLevel, null);
	}

	/**
	 * 返回指定嵌套级别的可解析类型。
	 * 嵌套级别是指应该返回的特定泛型参数。
	 * 嵌套级别1表示此类型；2表示第一个嵌套泛型；3表示第二个；依此类推。
	 * 例如，给定的{@code List<Set<Integer>>} level 1表示{@code List}，level 2表示{@code Set}，level 3表示{@code Integer}。
	 * typeIndexesPerLevel映射可用于引用给定级别的特定泛型。
	 * 例如，索引0将引用映射键；而1将引用该值。
	 * 如果映射不包含特定级别的值，则将使用最后一个泛型（例如映射值）。
	 * 嵌套级别也可以应用于数组类型；例如给定的String[]，嵌套级别2表示String。
	 * 如果类型不包含泛型，则将考虑超类型层次结构。
	 * <p>
	 * Return a {@link ResolvableType} for the specified nesting level.
	 * <p>The nesting level refers to the specific generic parameter that should be returned.
	 * A nesting level of 1 indicates this type; 2 indicates the first nested generic;
	 * 3 the second; and so on. For example, given {@code List<Set<Integer>>} level 1 refers
	 * to the {@code List}, level 2 the {@code Set}, and level 3 the {@code Integer}.
	 * <p>The {@code typeIndexesPerLevel} map can be used to reference a specific generic
	 * for the given level. For example, an index of 0 would refer to a {@code Map} key;
	 * whereas, 1 would refer to the value. If the map does not contain a value for a
	 * specific level the last generic will be used (e.g. a {@code Map} value).
	 * <p>Nesting levels may also apply to array types; for example given
	 * {@code String[]}, a nesting level of 2 refers to {@code String}.
	 * <p>If a type does not {@link #hasGenerics() contain} generics the
	 * {@link #getSuperType() supertype} hierarchy will be considered.
	 *
	 * @param nestingLevel        所需的嵌套接，1表示当前类型，2 表示第一个嵌套的泛型，3表示第二个，以此类推
	 *                            the required nesting level, indexed from 1 for the
	 *                            current type, 2 for the first nested generic, 3 for the second and so on
	 * @param typeIndexesPerLevel 包含给定嵌套级别泛型索引的map
	 *                            a map containing the generic index for a given
	 *                            nesting level (may be {@code null})
	 * @return a {@link ResolvableType} for the nested level, or {@link #NONE}
	 */
	public ResolvableType getNested(int nestingLevel, @Nullable Map<Integer, Integer> typeIndexesPerLevel) {
		ResolvableType result = this;
		for (int i = 2; i <= nestingLevel; i++) {
			if (result.isArray()) {
				result = result.getComponentType();
			} else {
				// Handle derived types
				while (result != ResolvableType.NONE && !result.hasGenerics()) {
					result = result.getSuperType();
				}
				Integer index = (typeIndexesPerLevel != null ? typeIndexesPerLevel.get(i) : null);
				index = (index == null ? result.getGenerics().length - 1 : index);
				result = result.getGeneric(index);
			}
		}
		return result;
	}

	/**
	 * 返回表示给定索引的泛型参数的ResolvableType。索引从0开始
	 * 对于{@code Map<Integer, List<String>>},{@code getGeneric(0)}将访问Integer。
	 * 通过指定多个索引访问嵌套的泛型，例如{@code getGeneric(1, 0)}将访问嵌套的List中的String。
	 * 如果没有指定索引，将返回第一个泛型
	 * Return a {@link ResolvableType} representing the generic parameter for the
	 * given indexes. Indexes are zero based; for example given the type
	 * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
	 * {@code Integer}. Nested generics can be accessed by specifying multiple indexes;
	 * for example {@code getGeneric(1, 0)} will access the {@code String} from the
	 * nested {@code List}. For convenience, if no indexes are specified the first
	 * generic is returned.
	 * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
	 *
	 * @param indexes the indexes that refer to the generic parameter
	 *                (may be omitted to return the first generic)
	 * @return a {@link ResolvableType} for the specified generic, or {@link #NONE}
	 * @see #hasGenerics()
	 * @see #getGenerics()
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public ResolvableType getGeneric(@Nullable int... indexes) {
		ResolvableType[] generics = getGenerics();
		if (indexes == null || indexes.length == 0) {
			return (generics.length == 0 ? NONE : generics[0]);
		}
		ResolvableType generic = this;
		for (int index : indexes) {
			//通过迭代支持嵌套的泛型参数
			generics = generic.getGenerics();
			if (index < 0 || index >= generics.length) {
				return NONE;
			}
			generic = generics[index];
		}
		return generic;
	}

	/**
	 * 返回泛型参数数组
	 * Return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters of
	 * this type. If no generics are available an empty array is returned. If you need to
	 * access a specific generic consider using the {@link #getGeneric(int...)} method as
	 * it allows access to nested generics and protects against
	 * {@code IndexOutOfBoundsExceptions}.
	 *
	 * @return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters
	 * (never {@code null})
	 * @see #hasGenerics()
	 * @see #getGeneric(int...)
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public ResolvableType[] getGenerics() {
		if (this == NONE) {
			return EMPTY_TYPES_ARRAY;
		}
		//优先使用之前解析的值
		ResolvableType[] generics = this.generics;
		if (generics == null) {
			if (this.type instanceof Class) {
				//处理Class
				Type[] typeParams = ((Class<?>) this.type).getTypeParameters();
				generics = new ResolvableType[typeParams.length];
				for (int i = 0; i < generics.length; i++) {
					generics[i] = ResolvableType.forType(typeParams[i], this);
				}
			} else if (this.type instanceof ParameterizedType) {
				//处理参数化类型
				Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
				generics = new ResolvableType[actualTypeArguments.length];
				for (int i = 0; i < actualTypeArguments.length; i++) {
					generics[i] = forType(actualTypeArguments[i], this.variableResolver);
				}
			} else {
				//其他情况，如通配符类型、类型变量，先进行解析，然后获取解析后类型的泛型参数
				generics = resolveType().getGenerics();
			}
			this.generics = generics;
		}
		return generics;
	}

	/**
	 * 获取和解析泛型参数的简便方法
	 * Convenience method that will {@link #getGenerics() get} and
	 * {@link #resolve() resolve} generic parameters.
	 *
	 * @return an array of resolved generic parameters (the resulting array
	 * will never be {@code null}, but it may contain {@code null} elements})
	 * @see #getGenerics()
	 * @see #resolve()
	 */
	public Class<?>[] resolveGenerics() {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve();
		}
		return resolvedGenerics;
	}

	/**
	 * 获取并解析泛型参数的方法，如果解析失败，则使用给定的fallback
	 * Convenience method that will {@link #getGenerics() get} and {@link #resolve()
	 * resolve} generic parameters, using the specified {@code fallback} if any type
	 * cannot be resolved.
	 *
	 * @param fallback the fallback class to use if resolution fails
	 * @return an array of resolved generic parameters
	 * @see #getGenerics()
	 * @see #resolve()
	 */
	public Class<?>[] resolveGenerics(Class<?> fallback) {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve(fallback);
		}
		return resolvedGenerics;
	}

	/**
	 * 获取并解析泛型参数的简便犯法
	 * Convenience method that will {@link #getGeneric(int...) get} and
	 * {@link #resolve() resolve} a specific generic parameters.
	 *
	 * @param indexes 泛型参数的索引，如果省略将返回第一个泛型参数
	 *                the indexes that refer to the generic parameter
	 *                (may be omitted to return the first generic)
	 * @return a resolved {@link Class} or {@code null}
	 * @see #getGeneric(int...)
	 * @see #resolve()
	 */
	@Nullable
	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	/**
	 * 将当前类型解析为Class，如果无法解析该类型，则返回null。
	 * 如果直接解析失败，此方法将考虑类型变量和通配符类型的边界；但是Class将被忽略。
	 * Resolve this type to a {@link java.lang.Class}, returning {@code null}
	 * if the type cannot be resolved. This method will consider bounds of
	 * {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
	 * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
	 * <p>If this method returns a non-null {@code Class} and {@link #hasGenerics()}
	 * returns {@code false}, the given type effectively wraps a plain {@code Class},
	 * allowing for plain {@code Class} processing if desirable.
	 *
	 * @return the resolved {@link Class}, or {@code null} if not resolvable
	 * @see #resolve(Class)
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	@Nullable
	public Class<?> resolve() {
		return this.resolved;
	}

	/**
	 * 将当前类型解析为Class，如果无法解析当前类型，则返回指定的fallback。
	 * 如果直接解析失败，此方法将考虑类型变量和通配符类型的边界；但是对象类将被忽略。
	 * Resolve this type to a {@link java.lang.Class}, returning the specified
	 * {@code fallback} if the type cannot be resolved. This method will consider bounds
	 * of {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
	 * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
	 *
	 * @param fallback the fallback class to use if resolution fails
	 * @return the resolved {@link Class} or the {@code fallback}
	 * @see #resolve()
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public Class<?> resolve(Class<?> fallback) {
		return (this.resolved != null ? this.resolved : fallback);
	}

	/**
	 * 解析管理的type为Class
	 *
	 * @return
	 */
	@Nullable
	private Class<?> resolveClass() {
		if (this.type == EmptyType.INSTANCE) {
			return null;
		}
		//原始类型直接返回
		if (this.type instanceof Class) {
			return (Class<?>) this.type;
		}
		//泛型数组类型解析为元素类型对应的数组类型
		if (this.type instanceof GenericArrayType) {
			Class<?> resolvedComponent = getComponentType().resolve();
			return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
		}
		//其他情况，如参数化类型、通配符类型、类型变量等
		return resolveType().resolve();
	}

	/**
	 * 解析当前对象管理的type
	 * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
	 * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
	 * as it cannot be serialized.
	 */
	ResolvableType resolveType() {
		if (this.type instanceof ParameterizedType) {
			//如果管理的类型是参数化类型，返回参数化类型对应的原始类型
			return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
		}
		if (this.type instanceof WildcardType) {
			//如果管理的类型是通配符类型，返回通配符类型的上界或下界
			Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
			if (resolved == null) {
				resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
			}
			return forType(resolved, this.variableResolver);
		}
		if (this.type instanceof TypeVariable) {
			//如果管理的类型器是类型变量，返回变量解析器解析的结果或类型变量的边界类
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			// Try default variable resolution
			if (this.variableResolver != null) {
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				if (resolved != null) {
					return resolved;
				}
			}
			// Fallback to bounds
			return forType(resolveBounds(variable.getBounds()), this.variableResolver);
		}
		return NONE;
	}

	/**
	 * 解析通配符类型的边界
	 *
	 * @param bounds
	 * @return
	 */
	@Nullable
	private Type resolveBounds(Type[] bounds) {
		if (bounds.length == 0 || bounds[0] == Object.class) {
			return null;
		}
		return bounds[0];
	}

	/**
	 * 将类型变量真实的类型解析为ResolvableType
	 *
	 * @param variable
	 * @return
	 */
	@Nullable
	private ResolvableType resolveVariable(TypeVariable<?> variable) {
		if (this.type instanceof TypeVariable) {
			//如果当前管理的type是类型变量，则先解析为类型变量对应的边界类，再进行解析
			return resolveType().resolveVariable(variable);
		}
		if (this.type instanceof ParameterizedType) {
			//如果当前管理的type是参数化类型
			ParameterizedType parameterizedType = (ParameterizedType) this.type;
			Class<?> resolved = resolve();
			if (resolved == null) {
				return null;
			}
			TypeVariable<?>[] variables = resolved.getTypeParameters();
			for (int i = 0; i < variables.length; i++) {
				if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
					//参数化类型的类型参数名称和给定的类型变量名称一致，返回该参数化类型
					Type actualType = parameterizedType.getActualTypeArguments()[i];
					return forType(actualType, this.variableResolver);
				}
			}
			Type ownerType = parameterizedType.getOwnerType();
			if (ownerType != null) {
				return forType(ownerType, this.variableResolver).resolveVariable(variable);
			}
		}
		if (this.type instanceof WildcardType) {
			//通配符类型先解析为通配符类型的边界类型然后进行解析
			ResolvableType resolved = resolveType().resolveVariable(variable);
			if (resolved != null) {
				return resolved;
			}
		}
		if (this.variableResolver != null) {
			//最后再通过变量解析器解析
			return this.variableResolver.resolveVariable(variable);
		}
		return null;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResolvableType)) {
			return false;
		}

		ResolvableType otherType = (ResolvableType) other;
		if (!ObjectUtils.nullSafeEquals(this.type, otherType.type)) {
			return false;
		}
		if (this.typeProvider != otherType.typeProvider &&
				(this.typeProvider == null || otherType.typeProvider == null ||
						!ObjectUtils.nullSafeEquals(this.typeProvider.getType(), otherType.typeProvider.getType()))) {
			return false;
		}
		if (this.variableResolver != otherType.variableResolver &&
				(this.variableResolver == null || otherType.variableResolver == null ||
						!ObjectUtils.nullSafeEquals(this.variableResolver.getSource(),
								otherType.variableResolver.getSource()))) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(this.componentType, otherType.componentType)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return (this.hash != null ? this.hash : calculateHashCode());
	}

	/**
	 * 计算哈希码
	 *
	 * @return
	 */
	private int calculateHashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.type);
		if (this.typeProvider != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.typeProvider.getType());
		}
		if (this.variableResolver != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
		}
		if (this.componentType != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
		}
		return hashCode;
	}

	/**
	 * 将ResolvableType适配为VariableResolver
	 * Adapts this {@link ResolvableType} to a {@link VariableResolver}.
	 */
	@Nullable
	VariableResolver asVariableResolver() {
		if (this == NONE) {
			return null;
		}
		return new DefaultVariableResolver(this);
	}

	/**
	 * Custom serialization support for {@link #NONE}.
	 */
	private Object readResolve() {
		return (this.type == EmptyType.INSTANCE ? NONE : this);
	}

	/**
	 * Return a String representation of this type in its fully resolved form
	 * (including any generic parameters).
	 */
	@Override
	public String toString() {
		if (isArray()) {
			return getComponentType() + "[]";
		}
		if (this.resolved == null) {
			return "?";
		}
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
				// Don't bother with variable boundaries for toString()...
				// Can cause infinite recursions in case of self-references
				return "?";
			}
		}
		if (hasGenerics()) {
			return this.resolved.getName() + '<' + StringUtils.arrayToDelimitedString(getGenerics(), ", ") + '>';
		}
		return this.resolved.getName();
	}

	// Factory methods

	/**
	 * 使用完整的泛型类型信息进行可分配性检查，返回指定类的可解析类型。
	 * 例如：类的可解析类型(MyArrayList.class类).
	 * <p>
	 * Return a {@link ResolvableType} for the specified {@link Class},
	 * using the full generic type information for assignability checks.
	 * For example: {@code ResolvableType.forClass(MyArrayList.class)}.
	 *
	 * @param clazz the class to introspect ({@code null} is semantically
	 *              equivalent to {@code Object.class} for typical use cases here)
	 * @return a {@link ResolvableType} for the specified class
	 * @see #forClass(Class, Class)
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClass(@Nullable Class<?> clazz) {
		return new ResolvableType(clazz);
	}

	/**
	 * 返回指定类的可解析类型，仅对原始类执行可分配性检查
	 * Return a {@link ResolvableType} for the specified {@link Class},
	 * doing assignability checks against the raw class only (analogous to
	 * {@link Class#isAssignableFrom}, which this serves as a wrapper for.
	 * For example: {@code ResolvableType.forRawClass(List.class)}.
	 *
	 * @param clazz the class to introspect ({@code null} is semantically
	 *              equivalent to {@code Object.class} for typical use cases here)
	 * @return a {@link ResolvableType} for the specified class
	 * @see #forClass(Class)
	 * @see #getRawClass()
	 * @since 4.2
	 */
	public static ResolvableType forRawClass(@Nullable Class<?> clazz) {
		return new ResolvableType(clazz) {
			@Override
			public ResolvableType[] getGenerics() {
				return EMPTY_TYPES_ARRAY;
			}

			@Override
			public boolean isAssignableFrom(Class<?> other) {
				return (clazz == null || ClassUtils.isAssignable(clazz, other));
			}

			@Override
			public boolean isAssignableFrom(ResolvableType other) {
				Class<?> otherClass = other.resolve();
				return (otherClass != null && (clazz == null || ClassUtils.isAssignable(clazz, otherClass)));
			}
		};
	}

	/**
	 * 返回指定类的可解析类型，仅对原始类执行可分配性检查
	 * Return a {@link ResolvableType} for the specified base type
	 * (interface or base class) with a given implementation class.
	 * For example: {@code ResolvableType.forClass(List.class, MyArrayList.class)}.
	 *
	 * @param baseType            the base type (must not be {@code null})
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified base type backed by the
	 * given implementation class
	 * @see #forClass(Class)
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClass(Class<?> baseType, Class<?> implementationClass) {
		Assert.notNull(baseType, "Base type must not be null");
		ResolvableType asType = forType(implementationClass).as(baseType);
		return (asType == NONE ? forType(baseType) : asType);
	}

	/**
	 * 使用预先声明的泛型返回指定类的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
	 *
	 * @param clazz    the class (or interface) to introspect
	 * @param generics the generics of the class
	 * @return a {@link ResolvableType} for the specific class and generics
	 * @see #forClassWithGenerics(Class, ResolvableType...)
	 */
	public static ResolvableType forClassWithGenerics(Class<?> clazz, Class<?>... generics) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(generics, "Generics array must not be null");
		ResolvableType[] resolvableGenerics = new ResolvableType[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvableGenerics[i] = forClass(generics[i]);
		}
		return forClassWithGenerics(clazz, resolvableGenerics);
	}

	/**
	 * 使用预先声明的泛型返回指定类的可解析类型
	 * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
	 *
	 * @param clazz    the class (or interface) to introspect
	 * @param generics the generics of the class
	 * @return a {@link ResolvableType} for the specific class and generics
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClassWithGenerics(Class<?> clazz, ResolvableType... generics) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(generics, "Generics array must not be null");
		TypeVariable<?>[] variables = clazz.getTypeParameters();
		Assert.isTrue(variables.length == generics.length, "Mismatched number of generics specified");

		Type[] arguments = new Type[generics.length];
		for (int i = 0; i < generics.length; i++) {
			ResolvableType generic = generics[i];
			Type argument = (generic != null ? generic.getType() : null);
			arguments[i] = (argument != null && !(argument instanceof TypeVariable) ? argument : variables[i]);
		}

		ParameterizedType syntheticType = new SyntheticParameterizedType(clazz, arguments);
		return forType(syntheticType, new TypeVariablesVariableResolver(variables, generics));
	}

	/**
	 * 返回指定实例的可解析类型。
	 * Return a {@link ResolvableType} for the specified instance. The instance does not
	 * convey generic information but if it implements {@link ResolvableTypeProvider} a
	 * more precise {@link ResolvableType} can be used than the simple one based on
	 * the {@link #forClass(Class) Class instance}.
	 *
	 * @param instance the instance
	 * @return a {@link ResolvableType} for the specified instance
	 * @see ResolvableTypeProvider
	 * @since 4.2
	 */
	public static ResolvableType forInstance(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		if (instance instanceof ResolvableTypeProvider) {
			ResolvableType type = ((ResolvableTypeProvider) instance).getResolvableType();
			if (type != null) {
				return type;
			}
		}
		return ResolvableType.forClass(instance.getClass());
	}

	/**
	 * 返回指定字段的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Field}.
	 *
	 * @param field the source field
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field, Class)
	 */
	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return forType(null, new FieldTypeProvider(field), null);
	}

	/**
	 * 返回具有给定实现的指定字段的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Field} with a given
	 * implementation.
	 * <p>Use this variant when the class that declares the field includes generic
	 * parameter variables that are satisfied by the implementation class.
	 *
	 * @param field               the source field
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
	}

	/**
	 * 返回具有给定实现的指定字段的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Field} with a given
	 * implementation.
	 * <p>Use this variant when the class that declares the field includes generic
	 * parameter variables that are satisfied by the implementation type.
	 *
	 * @param field              the source field
	 * @param implementationType the implementation type
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, @Nullable ResolvableType implementationType) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = (implementationType != null ? implementationType : NONE);
		owner = owner.as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
	}

	/**
	 * 返回具有给定嵌套级别的指定字段的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Field} with the
	 * given nesting level.
	 *
	 * @param field        the source field
	 * @param nestingLevel the nesting level (1 for the outer level; 2 for a nested
	 *                     generic type; etc)
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, int nestingLevel) {
		Assert.notNull(field, "Field must not be null");
		return forType(null, new FieldTypeProvider(field), null).getNested(nestingLevel);
	}

	/**
	 * 返回具有给定实现和给定嵌套级别的指定字段的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Field} with a given
	 * implementation and the given nesting level.
	 * <p>Use this variant when the class that declares the field includes generic
	 * parameter variables that are satisfied by the implementation class.
	 *
	 * @param field               the source field
	 * @param nestingLevel        the nesting level (1 for the outer level; 2 for a nested
	 *                            generic type; etc)
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, int nestingLevel, @Nullable Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver()).getNested(nestingLevel);
	}

	/**
	 * 返回指定构造函数参数的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Constructor} parameter.
	 *
	 * @param constructor    the source constructor (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @return a {@link ResolvableType} for the specified constructor parameter
	 * @see #forConstructorParameter(Constructor, int, Class)
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex) {
		Assert.notNull(constructor, "Constructor must not be null");
		return forMethodParameter(new MethodParameter(constructor, parameterIndex));
	}

	/**
	 * 返回具有给定实现的指定构造函数参数的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Constructor} parameter
	 * with a given implementation. Use this variant when the class that declares the
	 * constructor includes generic parameter variables that are satisfied by the
	 * implementation class.
	 *
	 * @param constructor         the source constructor (must not be {@code null})
	 * @param parameterIndex      the parameter index
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified constructor parameter
	 * @see #forConstructorParameter(Constructor, int)
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex,
			Class<?> implementationClass) {

		Assert.notNull(constructor, "Constructor must not be null");
		MethodParameter methodParameter = new MethodParameter(constructor, parameterIndex, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 返回指定方法返回类型的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Method} return type.
	 *
	 * @param method the source for the method return type
	 * @return a {@link ResolvableType} for the specified method return
	 * @see #forMethodReturnType(Method, Class)
	 */
	public static ResolvableType forMethodReturnType(Method method) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(new MethodParameter(method, -1));
	}

	/**
	 * 返回指定方法返回类型的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Method} return type.
	 * Use this variant when the class that declares the method includes generic
	 * parameter variables that are satisfied by the implementation class.
	 *
	 * @param method              the source for the method return type
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified method return
	 * @see #forMethodReturnType(Method)
	 */
	public static ResolvableType forMethodReturnType(Method method, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		MethodParameter methodParameter = new MethodParameter(method, -1, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 为指定的方法参数返回可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Method} parameter.
	 *
	 * @param method         the source method (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int, Class)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(new MethodParameter(method, parameterIndex));
	}

	/**
	 * 返回具有给定实现的指定方法参数的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Method} parameter with a
	 * given implementation. Use this variant when the class that declares the method
	 * includes generic parameter variables that are satisfied by the implementation class.
	 *
	 * @param method              the source method (must not be {@code null})
	 * @param parameterIndex      the parameter index
	 * @param implementationClass the implementation class
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int, Class)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		MethodParameter methodParameter = new MethodParameter(method, parameterIndex, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 为指定的MethodParameter返回ResolvableType。
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter}.
	 *
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
		return forMethodParameter(methodParameter, (Type) null);
	}

	/**
	 * 返回具有给定实现类型的指定MethodParameter的ResolvableType。
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter} with a
	 * given implementation type. Use this variant when the class that declares the method
	 * includes generic parameter variables that are satisfied by the implementation type.
	 *
	 * @param methodParameter    the source method parameter (must not be {@code null})
	 * @param implementationType the implementation type
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter,
			@Nullable ResolvableType implementationType) {

		Assert.notNull(methodParameter, "MethodParameter must not be null");
		implementationType = (implementationType != null ? implementationType :
				forType(methodParameter.getContainingClass()));
		ResolvableType owner = implementationType.as(methodParameter.getDeclaringClass());
		return forType(null, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
				getNested(methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
	}

	/**
	 * 为指定的MethodParameter返回ResollvableType，用特定的给定类型重写要解析的目标类型。
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter},
	 * overriding the target type to resolve with a specific given type.
	 *
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @param targetType      the type to resolve (a part of the method parameter's type)
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter, @Nullable Type targetType) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return forMethodParameter(methodParameter, targetType, methodParameter.getNestingLevel());
	}

	/**
	 * 在特定的嵌套级别返回指定MethodParameter的ResollvableType，用特定的给定类型重写要解析的目标类型。
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter} at
	 * a specific nesting level, overriding the target type to resolve with a specific
	 * given type.
	 *
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @param targetType      the type to resolve (a part of the method parameter's type)
	 * @param nestingLevel    the nesting level to use
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int)
	 * @since 5.2
	 */
	static ResolvableType forMethodParameter(
			MethodParameter methodParameter, @Nullable Type targetType, int nestingLevel) {

		ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
		return forType(targetType, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
				getNested(nestingLevel, methodParameter.typeIndexesPerLevel);
	}

	/**
	 * 返回可解析类型作为指定组件类型的数组。
	 * Return a {@link ResolvableType} as a array of the specified {@code componentType}.
	 *
	 * @param componentType the component type
	 * @return a {@link ResolvableType} as an array of the specified component type
	 */
	public static ResolvableType forArrayComponent(ResolvableType componentType) {
		Assert.notNull(componentType, "Component type must not be null");
		Class<?> arrayClass = Array.newInstance(componentType.resolve(), 0).getClass();
		return new ResolvableType(arrayClass, null, null, componentType);
	}

	/**
	 * 返回指定类型的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Type}.
	 * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
	 *
	 * @param type the source type (potentially {@code null})
	 * @return a {@link ResolvableType} for the specified {@link Type}
	 * @see #forType(Type, ResolvableType)
	 */
	public static ResolvableType forType(@Nullable Type type) {
		return forType(type, null, null);
	}

	/**
	 * 返回由给定所有者类型支持的指定类型的可解析类型。
	 * Return a {@link ResolvableType} for the specified {@link Type} backed by the given
	 * owner type.
	 * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
	 *
	 * @param type  the source type or {@code null}
	 * @param owner the owner type used to resolve variables
	 * @return a {@link ResolvableType} for the specified {@link Type} and owner
	 * @see #forType(Type)
	 */
	public static ResolvableType forType(@Nullable Type type, @Nullable ResolvableType owner) {
		VariableResolver variableResolver = null;
		if (owner != null) {
			variableResolver = owner.asVariableResolver();
		}
		return forType(type, variableResolver);
	}


	/**
	 * 为指定的ParameterizedTypeReference返回ResolvableType。
	 * Return a {@link ResolvableType} for the specified {@link ParameterizedTypeReference}.
	 * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
	 *
	 * @param typeReference the reference to obtain the source type from
	 * @return a {@link ResolvableType} for the specified {@link ParameterizedTypeReference}
	 * @see #forType(Type)
	 * @since 4.3.12
	 */
	public static ResolvableType forType(ParameterizedTypeReference<?> typeReference) {
		return forType(typeReference.getType(), null, null);
	}

	/**
	 * 返回由给定的可解析类型变量解析器.
	 * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
	 * {@link VariableResolver}.
	 *
	 * @param type             the source type or {@code null}
	 * @param variableResolver the variable resolver or {@code null}
	 * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
	 */
	static ResolvableType forType(@Nullable Type type, @Nullable VariableResolver variableResolver) {
		return forType(type, null, variableResolver);
	}

	/**
	 * 返回由给定的可解析类型变量解析器.
	 * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
	 * {@link VariableResolver}.
	 *
	 * @param type             the source type or {@code null}
	 * @param typeProvider     the type provider or {@code null}
	 * @param variableResolver the variable resolver or {@code null}
	 * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
	 */
	static ResolvableType forType(
			@Nullable Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

		if (type == null && typeProvider != null) {
			type = SerializableTypeWrapper.forTypeProvider(typeProvider);
		}
		if (type == null) {
			return NONE;
		}

		// For simple Class references, build the wrapper right away -
		// no expensive resolution necessary, so not worth caching...
		if (type instanceof Class) {
			return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
		}

		// Purge empty entries on access since we don't have a clean-up thread or the like.
		cache.purgeUnreferencedEntries();

		// Check the cache - we may have a ResolvableType which has been resolved before...
		ResolvableType resultType = new ResolvableType(type, typeProvider, variableResolver);
		ResolvableType cachedType = cache.get(resultType);
		if (cachedType == null) {
			cachedType = new ResolvableType(type, typeProvider, variableResolver, resultType.hash);
			cache.put(cachedType, cachedType);
		}
		resultType.resolved = cachedType.resolved;
		return resultType;
	}

	/**
	 * Clear the internal {@code ResolvableType}/{@code SerializableTypeWrapper} cache.
	 *
	 * @since 4.2
	 */
	public static void clearCache() {
		cache.clear();
		SerializableTypeWrapper.cache.clear();
	}


	/**
	 * 解析TypeVariables的策略接口
	 * Strategy interface used to resolve {@link TypeVariable TypeVariables}.
	 */
	interface VariableResolver extends Serializable {

		/**
		 * 返回解析的来源
		 * Return the source of the resolver (used for hashCode and equals).
		 */
		Object getSource();

		/**
		 * 解析指定的TypeVariable
		 * Resolve the specified variable.
		 *
		 * @param variable the variable to resolve
		 * @return the resolved variable, or {@code null} if not found
		 */
		@Nullable
		ResolvableType resolveVariable(TypeVariable<?> variable);
	}


	@SuppressWarnings("serial")
	private static class DefaultVariableResolver implements VariableResolver {

		private final ResolvableType source;

		DefaultVariableResolver(ResolvableType resolvableType) {
			this.source = resolvableType;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			return this.source.resolveVariable(variable);
		}

		@Override
		public Object getSource() {
			return this.source;
		}
	}


	@SuppressWarnings("serial")
	private static class TypeVariablesVariableResolver implements VariableResolver {

		private final TypeVariable<?>[] variables;

		private final ResolvableType[] generics;

		public TypeVariablesVariableResolver(TypeVariable<?>[] variables, ResolvableType[] generics) {
			this.variables = variables;
			this.generics = generics;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			TypeVariable<?> variableToCompare = SerializableTypeWrapper.unwrap(variable);
			for (int i = 0; i < this.variables.length; i++) {
				TypeVariable<?> resolvedVariable = SerializableTypeWrapper.unwrap(this.variables[i]);
				if (ObjectUtils.nullSafeEquals(resolvedVariable, variableToCompare)) {
					return this.generics[i];
				}
			}
			return null;
		}

		@Override
		public Object getSource() {
			return this.generics;
		}
	}


	private static final class SyntheticParameterizedType implements ParameterizedType, Serializable {

		private final Type rawType;

		private final Type[] typeArguments;

		public SyntheticParameterizedType(Type rawType, Type[] typeArguments) {
			this.rawType = rawType;
			this.typeArguments = typeArguments;
		}

		@Override
		public String getTypeName() {
			String typeName = this.rawType.getTypeName();
			if (this.typeArguments.length > 0) {
				StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
				for (Type argument : this.typeArguments) {
					stringJoiner.add(argument.getTypeName());
				}
				return typeName + stringJoiner;
			}
			return typeName;
		}

		@Override
		@Nullable
		public Type getOwnerType() {
			return null;
		}

		@Override
		public Type getRawType() {
			return this.rawType;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return this.typeArguments;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ParameterizedType)) {
				return false;
			}
			ParameterizedType otherType = (ParameterizedType) other;
			return (otherType.getOwnerType() == null && this.rawType.equals(otherType.getRawType()) &&
					Arrays.equals(this.typeArguments, otherType.getActualTypeArguments()));
		}

		@Override
		public int hashCode() {
			return (this.rawType.hashCode() * 31 + Arrays.hashCode(this.typeArguments));
		}

		@Override
		public String toString() {
			return getTypeName();
		}
	}


	/**
	 * 内部处理通配符边界的帮助类
	 * Internal helper to handle bounds from {@link WildcardType WildcardTypes}.
	 */
	private static class WildcardBounds {

		private final Kind kind;

		private final ResolvableType[] bounds;

		/**
		 * Internal constructor to create a new {@link WildcardBounds} instance.
		 *
		 * @param kind   the kind of bounds
		 * @param bounds the bounds
		 * @see #get(ResolvableType)
		 */
		public WildcardBounds(Kind kind, ResolvableType[] bounds) {
			this.kind = kind;
			this.bounds = bounds;
		}

		/**
		 * Return {@code true} if this bounds is the same kind as the specified bounds.
		 */
		public boolean isSameKind(WildcardBounds bounds) {
			return this.kind == bounds.kind;
		}

		/**
		 * 如果当前边界可以分配给指定的类型则返回true
		 * Return {@code true} if this bounds is assignable to all the specified types.
		 *
		 * @param types the types to test against
		 * @return {@code true} if this bounds is assignable to all types
		 */
		public boolean isAssignableFrom(ResolvableType... types) {
			for (ResolvableType bound : this.bounds) {
				for (ResolvableType type : types) {
					if (!isAssignable(bound, type)) {
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * 根据边界类型比较当前边界和给定的边界类型是否具有继承或实现的关系
		 *
		 * @param source 当前保存的边界
		 * @param from   要比较的边界类型
		 * @return
		 */
		private boolean isAssignable(ResolvableType source, ResolvableType from) {
			return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
		}

		/**
		 * Return the underlying bounds.
		 */
		public ResolvableType[] getBounds() {
			return this.bounds;
		}

		/**
		 * 根据指定类型获取WildcardBounds
		 * Get a {@link WildcardBounds} instance for the specified type, returning
		 * {@code null} if the specified type cannot be resolved to a {@link WildcardType}.
		 *
		 * @param type the source type
		 * @return a {@link WildcardBounds} instance or {@code null}
		 */
		@Nullable
		public static WildcardBounds get(ResolvableType type) {
			ResolvableType resolveToWildcard = type;
			while (!(resolveToWildcard.getType() instanceof WildcardType)) {
				if (resolveToWildcard == NONE) {
					return null;
				}
				resolveToWildcard = resolveToWildcard.resolveType();
			}
			WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
			Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
			Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
			ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
			for (int i = 0; i < bounds.length; i++) {
				resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
			}
			return new WildcardBounds(boundsType, resolvableBounds);
		}

		/**
		 * The various kinds of bounds.
		 */
		enum Kind {UPPER, LOWER}
	}


	/**
	 * Internal {@link Type} used to represent an empty value.
	 */
	@SuppressWarnings("serial")
	static class EmptyType implements Type, Serializable {

		static final Type INSTANCE = new EmptyType();

		Object readResolve() {
			return INSTANCE;
		}
	}

}
