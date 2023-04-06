package Video;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import util.Beatmap;
import util.Koohii;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Random;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;
import static util.Koohii.*;

public class VideoRenderer {
    public VideoRenderer(MessageChannelUnion channel, Beatmap beatmap){
        SeekableByteChannel out = null;

        if(beatmap == null){
            channel.sendMessage("Couldn't find beatmap, please try again").queue();
            return;
        }

        String dir = "./tmpfiles/pattern/" + beatmap.beatmap_id + ".mp4";
        String waitmsg = "Rendering video, please wait...";

        File video = new File(dir);
        if(video.exists()){ // video already exists
            //channel.sendFile(new File(dir)).queue();
            return;
        }

        final Message[] msg = new Message[1];

        channel.sendMessage(waitmsg).queue((message -> msg[0] = message));

        try {
            out = NIOUtils.writableFileChannel(dir);
            AWTSequenceEncoder encoder = new AWTSequenceEncoder(out, Rational.R(20, 1));

            URL osuURL = new URL("https://osu.ppy.sh/osu/" + beatmap.beatmap_id);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(osuURL.openStream()));
            Koohii.Map osumap = new Koohii.Parser().map(in);

            int mapstartTime = 99999;
            int mapendTime = 0;

            for(Koohii.HitObject obj : osumap.objects){
                if(obj.time > mapendTime) mapendTime = (int)obj.time;
                if(obj.time < mapstartTime) mapstartTime = (int)obj.time;
            }

            BufferedImage cv = new BufferedImage(512 + 32, 384 + 32, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g2d = cv.createGraphics();

            float circleSize = 54.4f - 4.48f * osumap.cs;
            circleSize *= 1.7;
            float approachingTime = 1950 - osumap.ar * 150;

            BasicStroke basicStroke = new BasicStroke(2f, CAP_ROUND, JOIN_ROUND);
            BasicStroke sliderStroke = new BasicStroke(circleSize, CAP_ROUND, JOIN_ROUND);

            int startTime = new Random().nextInt(mapstartTime, mapendTime - mapstartTime);
            startTime = beatmap.previewTime + new Random().nextInt(-5000, 5000);

            int time;
            int posX, posY;
            float approachSize;
            int alpha;

            for (int i = 0; i <= 200 ; i++) {
                time = (int)(startTime + i * 50d);
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, 512 + 32, 384 + 32);

                g2d.setColor(Color.WHITE);

                for(int j = osumap.objects.size() - 1 ; j >= 0 ; j--){
                    Koohii.HitObject obj = osumap.objects.get(j);
                    posX = (int)obj.pos.x + 16;
                    posY = (int)obj.pos.y + 16;
                    if(obj.time - approachingTime <= time){
                        if(obj.time + 1000 < time && (((obj.type & OBJ_CIRCLE) != 0) || ((obj.type & OBJ_SPINNER) != 0))) continue;
                        if((obj.type & OBJ_SLIDER) != 0 && ((Koohii.Slider)obj.data).sliderTime + obj.time <= time) continue;
                        //ignore out of scope objects

                        approachSize = ((float)obj.time - time) / approachingTime * 3.5f + 1f;
                        alpha = (int)(time - obj.time + approachingTime);
                        alpha = Math.min(Math.max(alpha, 0), 255);

                        if(((obj.type & OBJ_CIRCLE) != 0) && obj.time < time){
                            alpha = 255 - (int)(time - obj.time);
                            alpha = Math.min(Math.max(alpha, 0), 255);
                        }

                        if ((obj.type & OBJ_SLIDER) != 0) {
                            g2d.setColor(new Color(80, 80, 80, alpha));
                            g2d.setStroke(sliderStroke);
                            g2d.translate(16, 16);
                            g2d.draw(((Koohii.Slider)obj.data).sliderPath);
                            g2d.translate(-16, -16);
                        }

                        g2d.setColor(new Color(255, 255, 255, alpha));
                        g2d.fillOval((int)(posX - circleSize / 2), (int)(posY - circleSize / 2), (int)(circleSize), (int)(circleSize));

                        approachSize *= circleSize;
                        g2d.setStroke(basicStroke);

                        if(approachSize >= circleSize) g2d.drawOval((int)(posX - approachSize / 2), (int)(posY - approachSize / 2), (int)approachSize, (int)approachSize);
                    }
                }
                encoder.encodeImage(cv);
                if(i % 40 == 0 && msg[0] != null){
                    msg[0].editMessage(waitmsg + " (" + i/2 + "%)").queue();
                }
            }
            g2d.dispose();
            encoder.finish();
            channel.sendFile(new File(dir)).queue();
        } catch (Exception e) {
            System.out.println("An error occurred : " + e);
            throw new RuntimeException(e);
        } finally {
            NIOUtils.closeQuietly(out);
        }
    }



}
