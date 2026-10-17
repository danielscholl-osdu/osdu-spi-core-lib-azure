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

package org.opengroup.osdu.azure.di;

import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Defines configurations for Cosmos bulk executor.
 */
@Configuration
@Getter
@Lazy
public class CosmosBulkExecutorConfiguration {
    /**
     * @return the connection pool size for the http client used by document client. Use SDK default if not set.
     */
    @Bean
    public int documentClientMaxPoolSize() {
        return Integer.parseInt(System.getenv().getOrDefault("DOCUMENT_CLIENT_MAX_POOL_SIZE", "100"));
    }

    /**
     * @return the amount of RUs allocated to bulk executor.
     */
    @Bean
    public Integer bulkExecutorMaxRUs() {
        return Integer.parseInt(System.getenv().getOrDefault("BULK_EXECUTOR_MAX_RUS", "4000"));
    }
}
