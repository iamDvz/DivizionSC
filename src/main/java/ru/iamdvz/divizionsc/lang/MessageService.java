package ru.iamdvz.divizionsc.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.util.ColorUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class MessageService {

    private final JavaPlugin plugin;
    private PluginConfig config;
    private final Map<String, Map<String, String>> messages = new HashMap<>();

    public MessageService(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        reload(config);
    }

    public void reload(PluginConfig newConfig) {
        config = newConfig;
        messages.clear();
        loadLocale("ru", defaultRu());
        loadLocale("en", defaultEn());
    }

    public void send(Player player, String key) {
        send(player, key, Component.empty());
    }

    public void send(Player player, String key, Component replacement) {
        String raw = resolve(key);
        Component message = ColorUtil.component(raw).replaceText(
                TextReplacementConfig.builder().matchLiteral("{value}").replacement(replacement).build()
        );
        player.sendMessage(message);
    }

    public Component format(String key, String placeholder, String value) {
        String raw = resolve(key).replace("{" + placeholder + "}", value);
        return ColorUtil.component(raw);
    }

    public Component format(String key, Map<String, String> placeholders) {
        String raw = resolve(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ColorUtil.component(raw);
    }

    private String resolve(String key) {
        Map<String, String> localeMessages = messages.getOrDefault(config.locale(), messages.get("ru"));
        return localeMessages.getOrDefault(key, "&cMissing message: " + key);
    }

    private void loadLocale(String locale, Map<String, String> defaults) {
        File file = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        FileConfiguration yaml;
        if (!file.exists()) {
            yaml = new YamlConfiguration();
            for (Map.Entry<String, String> entry : defaults.entrySet()) {
                yaml.set(entry.getKey(), entry.getValue());
            }
            try {
                yaml.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save lang/" + locale + ".yml", e);
            }
        } else {
            yaml = YamlConfiguration.loadConfiguration(file);
            boolean changed = false;
            for (Map.Entry<String, String> entry : defaults.entrySet()) {
                if (!yaml.contains(entry.getKey())) {
                    yaml.set(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
            if (changed) {
                try {
                    yaml.save(file);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to merge lang/" + locale + ".yml", e);
                }
            }
        }

        Map<String, String> loaded = new HashMap<>();
        for (String key : yaml.getKeys(false)) {
            loaded.put(key, yaml.getString(key, defaults.get(key)));
        }
        messages.put(locale, loaded);
    }

    private static Map<String, String> defaultRu() {
        Map<String, String> map = new HashMap<>();
        map.put("no-permission", "&cНет прав.");
        map.put("player-only", "&cТолько для игроков.");
        map.put("reload-success", "&aDivizionSC перезагружен. Def: {count}");
        map.put("def-not-found", "&cDef &e{def} &cне найден.");
        map.put("ability-not-found", "&cDef &e{id} &cне найден.");
        map.put("cooldown", "&cПодождите &e{value}&c сек.");
        map.put("no-target", "&cНет цели в прицеле.");
        map.put("cast-success", "&aВы использовали &e{def}&a.");
        map.put("give-success", "&aВыдан предмет def &e{def}&a игроку &e{player}&a.");
        map.put("helper-only", "&cDef &e{def} &c— helper, кастуется только через chain.");
        map.put("wrong-trigger", "&cDef &e{def} &cнельзя кастовать так. Используй нужный триггер или &e/dsc give {def}&c.");
        map.put("list-header", "&6=== Defs ({count}) ===");
        map.put("list-line", "&e- {id} &7({name}) &8[{trigger}]");
        map.put("info-header", "&6=== {name} &7({id}) ===");
        map.put("info-description", "&7{description}");
        map.put("info-cooldown", "&bКулдаун: &f{cooldown}s");
        map.put("info-trigger", "&bТриггер: &f{trigger}");
        map.put("info-target", "&bЦель: &f{target}");
        map.put("usage-cast", "&c/dsc cast <def>");
        map.put("usage-give", "&c/dsc give <def> [игрок]");
        map.put("usage-info", "&c/dsc info <def>");
        map.put("skillbar-title", "&6Панель скиллов");
        map.put("skillbar-no-permission", "&cНет доступа к панели скиллов.");
        map.put("skillbar-disabled", "&cПанель скиллов отключена.");
        map.put("skillbar-bound", "&a&l{def} &a→ слот &e{slot}&a.");
        map.put("skillbar-unbound", "&eСлот &f{slot} &eочищен.");
        map.put("skillbar-slot-selected", "&eСлот &f{slot} &eвыбран. Кликните скилл сверху.");
        map.put("skillbar-no-empty-slot", "&cВсе слоты заняты. Shift+ЛКМ по слоту — очистить.");
        map.put("bind-header", "&6=== Слоты 1-9 ===");
        map.put("bind-line", "&e{slot}&7: {def}");
        map.put("bind-usage", "&7/dsc bind <1-9> <def> &8|&7 &7/dsc bind <1-9> clear");
        map.put("bind-invalid-slot", "&cСлот должен быть от 1 до 9.");
        map.put("player-not-found", "&cИгрок не найден.");
        map.put("wrong-trigger", "&cНельзя использовать &e{def} &cэтим действием.");
        map.put("reload-detail", "&7({errors} ошибок, {warnings} предупреждений)");
        map.put("list-page-footer", "&7Стр. {page}/{pages} — /dsc list <страница>");
        return map;
    }

    private static Map<String, String> defaultEn() {
        Map<String, String> map = new HashMap<>();
        map.put("no-permission", "&cNo permission.");
        map.put("player-only", "&cPlayers only.");
        map.put("reload-success", "&aDivizionSC reloaded. Defs: {count}");
        map.put("def-not-found", "&cDef &e{def} &cnot found.");
        map.put("ability-not-found", "&cDef &e{id} &cnot found.");
        map.put("cooldown", "&cWait &e{value}&c sec.");
        map.put("no-target", "&cNo target in sight.");
        map.put("cast-success", "&aYou cast &e{def}&a.");
        map.put("give-success", "&aGave def &e{def} &ato &e{player}&a.");
        map.put("helper-only", "&cDef &e{def} &cis helper-only (chain only).");
        map.put("wrong-trigger", "&cDef &e{def} &ccannot be cast this way. Use the correct trigger or &e/dsc give {def}&c.");
        map.put("list-header", "&6=== Defs ({count}) ===");
        map.put("list-line", "&e- {id} &7({name}) &8[{trigger}]");
        map.put("info-header", "&6=== {name} &7({id}) ===");
        map.put("info-description", "&7{description}");
        map.put("info-cooldown", "&bCooldown: &f{cooldown}s");
        map.put("info-trigger", "&bTrigger: &f{trigger}");
        map.put("info-target", "&bTarget: &f{target}");
        map.put("usage-cast", "&c/dsc cast <def>");
        map.put("usage-give", "&c/dsc give <def> [player]");
        map.put("usage-info", "&c/dsc info <def>");
        map.put("skillbar-title", "&6Skill Bar");
        map.put("skillbar-no-permission", "&cNo access to skill bar.");
        map.put("skillbar-disabled", "&cSkill bar is disabled.");
        map.put("skillbar-bound", "&a&l{def} &a→ slot &e{slot}&a.");
        map.put("skillbar-unbound", "&eSlot &f{slot} &ecleared.");
        map.put("skillbar-slot-selected", "&eSlot &f{slot} &eselected. Click a skill above.");
        map.put("skillbar-no-empty-slot", "&cAll slots are full. Shift+click a slot to clear.");
        map.put("bind-header", "&6=== Slots 1-9 ===");
        map.put("bind-line", "&e{slot}&7: {def}");
        map.put("bind-usage", "&7/dsc bind <1-9> <def> &8|&7 &7/dsc bind <1-9> clear");
        map.put("bind-invalid-slot", "&cSlot must be between 1 and 9.");
        map.put("player-not-found", "&cPlayer not found.");
        map.put("wrong-trigger", "&cCannot use &e{def} &cwith this action.");
        map.put("reload-detail", "&7({errors} errors, {warnings} warnings)");
        map.put("list-page-footer", "&7Page {page}/{pages} — /dsc list <page>");
        return map;
    }
}
