import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import util.Config;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;


public class main{

    public static void main(String[] args) throws LoginException {
        EnumSet<GatewayIntent> intents = EnumSet.of(
                // We need messages in guilds to accept commands from users
                GatewayIntent.GUILD_MESSAGES,
                // We need voice states to connect to the voice channel
                GatewayIntent.GUILD_VOICE_STATES,
                // Enable access to message.getContentRaw()
                GatewayIntent.MESSAGE_CONTENT
        );

        JDA jda = JDABuilder.createDefault(Config.get("TESTTOKEN"), intents)
                .setActivity(Activity.playing("!omqhelp"))
                .enableCache(CacheFlag.VOICE_STATE)
                .addEventListeners(new OMQBot())
                .build();

        jda.updateCommands().addCommands(
                Commands.slash("guess", "Start playing osu! beatmap guessing game")
                        .addOption(OptionType.STRING, "gametype", "Select type of the game", true, true)
        ).queue();
    }
}
