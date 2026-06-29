package ru.iamdvz.divizionsc.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.config.settings.Settings;

/**
 * Управляет пулом соединений (HikariCP) и схемой БД. Поддерживает SQLite (по умолчанию)
 * и MySQL/MariaDB (кросс-сервер). Инициализация и запросы — вне main thread.
 */
public final class DatabaseManager {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

    private final JavaPlugin plugin;
    private final Settings.Database settings;
    private final boolean sqlite;
    private final Queue<Runnable> pendingUntilReady = new ConcurrentLinkedQueue<>();

    private HikariDataSource dataSource;
    private ExecutorService executor;
    private volatile boolean ready;

    public DatabaseManager(JavaPlugin plugin, Settings.Database settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.sqlite = !"mysql".equalsIgnoreCase(settings.type) && !"mariadb".equalsIgnoreCase(settings.type);
    }

    public CompletableFuture<Void> init() {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "DivizionSC-DB-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        this.executor = Executors.newFixedThreadPool(sqlite ? 1 : Math.max(1, settings.poolSize), factory);
        return CompletableFuture.runAsync(() -> {
            HikariConfig config = new HikariConfig();
            config.setPoolName("DivizionSC-DB");
            if (sqlite) {
                File file = new File(plugin.getDataFolder(), settings.sqliteFile);
                config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
                config.setDriverClassName("org.sqlite.JDBC");
                config.setMaximumPoolSize(1);
            } else {
                config.setJdbcUrl("jdbc:mariadb://" + settings.host + ":" + settings.port + "/" + settings.name);
                config.setUsername(settings.user);
                config.setPassword(settings.password);
                config.setDriverClassName("org.mariadb.jdbc.Driver");
                config.setMaximumPoolSize(Math.max(1, settings.poolSize));
            }
            this.dataSource = new HikariDataSource(config);
            runMigrations();
            this.ready = true;
        }, executor).whenComplete((ignored, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialise database", error);
            } else {
                plugin.getLogger().info("Database ready (" + (sqlite ? "sqlite" : "mysql") + ").");
                drainPending();
            }
        });
    }

    private void drainPending() {
        Runnable task;
        while ((task = pendingUntilReady.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Pending database task failed", e);
            }
        }
    }

    private void runMigrations() {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table("binds") + " ("
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "slot INT NOT NULL, "
                    + "def_id VARCHAR(64) NOT NULL, "
                    + "PRIMARY KEY (player_uuid, slot))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table("cooldowns") + " ("
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "def_id VARCHAR(64) NOT NULL, "
                    + "expires_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (player_uuid, def_id))");
        } catch (SQLException e) {
            throw new IllegalStateException("Migration failed", e);
        }
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isSqlite() {
        return sqlite;
    }

    public String table(String name) {
        return settings.tablePrefix.toLowerCase(Locale.ROOT) + name;
    }

    public <T> CompletableFuture<T> supplyAsync(SqlFunction<T> function, T fallback) {
        if (!ready) {
            CompletableFuture<T> deferred = new CompletableFuture<>();
            pendingUntilReady.add(() -> doSupplyAsync(function, fallback).whenComplete((result, error) -> {
                if (error != null) {
                    deferred.complete(fallback);
                } else {
                    deferred.complete(result);
                }
            }));
            return deferred;
        }
        return doSupplyAsync(function, fallback);
    }

    private <T> CompletableFuture<T> doSupplyAsync(SqlFunction<T> function, T fallback) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return function.apply(connection);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Database query failed", e);
                return fallback;
            }
        }, executor);
    }

    public CompletableFuture<Void> runAsync(SqlConsumer consumer) {
        if (!ready) {
            CompletableFuture<Void> deferred = new CompletableFuture<>();
            pendingUntilReady.add(() -> doRunAsync(consumer).whenComplete((ignored, error) -> {
                if (error != null) {
                    deferred.complete(null);
                } else {
                    deferred.complete(null);
                }
            }));
            return deferred;
        }
        return doRunAsync(consumer);
    }

    private CompletableFuture<Void> doRunAsync(SqlConsumer consumer) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                consumer.accept(connection);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Database update failed", e);
            }
        }, executor);
    }

    public void close() {
        ready = false;
        if (dataSource != null) {
            dataSource.close();
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("Database executor did not finish in time; forcing shutdown.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        pendingUntilReady.clear();
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
