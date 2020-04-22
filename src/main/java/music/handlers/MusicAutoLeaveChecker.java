package music.handlers;

import app.Bot;
import log.Logger;
import music.MusicState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import utils.MinecraftColor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MusicAutoLeaveChecker {
    private final Map<Long, MusicState> states;

    private final ShardManager manager;
    private final Logger logger;

    private final MusicPlayHandler playHandler;

    public MusicAutoLeaveChecker(Bot bot, Map<Long, MusicState> states, MusicPlayHandler playHandler) {
        this.states = states;
        this.manager = bot.getManager();
        this.logger = bot.getLogger();
        this.playHandler = playHandler;
    }

    private final static long TIME_TILL_LEAVE = TimeUnit.MINUTES.toMillis(3);

    /**
     * Checks if the bot can safely leave the voice channel.
     * When one of the following is satisfied:
     * 1. If there are no songs in the queue
     * 2. No one's listening to the music
     * @return If the bot should leave the vc.
     */
    private boolean canSafelyLeave(long guildId, MusicState state) {
        if (System.currentTimeMillis() - state.getLastInteract() < TIME_TILL_LEAVE) {
            return false;
        }

        if (state.getCurrentQueue().getQueue().isEmpty()) {
            return true;
        }

        Guild guild = this.manager.getGuildById(guildId);
        if (guild == null) {
            return false;
        }
        VoiceChannel channel = guild.getAudioManager().getConnectedChannel();
        if (channel == null) {
            // Not connected?
            return true;
        }
        // Number of people listening
        long listeningCount = channel.getMembers().stream().filter(m -> {
            GuildVoiceState vs = m.getVoiceState();
            return vs != null && !vs.isDeafened();
        }).count();

        return listeningCount == 0;
    }

    public void checkAllGuilds() {
        synchronized (states) {
            for (Map.Entry<Long, MusicState> entry : states.entrySet()) {
                long guildId = entry.getKey();
                MusicState state = entry.getValue();

                if (canSafelyLeave(guildId, state)) {
                    TextChannel channel = this.manager.getTextChannelById(state.getBoundChannelId());
                    if (channel == null) {
                        this.logger.log(0, "Music auto leave: Failed to retrieve channel for ID: " + state.getBoundChannelId());
                        continue;
                    }
                    try {
                        this.playHandler.shutdownPlayer(true, guildId, state);
                    } catch (RuntimeException e) {
                        channel.sendMessage("Something went wrong while leaving channel: " + e.getMessage()).queue();
                        continue;
                    }
                    channel.sendMessage(new EmbedBuilder()
                            .setColor(MinecraftColor.RED.getColor())
                            .setDescription("Left the voice channel due to inactivity.")
                            .build()).queue();
                }
            }
        }
    }
}
