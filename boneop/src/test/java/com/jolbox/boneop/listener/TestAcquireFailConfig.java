/**
 *  Copyright 2010 Wallace Wadge
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 *
 */
package com.jolbox.boneop.listener;


import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;


/**
 * Test for AcquireFailConfig class.
 *
 * @author Wallace
 */
public class TestAcquireFailConfig {

    /**
     * Test getters/setters for acquireFail class.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testGettersSetters() {
        Object obj = new Object();
        AcquireFailConfig config = new AcquireFailConfig();
        config.setAcquireRetryAttempts(new AtomicInteger(1));
        config.setAcquireRetryDelayInMillis(123);
        config.setAcquireRetryDelayInMillis(123);
        config.setLogMessage("test");
        config.setDebugHandle(obj);

        assertEquals(1, config.getAcquireRetryAttempts().get());
        assertEquals(123, config.getAcquireRetryDelayInMillis());
        assertEquals(123, config.getAcquireRetryDelayInMillis());
        assertEquals("test", config.getLogMessage());
        assertEquals(obj, config.getDebugHandle());

    }

}
