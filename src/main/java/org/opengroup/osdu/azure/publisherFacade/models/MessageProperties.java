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

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Data model for Message properties.
 **/
@Data
@Builder
@AllArgsConstructor
public class MessageProperties {

    private JsonElement data;

    @SerializedName("account-id")
    private String accountId;

    @SerializedName("data-partition-id")
    private String partitionId;

    @SerializedName("correlation-id")
    private String correlationId;

    @SerializedName("x-collaboration")
    private String collaborationDirectives;

}
