package db.model.world;

import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

public class World implements WorldId {
    private String name;

    private int players;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    public World(String name, int players, Timestamp createdAt, Timestamp updatedAt) {
        this.name = name;
        this.players = players;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public int getPlayers() {
        return players;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
}
