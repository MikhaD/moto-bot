package api.structs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Territory {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TimeZone wynnTimeZone;

    private String territory;
    private String guild;
    private String acquired;
    private String attacker;
    private Location location;

    public String getTerritory() {
        return territory;
    }

    public String getGuild() {
        return guild;
    }

    private Date getAcquired() throws ParseException {
        return format.parse(this.acquired);
    }

    @Nullable
    public String getAttacker() {
        return attacker;
    }

    public Location getLocation() {
        return location;
    }

    public static class Location {
        private int startX;
        private int startZ;
        private int endX;
        private int endZ;

        public int getStartX() {
            return startX;
        }

        public int getStartZ() {
            return startZ;
        }

        public int getEndX() {
            return endX;
        }

        public int getEndZ() {
            return endZ;
        }
    }

    @NotNull
    static Territory parse(String body, TimeZone wynnTimeZone) throws JsonProcessingException {
        Territory instance = mapper.readValue(body, Territory.class);
        instance.wynnTimeZone = wynnTimeZone;
        return instance;
    }

    /**
     * Converts this instance to db model instance.
     * @return DB model instance.
     * @throws ParseException if acquired parameter was in an unexpected format.
     */
    @NotNull
    public db.model.territory.Territory convert() throws ParseException {
        return new db.model.territory.Territory(
                this.territory,
                this.guild,
                this.getAcquired(),
                this.attacker,
                convertLocation(this.location)
        );
    }

    private static db.model.territory.Territory.Location convertLocation(Location l) {
        return new db.model.territory.Territory.Location(l.startX, l.startZ, l.endX, l.endZ);
    }

}
