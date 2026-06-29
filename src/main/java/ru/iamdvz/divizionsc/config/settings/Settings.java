package ru.iamdvz.divizionsc.config.settings;

import java.util.List;
import net.elytrium.serializer.NameStyle;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

/**
 * Типизированный config.yml (Elytrium). Дефолты — значения полей, это боевой конфиг
 * после первого reload. camelCase-поля → kebab-case-узлы YAML.
 */
public final class Settings extends YamlSerializable {

    public static final int CONFIG_VERSION = 1;

    public static final SerializerConfig CONFIG = new SerializerConfig.Builder()
            .setFieldNameStyle(NameStyle.CAMEL_CASE)
            .setNodeNameStyle(NameStyle.KEBAB_CASE)
            .setBackupOnErrors(true)
            .build();

    public Settings() {
        super(Settings.CONFIG);
    }

    @Comment({
            @CommentValue(" Версия схемы config.yml (обновляется плагином автоматически)"),
            @CommentValue(" При обновлении плагина недостающие ключи дописываются, устаревшие — удаляются.")
    })
    public int configVersion = CONFIG_VERSION;

    @Comment(@CommentValue(" Язык сообщений: ru | en"))
    public String locale = "ru";

    @Comment(@CommentValue(" Папка с def-файлами внутри plugins/DivizionSC"))
    public String defsFolder = "defs";

    @Comment(@CommentValue(" Дальность поиска цели по умолчанию (блоков)"))
    public double defaultRange = 32.0;

    public String adminPermission = "divizionsc.admin";
    public String castPermissionPrefix = "divizionsc.def.";

    @Comment(@CommentValue(" Сообщать игроку о кулдауне способности"))
    public boolean cooldownMessages = true;

    @Comment(@CommentValue(" Сообщать игроку об успешном касте"))
    public boolean castMessages = false;

    @Comment(@CommentValue(" Логировать подробности ошибок загрузки def"))
    public boolean debugLoadErrors = true;

    @Comment(@CommentValue(" Размер страницы в /dsc list"))
    public int listPageSize = 15;

    @Comment(@CommentValue(" Обход кулдауна и маны для операторов (player.isOp())"))
    public OpBypass opBypass = new OpBypass();

    @Comment(@CommentValue(" Мана способностей (поле mana / mp / cost в def)"))
    public Mana mana = new Mana();

    public SkillBar skillBar = new SkillBar();

    public Database database = new Database();

    public static final class OpBypass {

        @Comment(@CommentValue(" Включить обход для op"))
        public boolean enabled = true;

        @Comment(@CommentValue(" Op не получают кулдаун способностей"))
        public boolean ignoreCooldown = true;

        @Comment(@CommentValue(" Op не тратят и не проверяют ману"))
        public boolean ignoreMana = true;
    }

    public static final class Mana {

        @Comment(@CommentValue(" Включить систему маны"))
        public boolean enabled = true;

        @Comment(@CommentValue(" Максимум маны при входе"))
        public double defaultMax = 100.0;

        @Comment(@CommentValue(" Текущая мана при входе"))
        public double defaultAmount = 100.0;
    }

    public static final class SkillBar {

        public boolean enabled = true;
        public String permission = "divizionsc.skills";
        public int guiSize = 54;
        public int listStartSlot = 0;
        public int listPageSize = 27;
        public int bindRowStartSlot = 36;
        public int bindSlotCount = 9;
        public boolean requireEmptyHand = false;
        public boolean openOnSwapHands = true;

        @Comment(@CommentValue(" Триггеры, которыми срабатывают привязки на хотбаре"))
        public List<String> bindTriggers = List.of(
                "right_click", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Comment(@CommentValue(" Хранилище кулдаунов и привязок. sqlite — локально, mysql — кросс-сервер."))
    public static final class Database {

        @Comment(@CommentValue(" sqlite | mysql"))
        public String type = "sqlite";

        @Comment(@CommentValue(" Файл SQLite относительно папки плагина"))
        public String sqliteFile = "data.db";

        public String host = "127.0.0.1";
        public int port = 3306;
        public String name = "divizionsc";
        public String user = "root";
        public String password = "";

        @Comment(@CommentValue(" Префикс таблиц"))
        public String tablePrefix = "dsc_";

        @Comment(@CommentValue(" Размер пула HikariCP"))
        public int poolSize = 6;

        @Comment(@CommentValue(" Идентификатор сервера для кросс-серверной синхронизации (уникален в сети)"))
        public String serverId = "server-1";

        @Comment(@CommentValue(" Хранить кулдауны в БД (иначе только в памяти)"))
        public boolean persistCooldowns = true;
    }
}
