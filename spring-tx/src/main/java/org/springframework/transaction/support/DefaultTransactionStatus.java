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

package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.util.Assert;

/**
 * TransactionStatus 的默认实现
 * <p>
 * Default implementation of the {@link org.springframework.transaction.TransactionStatus}
 * interface, used by {@link AbstractPlatformTransactionManager}. Based on the concept
 * of an underlying "transaction object".
 *
 * <p>Holds all status information that {@link AbstractPlatformTransactionManager}
 * needs internally, including a generic transaction object determined by the
 * concrete transaction manager implementation.
 *
 * <p>Supports delegating savepoint-related methods to a transaction object
 * that implements the {@link SavepointManager} interface.
 *
 * <p><b>NOTE:</b> This is <i>not</i> intended for use with other PlatformTransactionManager
 * implementations, in particular not for mock transaction managers in testing environments.
 * Use the alternative {@link SimpleTransactionStatus} class or a mock for the plain
 * {@link org.springframework.transaction.TransactionStatus} interface instead.
 *
 * @author Juergen Hoeller
 * @see AbstractPlatformTransactionManager
 * @see org.springframework.transaction.SavepointManager
 * @see #getTransaction
 * @see #createSavepoint
 * @see #rollbackToSavepoint
 * @see #releaseSavepoint
 * @see SimpleTransactionStatus
 * @since 19.01.2004
 */
public class DefaultTransactionStatus extends AbstractTransactionStatus {

	/**
	 * 当前事务对象（可能是已存在的事务对象）
	 *
	 * @see org.springframework.jdbc.datasource.JdbcTransactionObjectSupport
	 */
	@Nullable
	private final Object transaction;

	/**
	 * 当前事务是否是一个新的事务
	 */
	private final boolean newTransaction;

	/**
	 * 是否为当前事务打开新的事务同步
	 */
	private final boolean newSynchronization;

	/**
	 * 当前事务是否标记为只读
	 */
	private final boolean readOnly;

	/**
	 * 是否为当前事务启用调试日志
	 */
	private final boolean debug;

	/**
	 * 为当前事务挂起的资源持有者
	 *
	 * @see org.springframework.jdbc.datasource.ConnectionHolder
	 */
	@Nullable
	private final Object suspendedResources;


	/**
	 * Create a new {@code DefaultTransactionStatus} instance.
	 *
	 * @param transaction        underlying transaction object that can hold state
	 *                           for the internal transaction implementation
	 * @param newTransaction     if the transaction is new, otherwise participating
	 *                           in an existing transaction
	 * @param newSynchronization if a new transaction synchronization has been
	 *                           opened for the given transaction
	 * @param readOnly           whether the transaction is marked as read-only
	 * @param debug              should debug logging be enabled for the handling of this transaction?
	 *                           Caching it in here can prevent repeated calls to ask the logging system whether
	 *                           debug logging should be enabled.
	 * @param suspendedResources a holder for resources that have been suspended
	 *                           for this transaction, if any
	 */
	public DefaultTransactionStatus(
			@Nullable Object transaction, boolean newTransaction, boolean newSynchronization,
			boolean readOnly, boolean debug, @Nullable Object suspendedResources) {

		this.transaction = transaction;
		this.newTransaction = newTransaction;
		this.newSynchronization = newSynchronization;
		this.readOnly = readOnly;
		this.debug = debug;
		this.suspendedResources = suspendedResources;
	}


	/**
	 * 获取底层事务对象
	 * <p>
	 * Return the underlying transaction object.
	 *
	 * @throws IllegalStateException if no transaction is active
	 */
	public Object getTransaction() {
		Assert.state(this.transaction != null, "No transaction active");
		return this.transaction;
	}

	/**
	 * 是否有真实的事务激活
	 * <p>
	 * Return whether there is an actual transaction active.
	 */
	public boolean hasTransaction() {
		return (this.transaction != null);
	}

	@Override
	public boolean isNewTransaction() {
		return (hasTransaction() && this.newTransaction);
	}

	/**
	 * 是否为当前事务打开新的事务同步
	 * <p>
	 * Return if a new transaction synchronization has been opened
	 * for this transaction.
	 */
	public boolean isNewSynchronization() {
		return this.newSynchronization;
	}

	/**
	 * 当前事务是否只读
	 * <p>
	 * Return if this transaction is defined as read-only transaction.
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Return whether the progress of this transaction is debugged. This is used by
	 * {@link AbstractPlatformTransactionManager} as an optimization, to prevent repeated
	 * calls to {@code logger.isDebugEnabled()}. Not really intended for client code.
	 */
	public boolean isDebug() {
		return this.debug;
	}

	/**
	 * 获取挂起的资源持有者
	 * <p>
	 * Return the holder for resources that have been suspended for this transaction,
	 * if any.
	 */
	@Nullable
	public Object getSuspendedResources() {
		return this.suspendedResources;
	}


	//---------------------------------------------------------------------
	// Enable functionality through underlying transaction object
	//---------------------------------------------------------------------

	/**
	 * 是否全局回滚
	 * <p>
	 * Determine the rollback-only flag via checking the transaction object, provided
	 * that the latter implements the {@link SmartTransactionObject} interface.
	 * <p>Will return {@code true} if the global transaction itself has been marked
	 * rollback-only by the transaction coordinator, for example in case of a timeout.
	 *
	 * @see SmartTransactionObject#isRollbackOnly()
	 */
	@Override
	public boolean isGlobalRollbackOnly() {
		return ((this.transaction instanceof SmartTransactionObject) &&
				((SmartTransactionObject) this.transaction).isRollbackOnly());
	}

	/**
	 * 获取保存点管理器
	 * <p>
	 * This implementation exposes the {@link SavepointManager} interface
	 * of the underlying transaction object, if any.
	 *
	 * @throws NestedTransactionNotSupportedException if savepoints are not supported
	 * @see #isTransactionSavepointManager()
	 */
	@Override
	protected SavepointManager getSavepointManager() {
		Object transaction = this.transaction;
		if (!(transaction instanceof SavepointManager)) {
			throw new NestedTransactionNotSupportedException(
					"Transaction object [" + this.transaction + "] does not support savepoints");
		}
		return (SavepointManager) transaction;
	}

	/**
	 * 底层的事务是否为一个保存点管理器
	 * <p>
	 * Return whether the underlying transaction implements the {@link SavepointManager}
	 * interface and therefore supports savepoints.
	 *
	 * @see #getTransaction()
	 * @see #getSavepointManager()
	 */
	public boolean isTransactionSavepointManager() {
		return (this.transaction instanceof SavepointManager);
	}

	/**
	 * 刷新
	 * <p>
	 * Delegate the flushing to the transaction object, provided that the latter
	 * implements the {@link SmartTransactionObject} interface.
	 *
	 * @see SmartTransactionObject#flush()
	 */
	@Override
	public void flush() {
		if (this.transaction instanceof SmartTransactionObject) {
			((SmartTransactionObject) this.transaction).flush();
		}
	}

}
