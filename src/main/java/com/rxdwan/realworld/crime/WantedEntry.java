package com.rxdwan.realworld.crime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WantedEntry {
    private UUID playerUUID;
    private String playerName;
    private double bounty;
    private List<String> crimes;

    public WantedEntry() {
        this.crimes = new ArrayList<>();
    }

    public WantedEntry(UUID playerUUID, String playerName, double initialBounty, String firstCrime) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.bounty = initialBounty;
        this.crimes = new ArrayList<>();
        this.crimes.add(firstCrime);
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public double getBounty() { return bounty; }
    public void setBounty(double bounty) { this.bounty = bounty; }
    public List<String> getCrimes() { return crimes; }
    public void addCrime(String crime) { this.crimes.add(crime); }
}
