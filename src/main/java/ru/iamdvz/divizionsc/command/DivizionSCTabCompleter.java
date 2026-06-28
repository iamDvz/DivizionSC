package ru.iamdvz.divizionsc.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.DefDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DivizionSCTabCompleter implements TabCompleter {

    private static final List<String> ROOT = List.of("list", "info", "cast", "give", "bind", "skills", "reload");

    private final PluginContext context;

    public DivizionSCTabCompleter(PluginContext context) {
        this.context = context;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(ROOT, args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "info", "cast", "give" -> filterDefIds(args[1]);
                case "bind" -> filterHotbarSlots(args[1]);
                case "list" -> filter(List.of("1", "2", "3"), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give" -> filterOnlinePlayers(args[2]);
                case "bind" -> {
                    if ("clear".startsWith(args[2].toLowerCase(Locale.ROOT))) {
                        yield List.of("clear");
                    }
                    yield filterDefIds(args[2]);
                }
                default -> List.of();
            };
        }
        return List.of();
    }

    private List<String> filterDefIds(String prefix) {
        List<String> ids = new ArrayList<>();
        for (DefDefinition def : context.defRegistry().all()) {
            if (!def.helper()) {
                ids.add(def.id());
            }
        }
        return filter(ids, prefix);
    }

    private List<String> filterHotbarSlots(String prefix) {
        return filter(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), prefix);
    }

    private List<String> filterOnlinePlayers(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return filter(names, prefix);
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
