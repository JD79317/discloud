package me.JD79317.discloud;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public final class DiscordBotManager {
    public static final int MAX_ATTACHMENTS_PER_MESSAGE = 5;

    private DiscordBotManager() {
    }

    public static JDA init(String botToken) {
        try {
            return JDABuilder.createDefault(botToken).build().awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (LoginException e) {
            Util.reportError(e, "Bot login failed");
        }
        return null;
    }
}
