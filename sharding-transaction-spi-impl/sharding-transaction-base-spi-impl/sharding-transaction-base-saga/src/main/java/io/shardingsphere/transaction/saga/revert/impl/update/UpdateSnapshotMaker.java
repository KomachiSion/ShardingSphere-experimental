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

package io.shardingsphere.transaction.saga.revert.impl.update;

import java.util.LinkedList;
import java.util.List;

import io.shardingsphere.transaction.saga.revert.api.SnapshotParameter;
import io.shardingsphere.transaction.saga.revert.impl.delete.DeleteSnapshotMaker;

/**
 * Update snapshot maker.
 *
 * @author duhongjun
 */
public final class UpdateSnapshotMaker extends DeleteSnapshotMaker {
    
    @Override
    protected void fillSelectItem(final StringBuilder builder, final SnapshotParameter snapshotParameter, final List<String> keys) {
        if (keys.isEmpty()) {
            super.fillSelectItem(builder, snapshotParameter, keys);
            return;
        }
        List<String> tableKeys = new LinkedList<>(keys);
        boolean first = true;
        for (String each : snapshotParameter.getStatement().getUpdateColumns().keySet()) {
            int dotPos = each.indexOf('.');
            String realColumnName = null;
            if (dotPos > 0) {
                realColumnName = each.substring(dotPos + 1).toLowerCase();
            } else {
                realColumnName = each.toLowerCase();
            }
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(realColumnName);
            if (tableKeys.contains(realColumnName)) {
                tableKeys.remove(realColumnName);
            }
        }
        for (String each : tableKeys) {
            builder.append(", ");
            builder.append(each);
        }
        builder.append(" ");
    }
}
