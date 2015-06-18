package com.maffiou.kidCraft;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.configuration.file.FileConfiguration;

public class PlayerManager {

    PlayerManager(FileConfiguration myConfig) {
        config = myConfig;
    }
    FileConfiguration config ;

    synchronized String getRawPlayerList() {
        return config.getString("UserList");
    }

    synchronized ArrayList<String> getPlayerList(){
        String  uL = getRawPlayerList();

        if(uL.length()!=0) {
            return new ArrayList<String>(Arrays.asList(uL.split(",")));
        }
        return null;
    }

    synchronized void addPlayer(String newPlayer, int status, int time, boolean gift) {
        String pL = getRawPlayerList();

        if((pL==null)||(pL.length()==0)) {
            pL = newPlayer;
        } else {
            pL +=","+newPlayer;
        }

        config.set("UserList", pL);
        setPlayerStatus(newPlayer,status);

        setPlayTime(newPlayer, time);
        setGiftState(newPlayer,gift);

    }

    synchronized int getPlayerStatus(String player) {
        return config.getInt(player+"_status");
    }

    synchronized void setPlayerRegStatus(String player, int status) {
        config.set(player+"_RegStatus",status);
    }

    public int getPlayTime(String playerName) {

        return config.getInt(playerName+"_time");
    }

    public void setPlayTime(String playerName, int time) {

        config.set(playerName+"_time",time);
    }

    public boolean getGiftState(String playerName) {
        return config.getBoolean(playerName+"_gift");
    }

    public void setGiftState(String playerName, boolean giftState) {
        config.set(playerName+"_gift",giftState);
    }

    public void setPlayerStatus(String playerName, int status) {
        config.set(playerName+"_status",status);
    }

    public ArrayList<String> getPlayerListByStatus(int status) {
        ArrayList<String> playerList = getPlayerList();
        ArrayList<String> filteredPlayerList=new ArrayList<String>();
        if(playerList==null)
            return null;

        for(String player:playerList) {
            if(getPlayerStatus(player)==status) {
                filteredPlayerList.add(player);
            }
        }
        return filteredPlayerList;
    }

    public void decrementTime(String player) {
        int time = getPlayTime(player);

        if(time>0) {
            time--;
        }
        setPlayTime(player,time);
    }
}
