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

package com.jolbox.boneop;


import com.jolbox.boneop.listener.AbstractObjectListener;
import com.jolbox.boneop.listener.ObjectListener;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * Tests config object.
 *
 * @author wwadge
 */
public class TestBoneCPConfig {
    /**
     * Config handle.
     */
    static BoneOPConfig config;

    /**
     * Stub out any calls to logger.
     *
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws CloneNotSupportedException
     */
    @BeforeClass
    public static void setup() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, CloneNotSupportedException {
        config = CommonTestUtils.getConfigClone();
    }

    /**
     * Tests configs using xml setups.
     *
     * @throws Exception
     */
    @Test
    public void testXMLConfig() throws Exception {
        // read off from the default boneop-config.xml
//		System.out
//				.println(BoneCPConfig.class.getResource("/boneop-config.xml"));
        BoneOPConfig config = new BoneOPConfig("specialApp");
        assertEquals(99, config.getMinObjectsPerPartition());
    }

    /**
     * Tests configs using xml setups.
     *
     * @throws Exception
     */
    @Test
    public void testXMLConfig2() throws Exception {
        // read off from the default boneop-config.xml
        BoneOPConfig config = new BoneOPConfig("specialApp2");
        assertEquals(123, config.getMinObjectsPerPartition());
    }

    /**
     * Load properties via a given stream.
     *
     * @throws Exception
     */
    @Test
    public void testXmlConfigViaInputStream() throws Exception {
        // read off from an input stream
        BoneOPConfig config = new BoneOPConfig(this.getClass().getResourceAsStream("/boneop-config.xml"), "specialApp");
        assertEquals(99, config.getMinObjectsPerPartition());
    }

    /**
     * XML based config.
     *
     * @throws Exception
     */
    @Test
    public void testXMLConfigWithUnfoundSection() throws Exception {
        BoneOPConfig config = new BoneOPConfig("non-existant");
        assertEquals(20, config.getMinObjectsPerPartition());
    }

    /**
     * Test error condition for xml config.
     */
    @Test
    public void testXmlConfigWithInvalidStream() {
        // throw errors
        try {
            new BoneOPConfig(null, "specialApp");
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // do nothing
        }
    }

    /**
     * Tests configs using xml setups.
     *
     * @throws Exception
     */
    @Test
    public void testPropertyBasedConfig() throws Exception {
        Properties props = new Properties();
        props.setProperty("minObjectsPerPartition", "123");
        props.setProperty("boneop.maxObjectsPerPartition", "456");
        props.setProperty("idleConnectionTestPeriodInSeconds", "999");
        props.setProperty("username", "test");
        props.setProperty("partitionCount", "an int which is invalid");
        props.setProperty("idleMaxAgeInSeconds", "a long which is invalid");
        BoneOPConfig config = new BoneOPConfig(props);
        assertEquals(123, config.getMinObjectsPerPartition());
        assertEquals(456, config.getMaxObjectsPerPartition());
        assertEquals(1, config.getPartitionCount());
        assertEquals(999, config.getIdleConnectionTestPeriod(TimeUnit.SECONDS));
        assertEquals(3600, config.getIdleMaxAge(TimeUnit.SECONDS));
    }

    /**
     * Property get/set
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testGettersSetters() {
        Properties driverProperties = new Properties();
        config.setIdleConnectionTestPeriod(60);
        config.setIdleMaxAge(60);
        config.setReleaseHelperThreads(3);
        config.setMaxObjectsPerPartition(5);
        config.setMinObjectsPerPartition(5);
        config.setPartitionCount(1);
        config.setConnectionTestStatement("test");
        config.setAcquireIncrement(6);
        config.setDefaultTransactionIsolation("foo");
        config.setDefaultTransactionIsolationValue(123);
        config.setAcquireRetryDelay(60, TimeUnit.SECONDS);
        config.setWaitTime(60, TimeUnit.SECONDS);
        config.setIdleMaxAge(60, TimeUnit.SECONDS);
        config.setIdleMaxAgeInSeconds(60);
        config.setIdleConnectionTestPeriod(60, TimeUnit.SECONDS);
        config.setMaxConnectionAge(60, TimeUnit.SECONDS);
        config.setDefaultReadOnly(true);
        config.setDefaultCatalog("foo");
        config.setDefaultAutoCommit(true);
        config.setStatisticsEnabled(true);
        config.setDeregisterDriverOnClose(true);
        config.setNullOnObjectTimeout(true);
        config.setResetObjectOnClose(true);

        assertTrue(config.isNullOnObjectTimeout());
        assertTrue(config.isResetObjectOnClose());
        assertEquals("foo", config.getDefaultCatalog());
        assertTrue(config.isDeregisterDriverOnClose());
        assertTrue(config.getDefaultAutoCommit());
        assertTrue(config.isStatisticsEnabled());
        assertTrue(config.getDefaultReadOnly());

        config.setMaxConnectionAge(60);
        assertEquals(60, config.getMaxConnectionAge());
        assertEquals(1, config.getIdleConnectionTestPeriod());
        assertEquals(1, config.getIdleMaxAge());
        assertEquals(60000, config.getWaitTime());
        assertEquals(60, config.getWaitTime(TimeUnit.SECONDS));

        assertEquals(60000, config.getAcquireRetryDelay());
        assertEquals("foo", config.getDefaultTransactionIsolation());
        assertEquals(123, config.getDefaultTransactionIsolationValue());

        ObjectListener hook = new AbstractObjectListener() {
            // do nothing
        };
        config.setObjectListener(hook);

        config.setPoolName("foo");
        config.setDisableJMX(false);
        config.setQueryExecuteTimeLimit(123);
        config.setQueryExecuteTimeLimitInMs(123);
        config.setDisableObjectTracking(true);
        config.setWaitTime(9999);
        config.setDriverProperties(driverProperties);
        config.setCloseObjectWatchTimeout(Long.MAX_VALUE);
        String lifo = "LIFO";
        config.setServiceOrder(lifo);
        config.setConfigFile("abc");
        config.setIdleConnectionTestPeriodInMinutes(1);
        config.setWaitTimeInMs(1000);
        config.setCloseObjectWatchTimeoutInMs(1000);

        assertEquals(hook, config.getObjectListener());
        assertEquals(1000, config.getWaitTimeInMs());
        assertEquals(123, config.getQueryExecuteTimeLimit(TimeUnit.MILLISECONDS));
        assertEquals(1000, config.getCloseConnectionWatchTimeout(TimeUnit.MILLISECONDS));

        assertEquals(1000, config.getCloseObjectWatchTimeoutInMs());
        assertEquals(1, config.getIdleConnectionTestPeriodInMinutes());
        assertEquals(lifo, config.getServiceOrder());
        assertEquals("abc", config.getConfigFile());
        assertEquals(1000, config.getCloseObjectWatchTimeout());
        assertEquals("foo", config.getPoolName());
        assertEquals(3, config.getReleaseHelperThreads());
        assertEquals(5, config.getMaxObjectsPerPartition());
        assertEquals(5, config.getMinObjectsPerPartition());
        assertEquals(6, config.getAcquireIncrement());
        assertEquals(1000, config.getWaitTime());
        assertEquals(true, config.isDisableObjectTracking());
        assertEquals(123, config.getQueryExecuteTimeLimit());
        assertEquals(1, config.getPartitionCount());
        assertEquals("test", config.getConnectionTestStatement());
    }

    /**
     * Config file scrubbing
     */
    @Test
    public void testConfigSanitize() {
        config.setMaxObjectsPerPartition(-1);
        config.setMinObjectsPerPartition(-1);
        config.setPartitionCount(-1);

        config.setConnectionTestStatement("");

        config.setAcquireIncrement(0);

        config.setPoolAvailabilityThreshold(-50);
        config.setWaitTimeInMs(0);
        config.setServiceOrder("something non-sensical");
        config.setAcquireRetryDelayInMs(-1);

        config.setReleaseHelperThreads(-1);
        config.sanitize();

        assertEquals(1000, config.getAcquireRetryDelay(TimeUnit.MILLISECONDS));
        assertEquals(1000, config.getAcquireRetryDelayInMs());
        assertEquals("FIFO", config.getServiceOrder());
        assertEquals(0, config.getWaitTimeInMs());
        assertNotNull(config.toString());
        assertFalse(config.getAcquireIncrement() == 0);
        assertFalse(config.getReleaseHelperThreads() == -1);
        assertFalse(config.getMaxObjectsPerPartition() == -1);
        assertFalse(config.getMinObjectsPerPartition() == -1);
        assertFalse(config.getPartitionCount() == -1);

        config.setMinObjectsPerPartition(config.getMaxObjectsPerPartition() + 1);
        config.setServiceOrder(null);
        config.sanitize();
        assertEquals("FIFO", config.getServiceOrder());
        assertEquals(config.getMinObjectsPerPartition(), config.getMaxObjectsPerPartition());
        assertEquals(20, config.getPoolAvailabilityThreshold());

        config.setDefaultTransactionIsolation("NONE");
        config.sanitize();
        assertEquals(Connection.TRANSACTION_NONE, config.getDefaultTransactionIsolationValue());

        config.setDefaultTransactionIsolation("READ_COMMITTED");
        config.sanitize();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, config.getDefaultTransactionIsolationValue());

        config.setDefaultTransactionIsolation("READ_UNCOMMITTED");
        config.sanitize();
        assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, config.getDefaultTransactionIsolationValue());

        config.setDefaultTransactionIsolation("SERIALIZABLE");
        config.sanitize();
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, config.getDefaultTransactionIsolationValue());

        config.setDefaultTransactionIsolation("REPEATABLE_READ");
        config.sanitize();
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, config.getDefaultTransactionIsolationValue());

        config.setDefaultTransactionIsolation("BAD_VALUE");
        config.sanitize();
        assertEquals(-1, config.getDefaultTransactionIsolationValue());

        // coverage
        BoneOPConfig config = new BoneOPConfig();
        config.setDriverProperties(null);
        config.sanitize();
    }


    /**
     * Tests general methods.
     *
     * @throws CloneNotSupportedException
     */
    @Test
    public void testCloneEqualsConfigHashCode() throws CloneNotSupportedException {
        BoneOPConfig clone = config.clone();
        assertTrue(clone.hasSameConfiguration(config));
        assertFalse(clone.hasSameConfiguration(null));
        assertTrue(clone.hasSameConfiguration(clone));
        clone.setPoolName("different pool name.");
        assertFalse(clone.hasSameConfiguration(config));
    }

    /**
     * Tries to load an invalid property file.
     *
     * @throws CloneNotSupportedException
     * @throws IOException
     */
    @Test
    public void testLoadPropertyFileInvalid() throws CloneNotSupportedException, IOException {
        BoneOPConfig config = new BoneOPConfig();
        BoneOPConfig clone = config.clone();

        config.loadProperties("invalid-property-file.xml");
        assertTrue(config.hasSameConfiguration(clone));
    }

    /**
     * Tries to load an invalid property file.
     *
     * @throws CloneNotSupportedException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testLoadPropertyFileValid() throws CloneNotSupportedException, IOException, URISyntaxException {
        BoneOPConfig config = new BoneOPConfig();
        //coverage
        config.loadProperties("boneop-config.xml");
    }

    /**
     * See how the config handles a garbage filled file.
     *
     * @throws CloneNotSupportedException
     * @throws IOException
     */
    @Test
    public void testLoadPropertyFileInvalid2() throws CloneNotSupportedException, IOException {
        BoneOPConfig config = new BoneOPConfig();
        BoneOPConfig clone = config.clone();

        config.loadProperties("java/lang/String.class");
        assertTrue(config.hasSameConfiguration(clone));
    }


}