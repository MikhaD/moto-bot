package log;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Logger {
    void log(int botLogCh, CharSequence message);
    void debug(CharSequence message);
    // Returns true if the message event is considered spam.
    boolean logEvent(MessageReceivedEvent event);
    void logException(CharSequence message, Exception e);
}
