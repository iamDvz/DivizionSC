package ru.iamdvz.divizionsc.integration;

import org.bukkit.entity.Player;

/**
 * Абстракция экономики (реализуется VaultHook). Позволяет ядру не зависеть от Vault напрямую.
 */
public interface EconomyBridge {

    double balance(Player player);

    boolean withdraw(Player player, double amount);

    boolean deposit(Player player, double amount);
}
