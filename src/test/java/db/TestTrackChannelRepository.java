package db;

import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.repository.TrackChannelRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

class TestTrackChannelRepository {
    @TestOnly
    private static TrackChannelRepository getRepository() {
        DatabaseConnection db = TestDBUtils.createConnection();
        return db.getTrackingChannelRepository();
    }

    @TestOnly
    private static void clearTable() {
        TrackChannelRepository repo = getRepository();
        List<TrackChannel> list = repo.findAll();
        list.forEach(repo::delete);
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        TrackChannelRepository repo = getRepository();

        TrackChannel entity = new TrackChannel(TrackType.TERRITORY_ALL, 1000L, 5000L);

        assert repo.count() == 0;
        assert !repo.exists(entity);

        assert repo.create(entity);

        assert repo.count() == 1;
        assert repo.exists(entity);

        assert repo.findAllOfType(TrackType.TERRITORY_ALL).size() == 1;
        assert repo.findAllOfType(TrackType.WAR_ALL).size() == 0;

        assert repo.delete(entity);

        assert repo.count() == 0;
        assert !repo.exists(entity);
    }

    @Test
    void testNullableValues() {
        clearTable();
        TrackChannelRepository repo = getRepository();

        long guildId = 2000L;
        long channelId = 6000L;
        TrackChannel entity = new TrackChannel(TrackType.WAR_SPECIFIC, guildId, channelId);
        entity.setGuildName("Salted Test");

        assert repo.create(entity);
        assert repo.exists(entity);

        entity.setGuildName("Different Guild");
        assert !repo.exists(entity);

        entity.setGuildName("Salted Test");
        assert repo.count() == 1;
        assert repo.delete(entity);
        assert repo.count() == 0;
    }
}