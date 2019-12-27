CREATE DATABASE IF NOT EXISTS `moto-bot_test`;

USE `moto-bot_test`;

CREATE TABLE IF NOT EXISTS `track_channel` (
    `type` VARCHAR(30) NOT NULL,
    `guild_id` BIGINT NOT NULL,
    `channel_id` BIGINT NOT NULL,
    `guild_name` VARCHAR(30) NULL,
    `player_name` VARCHAR(16) NULL,
    `guild_name_v` VARCHAR(30) AS (IF(`guild_name` IS NULL, '', `guild_name`)) VIRTUAL,
    `player_name_v` VARCHAR(16) AS (IF(`player_name` IS NULL, '', `player_name`)) VIRTUAL,
    UNIQUE KEY (`type`, `guild_id`, `channel_id`, `guild_name_v`, `player_name_v`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `world` (
    `name` VARCHAR(30) NOT NULL,
    `players` INT NOT NULL,
    `created_at` DATETIME DEFAULT NOW(),
    `updated_at` DATETIME DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `command_log` (
    `id` INT AUTO_INCREMENT NOT NULL,
    `kind` VARCHAR(30) NOT NULL,
    `full` VARCHAR(2500) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `dm` BOOLEAN NOT NULL,
    PRIMARY KEY (`id`),
    KEY `kind_idx` (`kind`),
    KEY `user_id_idx` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `guild` (
    `name` VARCHAR(30) PRIMARY KEY NOT NULL,
    `prefix` CHAR(3) NOT NULL,
    `created_at` DATETIME NOT NULL,
    KEY `prefix_idx` (`prefix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `territory` (
    `name` VARCHAR(100) PRIMARY KEY NOT NULL,
    `guild_name` VARCHAR(30),
    `acquired` DATETIME,
    `attacker` VARCHAR(30) NULL,
    `start_x` INT,
    `start_z` INT,
    `end_x` INT,
    `end_z` INT,
    KEY `guild_idx` (`guild_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `territory_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `territory_name` VARCHAR(100) NOT NULL,
    `old_guild_name` VARCHAR(30),
    `new_guild_name` VARCHAR(30),
    `old_guild_terr_amt` INT NOT NULL,
    `new_guild_terr_amt` INT NOT NULL,
    `acquired` DATETIME NOT NULL DEFAULT NOW(),
    # milliseconds
    `time_diff` BIGINT NOT NULL,
    KEY `territory_name_idx` (`territory_name`, `acquired` DESC),
    KEY `old_guild_idx` (`old_guild_name`, `acquired` DESC),
    KEY `new_guild_idx` (`new_guild_name`, `acquired` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `war_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `server` VARCHAR(10) NOT NULL,
    `guild_name` VARCHAR(30) NULL,
    `created_at` DATETIME NOT NULL DEFAULT NOW(),
    `last_up` DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    `ended` BOOLEAN NOT NULL,
    KEY `guild_idx` (`guild_name`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `war_players_log` (
    `war_log_id` INT NOT NULL,
    `player` VARCHAR(30) NOT NULL,
    # A flag indicating player left war server before the war server itself ends,
    # or that guild acquired a territory (= `ended` flag in `war_log` table)
    `exited` BOOLEAN NOT NULL,
    PRIMARY KEY (`war_log_id`, `player`),
    CONSTRAINT `fk_war_log_id` FOREIGN KEY (`war_log_id`) REFERENCES `war_log` (`id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# A table that aggregates `war_log` / `territory_log` for a guild.
# Records all wars (<-> associated if possible), acquire territory, lost territory (never associated with a war log)
CREATE TABLE IF NOT EXISTS `guild_war_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `guild_name` VARCHAR(30) NOT NULL,
    `war_log_id` INT NULL,
    `territory_log_id` INT NULL,
    UNIQUE KEY `guild_idx` (`guild_name`, `id` DESC),
    UNIQUE KEY `war_log_idx` (`war_log_id`),
    UNIQUE KEY `territory_log_idx` (`territory_log_id`, `guild_name`),
    CONSTRAINT `fk_guild_war_log_id` FOREIGN KEY (`war_log_id`) REFERENCES `war_log` (`id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT `fk_guild_territory_log_id` FOREIGN KEY (`territory_log_id`) REFERENCES `territory_log` (`id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP FUNCTION IF EXISTS `count_guild_territories`;
DELIMITER //
CREATE FUNCTION `count_guild_territories` (g_name VARCHAR(30)) RETURNS INT
    BEGIN
        RETURN (SELECT COUNT(*) FROM `territory` WHERE `guild_name` = g_name);
    END; //
DELIMITER ;

# On `territory` table, if `guild` column (owner of the territory) was updated... insert into `territory_log` table
DELIMITER //
CREATE TRIGGER IF NOT EXISTS `territory_logger`
    AFTER UPDATE ON `territory` FOR EACH ROW
    BEGIN
        # on guild column update
        IF IF(NEW.guild_name <=> OLD.guild_name, 0, 1) THEN
            INSERT INTO `territory_log`
                (territory_name, old_guild_name, new_guild_name, old_guild_terr_amt, new_guild_terr_amt, acquired, time_diff)
                VALUES (NEW.name, OLD.guild_name, NEW.guild_name,
                        count_guild_territories(OLD.guild_name), count_guild_territories(NEW.guild_name),
                        NEW.acquired, (UNIX_TIMESTAMP(NEW.acquired) - UNIX_TIMESTAMP(OLD.acquired)) * 1000);
        END IF;
    END; //
DELIMITER ;

# Selects id of the last war log for guild that is not yet associated to an territory log
DROP FUNCTION IF EXISTS `last_unassociated_war_log_id`;
DELIMITER //
CREATE FUNCTION `last_unassociated_war_log_id` (g_name VARCHAR(30)) RETURNS INT
    BEGIN
        RETURN (SELECT `id` FROM `war_log` WHERE `guild_name` = g_name AND (SELECT `territory_log_id` IS NULL FROM `guild_war_log` WHERE `war_log_id` = `war_log`.`id`) = 1 ORDER BY `created_at` DESC LIMIT 1);
    END; //
DELIMITER ;

# if war log exists within 3 min... associate with the correct `war_log_id` and record of `guild_war_log` table
DELIMITER //
CREATE TRIGGER IF NOT EXISTS `guild_territory_logger`
    AFTER INSERT ON `territory_log` FOR EACH ROW
    BEGIN
        # for old owner guild
        INSERT INTO `guild_war_log` (guild_name, territory_log_id) VALUES (NEW.old_guild_name, NEW.id);

        # for new owner guild
        SET @war_log_id = last_unassociated_war_log_id(NEW.new_guild_name);

        IF @war_log_id IS NOT NULL THEN
            # if the last war log for that guild is within 3 minutes
            IF ((UNIX_TIMESTAMP(NOW()) - UNIX_TIMESTAMP((SELECT `last_up` FROM `war_log` WHERE `id` = @war_log_id)))) <= 180 THEN
                UPDATE `guild_war_log` SET `territory_log_id` = NEW.id WHERE guild_name = NEW.new_guild_name AND `war_log_id` = @war_log_id;
                UPDATE `war_log` SET `ended` = 1 WHERE `id` = @war_log_id;
            ELSE
                INSERT INTO `guild_war_log` (guild_name, territory_log_id) VALUES (NEW.new_guild_name, NEW.id);
            END IF;
        ELSE
            INSERT INTO `guild_war_log` (guild_name, territory_log_id) VALUES (NEW.new_guild_name, NEW.id);
        END IF;
    END; //
DELIMITER ;

DELIMITER //
CREATE TRIGGER IF NOT EXISTS `guild_war_logger`
    AFTER INSERT ON `war_log` FOR EACH ROW
    BEGIN
        INSERT INTO `guild_war_log` (guild_name, war_log_id) VALUES (NEW.guild_name, NEW.id);
    END; //
DELIMITER ;