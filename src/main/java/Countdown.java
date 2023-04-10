import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import util.Beatmap;
import util.GameType;
import util.Hint;

import java.util.Timer;
import java.util.TimerTask;

public class Countdown extends TimerTask {
    private final MessageChannelUnion textChannel;
    private final Beatmap beatmap;

    private final GameType gameType;

    private final TimerTask endTimer = new TimerTask() {
        @Override
        public void run() {
            textChannel.sendMessage("Time over! The answer was : `" + beatmap.toString() + "`").queue();
        }
    };

    private final TimerTask timer2 = new TimerTask() {
        @Override
        public void run() {
            switch(gameType){
                case PATTERN -> textChannel.sendMessage("**Hint** : " + new Hint(beatmap).getCreatorHint()).queue();
                default -> textChannel.sendMessage("**Hint** : " + new Hint(beatmap).getArtistHint()).queue();
            }

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
    }

    @Override
    public void run(){
        textChannel.sendMessage("**Hint** : " + new Hint(beatmap).getTitleHint()).queue();
    }

    public MessageChannel getTextChannel(){
        return textChannel;
    }

    public void stop(){
        this.cancel();
        timer2.cancel();
        endTimer.cancel();
    }
}
