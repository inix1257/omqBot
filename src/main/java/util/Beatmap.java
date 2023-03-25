package util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.ResultSet;

public class Beatmap {
    public String creator, artist, title, approved_date;
    public int beatmapset_id, beatmap_id;
    public String version = null;

    public int playcount, playcount_answer;

    public double CS;
    public int previewTime;

    public String channelID;

    public Beatmap(JSONObject jsonObject){
//        if (jsonObject.containsKey("beatmap_id")) beatmap_id = jsonObject.get("beatmap_id").toString();
//        if (jsonObject.containsKey("beatmapset_id")) beatmapset_id = jsonObject.get("beatmapset_id").toString();
//        if (jsonObject.containsKey("previewTime")) previewTime = Integer.parseInt(jsonObject.get("previewTime").toString());
//        if (jsonObject.containsKey("version")) version = jsonObject.get("version").toString();
//        if (jsonObject.containsKey("playcount")) playcount = Integer.parseInt(jsonObject.get("playcount").toString());
//        if (jsonObject.containsKey("playcount_answer")) playcount_answer = Integer.parseInt(jsonObject.get("playcount_answer").toString());
//        creator = jsonObject.get("creator").toString();
//        artist = jsonObject.get("artist").toString();
//        title = jsonObject.get("title").toString();
//        approved_date = jsonObject.get("approved_date").toString();
    }

    public Beatmap(ResultSet rs, GameType gameType){
        try {
            beatmapset_id = rs.getInt("beatmapset_id");
            artist = rs.getString("artist");
            title = rs.getString("title");
            creator = rs.getString("creator");
            approved_date = rs.getString("approved_date");
            switch(gameType){
                case MUSIC, BACKGROUND -> {

                }

                case PATTERN -> {
                    beatmap_id = rs.getInt("beatmap_id");
                    previewTime = rs.getInt("previewTime");
                    version = rs.getString("version");
                }
            }


        }catch(Exception e){
            System.out.println("Error while setting up beatmap : " + e);
        }
    }


    public Beatmap(String str){

    }

    public void setChannelID(String str){channelID = str;}

    public Beatmap(){
        beatmapset_id = 0;
        creator = "null";
        artist = "null";
        title = "null";
        approved_date = "2007-01-01";
    }

    @Override
    public String toString() {
        if(version == null) return artist + " - " + title;
        else return artist + " - " + title + " [" + version + "] (" + creator + ")";
    }
}
