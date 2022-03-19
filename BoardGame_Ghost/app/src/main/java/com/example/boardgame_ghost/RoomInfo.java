package com.example.boardgame_ghost;

public class RoomInfo {
    private String id;
    private String name;
    private String playing;
    private int playerCnt;
    private String use_password;

    public RoomInfo(String id, String name, String playing, int cnt, String use_password){
        this.id = id;
        this.name = name;
        this.playing = playing;
        this.playerCnt = cnt;
        this.use_password = use_password;
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPlaying() {
        return playing;
    }

    public void setPlaying(String playing) {
        this.playing = playing;
    }

    public int getPlayerCnt() {
        return playerCnt;
    }

    public void setPlayerCnt(int playerCnt) {
        this.playerCnt = playerCnt;
    }

    public String getUse_password() {
        return use_password;
    }

    public void setUse_password(String use_password) {
        this.use_password = use_password;
    }
}
