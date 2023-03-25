package Video;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import util.Beatmap;
import util.Koohii;
import util.Vector2;

import java.awt.*;
import java.awt.geom.Path2D;
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
        String dir = "./tmpfiles/" + channel.getId() + ".mp4";
        String waitmsg = "Rendering video, please wait...";

        if(beatmap == null){
            channel.sendMessage("Couldn't find beatmap, please try again").queue();
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
                            switch(((Slider)obj.data).sliderType){
                                case "B", "L" -> {
                                    g2d.draw(((Koohii.Slider)obj.data).sliderPath);
                                }
                                case "P" -> {
                                    g2d.draw(perfectCurve((((Slider)obj.data))));
                                }
                            }
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
            channel.sendMessage("An error occurred : " + e).queue();
            throw new RuntimeException(e);
        } finally {
            NIOUtils.closeQuietly(out);
        }
    }

    private Path2D perfectCurve(Slider slider){
        Path2D path = new Path2D.Double();

        Koohii.Vector2 pos1 = slider.sliderPoints.get(0);
        Koohii.Vector2 pos2 = slider.sliderPoints.get(1);
        Koohii.Vector2 pos3 = slider.sliderPoints.get(2);

        double yDelta_a = (pos2.y - pos1.y == 0) ? 0.001 : pos2.y - pos1.y;
        double xDelta_a = (pos2.x - pos1.x == 0) ? 0.001 : pos2.x - pos1.x;
        double yDelta_b = (pos3.y - pos2.y == 0) ? 0.001 : pos3.y - pos2.y;
        double xDelta_b = (pos3.x - pos2.x == 0) ? 0.001 : pos3.x - pos2.x;

        double aSlope = yDelta_a / xDelta_a;
        double bSlope = yDelta_b / xDelta_b;

        double centerX = (aSlope * bSlope * (pos1.y - pos3.y) + bSlope * (pos1.x + pos2.x)
                - aSlope * (pos2.x + pos3.x)) / (2 * (bSlope - aSlope));
        double centerY = -1f * (centerX - (pos1.x + pos2.x) / 2f) / aSlope + (pos1.y + pos2.y) / 2f;

        Vector2 center = new Vector2(centerX, centerY);

        double sliderLength = slider.distance;

        double r = Vector2.getDistance(center, new Vector2(pos1.x, pos1.y)); // radius

        double rad = Math.atan2(pos1.y - centerY, pos1.x - centerX);
        double startX = centerX + r * Math.cos(rad);
        double startY = centerY + r * Math.sin(rad);

        path.moveTo(startX, startY);

        double totalLength = 0.0;
        double rate = 0.1 * getCurveDirection(pos1, pos2, pos3);

        for (double a = rad; true ; a += rate) {
            double prev_x = centerX + r * Math.cos(a - rate);
            double prev_y = centerY + r * Math.sin(a - rate);
            double x = centerX + r * Math.cos(a);
            double y = centerY + r * Math.sin(a);
            path.lineTo(x, y);
            totalLength += Vector2.getDistance(new Vector2(x, y), new Vector2(prev_x, prev_y));
            if(totalLength > sliderLength) break;
        }

        return path;
    }

    public static int getCurveDirection(Koohii.Vector2 p1, Koohii.Vector2 p2, Koohii.Vector2 p3) {
        double x1 = p1.x, y1 = p1.y;
        double x2 = p2.x - x1, y2 = p2.y - y1;
        double x3 = p3.x - x1, y3 = p3.y - y1;
        double crossProduct = (x2 * y3) - (y2 * x3);
        return Double.compare(crossProduct, 0.0);
    }

}
