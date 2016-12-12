/**
 * Copyright 2016 Netflix, Inc. <p/> Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at <p/>
 * http://www.apache.org/licenses/LICENSE-2.0 <p/> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.netflix.dynomitemanager.sidecore.utils.test;

import org.junit.Test;

import com.netflix.dynomitemanager.sidecore.utils.ThreadSleeper;

/**
 * Unit Tests for ThreadSleeper
 *
 * @author diegopacheco
 *
 */
public class ThreadSleeperTest {

    @Test
    public void sleepTest() throws InterruptedException {
        ThreadSleeper t = new ThreadSleeper();
        t.sleep(100L);
    }

    @Test
    public void sleepQuietlyTest() throws InterruptedException {
        ThreadSleeper t = new ThreadSleeper();
        t.sleepQuietly(100L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sleepQuietlyWrongArgTest() throws InterruptedException {
        ThreadSleeper t = new ThreadSleeper();
        t.sleepQuietly(-1);
    }

}
