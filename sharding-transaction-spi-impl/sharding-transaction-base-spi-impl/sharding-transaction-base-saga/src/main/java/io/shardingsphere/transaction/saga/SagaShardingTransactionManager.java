/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shardingsphere.transaction.saga;

import io.shardingsphere.transaction.saga.config.SagaConfiguration;
import io.shardingsphere.transaction.saga.config.SagaConfigurationLoader;
import io.shardingsphere.transaction.saga.context.SagaBranchTransaction;
import io.shardingsphere.transaction.saga.context.SagaBranchTransactionGroup;
import io.shardingsphere.transaction.saga.context.SagaTransaction;
import io.shardingsphere.transaction.saga.resource.SagaResourceManager;
import io.shardingsphere.transaction.saga.revert.SQLRevertResult;
import io.shardingsphere.transaction.saga.servicecomb.definition.SagaDefinitionBuilder;
import io.shardingsphere.transaction.saga.servicecomb.transport.ShardingTransportFactory;
import lombok.SneakyThrows;

import org.apache.servicecomb.saga.core.RecoveryPolicy;
import org.apache.shardingsphere.core.constant.DatabaseType;
import org.apache.shardingsphere.core.executor.ShardingExecuteDataMap;
import org.apache.shardingsphere.transaction.core.ResourceDataSource;
import org.apache.shardingsphere.transaction.core.TransactionType;
import org.apache.shardingsphere.transaction.spi.ShardingTransactionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Sharding transaction manager for Saga.
 *
 * @author yangyi
 */
public final class SagaShardingTransactionManager implements ShardingTransactionManager {
    
    public static final String CURRENT_TRANSACTION_KEY = "current_transaction";
    
    private static final ThreadLocal<SagaTransaction> CURRENT_TRANSACTION = new ThreadLocal<>();
    
    private final SagaConfiguration sagaConfiguration;
    
    private final SagaResourceManager resourceManager;
    
    public SagaShardingTransactionManager() {
        sagaConfiguration = SagaConfigurationLoader.load();
        resourceManager = new SagaResourceManager(sagaConfiguration);
    }
    
    /**
     * Get saga transaction for current thread.
     *
     * @return saga transaction
     */
    public static SagaTransaction getCurrentTransaction() {
        return CURRENT_TRANSACTION.get();
    }
    
    @Override
    public void init(final DatabaseType databaseType, final Collection<ResourceDataSource> resourceDataSources) {
        for (ResourceDataSource each : resourceDataSources) {
            resourceManager.registerDataSourceMap(each.getOriginalName(), each.getDataSource());
        }
    }
    
    @Override
    public TransactionType getTransactionType() {
        return TransactionType.BASE;
    }
    
    @Override
    public boolean isInTransaction() {
        return null != CURRENT_TRANSACTION.get();
    }
    
    @Override
    public Connection getConnection(final String dataSourceName) throws SQLException {
        return resourceManager.getConnection(dataSourceName, CURRENT_TRANSACTION.get());
    }
    
    @Override
    public void begin() {
        if (null == CURRENT_TRANSACTION.get()) {
            SagaTransaction transaction = new SagaTransaction(sagaConfiguration.getRecoveryPolicy());
            resourceManager.registerTransactionResource(transaction);
            ShardingExecuteDataMap.getDataMap().put(CURRENT_TRANSACTION_KEY, transaction);
            CURRENT_TRANSACTION.set(transaction);
            ShardingTransportFactory.getInstance().cacheTransport(transaction);
        }
    }
    
    @Override
    public void commit() {
        if (null != CURRENT_TRANSACTION.get() && CURRENT_TRANSACTION.get().isContainsException()) {
            submitToSagaEngine(false);
        }
        cleanTransaction();
    }
    
    @Override
    public void rollback() {
        if (null != CURRENT_TRANSACTION.get()) {
            submitToSagaEngine(isforcedRollback());
        }
        cleanTransaction();
    }
    
    private boolean isforcedRollback() {
        return !CURRENT_TRANSACTION.get().isContainsException() && RecoveryPolicy.SAGA_BACKWARD_RECOVERY_POLICY.equals(sagaConfiguration.getRecoveryPolicy());
    }
    
    @SneakyThrows
    private void submitToSagaEngine(final boolean isForcedRollback) {
        SagaDefinitionBuilder sagaDefinitionBuilder = getSagaDefinitionBuilder();
        if (isForcedRollback) {
            sagaDefinitionBuilder.addRollbackRequest();
        }
        resourceManager.getSagaExecutionComponent().run(sagaDefinitionBuilder.build());
    }
    
    private SagaDefinitionBuilder getSagaDefinitionBuilder() {
        SagaDefinitionBuilder result = new SagaDefinitionBuilder(sagaConfiguration.getRecoveryPolicy(),
            sagaConfiguration.getTransactionMaxRetries(), sagaConfiguration.getCompensationMaxRetries(), sagaConfiguration.getTransactionRetryDelayMilliseconds());
        for (SagaBranchTransactionGroup each : CURRENT_TRANSACTION.get().getBranchTransactionGroups()) {
            result.switchParents();
            initSagaDefinitionForGroup(result, each);
        }
        return result;
    }
    
    private void initSagaDefinitionForGroup(final SagaDefinitionBuilder sagaDefinitionBuilder, final SagaBranchTransactionGroup sagaBranchTransactionGroup) {
        SagaTransaction currentTransaction = CURRENT_TRANSACTION.get();
        for (SagaBranchTransaction each : sagaBranchTransactionGroup.getBranchTransactions()) {
            SQLRevertResult revertResult = currentTransaction.getRevertResults().containsKey(each) ? currentTransaction.getRevertResults().get(each) : new SQLRevertResult();
            sagaDefinitionBuilder.addChildRequest(
                String.valueOf(each.hashCode()), each.getDataSourceName(), each.getSql(), each.getParameterSets(), revertResult.getSql(), revertResult.getParameterSets());
        }
    }
    
    private void cleanTransaction() {
        if (null != CURRENT_TRANSACTION.get()) {
            resourceManager.getSagaPersistence().cleanSnapshot(CURRENT_TRANSACTION.get().getId());
            resourceManager.releaseTransactionResource(CURRENT_TRANSACTION.get());
        }
        ShardingTransportFactory.getInstance().remove();
        ShardingExecuteDataMap.getDataMap().remove(CURRENT_TRANSACTION_KEY);
        CURRENT_TRANSACTION.remove();
    }
    
    @Override
    public void close() {
        resourceManager.releaseDataSourceMap();
    }
}
