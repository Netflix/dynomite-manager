package com.netflix.dynomitemanager.defaultimpl.test;

import com.netflix.dynomitemanager.IInstanceState;

/**
 * Unit Tests for FakeInstanceState
 *
 * @author ipapapa
 */

public class FakeInstanceState implements IInstanceState {

	@Override
	public boolean isSideCarProcessAlive() {
		return false;
	}

	@Override
	public boolean isBootstrapping() {
		return false;
	}

	@Override
	public boolean getYmlWritten() {
		return false;
	}

	@Override
	public void setYmlWritten(boolean b) {
		// TODO Auto-generated method stub

	}

}
