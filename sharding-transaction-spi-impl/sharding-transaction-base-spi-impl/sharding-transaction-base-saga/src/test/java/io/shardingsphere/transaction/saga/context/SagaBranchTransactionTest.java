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

package io.shardingsphere.transaction.saga.context;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public final class SagaBranchTransactionTest {
    
    private String dataSourceName = "dataSourceName";
    
    private String actualTableName = "actualTableName";
    
    private String sql = "sql";
    
    @Test
    public void assertHashCode() {
        SagaBranchTransaction sagaBranchTransaction = new SagaBranchTransaction(dataSourceName, sql, getStringParameterSets());
        sagaBranchTransaction.setActualTableName(actualTableName);
        assertTrue(new SagaBranchTransaction(dataSourceName, sql, getStringParameterSets()).hashCode() == sagaBranchTransaction.hashCode());
        assertTrue(new SagaBranchTransaction(dataSourceName, sql, getMixedParameterSets()).hashCode() == sagaBranchTransaction.hashCode());
    }
    
    @Test
    public void assertEquals() {
        SagaBranchTransaction sagaBranchTransaction = new SagaBranchTransaction(dataSourceName, sql, getStringParameterSets());
        sagaBranchTransaction.setActualTableName(actualTableName);
        assertTrue(sagaBranchTransaction.equals(new SagaBranchTransaction(dataSourceName, sql, getStringParameterSets())));
        assertTrue(sagaBranchTransaction.equals(new SagaBranchTransaction(dataSourceName, sql, getMixedParameterSets())));
    }
    
    @Test
    public void assertToString() {
        SagaBranchTransaction sagaBranchTransaction = new SagaBranchTransaction(dataSourceName, sql, getStringParameterSets());
        sagaBranchTransaction.setActualTableName(actualTableName);
        assertTrue(new SagaBranchTransaction(dataSourceName, sql, getStringParameterSets()).toString().equals(sagaBranchTransaction.toString()));
        assertTrue(new SagaBranchTransaction(dataSourceName, sql, getMixedParameterSets()).toString().equals(sagaBranchTransaction.toString()));
    }
    
    private List<List<Object>> getStringParameterSets() {
        List<List<Object>> result = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        parameters.add("1");
        parameters.add("x");
        result.add(parameters);
        parameters = new ArrayList<>();
        parameters.add("2");
        parameters.add("y");
        return result;
    }
    
    private List<List<Object>> getMixedParameterSets() {
        List<List<Object>> result = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        parameters.add(1);
        parameters.add("x");
        result.add(parameters);
        parameters = new ArrayList<>();
        parameters.add(2);
        parameters.add("y");
        return result;
    }
}
