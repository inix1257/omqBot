import Video.VideoRenderer;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.io.FileUtils;
import util.Beatmap;
import util.BeatmapManager;
import util.Config;
import util.GameType;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static util.CheckAnswer.CheckAnswer;

public class OMQBot extends ListenerAdapter {
    private final String[] BOT_IDS = Config.get("BOT_ID").split(" ");
    private final String[] OWNER_IDS = Config.get("OWNER_ID").split(" ");

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
    public void onMessageReceived(MessageReceivedEvent event) {
        String msg = event.getMessage().getContentRaw();
        String[] command = msg.split(" ", 2);
        String channelID = event.getMessage().getChannel().getId();
        MessageChannelUnion channel = event.getChannel();

        if(event.getAuthor().isBot() && !Arrays.asList(BOT_IDS).contains(event.getAuthor().getId())) return;
        if(Arrays.asList(BOT_IDS).contains(event.getAuthor().getId()) && msg.contains("Time over! The answer was : ")){

            PlayingChannel pc = getPlayingChannel(event.getChannel().getId());
            beatmapManager.updateBeatmap(pc.beatmap.beatmapset_id, pc.gameType, false);

            event.getChannel().getHistory().retrievePast(6)
                    .queue(message -> { // success callback
                        boolean check = false;
                        for(Message m : message){
                            if (!Arrays.asList(BOT_IDS).contains(m.getAuthor().getId())) check = true;
                        }

                        if(!check){
                            event.getChannel().sendMessage("Seems like no one is playing, shutting down omq session...").queue();
                            stopPlaying(event.getChannel());
                        }else{
                            stopCountdown(channelID);
                            setupGame(event);
                        }
                    });

            return;
        }

        switch(command[0]){
            case "!omq", "!omqplay" -> {
                if(isPlaying(channelID)){
                    event.getChannel().sendMessage("This channel is already playing omq!").queue();
                    return;
                }
                channel.sendMessage("Guess the name of the song below!\nType **!skip** to skip current song, **!stop** to stop playing").queue();
                playingChannels.add(new PlayingChannel(event.getChannel().getId(), GameType.MUSIC));
                setupGame(event);

                System.out.println("Currently playing in " + playingChannels.size() + " servers");
            }

            case "!pass", "!skip", "!omqpass", "!omqskip" -> {
                if(!isPlaying(channelID)) return;
                PlayingChannel playingChannel = getPlayingChannel(channelID);
                Beatmap beatmap = playingChannel.beatmap;
                beatmapManager.updateBeatmap(beatmap.beatmapset_id, playingChannel.gameType, false);
                event.getChannel().sendMessage("The answer was `" + beatmap.artist + " - " + beatmap.title + "`\nPlaying next song...").queue();
                stopCountdown(channelID);
                setupGame(event);
            }

            case "!close", "!stop", "!omqclose", "!omqstop" -> {
                if(!isPlaying(channelID)) return;
                event.getChannel().sendMessage("Closing omq session...").queue();
                stopPlaying(event.getChannel());
            }

            case "!omqhelp" -> onCommandHelp(event);

            case "!omqlb", "!omqleaderboard", "!omqrank", "!omqranking", "!omqrankings" -> event.getChannel().sendMessage(beatmapManager.getLeaderboard()).queue();

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
        String txt = """
                Available commands for OMQ (osu! Music Quiz) bot :
                **/guess [gametype]** : Starts playing guessing game, supported gamemodes : MUSIC, BACKGROUND, PATTERN
                **!skip/pass** : Shows the answer of the current song and skips to the next song.
                **!stop/close** : Manually closes current OMQ session.
                **!omqlb, !omqleaderboard** : Shows omq leaderboard
                """;
        event.getChannel().sendMessage(txt).queue();
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
                beatmapManager.removeBeatmap(command[1], event.getChannel(), 0);
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
                beatmapManager.removeBeatmap(command[1], event.getChannel(), 1);
            }
            case "!announce" -> {
                if (command.length == 1) {
                    event.getChannel().sendMessage("Please provide valid command").queue();
                    return;
                }
                for (PlayingChannel pc : playingChannels) {
                    String channelID = pc.channelID;
                    event.getJDA().getTextChannelById(channelID).sendMessage("**Announcement :**\n" + msg.replace("!announce ", "") + "").queue();
                }
            }
            case "!playingservers" -> event.getChannel().sendMessage("Currently playing in " + playingChannels.size() + " channels").queue();
            case "!render" -> {
                PlayingChannel pc = getPlayingChannel(event.getChannel().getId());
                Thread t = new Thread(()-> new VideoRenderer(event.getChannel(), beatmapManager.getBeatmap(Integer.parseInt(command[1]), pc.gameType)));
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

        if(isPlaying(event.getChannel().getId())){
            event.reply("This channel is already playing omq!").queue();
            return;
        }

        switch (optionName){
            case "Music" -> {
                event.reply("Guess the name of the song below!\nType **!skip** to skip current song, **!stop** to stop playing").queue();
                playingChannels.add(new PlayingChannel(event.getChannel().getId(), GameType.MUSIC));
            }
            case "Background" -> {
                event.reply("Guess the name of the song below!\nType **!skip** to skip current song, **!stop** to stop playing").queue();
                playingChannels.add(new PlayingChannel(event.getChannel().getId(), GameType.BACKGROUND));
            }
            case "Pattern" -> {
                event.reply("Guess the name of the beatmap below!\nType **!skip** to skip current beatmap, **!stop** to stop playing").queue();
                playingChannels.add(new PlayingChannel(event.getChannel().getId(), GameType.PATTERN));
            }
        }

        setupGame(event.getChannel());
        System.out.println("Currently playing in " + playingChannels.size() + " servers");
    }

    private final String[] gameTypes = new String[]{"Music", "Background", "Pattern"};
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event){
        if (event.getName().equals("guess") && event.getFocusedOption().getName().equals("gametype")) {
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
            event.getMessage().reply(username + " got it right! The answer was : `" + pc.beatmap.toString() + "`").queue();
            beatmapManager.updateLeaderboard(userid, username, pc.gameType);
            beatmapManager.updateBeatmap(pc.beatmap.beatmapset_id, pc.gameType, true);
            pc.leaderboard.putIfAbsent(username, 0); // add new if null
            pc.leaderboard.put(username, pc.leaderboard.get(username) + 1);
            stopCountdown(pc.channelID);
            setupGame(event);
            return;
        }

        double artistpoints = CheckAnswer(event.getMessage().getContentRaw(), pc.beatmap.artist);

        if(artistpoints >= 0.9) event.getChannel().sendMessage(event.getMessage().getAuthor().getName() + " got the artist name right! (" + pc.beatmap.artist + ")").queue();
    }

    private Beatmap setupGame(MessageReceivedEvent event){
        return setupGame(event.getChannel());
    }

    private Beatmap setupGame(SlashCommandInteractionEvent event){
        // MUSIC GUESSER

        return setupGame(event.getChannel());
    }
    private Beatmap setupGame(MessageChannelUnion channel){
        Beatmap beatmap = null;
        PlayingChannel playingChannel = getPlayingChannel(channel.getId());
        try {
            if(playingChannel == null) return null;
            beatmap = beatmapManager.getRandomBeatmap(playingChannel.gameType);
            if(beatmapManager.getBeatmapCount(playingChannel.gameType) == playingChannel.playedBeatmapIDs.size()){
                channel.sendMessage("You have played all maps!").queue();
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

            System.out.println("setting up game, current map is : " + beatmap.toString());
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
                    file = new File("tmpfiles/" + channel.getId() + ".mp3");
                    //loadAndPlay((TextChannel) channel, url.toString());
                }
                case BACKGROUND -> {
                    url = new URL("https://assets.ppy.sh/beatmaps/" + beatmap.beatmapset_id + "/covers/raw.jpg");
                    file = new File("tmpfiles/" + channel.getId() + ".jpg");
                }
                case PATTERN -> {
                    Thread t = new Thread(()->{
                        new VideoRenderer(channel, finalBeatmap);
                        if(getPlayingChannel(playingChannel.channelID) != null){
                            playingCountdown.add(new Countdown(channel, finalBeatmap, playingChannel.gameType));
                        }

                    });
                    t.start();
                    return beatmap;
                }
            }

            playingCountdown.add(new Countdown(channel, beatmap, playingChannel.gameType));
            FileUtils.copyURLToFile(url, file);

            System.out.println(beatmap + " / " + beatmap.beatmapset_id);

            channel.sendFile(file).queue();
        } catch (IOException e) {
            e.printStackTrace();
            channel.sendMessage("An error occurred, trying again...").queue();
            setupGame(channel);
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

        List<Map.Entry<String, Integer>> list_entries = new ArrayList<>(playingChannel.leaderboard.entrySet());
        list_entries.sort((obj1, obj2) -> obj2.getValue().compareTo(obj1.getValue()));

        String str = "**Ranking for this OMQ session : **\n";

        int counter = 0;
        for(Map.Entry<String, Integer> entry : list_entries) {
            String username = entry.getKey();
            int point = entry.getValue();
            if(counter == 0){
                str += ":first_place: ";
            }else if(counter == 1){
                str += ":second_place: ";
            }else if(counter == 2){
                str += ":third_place: ";
            }
            str += ("**" + username + "** : " + point + " pt");
            str += (point == 1) ? "\n" : "s\n";
            counter++;
        }

        messageChannel.sendMessage(str).queue();

        playingChannels.remove(playingChannel);
        stopCountdown(channelID);
        System.out.println("Currently playing in " + playingChannels.size() + " servers");
    }

    private void stopCountdown(String channelID){
        for(Countdown c : playingCountdown){
            if(c.getTextChannel().getId().equals(channelID)){
                c.stop();
            }
        }
        playingCountdown.removeIf(c -> c.getTextChannel().getId().equals(channelID));
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
