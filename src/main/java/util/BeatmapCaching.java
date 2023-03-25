package util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

public class BeatmapCaching {

    public static void main(String[] args) {

        String last_date = "2007-01-01";

        JSONArray overallArr = new JSONArray();

        for(int i=0; i<200; i++) {
            try {
                StringBuilder sb = new StringBuilder();
                String api = Config.get("API");
                URL url = new URL("https://osu.ppy.sh/api/get_beatmaps?k=" + api + "&since=" + last_date + "&m=0)");
                BufferedReader bf; String line = ""; String result="";
                bf = new BufferedReader(new InputStreamReader(url.openStream()));

                while((line=bf.readLine())!=null){
                    result=result.concat(line);
                }


                JSONParser parser = new JSONParser();
                JSONArray arr = (JSONArray) parser.parse(result);
                String previd = "0";
                for(int j=0; j<arr.size(); j++){
                    JSONObject obj = (JSONObject)arr.get(j);

                    if(obj.get("beatmapset_id").equals(previd)){
                        //arr.remove(j);
                        continue;
                    } //ignore duplicating set

                    if((Integer.parseInt(obj.get("playcount").toString()) >= 1000000)
                    || Integer.parseInt(obj.get("favourite_count").toString()) >= 1000){
                        JSONObject tmp = new JSONObject();
                        tmp.put("approved_date", obj.get("approved_date"));
                        tmp.put("artist", obj.get("artist"));
                        tmp.put("title", obj.get("title"));
                        tmp.put("creator", obj.get("creator"));
                        tmp.put("beatmapset_id", obj.get("beatmapset_id"));
                        overallArr.add(tmp);
                    }




                    //System.out.println(obj.get("approved_date").toString().split(" ")[0] + " | " + obj.get("artist") + " - " + obj.get("title") + " mapped by " + obj.get("creator"));
                    previd = obj.get("beatmapset_id").toString();
                    last_date = obj.get("approved_date").toString().split(" ")[0];

                }

                System.out.println("Currently working on : " + i);
                Thread.sleep(60);

            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            FileWriter file = new FileWriter(Config.get("JSON_LOCATION"));
            file.write(overallArr.toJSONString());
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
