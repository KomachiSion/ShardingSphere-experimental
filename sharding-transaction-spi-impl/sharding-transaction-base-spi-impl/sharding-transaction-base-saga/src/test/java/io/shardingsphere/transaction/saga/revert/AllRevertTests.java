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

package io.shardingsphere.transaction.saga.revert;

import io.shardingsphere.transaction.saga.revert.impl.delete.AllRevertDeleteTests;
import io.shardingsphere.transaction.saga.revert.impl.insert.AllRevertInsertTests;
import io.shardingsphere.transaction.saga.revert.impl.update.AllRevertUpdateTests;
import io.shardingsphere.transaction.saga.revert.integration.MultiKeyTest;
import io.shardingsphere.transaction.saga.revert.integration.MultiValueTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        AllRevertInsertTests.class,
        AllRevertUpdateTests.class,
        AllRevertDeleteTests.class,
        MultiKeyTest.class,
        MultiValueTest.class
})
public final class AllRevertTests {
}
