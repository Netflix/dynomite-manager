package com.netflix.florida.defaultimpl.test;

import com.netflix.florida.identity.IInstanceState;

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