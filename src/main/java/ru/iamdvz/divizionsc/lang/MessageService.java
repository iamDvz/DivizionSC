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
        map.put("no-mana", "&cНедостаточно маны.");
        map.put("no-target", "&cНет цели в прицеле.");
        map.put("cast-success", "&aВы использовали &e{def}&a.");
        map.put("give-success", "&aВыдан предмет def &e{def}&a игроку &e{player}&a.");
        map.put("helper-only", "&cDef &e{def} &c— helper, кастуется только через chain.");
        map.put("passive-only", "&cDef &e{def} &c— пассивка, кастуется только по событию.");
        map.put("passive-header", "&6=== Активные пассивки ===");
        map.put("passive-line", "&e- {id} &7({name}) &8[{trigger}]");
        map.put("passive-granted", "&aПассивка &e{def} &aвыдана игроку &e{player}&a.");
        map.put("passive-revoked", "&eПассивка &f{def} &eснята с игрока &f{player}&e.");
        map.put("passive-not-found", "&cПассивка &e{def} &cне найдена.");
        map.put("usage-passive", "&e/dsc passive &7| &e/dsc passive list &7| &e/dsc passive add <id> [игрок]");
        map.put("passive-usage", "&7/dsc passive add|remove <id> [игрок] &8|&7 /dsc passive list");
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
        map.put("usage-header", "&6DivizionSC &7— команды:");
        map.put("usage-list", "&e/dsc list &7[страница|фильтр]");
        map.put("usage-skills", "&e/dsc skills &7— панель скиллов");
        map.put("usage-bind", "&e/dsc bind &7[слот] [def|clear]");
        map.put("usage-reload", "&e/dsc reload &7— перезагрузка defs");
        map.put("usage-validate", "&e/dsc validate &7— проверка defs без применения");
        map.put("validate-header", "&6Проверка defs: &a{count} &7ок, &c{errors} &7ошибок, &e{warnings} &7предупр.");
        map.put("validate-ok", "&aОшибок не найдено.");
        map.put("command-error", "&cОшибка выполнения команды: &7{error}");
        map.put("skillbar-title", "&6Панель скиллов");
        map.put("skillbar-no-permission", "&cНет доступа к панели скиллов.");
        map.put("skillbar-disabled", "&cПанель скиллов отключена.");
        map.put("skillbar-bound", "&a&l{def} &a→ слот &e{slot}&a.");
        map.put("skillbar-unbound", "&eСлот &f{slot} &eочищен.");
        map.put("skillbar-slot-selected", "&eСлот &f{slot} &eвыбран. Кликните скилл сверху.");
        map.put("skillbar-no-empty-slot", "&cВсе слоты заняты. Shift+ЛКМ по слоту — очистить.");
        map.put("skillbar-search-prompt", "&eВведите поисковый запрос в чат (или &fcancel&e для отмены).");
        map.put("bind-header", "&6=== Слоты 1-9 ===");
        map.put("bind-line", "&e{slot}&7: {def}");
        map.put("bind-usage", "&7/dsc bind <1-9> <def> &8|&7 &7/dsc bind <1-9> clear");
        map.put("bind-invalid-slot", "&cСлот должен быть от 1 до 9.");
        map.put("player-not-found", "&cИгрок не найден.");
        map.put("reload-detail", "&7({errors} ошибок, {warnings} предупреждений)");
        map.put("list-page-footer", "&7Стр. {page}/{pages} — /dsc list <страница>");
        map.put("browser-title", "&bБраузер способностей");
        map.put("browser-prev", "&e← Назад");
        map.put("browser-next", "&eВперёд →");
        map.put("browser-page-lore", "&7Страница {page}/{pages}");
        map.put("browser-info-title", "&bБраузер способностей");
        map.put("browser-info-lore-1", "&7Список доступных способностей");
        map.put("browser-info-lore-2", "&7ЛКМ по способности — каст");
        map.put("browser-def-id", "&7ID: &f{id}");
        map.put("browser-def-trigger", "&7Триггер: &f{trigger}");
        map.put("browser-def-target", "&7Цель: &f{target}");
        map.put("browser-def-range", "&7Дальность: &f{range}");
        map.put("browser-def-cd", "&7Кулдаун: &f{cooldown}s");
        map.put("browser-lmb-cast", "&aЛКМ &7— каст");
        map.put("skillbar-lmb-slot", "&eЛКМ &7→ слот &f{slot}");
        map.put("skillbar-shift-lmb-free", "&eShift+ЛКМ &7→ первый свободный");
        map.put("skillbar-slot-title", "{prefix}Слот {slot}");
        map.put("skillbar-slot-empty-active", "&eВыбран для привязки");
        map.put("skillbar-slot-empty-hint", "&7ЛКМ — выбрать слот");
        map.put("skillbar-slot-key", "&7Клавиша &f{key} &7— каст");
        map.put("skillbar-shift-clear", "&cShift+ЛКМ &7— очистить");
        map.put("skillbar-slot-broken", "&7Битый бинд: &e{def}");
        map.put("skillbar-slot-bound-active", "&e▶ Выбран для замены");
        map.put("skillbar-prev", "&e← Назад");
        map.put("skillbar-next", "&eВперёд →");
        map.put("skillbar-page-lore", "&7Страница {page}/{pages}");
        map.put("skillbar-search", "&eПоиск");
        map.put("skillbar-search-filter", "&aПоиск: &f{filter}");
        map.put("skillbar-search-hint", "&7ЛКМ — ввести запрос в чат");
        map.put("skillbar-search-change", "&7ЛКМ — изменить запрос");
        map.put("skillbar-search-reset", "&cПКМ — сбросить фильтр");
        map.put("skillbar-browser-btn", "&bБраузер способностей");
        map.put("skillbar-browser-lore-1", "&7ЛКМ — открыть список всех");
        map.put("skillbar-browser-lore-2", "&7способностей с описанием");
        map.put("skillbar-help-title", "&6Панель скиллов");
        map.put("skillbar-help-1", "&71. Выберите слот &a1-9 &7ниже");
        map.put("skillbar-help-2", "&72. Кликните скилл сверху");
        map.put("info-passive", "&bПассивка: &f{value}");
        map.put("info-combo", "&bКомбо: &f{value}");
        map.put("info-helper", "&cHelper — только chain");
        map.put("reload-error-line", "&c- {line}");
        map.put("reload-warning-line", "&e- {line}");
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
        map.put("no-mana", "&cNot enough mana.");
        map.put("no-target", "&cNo target in sight.");
        map.put("cast-success", "&aYou cast &e{def}&a.");
        map.put("give-success", "&aGave def &e{def} &ato &e{player}&a.");
        map.put("helper-only", "&cDef &e{def} &cis helper-only (chain only).");
        map.put("passive-only", "&cDef &e{def} &cis passive-only (event triggered).");
        map.put("passive-header", "&6=== Active passives ===");
        map.put("passive-line", "&e- {id} &7({name}) &8[{trigger}]");
        map.put("passive-granted", "&aPassive &e{def} &agranted to &e{player}&a.");
        map.put("passive-revoked", "&ePassive &f{def} &eremoved from &f{player}&e.");
        map.put("passive-not-found", "&cPassive &e{def} &cnot found.");
        map.put("usage-passive", "&e/dsc passive &7| &e/dsc passive list &7| &e/dsc passive add <id> [player]");
        map.put("passive-usage", "&7/dsc passive add|remove <id> [player] &8|&7 /dsc passive list");
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
        map.put("usage-header", "&6DivizionSC &7— commands:");
        map.put("usage-list", "&e/dsc list &7[page|filter]");
        map.put("usage-skills", "&e/dsc skills &7— skill bar");
        map.put("usage-bind", "&e/dsc bind &7[slot] [def|clear]");
        map.put("usage-reload", "&e/dsc reload &7— reload defs");
        map.put("usage-validate", "&e/dsc validate &7— validate defs without applying");
        map.put("validate-header", "&6Defs check: &a{count} &7ok, &c{errors} &7errors, &e{warnings} &7warnings.");
        map.put("validate-ok", "&aNo issues found.");
        map.put("command-error", "&cCommand failed: &7{error}");
        map.put("skillbar-title", "&6Skill Bar");
        map.put("skillbar-no-permission", "&cNo access to skill bar.");
        map.put("skillbar-disabled", "&cSkill bar is disabled.");
        map.put("skillbar-bound", "&a&l{def} &a→ slot &e{slot}&a.");
        map.put("skillbar-unbound", "&eSlot &f{slot} &ecleared.");
        map.put("skillbar-slot-selected", "&eSlot &f{slot} &eselected. Click a skill above.");
        map.put("skillbar-no-empty-slot", "&cAll slots are full. Shift+click a slot to clear.");
        map.put("skillbar-search-prompt", "&eType your search query in chat (or &fcancel&e to abort).");
        map.put("bind-header", "&6=== Slots 1-9 ===");
        map.put("bind-line", "&e{slot}&7: {def}");
        map.put("bind-usage", "&7/dsc bind <1-9> <def> &8|&7 &7/dsc bind <1-9> clear");
        map.put("bind-invalid-slot", "&cSlot must be between 1 and 9.");
        map.put("player-not-found", "&cPlayer not found.");
        map.put("reload-detail", "&7({errors} errors, {warnings} warnings)");
        map.put("list-page-footer", "&7Page {page}/{pages} — /dsc list <page>");
        map.put("browser-title", "&bAbility Browser");
        map.put("browser-prev", "&e← Back");
        map.put("browser-next", "&eNext →");
        map.put("browser-page-lore", "&7Page {page}/{pages}");
        map.put("browser-info-title", "&bAbility Browser");
        map.put("browser-info-lore-1", "&7List of available abilities");
        map.put("browser-info-lore-2", "&7LMB on ability — cast");
        map.put("browser-def-id", "&7ID: &f{id}");
        map.put("browser-def-trigger", "&7Trigger: &f{trigger}");
        map.put("browser-def-target", "&7Target: &f{target}");
        map.put("browser-def-range", "&7Range: &f{range}");
        map.put("browser-def-cd", "&7Cooldown: &f{cooldown}s");
        map.put("browser-lmb-cast", "&aLMB &7— cast");
        map.put("skillbar-lmb-slot", "&eLMB &7→ slot &f{slot}");
        map.put("skillbar-shift-lmb-free", "&eShift+LMB &7→ first free");
        map.put("skillbar-slot-title", "{prefix}Slot {slot}");
        map.put("skillbar-slot-empty-active", "&eSelected for binding");
        map.put("skillbar-slot-empty-hint", "&7LMB — select slot");
        map.put("skillbar-slot-key", "&7Key &f{key} &7— cast");
        map.put("skillbar-shift-clear", "&cShift+LMB &7— clear");
        map.put("skillbar-slot-broken", "&7Broken bind: &e{def}");
        map.put("skillbar-slot-bound-active", "&e▶ Selected for replace");
        map.put("skillbar-prev", "&e← Back");
        map.put("skillbar-next", "&eNext →");
        map.put("skillbar-page-lore", "&7Page {page}/{pages}");
        map.put("skillbar-search", "&eSearch");
        map.put("skillbar-search-filter", "&aSearch: &f{filter}");
        map.put("skillbar-search-hint", "&7LMB — type query in chat");
        map.put("skillbar-search-change", "&7LMB — change query");
        map.put("skillbar-search-reset", "&cRMB — reset filter");
        map.put("skillbar-browser-btn", "&bAbility Browser");
        map.put("skillbar-browser-lore-1", "&7LMB — open full list");
        map.put("skillbar-browser-lore-2", "&7with descriptions");
        map.put("skillbar-help-title", "&6Skill Bar");
        map.put("skillbar-help-1", "&71. Select slot &a1-9 &7below");
        map.put("skillbar-help-2", "&72. Click a skill above");
        map.put("info-passive", "&bPassive: &f{value}");
        map.put("info-combo", "&bCombo: &f{value}");
        map.put("info-helper", "&cHelper — chain only");
        map.put("reload-error-line", "&c- {line}");
        map.put("reload-warning-line", "&e- {line}");
        return map;
    }
}
