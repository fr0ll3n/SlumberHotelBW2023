package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HikariCP-backed MySQL storage. Falls back cleanly if disabled / fails.
 */
public class DatabaseManager {

    private final SlumberHotelCore plugin;
    private HikariDataSource dataSource;
    private String prefix = "sh_";
    private boolean enabled = false;

    // In-memory cache to avoid hammering MySQL every tick
    private final Map<UUID, PlayerRow> cache = new ConcurrentHashMap<UUID, PlayerRow>();

    public static class PlayerRow {
        public int walletTier = 0;
        public boolean enteredHotel = false;
        public String dailyDate = "";
        public String dailyAssigned = "";
        public String dailyProgress = ""; // key=value;key=value
        public String dailyCompleted = "";
        public boolean dirty = false;
    }

    public DatabaseManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return enabled && dataSource != null && !dataSource.isClosed();
    }

    public boolean setup() {
        FileConfiguration cfg = plugin.getConfig();
        String type = cfg.getString("storage.type", "YAML");
        if (type == null || !type.equalsIgnoreCase("MYSQL")) {
            plugin.getLogger().info("Storage: YAML (MySQL disabled in config).");
            return false;
        }

        String host = cfg.getString("storage.mysql.host", "localhost");
        int port = cfg.getInt("storage.mysql.port", 3306);
        String database = cfg.getString("storage.mysql.database", "slumberhotel");
        String user = cfg.getString("storage.mysql.username", "root");
        String pass = cfg.getString("storage.mysql.password", "");
        prefix = cfg.getString("storage.mysql.table-prefix", "sh_");

        try {
            HikariConfig hc = new HikariConfig();
            // MySQL 8 driver (shaded). Also works with MariaDB in most panels.
            hc.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&autoReconnect=true");
            hc.setUsername(user);
            hc.setPassword(pass);
            hc.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hc.setMaximumPoolSize(cfg.getInt("storage.mysql.pool-size", 10));
            hc.setMinimumIdle(cfg.getInt("storage.mysql.min-idle", 2));
            hc.setConnectionTimeout(10000);
            hc.setIdleTimeout(600000);
            hc.setMaxLifetime(1800000);
            hc.setPoolName("SlumberHotel-Hikari");
            hc.addDataSourceProperty("cachePrepStmts", "true");
            hc.addDataSourceProperty("prepStmtCacheSize", "250");
            hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(hc);

            // Test connection + create tables
            try (Connection c = dataSource.getConnection()) {
                createTables(c);
            }

            enabled = true;
            plugin.getLogger().info("MySQL connected via HikariCP → " + host + ":" + port + "/" + database);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL/Hikari setup failed: " + e.getMessage());
            e.printStackTrace();
            enabled = false;
            if (dataSource != null) {
                try { dataSource.close(); } catch (Exception ignored) {}
                dataSource = null;
            }
            plugin.getLogger().warning("Falling back to YAML storage.");
            return false;
        }
    }

    private void createTables(Connection c) throws SQLException {
        String players = "CREATE TABLE IF NOT EXISTS `" + prefix + "players` ("
                + "`uuid` VARCHAR(36) NOT NULL,"
                + "`wallet_tier` INT NOT NULL DEFAULT 0,"
                + "`entered_hotel` TINYINT(1) NOT NULL DEFAULT 0,"
                + "`daily_date` VARCHAR(16) NOT NULL DEFAULT '',"
                + "`daily_assigned` TEXT,"
                + "`daily_progress` TEXT,"
                + "`daily_completed` TEXT,"
                + "`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (`uuid`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        try (Statement st = c.createStatement()) {
            st.executeUpdate(players);
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isEnabled()) throw new SQLException("MySQL not enabled");
        return dataSource.getConnection();
    }

    public PlayerRow loadPlayer(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        PlayerRow row = new PlayerRow();
        if (!isEnabled()) {
            cache.put(uuid, row);
            return row;
        }
        String sql = "SELECT wallet_tier, entered_hotel, daily_date, daily_assigned, daily_progress, daily_completed "
                + "FROM `" + prefix + "players` WHERE uuid=?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    row.walletTier = rs.getInt("wallet_tier");
                    row.enteredHotel = rs.getInt("entered_hotel") == 1;
                    row.dailyDate = nullToEmpty(rs.getString("daily_date"));
                    row.dailyAssigned = nullToEmpty(rs.getString("daily_assigned"));
                    row.dailyProgress = nullToEmpty(rs.getString("daily_progress"));
                    row.dailyCompleted = nullToEmpty(rs.getString("daily_completed"));
                } else {
                    // Insert default row
                    insertDefault(uuid);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("loadPlayer failed: " + e.getMessage());
        }
        cache.put(uuid, row);
        return row;
    }

    private void insertDefault(UUID uuid) {
        String sql = "INSERT IGNORE INTO `" + prefix + "players` (uuid) VALUES (?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("insertDefault failed: " + e.getMessage());
        }
    }

    public void savePlayer(UUID uuid) {
        PlayerRow row = cache.get(uuid);
        if (row == null || !isEnabled()) return;
        String sql = "INSERT INTO `" + prefix + "players` "
                + "(uuid, wallet_tier, entered_hotel, daily_date, daily_assigned, daily_progress, daily_completed) "
                + "VALUES (?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE "
                + "wallet_tier=VALUES(wallet_tier), entered_hotel=VALUES(entered_hotel), "
                + "daily_date=VALUES(daily_date), daily_assigned=VALUES(daily_assigned), "
                + "daily_progress=VALUES(daily_progress), daily_completed=VALUES(daily_completed)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, row.walletTier);
            ps.setInt(3, row.enteredHotel ? 1 : 0);
            ps.setString(4, row.dailyDate);
            ps.setString(5, row.dailyAssigned);
            ps.setString(6, row.dailyProgress);
            ps.setString(7, row.dailyCompleted);
            ps.executeUpdate();
            row.dirty = false;
        } catch (SQLException e) {
            plugin.getLogger().warning("savePlayer failed: " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : new ArrayList<UUID>(cache.keySet())) {
            savePlayer(uuid);
        }
    }

    public void unload(UUID uuid) {
        savePlayer(uuid);
        cache.remove(uuid);
    }

    public void shutdown() {
        saveAll();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        enabled = false;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // ---- helpers for daily progress serialization ----
    public static Map<String, Integer> parseProgress(String raw) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        if (raw == null || raw.isEmpty()) return map;
        for (String part : raw.split(";")) {
            String[] kv = part.split("=");
            if (kv.length == 2) {
                try { map.put(kv[0], Integer.parseInt(kv[1])); } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    public static String serializeProgress(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    public static List<String> parseList(String raw) {
        List<String> list = new ArrayList<String>();
        if (raw == null || raw.isEmpty()) return list;
        for (String s : raw.split(",")) {
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    public static String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append(',');
            sb.append(s);
        }
        return sb.toString();
    }
}
