package util;

public class Hint {
    private final Beatmap beatmap;

    public Hint(Beatmap beatmap){
        this.beatmap = beatmap;
    }

    public String getTitleHint(){
        return "Title starts with the letter **" + beatmap.title.charAt(0) + "**!";
    }

    public String getArtistHint(){
        return "The artist of this song is **" + beatmap.artist + "**!";
    }
}
