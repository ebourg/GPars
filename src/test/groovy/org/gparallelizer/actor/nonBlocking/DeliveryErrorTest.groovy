//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.actor.nonBlocking

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import org.gparallelizer.actor.Actor
import org.gparallelizer.actor.Actors

public class DeliveryErrorTest  extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
        Actors.defaultPooledActorGroup.resize 5
    }

    public void testSuccessfulMessages() {
        volatile boolean flag = false
        CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.actor {
            react {}
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def message = 1
        message.metaClass.onDeliveryError = {->
            flag = true
        }
        actor << message

        latch.await()
        assertFalse flag
    }

    public void testFailedMessages() {
        volatile boolean flag1 = false
        volatile boolean flag2 = false
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.actor {
            react {
                barrier.await()
            }
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def message1 = 1
        message1.metaClass.onDeliveryError = {->
            flag1 = true
        }

        def message2 = 2
        message2.metaClass.onDeliveryError = {->
            flag2 = true
        }
        actor << message1
        actor << message2
        barrier.await()

        latch.await()
        assertFalse flag1
        assert flag2
    }

    public void testFailedMessagesOnException() {
        volatile boolean flag1 = false
        volatile boolean flag2 = false
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.actor {
            react {
                barrier.await()
                if (true) throw new RuntimeException('test')
            }
        }

        actor.metaClass.onException = {}
        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def message1 = 1
        message1.metaClass.onDeliveryError = {->
            flag1 = true
        }

        def message2 = 2
        message2.metaClass.onDeliveryError = {->
            flag2 = true
        }
        actor << message1
        actor << message2
        barrier.await()

        latch.await()
        assertFalse flag1
        assert flag2
    }

    public void testMessagesWithoutAfterStop() {
        volatile boolean flag = false
        CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.actor {
            latch.await()
        }
        actor.start()

        def message = 1
        message.metaClass.onDeliveryError = {->
            flag = true
        }
        actor << message
        latch.countDown()
        Thread.sleep 1000
        assert flag
    }

    public void testInterruptionFlag() {
        volatile boolean flag = true
        CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.actor {
            latch.await()
            stop()
        }
        actor.start()

        def message = 1
        message.metaClass.onDeliveryError = {->
            flag = Thread.currentThread().isInterrupted()
        }
        actor << message
        latch.countDown()
        Thread.sleep 1000
        assertFalse flag
    }
}