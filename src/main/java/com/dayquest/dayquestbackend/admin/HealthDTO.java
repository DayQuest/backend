package com.dayquest.dayquestbackend.admin;

import java.sql.Time;

public class HealthDTO {
    private double cpuUsage;
    private double memoryUsage;
    private double diskUsage;
    private double networkUsage;
    private double cpuTemperature;
    private boolean databaseConnection;
    private Time responseTime;

    public HealthDTO(double cpuUsage, double memoryUsage, double diskUsage, double networkUsage, double cpuTemperature, boolean databaseConnection, Time responseTime) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.diskUsage = diskUsage;
        this.networkUsage = networkUsage;
        this.cpuTemperature = cpuTemperature;
        this.databaseConnection = databaseConnection;
        this.responseTime = responseTime;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public double getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(double diskUsage) {
        this.diskUsage = diskUsage;
    }

    public double getNetworkUsage() {
        return networkUsage;
    }

    public void setNetworkUsage(double networkUsage) {
        this.networkUsage = networkUsage;
    }

    public double getCpuTemperature() {
        return cpuTemperature;
    }

    public void setCpuTemperature(double cpuTemperature) {
        this.cpuTemperature = cpuTemperature;
    }

    public boolean isDatabaseConnection() {
        return databaseConnection;
    }

    public void setDatabaseConnection(boolean databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public Time getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Time responseTime) {
        this.responseTime = responseTime;
    }
}
