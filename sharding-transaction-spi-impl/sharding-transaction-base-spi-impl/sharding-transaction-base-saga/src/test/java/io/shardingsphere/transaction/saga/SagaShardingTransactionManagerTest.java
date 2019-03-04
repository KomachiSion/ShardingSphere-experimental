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

import io.shardingsphere.transaction.saga.context.SagaBranchTransaction;
import io.shardingsphere.transaction.saga.context.SagaBranchTransactionGroup;
import io.shardingsphere.transaction.saga.context.SagaTransaction;
import io.shardingsphere.transaction.saga.resource.SagaResourceManager;
import io.shardingsphere.transaction.saga.persistence.SagaPersistence;
import io.shardingsphere.transaction.saga.servicecomb.transport.ShardingSQLTransport;
import io.shardingsphere.transaction.saga.servicecomb.transport.ShardingTransportFactory;
import lombok.SneakyThrows;
import org.apache.servicecomb.saga.core.application.SagaExecutionComponent;
import org.apache.shardingsphere.core.constant.DatabaseType;
import org.apache.shardingsphere.core.executor.ShardingExecuteDataMap;
import org.apache.shardingsphere.transaction.core.ResourceDataSource;
import org.apache.shardingsphere.transaction.core.TransactionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class SagaShardingTransactionManagerTest {
    
    private final SagaShardingTransactionManager sagaShardingTransactionManager = new SagaShardingTransactionManager();
    
    @Mock
    private SagaResourceManager sagaResourceManager;
    
    @Mock
    private SagaExecutionComponent sagaExecutionComponent;
    
    @Mock
    private SagaPersistence sagaPersistence;
    
    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field transactionManagerField = SagaShardingTransactionManager.class.getDeclaredField("resourceManager");
        transactionManagerField.setAccessible(true);
        transactionManagerField.set(sagaShardingTransactionManager, sagaResourceManager);
    }
    
    @After
    public void tearDown() {
        getTransactionThreadLocal().remove();
    }
    
    @SuppressWarnings("unchecked")
    @SneakyThrows
    private ThreadLocal<SagaTransaction> getTransactionThreadLocal() {
        Field transactionField = SagaShardingTransactionManager.class.getDeclaredField("CURRENT_TRANSACTION");
        transactionField.setAccessible(true);
        return (ThreadLocal<SagaTransaction>) transactionField.get(SagaShardingTransactionManager.class);
    }
    
    @Test
    public void assertGetTransactionType() {
        assertThat(sagaShardingTransactionManager.getTransactionType(), equalTo(TransactionType.BASE));
    }
    
    @Test
    public void assertInit() {
        Collection<ResourceDataSource> resourceDataSources = new ArrayList<>();
        ResourceDataSource resourceDataSource = mock(ResourceDataSource.class);
        DataSource dataSource = mock(DataSource.class);
        when(resourceDataSource.getDataSource()).thenReturn(dataSource);
        when(resourceDataSource.getOriginalName()).thenReturn("ds1");
        resourceDataSources.add(resourceDataSource);
        sagaShardingTransactionManager.init(DatabaseType.MySQL, resourceDataSources);
        verify(sagaResourceManager).registerDataSourceMap("ds1", dataSource);
    }
    
    @Test
    public void assertClose() {
        sagaShardingTransactionManager.close();
        verify(sagaResourceManager).releaseDataSourceMap();
    }
    
    @Test
    public void assertGetConnection() throws SQLException {
        SagaTransaction sagaTransaction = mock(SagaTransaction.class);
        Connection connection = mock(Connection.class);
        getTransactionThreadLocal().set(sagaTransaction);
        when(sagaResourceManager.getConnection("ds", SagaShardingTransactionManager.getCurrentTransaction())).thenReturn(connection);
        assertThat(sagaShardingTransactionManager.getConnection("ds"), is(connection));
    }
    
    @Test
    public void assertBegin() {
        sagaShardingTransactionManager.begin();
        verify(sagaResourceManager).registerTransactionResource(any(SagaTransaction.class));
        assertNotNull(SagaShardingTransactionManager.getCurrentTransaction());
        assertTrue(ShardingExecuteDataMap.getDataMap().containsKey(SagaShardingTransactionManager.CURRENT_TRANSACTION_KEY));
        assertThat(ShardingExecuteDataMap.getDataMap().get(SagaShardingTransactionManager.CURRENT_TRANSACTION_KEY), instanceOf(SagaTransaction.class));
        assertThat(ShardingTransportFactory.getInstance().getTransport(), instanceOf(ShardingSQLTransport.class));
    }
    
    @Test
    public void assertCommitWithoutBegin() {
        sagaShardingTransactionManager.commit();
        verify(sagaExecutionComponent, never()).run(anyString());
        verify(sagaResourceManager, never()).releaseTransactionResource(any(SagaTransaction.class));
        assertNull(SagaShardingTransactionManager.getCurrentTransaction());
        assertTrue(ShardingExecuteDataMap.getDataMap().isEmpty());
        assertNull(ShardingTransportFactory.getInstance().getTransport());
    }
    
    @Test
    public void assertCommitWithException() throws NoSuchFieldException, IllegalAccessException {
        when(sagaResourceManager.getSagaExecutionComponent()).thenReturn(sagaExecutionComponent);
        when(sagaResourceManager.getSagaPersistence()).thenReturn(sagaPersistence);
        sagaShardingTransactionManager.begin();
        Field containExceptionField = SagaTransaction.class.getDeclaredField("containsException");
        containExceptionField.setAccessible(true);
        containExceptionField.set(ShardingExecuteDataMap.getDataMap().get(SagaShardingTransactionManager.CURRENT_TRANSACTION_KEY), true);
        SagaTransaction sagaTransaction = SagaShardingTransactionManager.getCurrentTransaction();
        sagaShardingTransactionManager.commit();
        verify(sagaPersistence).cleanSnapshot(sagaTransaction.getId());
        verify(sagaExecutionComponent).run(anyString());
        verify(sagaResourceManager).releaseTransactionResource(any(SagaTransaction.class));
        assertNull(SagaShardingTransactionManager.getCurrentTransaction());
        assertTrue(ShardingExecuteDataMap.getDataMap().isEmpty());
        assertNull(ShardingTransportFactory.getInstance().getTransport());
    }
    
    @Test
    public void assertCommitWithoutException() {
        when(sagaResourceManager.getSagaPersistence()).thenReturn(sagaPersistence);
        sagaShardingTransactionManager.begin();
        SagaTransaction sagaTransaction = SagaShardingTransactionManager.getCurrentTransaction();
        sagaShardingTransactionManager.commit();
        verify(sagaPersistence).cleanSnapshot(sagaTransaction.getId());
        verify(sagaExecutionComponent, never()).run(anyString());
        verify(sagaResourceManager).releaseTransactionResource(any(SagaTransaction.class));
        assertNull(SagaShardingTransactionManager.getCurrentTransaction());
        assertTrue(ShardingExecuteDataMap.getDataMap().isEmpty());
        assertNull(ShardingTransportFactory.getInstance().getTransport());
    }
    
    @Test
    public void assertRollbackWithoutBegin() {
        sagaShardingTransactionManager.rollback();
        verify(sagaResourceManager, never()).releaseTransactionResource(any(SagaTransaction.class));
        assertNull(SagaShardingTransactionManager.getCurrentTransaction());
        assertTrue(ShardingExecuteDataMap.getDataMap().isEmpty());
        assertNull(ShardingTransportFactory.getInstance().getTransport());
    }
    
    @Test
    public void assertRollbackWithBegin() {
        when(sagaResourceManager.getSagaExecutionComponent()).thenReturn(sagaExecutionComponent);
        when(sagaResourceManager.getSagaPersistence()).thenReturn(sagaPersistence);
        sagaShardingTransactionManager.begin();
        SagaTransaction sagaTransaction = SagaShardingTransactionManager.getCurrentTransaction();
        SagaBranchTransactionGroup sagaBranchTransactionGroup = new SagaBranchTransactionGroup("", null, null);
        sagaBranchTransactionGroup.getBranchTransactions().add(new SagaBranchTransaction("", "", null));
        sagaTransaction.getBranchTransactionGroups().add(sagaBranchTransactionGroup);
        sagaShardingTransactionManager.rollback();
        assertNull(SagaShardingTransactionManager.getCurrentTransaction());
        assertTrue(ShardingExecuteDataMap.getDataMap().isEmpty());
        assertNull(ShardingTransportFactory.getInstance().getTransport());
        verify(sagaPersistence).cleanSnapshot(sagaTransaction.getId());
        verify(sagaResourceManager).releaseTransactionResource(any(SagaTransaction.class));
    }
    
    @Test
    public void assertIsInTransaction() {
        sagaShardingTransactionManager.begin();
        assertTrue(sagaShardingTransactionManager.isInTransaction());
    }
    
    @Test
    public void assertIsNotInTransaction() {
        assertFalse(sagaShardingTransactionManager.isInTransaction());
    }
}
