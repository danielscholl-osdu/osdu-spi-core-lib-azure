/*
 * Copyright © Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.azure.entitlements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;

public class EntilementsAPIConfigBeanTest {

    @Test
    public void should_return_correct_object_type() {
        EntilementsAPIConfigBean bean = new EntilementsAPIConfigBean();

        assertEquals(EntitlementsAPIConfig.class, bean.getObjectType());
    }

    @Test
    public void should_create_entitlements_config_instance() throws Exception {
        EntilementsAPIConfigBean bean = new EntilementsAPIConfigBean();
        bean.setAuthorizeAPI("http://entitlements/api/entitlements/v2");
        bean.setAuthorizeAPIKey("test-key");

        EntitlementsAPIConfig config = bean.createInstance();

        assertNotNull(config);
    }

    @Test
    public void should_create_config_with_null_values() throws Exception {
        EntilementsAPIConfigBean bean = new EntilementsAPIConfigBean();

        EntitlementsAPIConfig config = bean.createInstance();

        assertNotNull(config);
    }
}
