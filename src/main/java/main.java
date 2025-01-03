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
                GatewayIntent.GUILD_MESSAGES,
                // disabled voice channel perm for now
                //GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.MESSAGE_CONTENT
        );

        JDA jda = JDABuilder.createLight(Config.get("TOKEN"), intents)
                .setActivity(Activity.playing("!omqhelp"))
                .addEventListeners(new OMQBot())
                .build();

        jda.updateCommands().addCommands(
                Commands.slash("omq", "Start playing osu! Music Quiz")
                        .addOption(OptionType.STRING, "gametype", "Select type of the game", true, true)
        ).queue();
    }
}
