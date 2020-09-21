package com.chentong.serviceexample;

import java.util.ArrayList;

public class BSSIDInfo {
    private String SSID;
    private ArrayList<Integer> RSSILevels;

    public BSSIDInfo(String SSID, int RSSILevel){
        this.SSID = SSID;
        this.RSSILevels = new ArrayList<>();
        this.RSSILevels.add(RSSILevel);
    }

    public String getSSID() { return this.SSID; }
    public ArrayList<Integer> getRSSILevels() { return this.RSSILevels; }
    public void addRSSI(int RSSI) { this.RSSILevels.add(RSSI); }
}

