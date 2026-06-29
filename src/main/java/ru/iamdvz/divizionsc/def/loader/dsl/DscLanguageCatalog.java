package ru.iamdvz.divizionsc.def.loader.dsl;

import ru.iamdvz.divizionsc.def.effect.EffectVerbs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Единый каталог языка .dsc для IDE (VS Code) и документации.
 * Синхронизируется с {@link DscLanguageExporter} при сборке плагина.
 */
public final class DscLanguageCatalog {

    private DscLanguageCatalog() {
    }

    public static Map<String, Object> toMap(String pluginVersion) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", pluginVersion);
        root.put("languageId", "dsc");
        root.put("fileExtension", ".dsc");
        root.put("blockKeywords", DscSyntax.blockKeywords());
        root.put("deprecatedBlockKeywords", DscSyntax.deprecatedBlockKeywords());
        root.put("sectionKeywords", List.of(
                "meta", "cast", "effects", "start", "hit", "done",
                "stack", "after", "if", "when", "else", "chance",
                "area", "loop", "aura", "projectile", "fx", "vfx"
        ));
        root.put("sectionAliases", DscSyntax.sectionAliases());
        root.put("routeSuffixes", DscSyntax.routeSuffixes());
        root.put("deprecatedKeywords", List.of("use", "call", "do", "def", "effect", "wait"));
        root.put("syntax", syntaxGuide());
        root.put("targetDirectives", List.of(
                "self", "caster", "target", "entity", "location", "block", "eyes",
                "start", "начало", "отправная", "отправная_точка",
                "end", "конец", "цель", "таргет"
        ));
        root.put("routeSyntax", DscSyntax.ROUTE);
        root.put("moduleCallSyntax", DscSyntax.MODULE_CALL);
        root.put("stackSyntax", DscSyntax.STACK_BLOCK);
        root.put("moduleExtendsSyntax", DscSyntax.MODULE_EXTENDS);
        root.put("fxProperties", fxProperties());
        root.put("properties", properties());
        root.put("verbs", verbs());
        root.put("castTriggers", castTriggers());
        root.put("passiveTriggers", passiveTriggers());
        root.put("targets", targets());
        root.put("conditions", conditions());
        root.put("placeholders", placeholders());
        root.put("chainSections", chainSections());
        root.put("vfxProperties", vfxProperties());
        root.put("snippets", snippets());
        return root;
    }

    private static List<Map<String, Object>> properties() {
        List<Map<String, Object>> list = new ArrayList<>();
        addProperty(list, "cd", List.of("cooldown"), "Кулдаун в секундах", "8");
        addProperty(list, "key", List.of("trigger"), "Триггер каста или клавиша пассивки", "rclick");
        addProperty(list, "target", List.of("tgt", "to"), "Режим выбора цели", "entity");
        addProperty(list, "range", List.of("rng"), "Дальность прицеливания", "32");
        addProperty(list, "perm", List.of("permission"), "Право на каст", "divizionsc.def.heal");
        addProperty(list, "mana", List.of("mp", "cost"), "Стоимость маны", "15");
        addProperty(list, "item", List.of("cast-item", "cast_item"), "Предмет каста: MATERIAL | имя | lore", "BLAZE_ROD | &cОгонь");
        addProperty(list, "name", List.of(), "Отображаемое имя", "&aЛечение");
        addProperty(list, "desc", List.of("description"), "Описание", "");
        addProperty(list, "helper", List.of(), "Устарело: используйте module { }", "—");
        addProperty(list, "passive", List.of(), "Пассивная способность", "true");
        addProperty(list, "event", List.of("on", "passive-trigger", "ptrigger"), "Событие пассивки", "damage");
        addProperty(list, "presses", List.of("combo", "press-count"), "Число нажатий для комбо", "3");
        addProperty(list, "press-window", List.of("press-interval"), "Окно между нажатиями", "2s");
        addProperty(list, "interval", List.of("passive-interval"), "Период для on interval", "60t");
        addProperty(list, "extends", List.of("extend"), "Наследование module", "strike");
        return list;
    }

    private static void addProperty(
            List<Map<String, Object>> list,
            String key,
            List<String> aliases,
            String description,
            String example
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("aliases", aliases);
        entry.put("description", description);
        entry.put("example", example);
        list.add(entry);
    }

    private static List<Map<String, Object>> verbs() {
        Map<String, List<String>> grouped = new TreeMap<>();
        for (String token : EffectVerbs.all()) {
            String canonical = EffectVerbs.canonicalType(token);
            if (canonical == null) {
                continue;
            }
            grouped.computeIfAbsent(canonical, k -> new ArrayList<>()).add(token);
        }

        Map<String, String> help = verbHelp();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            String canonical = entry.getKey();
            List<String> tokens = entry.getValue().stream().distinct().sorted().toList();
            Map<String, Object> verb = new LinkedHashMap<>();
            verb.put("canonical", canonical);
            verb.put("tokens", tokens);
            verb.put("description", help.getOrDefault(canonical, "Эффект DivizionSC"));
            verb.put("example", verbExamples().getOrDefault(canonical, canonical + " ..."));
            result.add(verb);
        }
        return result;
    }

    private static Map<String, String> verbHelp() {
        Map<String, String> help = new LinkedHashMap<>();
        help.put("damage", "Нанести урон цели");
        help.put("heal", "Восстановить HP");
        help.put("teleport", "Телепорт кастера вперёд или к цели");
        help.put("velocity", "Задать скорость / отброс");
        help.put("sound", "Проиграть звук Bukkit");
        help.put("particle", "Частицы в точке эффекта");
        help.put("potion", "Наложить зелье");
        help.put("lightning", "Молния (с уроном или fx)");
        help.put("message", "Сообщение кастеру");
        help.put("def", "Вызвать другой def / chain");
        help.put("projectile", "Запустить снаряд");
        help.put("effectlib", "EffectLib: fx() или блок fx { }");
        help.put("vfx", "ModelEngine VFX-блок (DSC_MEG)");
        help.put("command", "Консольная команда (%player%, %def%)");
        help.put("delay", "Задержка перед вложенными эффектами");
        help.put("set", "Переменная: set name value или формула");
        help.put("require", "Прервать каст, если условие ложно");
        help.put("if", "Условная ветка if / else");
        help.put("chance", "Случайная ветка с вероятностью");
        help.put("loop", "Повтор эффектов N раз");
        help.put("area", "Эффекты по сущностям в радиусе");
        help.put("stun", "Оглушение цели");
        help.put("raycast", "Raycast по взгляду — первый враг на луче");
        help.put("chain", "Raycast по нескольким целям на луче");
        help.put("ignite", "Поджечь цель (ticks)");
        help.put("glow", "Эффект свечения");
        help.put("invis", "Невидимость");
        help.put("title", "Title / subtitle / actionbar");
        help.put("swap", "Поменяться местами с целью");
        help.put("explosion", "Взрыв в точке (без grief по умолчанию)");
        help.put("cleanse", "Снять негативные эффекты");
        help.put("launch", "Подбросить вверх");
        help.put("root", "Обездвижить (root)");
        help.put("particle_projectile", "Частицы-снаряд");
        help.put("pull", "Притянуть цель к кастеру");
        help.put("push", "Оттолкнуть цель");
        help.put("money", "Операции с Vault");
        help.put("give-money", "Выдать деньги (Vault)");
        help.put("take-money", "Снять деньги (Vault)");
        help.put("dash", "Рывок вперёд");
        help.put("blink", "Короткий телепорт вперёд");
        help.put("shield", "Щит / неуязвимость на время");
        help.put("summon", "Призвать моба");
        help.put("totem", "Эффект тотема");
        help.put("give", "Выдать предмет");
        help.put("repeat", "Повторить эффекты");
        help.put("shape", "Фигура из частиц");
        help.put("aura", "Аура вокруг кастера");
        return help;
    }

    private static Map<String, String> verbExamples() {
        Map<String, String> examples = new LinkedHashMap<>();
        examples.put("damage", "damage(5) >> target");
        examples.put("heal", "heal(6) >> self");
        examples.put("teleport", "teleport(8) >> self");
        examples.put("velocity", "velocity(x=0, y=0.4, z=0) >> self");
        examples.put("sound", "sound(entity_blaze_shoot, volume=1.0, pitch=0.9) >> self");
        examples.put("particle", "particles(portal, count=30) >> target");
        examples.put("potion", "potion(speed, duration=5s, amplifier=1) >> self");
        examples.put("lightning", "lightning() >> target");
        examples.put("message", "message(\"&aГотово!\") >> self");
        examples.put("def", "@hearts_vfx >> self");
        examples.put("module-call", "@instant_sting(dmg=3) >> target");
        examples.put("projectile", "projectile(FIREBALL, speed=1.4) >> target");
        examples.put("effectlib", "fx(sphere, particle=heart, radius=1.2, particles=20) >> self");
        examples.put("command", "command(\"say %player% cast\") >> self");
        examples.put("delay", "after(10t) >> self { particles(flame, count=5) >> target }");
        examples.put("set", "set(power, 5) >> self");
        examples.put("require", "require(has-target) >> self");
        examples.put("if", "if (distance < 8) { damage(8) >> target } else { message(\"&7Далеко\") >> self }");
        examples.put("chance", "chance(35%) >> target { lightning() >> target }");
        examples.put("loop", "loop(times=3, interval=5) >> self { particles(flame, count=10) >> self }");
        examples.put("area", "area(radius=5) >> self { damage(2) >> target }");
        examples.put("stun", "stun(2s) >> target");
        examples.put("raycast", "raycast(20, hit_radius=1.5) >> target");
        examples.put("chain", "chain(18, hits=3) { damage(4) >> target }");
        examples.put("ignite", "ignite(80) >> target");
        examples.put("glow", "glow(100) >> target");
        examples.put("title", "title(\"&6Удар!\", subtitle=\"&7Крит\") >> self");
        examples.put("swap", "swap >> target");
        examples.put("explosion", "explosion(2, fire=true) >> location");
        examples.put("cleanse", "cleanse >> self");
        examples.put("launch", "launch(1.2) >> target");
        examples.put("root", "root(60) >> target");
        examples.put("particle_projectile", "particle_projectile(flame, speed=1.2) >> target");
        examples.put("pull", "pull(1.0) >> target");
        examples.put("push", "push(1.2) >> target");
        examples.put("give-money", "give_money(10) >> self");
        examples.put("take-money", "take_money(5) >> self");
        examples.put("dash", "dash(1.6) >> self");
        examples.put("blink", "blink(8) >> self");
        examples.put("shield", "shield(4) >> self");
        examples.put("summon", "summon(zombie, count=1) >> location");
        examples.put("totem", "totem() >> self");
        examples.put("give", "give(DIAMOND, count=1) >> self");
        examples.put("repeat", "repeat(5) >> self { particles(crit, count=3) >> self }");
        examples.put("shape", "shape(circle, radius=3) >> location");
        examples.put("aura", "aura(5) >> self { heal(1) >> self }");
        return examples;
    }

    private static List<Map<String, String>> castTriggers() {
        return List.of(
                entry("rclick", "ПКМ"),
                entry("lclick", "ЛКМ"),
                entry("shift", "Shift"),
                entry("ctrl", "Ctrl (бег)"),
                entry("space", "Пробел"),
                entry("w", "Вперёд"),
                entry("s", "Назад"),
                entry("a", "Влево"),
                entry("d", "Вправо"),
                entry("q", "Выбросить"),
                entry("f", "Swap hands"),
                entry("1", "Слот хотбара 1"),
                entry("2", "Слот хотбара 2"),
                entry("3", "Слот хотбара 3"),
                entry("4", "Слот хотбара 4"),
                entry("5", "Слот хотбара 5"),
                entry("6", "Слот хотбара 6"),
                entry("7", "Слот хотбара 7"),
                entry("8", "Слот хотбара 8"),
                entry("9", "Слот хотбара 9"),
                entry("cmd", "Только /dsc cast"),
                entry("any", "Любой триггер (Skill Bar)")
        );
    }

    private static List<Map<String, String>> passiveTriggers() {
        return List.of(
                entry("damage", "Получил урон"),
                entry("attack", "Нанёс урон (hit — синоним)"),
                entry("kill", "Убил существо"),
                entry("death", "Смерть"),
                entry("join", "Вход на сервер"),
                entry("break", "Сломал блок"),
                entry("fall", "Урон от падения"),
                entry("interval", "Периодически (нужен interval)")
        );
    }

    private static List<Map<String, String>> targets() {
        return List.of(
                entry("self", "Кастер"),
                entry("entity", "Существо в прицеле"),
                entry("mob", "Существо (алиас entity)"),
                entry("block", "Блок в прицеле"),
                entry("none", "Точка по взгляду + range")
        );
    }

    private static List<Map<String, String>> conditions() {
        return List.of(
                entry("health", "HP кастера (< > <= >= == !=, проценты)"),
                entry("distance", "Дистанция до цели"),
                entry("permission", "Право игрока"),
                entry("holding", "Предмет в руке"),
                entry("sneaking", "Присел"),
                entry("on-ground", "На земле"),
                entry("has-target", "Есть цель в прицеле"),
                entry("world", "Имя мира"),
                entry("region", "Регион WorldGuard"),
                entry("money", "Баланс Vault"),
                entry("variable", "Переменная set/var"),
                entry("chance", "Случайный roll")
        );
    }

    private static List<Map<String, String>> placeholders() {
        return List.of(
                entry("{caster_health}", "HP кастера"),
                entry("{caster_name}", "Имя кастера"),
                entry("{target_distance}", "Дистанция до цели"),
                entry("{target_health}", "HP цели"),
                entry("{def_id}", "id способности"),
                entry("{var_NAME}", "Переменная из set"),
                entry("{random}", "Случайное 0..1"),
                entry("{damage}", "Урон события (пассивки)"),
                entry("%player%", "Имя в cmd"),
                entry("%def%", "id в cmd"),
                entry("%papi_placeholder%", "PlaceholderAPI")
        );
    }

    private static List<Map<String, String>> syntaxGuide() {
        return List.of(
                entry("canonical", "SYNTAX.md — единый стандарт v2"),
                entry("blocks", "ability | passive | module { properties cast { } }"),
                entry("properties", "key: value — cooldown, key, target, range, perm, item"),
                entry("sections", "cast/effects — тело; start/hit/done — только @module"),
                entry("effects", "verb(args) >> route — канон; heal 6 — совместимость"),
                entry("modules", DscSyntax.MODULE_CALL),
                entry("stack", DscSyntax.STACK_BLOCK),
                entry("extends", DscSyntax.MODULE_EXTENDS),
                entry("route", DscSyntax.ROUTE),
                entry("deprecated", "def→ability, effect→module, do→cast, use/call→@module")
        );
    }

    private static List<Map<String, String>> chainSections() {
        return List.of(
                entry("start", "Эффекты в начале каста"),
                entry("cast", "Основное тело каста"),
                entry("hit", "Chain при попадании projectile"),
                entry("done", "Chain после cast"),
                entry("effects", "Тело module (helper)")
        );
    }

    private static List<String> vfxProperties() {
        return List.of(
                "position", "model-name", "model-scale", "remove-delay",
                "animations", "loop", "speed"
        );
    }

    private static List<String> fxProperties() {
        return List.of(
                "shape", "class", "particle", "at", "position",
                "sphere { radius particles y-offset radius-increase }",
                "helix { radius strands curve particles rotation }",
                "circle { radius particles whole-circle enable-rotation orient }",
                "line { length particles is-zig-zag zig-zags max-length }",
                "arc { height particles }",
                "tornado { tornado-particle max-tornado-radius show-cloud show-tornado }",
                "wave { rows width height velocity }",
                "timing { iterations period delay duration speed probability }",
                "base { visible-range auto-orient async offset relative-offset yaw-offset pitch-offset }",
                "target { at position }"
        );
    }

    private static List<Map<String, String>> snippets() {
        return List.of(
                snippet("ability", "ability ${1:id} {\n  meta {\n    name: \"&a${2:Name}\"\n    cd: ${3:8}\n    key: rclick\n    target: self\n    perm: divizionsc.def.${1:id}\n  }\n\n  cast {\n    $0\n  }\n}"),
                snippet("module", "module ${1:id} {\n  effects {\n    $0\n  }\n}"),
                snippet("passive", "passive ${1:id} {\n  meta {\n    name: \"&c${2:Name}\"\n    event: damage\n    cd: 2\n    target: entity\n    perm: divizionsc.def.${1:id}\n  }\n\n  cast {\n    $0\n  }\n}"),
                snippet("proj", "projectile(${1:FIREBALL}, speed=${2:1.4}) >> target {\n  hit {\n    @${3:pain}(dmg=5) >> target\n  }\n}"),
                snippet("fx", "fx {\n  shape ${1:sphere}\n  particle ${2:heart}\n  at self\n  ${1:sphere} { radius ${3:1.2} particles ${4:20} }\n  timing { iterations ${5:15} period ${6:2} }\n  base { visible-range 64 auto-orient true offset 0,1,0 }\n}")
        );
    }

    private static Map<String, String> entry(String value, String detail) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("value", value);
        map.put("detail", detail);
        return map;
    }

    private static Map<String, String> snippet(String name, String body) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("body", body);
        return map;
    }
}
