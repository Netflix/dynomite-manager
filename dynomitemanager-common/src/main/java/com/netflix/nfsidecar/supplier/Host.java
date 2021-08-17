package com.netflix.nfsidecar.supplier;

import java.util.HashSet;
import java.util.Set;

public class Host {
    private final String name;
    private final int port;
    private final Set<String> alternateIpAddresses = new HashSet<>();

    private String id;
    private String rack;

    public Host(final String name, final int port) {
        this.name = name;
        this.port = port;
    }

    public String getName() {
        return this.name;
    }

    public int getPort() {
        return this.port;
    }

    public Set<String> getAlternateIpAddresses() {
        return this.alternateIpAddresses;
    }

    public Host addAlternateIpAddress(final String ip) {
        this.alternateIpAddresses.add(ip);
        return this;
    }

    public String getId() {
        return this.id;
    }

    public Host setId(final String id) {
        this.id = id;
        return this;
    }

    public String getRack() {
        return rack;
    }

    public Host setRack(final String rack) {
        this.rack = rack;
        return this;
    }
}
