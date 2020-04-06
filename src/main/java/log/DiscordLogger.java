package log;

import app.Bot;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.BotUtils;
import utils.FormatUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * DiscordLogger implements Logger, discord channel logging, and checks message spams.
 */
public class DiscordLogger implements Logger {
    private final Bot bot;

    private final Map<Integer, Long> logChannels;

    private final DateFormat logFormat;

    private final boolean debug;

    public DiscordLogger(Bot bot, TimeZone logTimeZone) {
        this.bot = bot;
        // deep copy
        this.logChannels = bot.getProperties().logChannelId
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        this.logFormat.setTimeZone(logTimeZone);
        this.debug = "1".equals(System.getenv("DEBUG"));
    }

    /**
     * Logs normal message.
     * @param botLogCh Channel to log.
     * @param message Message to log.
     */
    public void log(int botLogCh, CharSequence message) {
        Date now = new Date();
        String msgTimeAppended = this.logFormat.format(now) + " " + message;

        System.out.println(msgTimeAppended);

        // Log to discord channels
        if (!this.logChannels.containsKey(botLogCh)) {
            return;
        }
        long logChannelId = this.logChannels.get(botLogCh);
        TextChannel ch = this.bot.getManager().getTextChannelById(logChannelId);
        if (ch == null) {
            return;
        }

        int shardId = this.bot.getShardId(ch.getJDA());
        if (!this.bot.isConnected(shardId)) {
            return;
        }

        BotUtils.sendLongMessage(msgTimeAppended, ch);
    }

    @Override
    public void debug(CharSequence message) {
        if (!debug) return;
        Date now = new Date();
        String msg = this.logFormat.format(now) + " " + message;
        System.out.println(msg);
    }

    /**
     * Logs message received event to bot-log-4.
     * @param event Guild message received event.
     */
    public void logEvent(MessageReceivedEvent event, boolean isSpam) {
        String logMsg = createCommandLog(event, isSpam);
        this.log(4, logMsg);
    }

    /**
     * Create a user command log String. <br/>
     * Example output <br/>
     * 2018/10/01 11:02:46.430 (Guild)[Channel]&lt;User&gt;: `>ping` <br/>
     * 2019/12/04 19:12:06.334 [DM UserName#1234]&lt;User&gt;: `>help`
     * @param event Guild message received event.
     * @return Human readable user command usage log.
     */
    static String createCommandLog(MessageReceivedEvent event, boolean isSpam) {
        if (event.isFromGuild()) {
            return String.format(
                    "(%s)[%s]<%s>: `%s`%s",
                    event.getGuild().getName(),
                    event.getChannel().getName(),
                    event.getAuthor().getName(),
                    event.getMessage().getContentRaw(),
                    isSpam ? " Spam detected" : ""
            );
        } else {
            return String.format(
                    "[DM %s]<%s>: `%s`%s",
                    FormatUtils.getUserFullName(event.getAuthor()),
                    event.getAuthor().getName(),
                    event.getMessage().getContentRaw(),
                    isSpam ? " Spam detected" : ""
            );
        }
    }

    @Override
    public void logException(CharSequence message, Throwable e) {
        e.printStackTrace();
        String formatted = message + "\n" + e.getMessage();
        // Print short version
        this.log(0, formatted);
        String fullStackTrace = formatted;
        fullStackTrace += Arrays.stream(e.getStackTrace()).map(elt -> "    " + elt.toString()).collect(Collectors.joining("\n"));
        // Full stack trace to another channel
        this.log(2, fullStackTrace);
    }
}
