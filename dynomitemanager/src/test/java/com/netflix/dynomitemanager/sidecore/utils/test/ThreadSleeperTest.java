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
	public void sleepTest() throws InterruptedException{
		ThreadSleeper t = new ThreadSleeper();
		t.sleep(100L);
	}
	
	@Test
	public void sleepQuietlyTest() throws InterruptedException{
		ThreadSleeper t = new ThreadSleeper();
		t.sleepQuietly(100L);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void sleepQuietlyWrongArgTest() throws InterruptedException{
		ThreadSleeper t = new ThreadSleeper();
		t.sleepQuietly(-1);
	}
	
}
