package Video;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import util.Beatmap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class EmptyVideoRenderer {
    public EmptyVideoRenderer(MessageChannelUnion channel, Beatmap beatmap){
        SeekableByteChannel out = null;

        if(beatmap == null){
            channel.sendMessage("Couldn't find beatmap, please try again").queue();
            return;
        }

        String dir = "./tmpfiles/pattern/" + beatmap.beatmap_id + ".mp4";
        String waitmsg = "Rendering video, please wait...";


        File video = new File(dir);
        if(video.exists()){ // video already exists
            channel.sendFile(new File(dir)).queue();
            return;
        }


        try {
            out = NIOUtils.writableFileChannel(dir);
            AWTSequenceEncoder encoder = new AWTSequenceEncoder(out, Rational.R(20, 1));

            BufferedImage cv = new BufferedImage(512 + 32, 384 + 32, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g2d = cv.createGraphics();

            for (int i = 0; i <= 200 ; i++) {
                g2d.setColor(Color.BLACK);
                encoder.encodeImage(cv);
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
