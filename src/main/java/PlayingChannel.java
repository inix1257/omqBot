import util.Beatmap;
import util.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayingChannel {
    final String channelID;
    Beatmap beatmap;
    GameType gameType;
    ArrayList<Integer> playedBeatmapIDs = new ArrayList<>();
    Map<String, Integer> leaderboard = new HashMap<>(); //userID, point

    public PlayingChannel(String channelID, GameType gameType){
        this.channelID = channelID;
        this.beatmap = new Beatmap();
        this.gameType = gameType;
    }
}
