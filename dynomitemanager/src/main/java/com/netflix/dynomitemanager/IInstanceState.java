package com.netflix.dynomitemanager;

public interface IInstanceState {
	
	public boolean isSideCarProcessAlive();
	
	public boolean isBootstrapping();
	
	public boolean getYmlWritten();

	public void setYmlWritten(boolean b);
		
}
