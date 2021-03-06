/**
 * Copyright 2010 Wallace Wadge
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.jolbox.boneop;

import org.slf4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;

import static org.easymock.EasyMock.*;

/**
 * Test for connection thread tester
 *
 * @author wwadge
 */
public class TestConnectionThreadTester {

    /**
     * Mock handle.
     */
    private static BoneOP mockPool;
    /**
     * Mock handle.
     */
    private static ObjectPartition mockConnectionPartition;
    /**
     * Mock handle.
     */
    private static ScheduledExecutorService mockExecutor;
    /**
     * Test class handle.
     */
    private ObjectTesterThread testClass;
    /**
     * Mock handle.
     */
    private static ObjectHandle mockConnection;
    /**
     * Mock handle.
     */
    private static BoneOPConfig config;
    /**
     * Mock handle.
     */
    private static Logger mockLogger;

    /**
     * Mock setup.
     *
     * @throws ClassNotFoundException
     */
    @BeforeClass
    public static void setup() throws ClassNotFoundException {
        mockPool = createNiceMock(BoneOP.class);
        mockConnectionPartition = createNiceMock(ObjectPartition.class);
        mockExecutor = createNiceMock(ScheduledExecutorService.class);
        mockConnection = createNiceMock(ObjectHandle.class);
        mockLogger = createNiceMock(Logger.class);

        makeThreadSafe(mockLogger, true);
        config = new BoneOPConfig();
        config.setIdleMaxAgeInMinutes(100);
        config.setIdleObjectTestPeriodInMinutes(100);

    }

    /**
     * Reset all mocks.
     */
    @BeforeMethod
    public void resetMocks() {
        reset(mockPool, mockConnectionPartition, mockExecutor, mockConnection, mockLogger);
    }

    /**
     * Tests that a connection that is marked broken is closed internally and that the partition is marked as being able
     * to create new connections.
     *
     * @throws SQLException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testConnectionMarkedBroken() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(100);
        fakeFreeConnections.add(mockConnection);

        BoneOPConfig localconfig = config.clone();
        expect(mockPool.getConfig()).andReturn(localconfig).anyTimes();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(1).anyTimes();

        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
        expect(mockConnection.isPossiblyBroken()).andReturn(true);

        // connection should be closed
        mockConnection.internalClose();
        mockPool.postDestroyObject(mockConnection);
        expectLastCall().once();
        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection);
    }

    /**
     * Tests that a connection that has been idle for more than the set time is closed off.
     *
     * @throws SQLException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testIdleConnectionIsKilled() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(100);
        fakeFreeConnections.add(mockConnection);
        fakeFreeConnections.add(mockConnection);
        BoneOPConfig localconfig = config.clone();
        expect(mockPool.getConfig()).andReturn(localconfig.clone()).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(2).anyTimes();

        expect(mockConnectionPartition.getMinObjects()).andReturn(0).once();
        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(0L);

        // connection should be closed
        mockConnection.internalClose();
        mockPool.postDestroyObject(mockConnection);
        expectLastCall().once();

        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection);
    }

    /**
     * Tests that a connection that has been idle for more than the set time is closed off but during closing, an
     * exception occurs (should update partition counts).
     *
     * @throws SQLException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testIdleConnectionIsKilledWithFailure() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(100);
        fakeFreeConnections.add(mockConnection);
        fakeFreeConnections.add(mockConnection);
        BoneOPConfig localconfig = config.clone();
        expect(mockPool.getConfig()).andReturn(localconfig.clone()).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(2).anyTimes();

        expect(mockConnectionPartition.getMinObjects()).andReturn(0).once();
        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(0L);

        // connection should be closed
        mockConnection.internalClose();
        expectLastCall().andThrow(new RuntimeException());
        mockPool.postDestroyObject(mockConnection);
        expectLastCall().once();

        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection);
    }

    /**
     * Tests that a connection gets to receive a keep-alive.
     *
     * @throws SQLException
     * @throws InterruptedException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testIdleConnectionIsSentKeepAlive() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(100);
        fakeFreeConnections.add(mockConnection);
        BoneOPConfig localconfig = config.clone();
        localconfig.setIdleObjectTestPeriodInMinutes(1);
        localconfig.setIdleMaxAgeInMinutes(0);
        expect(mockPool.getConfig()).andReturn(localconfig).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
//		expect(mockConnectionPartition.getMinConnections()).andReturn(10).once();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(2).anyTimes();

        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(0L);
        expect(mockPool.isObjectHandleAlive((ObjectHandle) anyObject())).andReturn(true).anyTimes();
        mockPool.putObjectBackInPartition((ObjectHandle) anyObject());

        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection);
    }

    /**
     * Tests that a connection gets to receive a keep-alive.
     *
     * @throws SQLException
     * @throws InterruptedException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testIdleMaxAge() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(100);
        fakeFreeConnections.add(mockConnection);
        BoneOPConfig localconfig = config.clone();
        localconfig.setIdleObjectTestPeriodInMinutes(0);
        localconfig.setIdleMaxAgeInMinutes(1);
        expect(mockPool.getConfig()).andReturn(localconfig).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
//		expect(mockConnectionPartition.getMinConnections()).andReturn(10).once();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(2).anyTimes();

        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(Long.MAX_VALUE);
        expect(mockPool.isObjectHandleAlive((ObjectHandle) anyObject())).andReturn(true).anyTimes();
        mockPool.putObjectBackInPartition((ObjectHandle) anyObject());

        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection);
    }

    /**
     * Tests that a connection gets to receive a keep-alive.
     *
     * @throws SQLException
     * @throws InterruptedException
     * @throws CloneNotSupportedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIdleMaxAgeLifoMode() throws Exception {
        LIFOQueue<ObjectHandle> mockFreeConnections = createNiceMock(LIFOQueue.class);
        expect(mockFreeConnections.poll()).andReturn(mockConnection).once();
        BoneOPConfig localconfig = config.clone();
        localconfig.setIdleObjectTestPeriodInMinutes(0);
        localconfig.setIdleMaxAgeInMinutes(1);
        expect(mockPool.getConfig()).andReturn(localconfig).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(mockFreeConnections).anyTimes();
        expect(mockConnection.getOriginatingPartition()).andReturn(mockConnectionPartition).once();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(2).anyTimes();
        expect(mockFreeConnections.offerLast((ObjectHandle) anyObject())).andReturn(false).anyTimes();
        mockConnection.internalClose();
        expectLastCall().once();
        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(Long.MAX_VALUE);
        expect(mockPool.isObjectHandleAlive((ObjectHandle) anyObject())).andReturn(true).anyTimes();
        //mockPool.putConnectionBackInPartition((ConnectionHandle)anyObject());

        replay(mockPool, mockFreeConnections, mockConnection, mockConnectionPartition, mockExecutor);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), true);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection);
    }

    /**
     * Tests that an active connection that fails the connection is alive test will get closed.
     *
     * @throws SQLException
     * @throws InterruptedException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testIdleConnectionFailedKeepAlive() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(100);
        fakeFreeConnections.add(mockConnection);
        BoneOPConfig localconfig = config.clone();
        localconfig.setIdleObjectTestPeriodInMinutes(1);
        expect(mockPool.getConfig()).andReturn(localconfig).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
        expect(mockConnectionPartition.getMinObjects()).andReturn(10).once();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(1).anyTimes();
        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(0L);
        expect(mockPool.isObjectHandleAlive((ObjectHandle) anyObject())).andReturn(false).anyTimes();

        // connection should be closed
        mockConnection.internalClose();
        mockPool.postDestroyObject(mockConnection);
        expectLastCall().once();

        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection);
    }

    /**
     * Tests fake exceptions, Mostly for code coverage.
     *
     * @throws SQLException
     * @throws InterruptedException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testInterruptedException() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(100);
        fakeFreeConnections.add(mockConnection);
        BoneOPConfig localconfig = config.clone();
        localconfig.setIdleObjectTestPeriodInMinutes(1);
        expect(mockPool.getConfig()).andReturn(localconfig).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
        expect(mockConnectionPartition.getMinObjects()).andReturn(10).once();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(1).anyTimes();
        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(0L);
        expect(mockPool.isObjectHandleAlive((ObjectHandle) anyObject())).andReturn(true).anyTimes();
        expect(mockExecutor.isShutdown()).andReturn(true);
        mockPool.putObjectBackInPartition((ObjectHandle) anyObject());
        expectLastCall().andThrow(new RuntimeException());

        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection);
    }

    /**
     * Tests fake exceptions, connection should be shutdown if the scheduler was marked as going down. Same test except
     * just used to check for a spurious interrupted exception (should be logged).
     *
     * @throws SQLException
     * @throws InterruptedException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testExceptionSpurious() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(10);
        fakeFreeConnections.add(mockConnection);
        BoneOPConfig localconfig = config.clone();
        localconfig.setIdleObjectTestPeriodInMinutes(1);
        expect(mockPool.getConfig()).andReturn(localconfig).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
        expect(mockConnectionPartition.getMinObjects()).andReturn(10).once();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(1).anyTimes();
        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(0L);
        expect(mockPool.isObjectHandleAlive((ObjectHandle) anyObject())).andReturn(true).anyTimes();
        expect(mockExecutor.isShutdown()).andReturn(false);
        mockPool.putObjectBackInPartition((ObjectHandle) anyObject());
        expectLastCall().andThrow(new RuntimeException());
        mockLogger.error((String) anyObject(), (Exception) anyObject());

        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor, mockLogger);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        Field loggerField = this.testClass.getClass().getDeclaredField("LOG");
        TestUtils.setFinalStatic(loggerField, mockLogger);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection, mockLogger);
    }

    /**
     * Tests fake exceptions, connection should be shutdown if the scheduler was marked as going down. Same test except
     * just used to check for a spurious interrupted exception (should be logged).
     *
     * @throws SQLException
     * @throws InterruptedException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws CloneNotSupportedException
     */
    @Test
    public void testExceptionOnCloseConnection() throws Exception {
        BoundedLinkedTransferQueue<ObjectHandle> fakeFreeConnections = new BoundedLinkedTransferQueue<ObjectHandle>(100);
        fakeFreeConnections.add(mockConnection);

        BoneOPConfig localconfig = config.clone();
        localconfig.setIdleObjectTestPeriodInMinutes(1);
        expect(mockPool.getConfig()).andReturn(localconfig).anyTimes();
        expect(mockConnectionPartition.getFreeObjects()).andReturn(fakeFreeConnections).anyTimes();
        expect(mockConnectionPartition.getMinObjects()).andReturn(10).once();
        expect(mockConnectionPartition.getAvailableObjects()).andReturn(1).anyTimes();
        expect(mockConnection.isPossiblyBroken()).andReturn(false);
        expect(mockConnection.getObjectLastUsedInMillis()).andReturn(0L);
        expect(mockPool.isObjectHandleAlive((ObjectHandle) anyObject())).andReturn(false).anyTimes();

        // connection should be closed
        mockConnection.internalClose();
        expectLastCall().andThrow(new PoolException());

        replay(mockPool, mockConnection, mockConnectionPartition, mockExecutor, mockLogger);
        this.testClass = new ObjectTesterThread(mockConnectionPartition, mockExecutor, mockPool, localconfig.getIdleMaxAgeInMinutes(), localconfig.getIdleObjectTestPeriodInMinutes(), false);
        Field loggerField = this.testClass.getClass().getDeclaredField("LOG");
        TestUtils.setFinalStatic(loggerField, mockLogger);
        this.testClass.run();
        verify(mockPool, mockConnectionPartition, mockExecutor, mockConnection, mockLogger);
    }
}
