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

package io.shardingsphere.transaction.saga.revert.impl.factory;

import org.apache.shardingsphere.core.parsing.parser.sql.dml.DMLStatement;
import org.apache.shardingsphere.core.parsing.parser.sql.dml.insert.InsertStatement;

import io.shardingsphere.transaction.saga.revert.api.RevertOperate;
import io.shardingsphere.transaction.saga.revert.impl.delete.RevertDelete;
import io.shardingsphere.transaction.saga.revert.impl.insert.RevertInsert;
import io.shardingsphere.transaction.saga.revert.impl.update.RevertUpdate;

/**
 * Revert operate factory.
 *
 * @author duhongjun
 */
public final class RevertOperateFactory {
    
    /**
     * Get RevertOperate by DMLStatement.
     *  
     * @param dmlStatement  DML statement
     * @return Revert Operate
     */
    public static RevertOperate getRevertSQLCreator(final DMLStatement dmlStatement) {
        if (dmlStatement instanceof InsertStatement) {
            return new RevertInsert();
        }
        if (dmlStatement.isDeleteStatement()) {
            return new RevertDelete();
        }
        return new RevertUpdate();
    }
}
