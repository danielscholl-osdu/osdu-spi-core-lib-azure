// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.azure.publisherFacade.models;

import lombok.Builder;
import org.opengroup.osdu.core.common.model.http.CollaborationContext;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;

/**
 * Implementation of PubSubAttributesBuilder.
 */

@Lazy
@Builder
public class PubSubAttributesBuilder {
    private DpsHeaders dpsHeaders;
    private CollaborationContext collaborationContext;

    /**
     * Create properties map from headers.
     *
     * @return attributesMap.
     */
    public HashMap<String, Object> createAttributesMap() {
        HashMap<String, Object> attributesMap = new HashMap();
        attributesMap.put("correlation-id", this.dpsHeaders.getCorrelationId());
        attributesMap.put("data-partition-id", this.dpsHeaders.getPartitionIdWithFallbackToAccountId());
        attributesMap.put("account-id", this.dpsHeaders.getPartitionIdWithFallbackToAccountId());

        if (this.collaborationContext != null && this.collaborationContext.hasId()) {
            attributesMap.put("x-collaboration",
                    "id=" + this.collaborationContext.getId()
                            + ",application=" + this.collaborationContext.getApplication());
        }

        return attributesMap;
    }

}
