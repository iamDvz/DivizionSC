package ru.iamdvz.divizionsc.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Интеграция с Vault: предоставляет {@link EconomyBridge} поверх найденного провайдера экономики.
 * Класс загружается только если Vault присутствует на сервере.
 */
public final class VaultHook {

    private VaultHook() {
    }

    public static EconomyBridge resolve() {
        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return null;
        }
        Economy economy = provider.getProvider();
        if (economy == null) {
            return null;
        }
        return new VaultEconomyBridge(economy);
    }

    private record VaultEconomyBridge(Economy economy) implements EconomyBridge {

        @Override
        public double balance(Player player) {
            return economy.getBalance(player);
        }

        @Override
        public boolean withdraw(Player player, double amount) {
            if (amount <= 0 || economy.getBalance(player) < amount) {
                return false;
            }
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        }

        @Override
        public boolean deposit(Player player, double amount) {
            if (amount <= 0) {
                return false;
            }
            return economy.depositPlayer(player, amount).transactionSuccess();
        }
    }
}
