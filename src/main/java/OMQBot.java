import Video.VideoRenderer;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.io.FileUtils;
import util.Beatmap;
import util.BeatmapManager;
import util.Config;
import util.GameType;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static util.CheckAnswer.CheckAnswer;

public class OMQBot extends ListenerAdapter {
    private final String[] BOT_IDS = Config.get("BOT_ID").split(" ");
    private final String[] OWNER_IDS = Config.get("OWNER_ID").split(" ");

    private final String tmpPath = "/tmpfiles";

    BeatmapManager beatmapManager;
    public OMQBot() {
    }
    ArrayList<PlayingChannel> playingChannels = new ArrayList<>();
    ArrayList<Countdown> playingCountdown = new ArrayList<>();

    private AudioPlayerManager playerManager;
    private Map<Long, GuildMusicManager> musicManagers;
    @Override
    public void onReady(ReadyEvent e){
        beatmapManager = new BeatmapManager();

        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) throws NullPointerException{
        String msg = event.getMessage().getContentRaw();
        String[] command = msg.split(" ", 2);
        String channelID = event.getMessage().getChannel().getId();

        if(event.getAuthor().isBot() && !Arrays.asList(BOT_IDS).contains(event.getAuthor().getId())) return;


        if(Arrays.asList(BOT_IDS).contains(event.getAuthor().getId()) && event.getMessage().getEmbeds().size() > 0
        && event.getMessage().getEmbeds().get(0).getDescription() != null
        && event.getMessage().getEmbeds().get(0).getDescription().equals("Time over!")){

            PlayingChannel pc = getPlayingChannel(event.getChannel().getId());
            beatmapManager.updateBeatmap(pc.beatmap, pc.gameType, false);

            event.getChannel().getHistory().retrievePast(6)
                    .queue(message -> { // success callback
                        boolean check = false;
                        boolean checkAnswer = false;
                        for(Message m : message){
                            if (!Arrays.asList(BOT_IDS).contains(m.getAuthor().getId())) check = true;
                            if (CheckAnswer(event.getMessage().getContentRaw(), pc.beatmap.title) >= 0.8f) checkAnswer = true;
                        }

                        if(!check){
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Seems like no one is playing, shutting down omq session...");
                            event.getChannel().sendMessageEmbeds(eb.build()).queue();
                            stopPlaying(event.getChannel());
                        }else{
                            if(checkAnswer) return;
                            stopCountdown(channelID);
                            setupGame(event.getChannel());
                        }
                    });

            return;
        }

        switch(command[0]){
            case "!pass", "!skip", "!omqpass", "!omqskip" -> {
                if(!isPlaying(channelID)) return;
                PlayingChannel playingChannel = getPlayingChannel(channelID);
                Beatmap beatmap = playingChannel.beatmap;
                beatmapManager.updateBeatmap(beatmap, playingChannel.gameType, false);

                double answerRate = 0;
                switch(playingChannel.gameType){
                    case PATTERN -> answerRate = beatmapManager.getAnswerRate(beatmap.beatmap_id, playingChannel.gameType);
                    default -> answerRate = beatmapManager.getAnswerRate(beatmap.beatmapset_id, playingChannel.gameType);
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(new Color(120, 120, 255));
                eb.setDescription("Difficulty : **" + Math.round((1 - answerRate) * 100) + "**/100\n");
                eb.setAuthor("No one got it right...", null, null);
                eb.setTitle("**" + beatmap.toString() + "**", "http://osu.ppy.sh/beatmapsets/" + beatmap.beatmapset_id);
                event.getChannel().sendMessageEmbeds(eb.build()).queue();
                stopCountdown(channelID);
                setupGame(event.getChannel());
            }

            case "!close", "!stop", "!omqclose", "!omqstop" -> {
                if(!isPlaying(channelID)) return;
                stopPlaying(event.getChannel());
            }

            case "!omqhelp" -> onCommandHelp(event);

            case "!omqlb", "!omqleaderboard", "!omqrank", "!omqranking", "!omqrankings" ->{
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("**Leaderboard for osu! Music Quiz**", null);
                eb.setColor(new Color(190, 120, 120));
                String result[] = beatmapManager.getLeaderboard();
                String str = "";
                for(String s : result){
                    str += s;
                }
                eb.setDescription(str);
                event.getChannel().sendMessageEmbeds(eb.build()).queue();
            }

            case "!mapcount" -> event.getChannel().sendMessage("OMQ library currently has **" + beatmapManager.getBeatmapCount(GameType.MUSIC) + "** maps").queue();

            case "!mapcount_pattern" -> event.getChannel().sendMessage("OMQ (Pattern) library currently has **" + beatmapManager.getBeatmapCount(GameType.PATTERN) + "** maps").queue();
        }

        if(Arrays.asList(OWNER_IDS).contains(event.getMessage().getAuthor().getId())){ //admin zone
            onAdminCommand(event, command);
        }

        if(event.getAuthor().isBot()) return;

        if(isPlaying(channelID)){
            checkAnswer(event);
        }
    }

    private void onCommandHelp(MessageReceivedEvent event){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("**Available commands for OMQ (osu! Music Quiz) bot**", null);
        eb.setDescription("**/omq [gametype]** : Starts playing guessing game, supported gamemodes : MUSIC, BACKGROUND, PATTERN\n" +
                "**!skip/pass** : Shows the answer of the current song and skips to the next song.\n" +
                "**!stop/close** : Manually closes current OMQ session.\n" +
                "**!omqlb, !omqleaderboard** : Shows global omq leaderboard");
        eb.setFooter("Contact : Luscent (osu!) / @luscentos (twitter)", "http://a.ppy.sh/2688581");
        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    private void onAdminCommand(MessageReceivedEvent event, String[] command){
        String msg = event.getMessage().getContentRaw();

        switch(command[0]){
            case "!shutdown" -> {
                event.getChannel().sendMessage("Shutting down...").queue();
                event.getJDA().shutdown();
            }
            case "!addmap" -> {
                if (command.length == 1) {
                    event.getChannel().sendMessage("Please provide valid beatmapset_id").queue();
                    return;
                }
                beatmapManager.addBeatmap(command[1], event.getChannel());
            }
            case "!removemap" -> {
                if (command.length == 1) {
                    event.getChannel().sendMessage("Please provide valid beatmapset_id").queue();
                    return;
                }
                beatmapManager.removeBeatmap(Integer.parseInt(command[1]), event.getChannel(), GameType.MUSIC);
            }
            case "!addmap_pattern" -> {
                if (command.length == 1) {
                    event.getChannel().sendMessage("Please provide valid beatmapset_id").queue();
                    return;
                }
                beatmapManager.addBeatmap_pattern(command[1], event.getChannel());
            }
            case "!removemap_pattern" -> {
                if (command.length == 1) {
                    event.getChannel().sendMessage("Please provide valid beatmapset_id").queue();
                    return;
                }
                beatmapManager.removeBeatmap(Integer.parseInt(command[1]), event.getChannel(), GameType.PATTERN);
            }
            case "!announce" -> {
                if (command.length == 1) {
                    event.getChannel().sendMessage("Please provide valid command").queue();
                    return;
                }
                for (PlayingChannel pc : playingChannels) {
                    try {
                        String channelID = pc.channelID;
                        event.getJDA().getTextChannelById(channelID).sendMessage("**Announcement :**\n" + msg.replace("!announce ", "")).queue();
                    }catch(Exception e){

                    }
                }
            }
            case "!playingservers" -> event.getChannel().sendMessage("Currently playing in " + playingChannels.size() + " channels").queue();
            case "!render" -> {
                Thread t = new Thread(()-> new VideoRenderer(event.getChannel(), Integer.parseInt(command[1])));
                t.start();
            }
        }

        if (event.getChannel().getId().equals(Config.get("TESTCHANNEL"))){
            beatmapManager.addBeatmap_pattern(command[0], event.getChannel());
        }

    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event){
        String optionName = event.getOption("gametype").getAsString();

        EmbedBuilder eb = new EmbedBuilder();

        if(isPlaying(event.getChannel().getId())){
            eb.setTitle("This channel is already playing omq!");
            eb.setColor(new Color(255, 40, 40));
            eb.setDescription("Please type !stop first if you want to start a new game");
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        eb.setTitle("**Ranking for this OMQ session**", null);
        eb.setDescription("Type **!skip** to skip current song, **!stop** to stop playing");
        eb.setColor(new Color(190, 120, 120));

        switch (optionName){
            case "Music" -> {
                eb.setTitle("**Guess the name of the song below!**", null);
                playingChannels.add(new PlayingChannel(event.getChannel().getId(), GameType.MUSIC));
            }
            case "Background" -> {
                eb.setTitle("**Guess the name of the beatmap below!**", null);
                playingChannels.add(new PlayingChannel(event.getChannel().getId(), GameType.BACKGROUND));
            }
            case "Pattern" -> {
                eb.setTitle("**Guess the name of the beatmap below!**", null);
                playingChannels.add(new PlayingChannel(event.getChannel().getId(), GameType.PATTERN));
            }
        }

        event.replyEmbeds(eb.build()).queue();

        setupGame(event.getChannel());
    }

    private final String[] gameTypes = new String[]{"Music", "Background", "Pattern"};
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event){
        if (event.getName().equals("omq") && event.getFocusedOption().getName().equals("gametype")) {
            List<Command.Choice> options = Stream.of(gameTypes)
                    .filter(gameType -> gameType.startsWith(event.getFocusedOption().getValue())) // only display words that start with the user's current input
                    .map(gameType -> new Command.Choice(gameType, gameType)) // map the words to choices
                    .collect(Collectors.toList());
            event.replyChoices(options).queue();
        }
    }

    private void checkAnswer(MessageReceivedEvent event){
        PlayingChannel pc = getPlayingChannel(event.getChannel().getId());

        double points = CheckAnswer(event.getMessage().getContentRaw(), pc.beatmap.title);
        String username = event.getMessage().getAuthor().getName();
        String userid = event.getMessage().getAuthor().getId();

        if(points >= 0.8f){
            pc.beatmap.playcount_answer++;
            long initTime = stopCountdown(pc.channelID);
            String timeTaken = String.format("%.1f", (System.currentTimeMillis() - initTime)/1000f) + "s";

            double answerRate = beatmapManager.updateLeaderboard(userid, username, pc.gameType, pc.beatmap);
            double diffBonus = 1.5d - answerRate;
            double timeBonus = getTimeBonus(initTime);
            double totalBonus = Math.round(diffBonus * timeBonus * 10d) / 10d;

            beatmapManager.updateBeatmap(pc.beatmap, pc.gameType, true);
            pc.leaderboard.putIfAbsent(username, 0d); // add new if null
            pc.leaderboard.put(username, pc.leaderboard.get(username) + totalBonus);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("**" + pc.beatmap.toString() + "**", "http://osu.ppy.sh/beatmapsets/" + pc.beatmap.beatmapset_id);
            eb.setColor(new Color(120, 120, 255));
            eb.setDescription("Time Taken : " + timeTaken + " / Difficulty : **" + Math.round((1 - answerRate) * 100) + "**/100\n");
            eb.setFooter("+" + totalBonus + " points", null);
            eb.setAuthor(event.getAuthor().getName() + " got it right!", null, event.getAuthor().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(eb.build()).queue();

            setupGame(event.getChannel());
            return;
        }

        double artistpoints = CheckAnswer(event.getMessage().getContentRaw(), pc.beatmap.artist);

        if(artistpoints >= 0.9) event.getChannel().sendMessage(event.getMessage().getAuthor().getName() + " got the artist name right! (" + pc.beatmap.artist + ")").queue();
    }

    private Beatmap setupGame(MessageChannelUnion channel){
        Beatmap beatmap = null;
        PlayingChannel playingChannel = getPlayingChannel(channel.getId());
        try {
            if(playingChannel == null) return null;
            beatmap = beatmapManager.getRandomBeatmap(playingChannel.gameType);
            if(beatmapManager.getBeatmapCount(playingChannel.gameType) == playingChannel.playedBeatmapIDs.size()){
                channel.sendMessage("This channel has played all maps!").queue();
                channel.sendMessage("Closing omq session...").queue();
                stopPlaying(channel);
                return null;
            }

            for(int s : playingChannel.playedBeatmapIDs){
                if(s == beatmap.beatmapset_id && ((playingChannel.gameType == GameType.MUSIC) || (playingChannel.gameType == GameType.BACKGROUND))){
                    // check if this map has been played already
                    return setupGame(channel);
                }
            }

            for(int s : playingChannel.playedBeatmapIDs){
                if(s == beatmap.beatmap_id && playingChannel.gameType == GameType.PATTERN){
                    // check if this map has been played already
                    return setupGame(channel);
                }
            }

            beatmap.playcount++;

            playingChannel.beatmap = beatmap;

            switch(playingChannel.gameType){
                case MUSIC, BACKGROUND -> playingChannel.playedBeatmapIDs.add(beatmap.beatmapset_id);
                case PATTERN -> playingChannel.playedBeatmapIDs.add(beatmap.beatmap_id);
            }

            URL url = null;
            File file = null;
            Beatmap finalBeatmap = beatmap;
            switch(playingChannel.gameType){
                case MUSIC -> {
                    url = new URL("https://b.ppy.sh/preview/" + beatmap.beatmapset_id + ".mp3");
                    file = new File(tmpPath + "/preview/" + channel.getId() + ".mp3");
                    //loadAndPlay((TextChannel) channel, url.toString());
                }
                case BACKGROUND -> {
                    url = new URL("https://assets.ppy.sh/beatmaps/" + beatmap.beatmapset_id + "/covers/raw.jpg");
                    file = new File(tmpPath + "/background/" + channel.getId() + ".jpg");
                }
                case PATTERN -> {
                    String dir = tmpPath + "/pattern/" + beatmap.beatmap_id + ".mp4";
                    file = new File(dir);
                    playingCountdown.add(new Countdown(channel, beatmap, playingChannel.gameType));
                    channel.sendFile(file).queue();
                    return beatmap;
                }
            }

            playingCountdown.add(new Countdown(channel, beatmap, playingChannel.gameType));
            FileUtils.copyURLToFile(url, file);

            channel.sendFile(file).queue();
        } catch (IOException e) {
            e.printStackTrace();
            channel.sendMessage("An error occurred, trying again...").queue();
            setupGame(channel);
        } catch (InsufficientPermissionException e){

        }
        return beatmap;
    }

    boolean isPlaying(String channelid){
        for (PlayingChannel pc : playingChannels){
            if(pc.channelID.equals(channelid)) return true;
        }
        return false;
    }

    private PlayingChannel getPlayingChannel(String channelID){
        for(PlayingChannel pc : playingChannels){
            if(pc.channelID.equals(channelID)) return pc;
        }
        return null;
    }

    private void stopPlaying(MessageChannel messageChannel){
        String channelID = messageChannel.getId();
        PlayingChannel playingChannel = getPlayingChannel(channelID);

        if(playingChannel == null) return;

        List<Map.Entry<String, Double>> list_entries = new ArrayList<>(playingChannel.leaderboard.entrySet());
        list_entries.sort((obj1, obj2) -> obj2.getValue().compareTo(obj1.getValue()));

        String str = "";

        int counter = 0;
        for(Map.Entry<String, Double> entry : list_entries) {
            String username = entry.getKey();
            double point = entry.getValue();
            if(counter == 0){
                str += ":first_place: ";
            }else if(counter == 1){
                str += ":second_place: ";
            }else if(counter == 2){
                str += ":third_place: ";
            }
            str += ("**" + username + "** : " + Math.round(point*10)/10d + " pt");
            str += (point == 1) ? "\n" : "s\n";
            counter++;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("**Ranking for this OMQ session**", null);
        eb.setColor(new Color(190, 120, 120));
        eb.setDescription(str);
        //eb.setAuthor("Thanks for playing! This bot was made by Luscent", "http://osu.ppy.sh/users/2688581", "http://a.ppy.sh/2688581");
        //.setFooter("Thanks for playing! This bot was made by Luscent\n http://osu.ppy.sh/users/2688581", "http://a.ppy.sh/2688581");
        //eb.setAuthor(event.getAuthor().getName() + " got it right!", null, event.getAuthor().getAvatarUrl());

        messageChannel.sendMessageEmbeds(eb.build()).queue();

        playingChannels.remove(playingChannel);
        stopCountdown(channelID);

        try{
            File mp3file = new File(tmpPath + "/preview/" + playingChannel.channelID + ".mp3");
            mp3file.delete();

            File bgfile = new File(tmpPath + "/background/" + playingChannel.channelID + ".jpg");
            bgfile.delete();
        }catch(Exception e){

        }
    }

    private double getTimeBonus(long initTime){
        double timeTaken = System.currentTimeMillis() - initTime;
        timeTaken /= 1000d;
            return -(1-Math.pow((1-0.01*timeTaken), 2)) + 1.3;
    }

    private long stopCountdown(String channelID){
        long initTime = 0;
        for(Countdown c : playingCountdown){
            if(c.getTextChannel().getId().equals(channelID)){
                initTime = c.stop();
                break;
            }
        }
        playingCountdown.removeIf(c -> c.getTextChannel().getId().equals(channelID));
        return initTime;
    }

    private void loadAndPlay(final TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

                play(channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(channel.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        guild.getAudioManager().openAudioConnection(guild.getVoiceChannelById("960903697922146314"));

        musicManager.scheduler.queue(track);
    }

    private void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped to next track.").queue();
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }
}
