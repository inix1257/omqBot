import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import util.Beatmap;
import util.GameType;
import util.Hint;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class Countdown extends TimerTask {
    private final MessageChannelUnion textChannel;
    private final Beatmap beatmap;

    private final GameType gameType;

    private final long initTime;

    private TimerTask endTimer = new TimerTask() {
        @Override
        public void run() {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setColor(new Color(120, 120, 255));
            eb.setAuthor("No one got it right...", null, null);
            eb.setTitle("**" + beatmap.toString() + "**", "http://osu.ppy.sh/beatmapsets/" + beatmap.beatmapset_id);
            eb.setDescription("Time over!");
            textChannel.sendMessageEmbeds(eb.build()).queue();
        }
    };

    private TimerTask timer2 = new TimerTask() {
        @Override
        public void run() {
            EmbedBuilder eb = new EmbedBuilder();

            switch(gameType){
                case PATTERN -> eb.setDescription("**Hint** : " + new Hint(beatmap).getCreatorHint());
                default -> eb.setDescription("**Hint** : " + new Hint(beatmap).getArtistHint());
            }

            textChannel.sendMessageEmbeds(eb.build()).queue();

        }
    };

    public Countdown(MessageChannelUnion textChannel, Beatmap beatmap, GameType gameType){
        this.textChannel = textChannel;
        this.beatmap = beatmap;
        this.gameType = gameType;
        Timer timer = new Timer();
        timer.schedule(this, 20000);
        timer.schedule(timer2, 35000);
        timer.schedule(endTimer, 50000);
        initTime = System.currentTimeMillis();
    }

    @Override
    public void run(){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription("**Hint** : " + new Hint(beatmap).getTitleHint());
        textChannel.sendMessageEmbeds(eb.build()).queue();
    }


    public MessageChannel getTextChannel(){
        return textChannel;
    }

    public long stop(){
        this.cancel();
        timer2.cancel();
        endTimer.cancel();

        timer2 = null;
        endTimer = null;

        return this.initTime;
    }
}
