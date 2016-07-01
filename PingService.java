package com.nortal.ping.simple;

/**
 * 
 * @author Margus Hanni <margus.hanni@nortal.com>
 *
 */
public interface PingService {

    String STATUS_FILE = "statuses.properties";
    String PING_FILE = "ping.properties";

	void ping();

}
