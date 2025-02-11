/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edqs;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.edqs.sync.enabled:true}' == 'true' && '${queue.type:null}' == 'in-memory'")
public class LocalEdqsSyncService extends EdqsSyncService {

    private final EdqsRocksDb db;

    @Override
    public boolean isSyncNeeded() {
        return db.isNew();
    }

}
