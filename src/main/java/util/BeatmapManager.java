package util;

import net.dv8tion.jda.api.entities.MessageChannel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.*;

public class BeatmapManager {
    private static final String SQLITE_JDBC_DRIVER = "org.sqlite.JDBC";
    private static final String SQLITE_FILE_DB_URL = "jdbc:sqlite:beatmap.db";
    private static final String SQLITE_MEMORY_DB_URL = "jdbc:sqlite::memory";

    //  - Database 옵션 변수
    private static final boolean OPT_AUTO_COMMIT = false;
    private static final int OPT_VALID_TIMEOUT = 500;

    // 변수 설정
    //   - Database 접속 정보 변수
    private Connection conn = null;
    private String driver = null;
    private String url = null;

    public BeatmapManager() {
        // JDBC Driver 설정
        this.driver = SQLITE_JDBC_DRIVER;
        this.url = SQLITE_FILE_DB_URL;
        createConnection();
    }

    public Connection createConnection() {
        try {
            Class.forName(this.driver);

            this.conn = DriverManager.getConnection(this.url);

            System.out.println("BEATMAP DB CONNECTED");
            this.conn.setAutoCommit(OPT_AUTO_COMMIT);

//            int beatmapset_id;
//            int beatmap_id;
//            String artist;
//            String title;
//            int playcount = 0;
//            int answercount = 0;
//            String approved_date;
//            String creator;
//            String version;
//            int previewTime;
//
//
//
//
//            for(Object obj : this.beatmapArray_pattern){
//                JSONObject jsonObject = (JSONObject)obj;
//                artist = jsonObject.get("artist").toString();
//                title = jsonObject.get("title").toString();
//                beatmapset_id = Integer.parseInt(jsonObject.get("beatmapset_id").toString());
//                beatmap_id = Integer.parseInt(jsonObject.get("beatmap_id").toString());
//                previewTime = Integer.parseInt(jsonObject.get("previewTime").toString());
//                approved_date = jsonObject.get("approved_date").toString();
//                creator = jsonObject.get("creator").toString();
//                version = jsonObject.get("version").toString();
//
//                String str = "insert into beatmap_pattern(beatmapset_id, beatmap_id, artist, title, version, creator, previewTime, approved_date, playcount, playcount_answer)" +
//                                " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//                        //("insert into beatmap(artist, title, beatmapset_id) values ('asdas', 'asdasda', '4423fds')");
//                PreparedStatement statement = conn.prepareStatement(str);
//                statement.setInt(1, beatmapset_id);
//                statement.setInt(2, beatmap_id);
//                statement.setString(3, artist);
//                statement.setString(4, title);
//                statement.setString(5, version);
//                statement.setString(6, creator);
//                statement.setInt(7, previewTime);
//                statement.setString(8, approved_date);
//                statement.setInt(9, 0);
//                statement.setInt(10, 0);
//                System.out.println(statement);
//                statement.executeUpdate();
//
//            }
//
//            conn.commit();


        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("DB CONNECTION FAILED : " + e);
            e.printStackTrace();
        }

        return this.conn;
    }

    public void updateLeaderboard(String userid, String username, GameType gameType){
        int bonusPoint = 0;

        switch(gameType){
            case MUSIC -> bonusPoint = 1;
            case BACKGROUND -> bonusPoint = 1;
            case PATTERN -> bonusPoint = 1;
        }

        try {
            String str =
                    "INSERT OR IGNORE INTO leaderboard (userid, username, point) VALUES (?, ?, 0)";
            PreparedStatement statement = conn.prepareStatement(str);

            statement.setLong(1, Long.parseLong(userid));
            statement.setString(2, username);

            System.out.println("current statement : " + statement);

            statement.executeUpdate();

            str = "UPDATE leaderboard SET point = point + ? WHERE userid = ?";
            statement = conn.prepareStatement(str);
            statement.setInt(1, bonusPoint);
            statement.setLong(2, Long.parseLong(userid));
            statement.executeUpdate();
            conn.commit();
            statement.close();
        }catch (SQLException e){
            System.out.println("SQLException error : " + e);
        }
    }

    public String getLeaderboard(){
        String result = "**Leaderboard for osu! Music Quiz**\n";
        int counter = 0;
        try {
            String str =
                    "SELECT * FROM leaderboard ORDER BY point DESC LIMIT 10";
            PreparedStatement statement = conn.prepareStatement(str);

            ResultSet rs = statement.executeQuery();

            while(rs.next()){
                if(counter == 0){
                    result += ":first_place: ";
                }else if(counter == 1){
                    result += ":second_place: ";
                }else if(counter == 2){
                    result += ":third_place: ";
                }else{
                    result += "**[#" + (counter+1) + "]**";
                }
                result += " **" + rs.getString("username") + "** : " + rs.getInt("point") + " points\n";
                counter++;
            }

            rs.close();

        }catch(SQLException e){}

        return result;
    }

    public void updateBeatmap(Beatmap beatmap, GameType gameType, boolean isAnswer){
        int beatmapID = beatmap.beatmap_id;
        int beatmapsetID = beatmap.beatmapset_id;

        String str = "UPDATE beatmap SET playcount = playcount + 1 WHERE beatmapset_id = ?";

        switch(gameType){
            case PATTERN -> str = "UPDATE beatmap_pattern SET playcount = playcount + 1 WHERE beatmap_id = ?";
        }

        try {
            PreparedStatement statement = conn.prepareStatement(str);

            switch(gameType){
                case PATTERN -> statement.setInt(1, beatmapID);
                default -> statement.setInt(1, beatmapsetID);
            }


            statement.executeUpdate();

            if(isAnswer){
                str = str.replace("playcount", "playcount_answer");
                statement = conn.prepareStatement(str);

                switch(gameType){
                    case PATTERN -> statement.setInt(1, beatmapID);
                    default -> statement.setInt(1, beatmapsetID);

                }
                statement.executeUpdate();
            }

            conn.commit();
            statement.close();
        }catch (SQLException e){
            System.out.println("SQLException error : " + e);
        }
    }

    public void closeConnection() {
        try {
            if( this.conn != null ) {
                this.conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            this.conn = null;

            // 로그 출력
            System.out.println("CLOSED");
        }
    }

    public Connection ensureConnection() {
        try {
            if( this.conn == null || this.conn.isValid(OPT_VALID_TIMEOUT) ) {
                closeConnection();      // 연결 종료
                createConnection();     // 연결
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return this.conn;
    }

    // DB 연결 객체 가져오기
    public Connection getConnection() {
        return this.conn;
    }

    public Beatmap getRandomBeatmap(GameType gameType){
        Beatmap beatmap = null;
        try {
            String str =
                    "SELECT * FROM beatmap ORDER BY RANDOM() LIMIT 1";
            if(gameType == GameType.PATTERN) str = "SELECT * FROM beatmap_pattern ORDER BY RANDOM() LIMIT 1";
            PreparedStatement statement = conn.prepareStatement(str);

            ResultSet rs = statement.executeQuery();
            return new Beatmap(rs, gameType);

        }catch(SQLException e){}

        return beatmap;
    }

    public Beatmap getBeatmap(int ID, GameType gameType){
        Beatmap beatmap = null;
        try {
            String str =
                    "SELECT * FROM beatmap WHERE beatmapset_id = ?";
            if(gameType == GameType.PATTERN) str = "SELECT * FROM beatmap_pattern WHERE beatmap_id = ?";
            PreparedStatement statement = conn.prepareStatement(str);
            statement.setInt(1, ID);

            ResultSet rs = statement.executeQuery();

            return new Beatmap(rs, gameType);

        }catch(SQLException e){
            System.out.println("This map doesn't exist in database! : " + ID);
        }

        return beatmap;
    }

    public void addBeatmap(String beatmapsetID, MessageChannel messageChannel){

        if(beatmapsetID.contains("osu.ppy.sh")) beatmapsetID = beatmapsetID.split("/")[4].split("#")[0];

        try {
            String api = Config.get("API");
            URL url = new URL("https://osu.ppy.sh/api/get_beatmaps?k=" + api + "&s=" + beatmapsetID);
            BufferedReader bf; String line; String result="";
            bf = new BufferedReader(new InputStreamReader(url.openStream()));

            while((line=bf.readLine())!=null){
                result=result.concat(line);
            }

            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray) parser.parse(result);
            JSONObject obj = (JSONObject)arr.get(0);

            int beatmapset_id = Integer.parseInt(obj.get("beatmapset_id").toString());
            String artist = obj.get("artist").toString();
            String title = obj.get("title").toString();
            String creator = obj.get("creator").toString();
            String approved_date = obj.get("approved_date").toString();

            String str =
                    "INSERT OR IGNORE INTO beatmap (beatmapset_id, artist, title, creator, approved_date, playcount, playcount_answer) " +
                            "VALUES (?, ?, ?, ?, ?, 0, 0)";
            PreparedStatement statement = conn.prepareStatement(str);

            statement.setInt(1, beatmapset_id);
            statement.setString(2, artist);
            statement.setString(3, title);
            statement.setString(4, creator);
            statement.setString(5, approved_date);

            System.out.println("current statement : " + statement.toString());

            statement.executeUpdate();
            conn.commit();

            statement.close();

            messageChannel.sendMessage("Successfully added beatmapsetid **" + beatmapset_id +"** `" + artist + " - " + title + "`").queue();
        } catch (ParseException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addBeatmap_pattern(String beatmapID, MessageChannel messageChannel){

        if(beatmapID.contains("osu.ppy.sh")) beatmapID = beatmapID.split("/")[5];
        try {
            String api = Config.get("API");
            URL url = new URL("https://osu.ppy.sh/api/get_beatmaps?k=" + api + "&b=" + beatmapID);
            BufferedReader bf; String line = ""; String result="";
            bf = new BufferedReader(new InputStreamReader(url.openStream()));

            while((line=bf.readLine())!=null){
                result=result.concat(line);
            }

            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray) parser.parse(result);
            JSONObject obj = (JSONObject)arr.get(0);

            int beatmapset_id = Integer.parseInt(obj.get("beatmapset_id").toString());
            int beatmap_id = Integer.parseInt(obj.get("beatmap_id").toString());
            String artist = obj.get("artist").toString();
            String title = obj.get("title").toString();
            String creator = obj.get("creator").toString();
            String version = obj.get("version").toString();
            String approved_date = obj.get("approved_date").toString();

            URL osuURL = new URL("https://osu.ppy.sh/osu/" + beatmap_id);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(osuURL.openStream()));
            Koohii.Map osumap = new Koohii.Parser().map(in);

            int previewTime = osumap.previewTime;

            addBeatmap(String.valueOf(beatmapset_id), messageChannel);

            String str =
                    "INSERT OR IGNORE INTO beatmap_pattern (beatmapset_id, beatmap_id, artist, title, version, creator, previewTime, approved_date, playcount, playcount_answer) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0)";
            PreparedStatement statement = conn.prepareStatement(str);

            statement.setInt(1, beatmapset_id);
            statement.setInt(2, beatmap_id);
            statement.setString(3, artist);
            statement.setString(4, title);
            statement.setString(5, version);
            statement.setString(6, creator);
            statement.setInt(7, previewTime);
            statement.setString(8, approved_date);

            System.out.println("current statement : " + statement.toString());

            statement.executeUpdate();
            conn.commit();

            statement.close();

            messageChannel.sendMessage("Successfully added beatmapID **" + beatmapID +"** `" + obj.get("artist") + " - " + obj.get("title") + "` (PATTERN)").queue();
        } catch (ParseException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeBeatmap(int ID, MessageChannel messageChannel, GameType gameType){
        try {
            String str =
                    "DELETE FROM beatmap WHERE beatmapset_id = ?";
            if(gameType == GameType.PATTERN) str = "DELETE FROM beatmap_pattern WHERE beatmap_id = ?";
            PreparedStatement statement = conn.prepareStatement(str);
            statement.setInt(1, ID);

            statement.executeUpdate();
            conn.commit();

            statement.close();

            switch(gameType){
                case PATTERN -> messageChannel.sendMessage("Successfully removed beatmapID **" + ID +"**").queue();
                default -> messageChannel.sendMessage("Successfully removed beatmapsetID **" + ID +"**").queue();
            }

        }catch(SQLException e){
            System.out.println("An error occured while deleteing a beatmap : " + ID);
        }
    }

    public int getBeatmapCount(GameType gameType){
        int count = 0;
        try{
            String str =
                    "SELECT COUNT(*) AS COUNT FROM beatmap";
            if(gameType == GameType.PATTERN) str = "SELECT COUNT(*) AS COUNT FROM beatmap_pattern";
            PreparedStatement statement = conn.prepareStatement(str);
            ResultSet rs = statement.executeQuery();

            count = rs.getInt("COUNT");
            statement.close();
            rs.close();
        }catch(Exception e){

        }
        return count;
    }
}
