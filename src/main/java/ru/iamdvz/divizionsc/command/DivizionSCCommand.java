package ru.iamdvz.divizionsc.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.service.DefService;
import ru.iamdvz.divizionsc.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class DivizionSCCommand implements CommandExecutor {

    private final PluginContext context;
    private final java.util.function.Supplier<DefLoadReport> reloadAction;

    public DivizionSCCommand(PluginContext context, java.util.function.Supplier<DefLoadReport> reloadAction) {
        this.context = context;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return dispatch(sender, args);
        } catch (Exception error) {
            String joined = String.join(" ", args);
            context.plugin().getLogger().log(
                    Level.SEVERE,
                    "Command failed: /" + label + (joined.isBlank() ? "" : " " + joined),
                    error
            );
            sender.sendMessage(context.messages().format(
                    "command-error",
                    Map.of("error", error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage())
            ));
            return true;
        }
    }

    private boolean dispatch(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return help(sender);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help", "?" -> help(sender);
            case "reload" -> reload(sender);
            case "validate", "check" -> validate(sender);
            case "list" -> list(sender, args);
            case "info" -> info(sender, args);
            case "cast" -> cast(sender, args);
            case "give" -> give(sender, args);
            case "bind", "slot" -> bind(sender, args);
            case "skills", "bar", "binds" -> skills(sender);
            default -> castOrList(sender, args);
        };
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage(context.messages().format("usage-header", Map.of()));
        sender.sendMessage(context.messages().format("usage-list", Map.of()));
        sender.sendMessage(context.messages().format("usage-info", Map.of("def", "<id>")));
        sender.sendMessage(context.messages().format("usage-cast", Map.of("def", "<id>")));
        sender.sendMessage(context.messages().format("usage-give", Map.of("def", "<id>")));
        sender.sendMessage(context.messages().format("usage-skills", Map.of()));
        sender.sendMessage(context.messages().format("usage-bind", Map.of()));
        if (sender.hasPermission(context.config().adminPermission())) {
            sender.sendMessage(context.messages().format("usage-reload", Map.of()));
            sender.sendMessage(context.messages().format("usage-validate", Map.of()));
        }
        return true;
    }

    private boolean castOrList(CommandSender sender, String[] args) {
        if (sender instanceof Player player && context.defRegistry().find(args[0]).isPresent()) {
            DefService.CastResult result = context.defs().castFromCommand(player, args[0]);
            if (result == DefService.CastResult.NOT_FOUND) {
                player.sendMessage(context.messages().format(
                        "def-not-found",
                        Map.of("def", args[0], "id", args[0])
                ));
            } else if (result != DefService.CastResult.SUCCESS) {
                context.defs().notifyCastFailure(player, result, args[0]);
            }
            return true;
        }
        return list(sender, new String[]{"list", args[0]});
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(context.config().adminPermission())) {
            sendNoPermission(sender);
            return true;
        }
        DefLoadReport report = reloadAction.get();
        sender.sendMessage(context.messages().format(
                "reload-success",
                Map.of("count", String.valueOf(report.loadedCount()))
        ));
        if (report.hasIssues()) {
            sender.sendMessage(context.messages().format(
                    "reload-detail",
                    Map.of(
                            "errors", String.valueOf(report.errors().size()),
                            "warnings", String.valueOf(report.warnings().size())
                    )
            ));
            for (String error : report.errors()) {
                sender.sendMessage(ColorUtil.component("&c- " + error));
            }
            for (String warning : report.warnings()) {
                sender.sendMessage(ColorUtil.component("&e- " + warning));
            }
        }
        return true;
    }

    private boolean validate(CommandSender sender) {
        if (!sender.hasPermission(context.config().adminPermission())) {
            sendNoPermission(sender);
            return true;
        }
        DefLoadReport report = context.validateDefs();
        sender.sendMessage(context.messages().format(
                "validate-header",
                Map.of(
                        "count", String.valueOf(report.loadedCount()),
                        "errors", String.valueOf(report.errors().size()),
                        "warnings", String.valueOf(report.warnings().size())
                )
        ));
        for (String error : report.errors()) {
            sender.sendMessage(ColorUtil.component("&c- " + error));
        }
        for (String warning : report.warnings()) {
            sender.sendMessage(ColorUtil.component("&e- " + warning));
        }
        if (!report.hasIssues()) {
            sender.sendMessage(context.messages().format("validate-ok", Map.of()));
        }
        return true;
    }

    private boolean list(CommandSender sender, String[] args) {
        int page = 1;
        String filter = "";

        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
                filter = args[1].toLowerCase(Locale.ROOT);
                if (args.length >= 3) {
                    try {
                        page = Math.max(1, Integer.parseInt(args[2]));
                    } catch (NumberFormatException ignoredPage) {
                        page = 1;
                    }
                }
            }
        }

        List<DefDefinition> defs = new ArrayList<>();
        for (DefDefinition def : context.defRegistry().all()) {
            if (!filter.isEmpty() && !def.id().contains(filter) && !stripColor(def.name()).toLowerCase(Locale.ROOT).contains(filter)) {
                continue;
            }
            defs.add(def);
        }

        int pageSize = context.config().listPageSize();
        int pages = Math.max(1, (int) Math.ceil(defs.size() / (double) pageSize));
        page = Math.min(page, pages);
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, defs.size());

        sender.sendMessage(context.messages().format(
                "list-header",
                Map.of("count", String.valueOf(context.defRegistry().size()))
        ));

        for (int i = from; i < to; i++) {
            DefDefinition def = defs.get(i);
            sender.sendMessage(context.messages().format(
                    "list-line",
                    Map.of(
                            "id", def.id(),
                            "name", stripColor(def.name()),
                            "trigger", def.trigger().name(),
                            "def", def.id(),
                            "ability", def.id()
                    )
            ));
        }

        if (pages > 1) {
            sender.sendMessage(context.messages().format(
                    "list-page-footer",
                    Map.of("page", String.valueOf(page), "pages", String.valueOf(pages))
            ));
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(context.messages().format("usage-info", Map.of("def", "")));
            return true;
        }
        return context.defRegistry().find(args[1]).map(def -> {
            sender.sendMessage(context.messages().format(
                    "info-header",
                    Map.of("name", stripColor(def.name()), "id", def.id(), "def", def.id())
            ));
            if (!def.description().isBlank()) {
                sender.sendMessage(context.messages().format(
                        "info-description",
                        Map.of("description", def.description())
                ));
            }
            sender.sendMessage(context.messages().format(
                    "info-cooldown",
                    Map.of("cooldown", String.valueOf(def.cooldown()))
            ));
            sender.sendMessage(context.messages().format(
                    "info-trigger",
                    Map.of("trigger", def.passive()
                            ? passiveTriggerLabel(def)
                            : def.trigger().name())
            ));
            if (def.passive()) {
                sender.sendMessage(ColorUtil.component("&7passive: &atrue"));
                if (def.passiveKeyTrigger() != null && def.passivePressCount() > 1) {
                    sender.sendMessage(ColorUtil.component(
                            "&7combo: &e" + def.passivePressCount() + "x &7window &e"
                                    + def.passivePressWindowTicks() + "t"));
                }
            }
            sender.sendMessage(context.messages().format(
                    "info-target",
                    Map.of("target", def.targetMode().name())
            ));
            if (def.helper()) {
                sender.sendMessage(ColorUtil.component("&7helper: &atrue"));
            }
            return true;
        }).orElseGet(() -> {
            sender.sendMessage(context.messages().format("def-not-found", Map.of("def", args[1], "id", args[1])));
            return true;
        });
    }

    private boolean cast(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(context.messages().format("player-only", Map.of("def", "")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(context.messages().format("usage-cast", Map.of("def", "")));
            return true;
        }
        DefService.CastResult result = context.defs().castFromCommand(player, args[1]);
        if (result == DefService.CastResult.NOT_FOUND) {
            player.sendMessage(context.messages().format("def-not-found", Map.of("def", args[1], "id", args[1])));
        } else if (result != DefService.CastResult.SUCCESS) {
            context.defs().notifyCastFailure(player, result, args[1]);
        }
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission(context.config().adminPermission())) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(context.messages().format("usage-give", Map.of("def", "")));
            return true;
        }
        return context.defRegistry().find(args[1]).map(def -> {
            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(context.messages().format("player-not-found", Map.of("player", args[2])));
                    return true;
                }
            } else if (sender instanceof Player player) {
                target = player;
            } else {
                sender.sendMessage(context.messages().format("player-only", Map.of("def", "")));
                return true;
            }

            ItemStack item = context.castItems().create(def);
            target.getInventory().addItem(item);
            sender.sendMessage(context.messages().format(
                    "give-success",
                    Map.of("def", stripColor(def.name()), "player", target.getName(), "ability", stripColor(def.name()))
            ));
            return true;
        }).orElseGet(() -> {
            sender.sendMessage(context.messages().format("def-not-found", Map.of("def", args[1], "id", args[1])));
            return true;
        });
    }

    private boolean skills(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(context.messages().format("player-only", Map.of("def", "")));
            return true;
        }
        context.skillBarListener().openSkillBar(player);
        return true;
    }

    private boolean bind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(context.messages().format("player-only", Map.of("def", "")));
            return true;
        }
        if (!context.config().skillBar().enabled()) {
            context.messages().send(player, "skillbar-disabled");
            return true;
        }
        if (!player.hasPermission(context.config().skillBar().permission())) {
            context.messages().send(player, "skillbar-no-permission");
            return true;
        }

        if (args.length == 1) {
            sender.sendMessage(context.messages().format("bind-header", Map.of()));
            for (int slot = 1; slot <= 9; slot++) {
                var bound = context.binds().boundDef(player, slot - 1);
                String defName = bound.flatMap(id -> context.defRegistry().find(id).map(DefDefinition::name))
                        .orElse("&8—");
                sender.sendMessage(context.messages().format(
                        "bind-line",
                        Map.of("slot", String.valueOf(slot), "def", defName)
                ));
            }
            sender.sendMessage(context.messages().format("bind-usage", Map.of()));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(context.messages().format("bind-usage", Map.of()));
            return true;
        }

        int hotbarSlot = parseHotbarSlot(args[1]);
        if (hotbarSlot < 0) {
            sender.sendMessage(context.messages().format("bind-invalid-slot", Map.of()));
            return true;
        }

        if (args.length < 3) {
            var bound = context.binds().boundDef(player, hotbarSlot);
            if (bound.isEmpty()) {
                player.sendMessage(context.messages().format(
                        "bind-line",
                        Map.of("slot", String.valueOf(hotbarSlot + 1), "def", "&8—")
                ));
            } else {
                String defName = context.defRegistry().find(bound.get()).map(DefDefinition::name).orElse(bound.get());
                player.sendMessage(context.messages().format(
                        "bind-line",
                        Map.of("slot", String.valueOf(hotbarSlot + 1), "def", defName)
                ));
            }
            if (args.length == 2) {
                sender.sendMessage(context.messages().format("bind-usage", Map.of()));
            }
            return true;
        }

        if ("clear".equalsIgnoreCase(args[2])) {
            context.binds().unbind(player, hotbarSlot);
            player.sendMessage(context.messages().format(
                    "skillbar-unbound",
                    Map.of("slot", String.valueOf(hotbarSlot + 1))
            ));
            return true;
        }

        String defId = args[2];
        if (context.defRegistry().find(defId).isEmpty()) {
            player.sendMessage(context.messages().format("def-not-found", Map.of("def", defId, "id", defId)));
            return true;
        }
        context.binds().bind(player, hotbarSlot, defId);
        String defName = context.defRegistry().find(defId).map(DefDefinition::name).orElse(defId);
        player.sendMessage(context.messages().format(
                "skillbar-bound",
                Map.of("def", defName, "slot", String.valueOf(hotbarSlot + 1))
        ));
        return true;
    }

    private int parseHotbarSlot(String raw) {
        try {
            int slot = Integer.parseInt(raw);
            if (slot >= 1 && slot <= 9) {
                return slot - 1;
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    private static String passiveTriggerLabel(DefDefinition def) {
        if (def.passiveTrigger() != null) {
            return "PASSIVE:" + def.passiveTrigger().name();
        }
        if (def.passiveKeyTrigger() != null) {
            return "PASSIVE:KEY:" + def.passiveKeyTrigger().name();
        }
        return "PASSIVE:?";
    }

    private void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player player) {
            context.messages().send(player, "no-permission");
        } else {
            sender.sendMessage(context.messages().format("no-permission", Map.of("x", "x")));
        }
    }

    private String stripColor(String text) {
        return text == null ? "" : text.replaceAll("&[0-9a-fk-or]", "");
    }
}
