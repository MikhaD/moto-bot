package db.repository;

import db.model.track.TrackChannel;
import db.model.track.TrackChannelId;
import db.model.track.TrackType;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TrackChannelRepository extends Repository<TrackChannel, TrackChannelId> {
    // NOTE: guild_name, player_name columns are NULL-able

    public TrackChannelRepository(Connection db, Logger logger) {
        super(db, logger);
    }

    @Override
    public boolean create(@NotNull TrackChannel entity) {
        return this.execute(
                "INSERT INTO `track_channel` (`type`, `guild_id`, `channel_id`, `guild_name`, `player_name`) VALUES (?, ?, ?, ?, ?)",
                entity.getType(),
                entity.getGuildId(),
                entity.getChannelId(),
                entity.getGuildName(),
                entity.getPlayerName()
        );
    }

    @Override
    public boolean exists(@NotNull TrackChannelId id) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `track_channel` WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ? AND `guild_name` <=> ? AND `player_name` <=> ?",
                id.getType(),
                id.getGuildId(),
                id.getChannelId(),
                id.getGuildName(),
                id.getPlayerName()
        );
        if (res == null) return false;

        try {
            if (res.next())
                return res.getInt(1) > 0;
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return false;
    }

    @Override
    public long count() {
        ResultSet res = this.executeQuery("SELECT COUNT(*) FROM `track_channel`");
        if (res == null) return 0;

        try {
            if (res.next())
                return res.getInt(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return 0;
    }

    @Override
    public TrackChannel findOne(@NotNull TrackChannelId id) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `track_channel` WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ? AND `guild_name` <=> ? AND `player_name` <=> ? LIMIT 1",
                id.getType(),
                id.getGuildId(),
                id.getChannelId(),
                id.getGuildName(),
                id.getPlayerName()
        );
        if (res == null) return null;

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @NotNull
    public List<TrackChannel> findAllOf(long guildId, long channelId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `track_channel` WHERE `guild_id` = ? AND `channel_id` = ?",
                guildId,
                channelId
        );
        if (res == null) return new ArrayList<>();

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return new ArrayList<>();
    }

    @NotNull
    @Override
    public List<TrackChannel> findAll() {
        ResultSet res = this.executeQuery("SELECT * FROM `track_channel`");

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean update(@NotNull TrackChannel entity) {
        return this.execute(
                "UPDATE `track_channel` SET `guild_name` = ?, `player_name` = ? WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ?",
                entity.getGuildName(),
                entity.getPlayerName(),
                entity.getType(),
                entity.getGuildId(),
                entity.getChannelId()
        );
    }

    @Override
    public boolean delete(@NotNull TrackChannelId id) {
        return this.execute(
                "DELETE FROM `track_channel` WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ? AND `guild_name` <=> ? AND `player_name` <=> ?",
                id.getType(),
                id.getGuildId(),
                id.getChannelId(),
                id.getGuildName(),
                id.getPlayerName()
        );
    }

    public List<TrackChannel> findAllOfType(TrackType type) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `track_channel` WHERE `type` = ?",
                type
        );

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return new ArrayList<>();
    }

    protected TrackChannel bind(@NotNull ResultSet res) throws SQLException {
        TrackChannel instance = new TrackChannel(TrackType.valueOf(res.getString(1)), res.getLong(2), res.getLong(3));
        instance.setGuildName(res.getString(4));
        instance.setPlayerName(res.getString(5));
        return instance;
    }
}