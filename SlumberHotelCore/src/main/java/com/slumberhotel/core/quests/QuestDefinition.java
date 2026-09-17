package com.slumberhotel.core.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestDefinition {
    public String id;
    public String name;
    public String description;
    public String npcId; // config key for NPC
    public List<String> requiresQuests = new ArrayList<String>();
    public boolean requiresEnteredHotel = false;
    public int requiresWalletTier = -1;
    public int requiresGamesPlayed = 0;
    public String objectiveType = "TALK"; // TALK, COLLECT_ITEM, PAY_TICKETS, CUSTOM
    public String itemKey = "";
    public int itemAmount = 1;
    public int ticketCost = 0;
    public int rewardTickets = 0;
    public int rewardWalletTier = -1; // set wallet to this tier on complete (-1 = no change)
    public boolean upgradeWallet = false; // upgrade +1 tier
    public List<String> rewardCommands = new ArrayList<String>();
    public List<String> unlockDoors = new ArrayList<String>();
    public List<String> startDialogue = new ArrayList<String>();
    public List<String> progressDialogue = new ArrayList<String>();
    public List<String> completeDialogue = new ArrayList<String>();
    public boolean autoStart = false;
    public boolean repeatable = false;
    public int rewardStars = 0;
    public int requiresStars = 0;
    public double rewardBoon = 0;
}
