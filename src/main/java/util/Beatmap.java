package util;

import java.sql.ResultSet;

public class Beatmap {
    public String creator, artist, title, approved_date;
    public int beatmapset_id, beatmap_id;
    public String version = null;

    public int playcount, playcount_answer;

    public int previewTime;

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
