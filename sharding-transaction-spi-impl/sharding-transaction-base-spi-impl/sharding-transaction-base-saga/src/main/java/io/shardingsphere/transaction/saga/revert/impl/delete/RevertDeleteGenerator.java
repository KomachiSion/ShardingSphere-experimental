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

package io.shardingsphere.transaction.saga.revert.impl.delete;

import java.util.Map;

import org.apache.shardingsphere.core.parsing.lexer.token.DefaultKeyword;

import com.google.common.base.Optional;

import io.shardingsphere.transaction.saga.revert.api.RevertContext;
import io.shardingsphere.transaction.saga.revert.impl.RevertContextGenerator;
import io.shardingsphere.transaction.saga.revert.impl.RevertContextGeneratorParameter;

/**
 * Revert delete generator.
 *
 * @author duhongjun
 */
public final class RevertDeleteGenerator implements RevertContextGenerator {
    
    @Override
    public Optional<RevertContext> generate(final RevertContextGeneratorParameter parameter) {
        RevertDeleteParameter deleteParameter = (RevertDeleteParameter) parameter;
        if (deleteParameter.getSelectSnapshot().isEmpty()) {
            return Optional.absent();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(DefaultKeyword.INSERT).append(" ");
        builder.append(DefaultKeyword.INTO).append(" ");
        builder.append(parameter.getActualTable()).append(" ");
        builder.append(DefaultKeyword.VALUES).append(" ");
        builder.append("(");
        int columnCount = deleteParameter.getSelectSnapshot().get(0).size();
        for (int i = 0; i < columnCount; i++) {
            builder.append("?");
            if (i < columnCount - 1) {
                builder.append(",");
            }
        }
        builder.append(")");
        RevertContext result = new RevertContext(builder.toString());
        for (Map<String, Object> each : deleteParameter.getSelectSnapshot()) {
            result.getRevertParams().add(each.values());
        }
        return Optional.of(result);
    }
}
