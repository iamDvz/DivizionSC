package ru.iamdvz.divizionsc.api;

import java.util.Collection;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.service.DefRegistry;
import ru.iamdvz.divizionsc.def.service.DefService;

/**
 * Тонкий статический фасад над {@link DivizionSCService}, зарегистрированным в ServicesManager.
 * Сохранён для обратной совместимости аддонов; новый код может брать сервис напрямую.
 */
public final class DivizionSCApi {

    private DivizionSCApi() {
    }

    public static Optional<DivizionSCService> service() {
        RegisteredServiceProvider<DivizionSCService> provider =
                Bukkit.getServicesManager().getRegistration(DivizionSCService.class);
        return provider == null ? Optional.empty() : Optional.ofNullable(provider.getProvider());
    }

    public static boolean isAvailable() {
        return service().isPresent();
    }

    private static DivizionSCService require() {
        return service().orElseThrow(() -> new IllegalStateException("DivizionSC service is not registered"));
    }

    public static EffectHandlerRegistry effectHandlers() {
        return require().effectHandlers();
    }

    public static DefRegistry defRegistry() {
        return require().defRegistry();
    }

    public static Optional<DefDefinition> findDef(String id) {
        return require().findDef(id);
    }

    public static Collection<DefDefinition> allDefs() {
        return require().allDefs();
    }

    public static DefService.CastResult cast(Player player, String defId) {
        return require().cast(player, defId);
    }

    public static DefService.CastResult castIntegration(
            Player player,
            String defId,
            LivingEntity targetEntity,
            Location targetLocation,
            IntegrationCastOptions options) {
        return require().castIntegration(player, defId, targetEntity, targetLocation, null, options);
    }

    public static DefService.CastResult castIntegration(
            Player player,
            String defId,
            LivingEntity targetEntity,
            Location targetLocation,
            Block targetBlock,
            IntegrationCastOptions options) {
        return require().castIntegration(player, defId, targetEntity, targetLocation, targetBlock, options);
    }

    public static DefLoadReport reloadDefs() {
        return require().reloadDefs();
    }

    public static DefLoadReport lastLoadReport() {
        return require().lastLoadReport();
    }

    public static Plugin plugin() {
        return require().plugin();
    }
}
