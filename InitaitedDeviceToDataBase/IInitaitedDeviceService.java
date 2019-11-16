package net.floodlightcontroller.InitaitedDeviceToDataBase;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IInitaitedDeviceService extends IFloodlightService {
	public void run() throws InterruptedException, JsonProcessingException;
	
	public String gettopology();
}
