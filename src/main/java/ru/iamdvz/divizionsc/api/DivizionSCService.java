package ru.iamdvz.divizionsc.api;

import java.util.Collection;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.service.DefRegistry;
import ru.iamdvz.divizionsc.def.service.DefService;

/**
 * Публичный API DivizionSC, регистрируемый в {@link org.bukkit.plugin.ServicesManager}.
 * Аддоны (DSC_MM/DSC_MEG) и сторонние плагины получают его через ServicesManager
 * (или через тонкий статический фасад {@link DivizionSCApi}).
 */
public interface DivizionSCService {

    EffectHandlerRegistry effectHandlers();

    DefRegistry defRegistry();

    Optional<DefDefinition> findDef(String id);

    Collection<DefDefinition> allDefs();

    DefService.CastResult cast(Player player, String defId);

    DefService.CastResult castIntegration(
            Player player,
            String defId,
            LivingEntity targetEntity,
            Location targetLocation,
            Block targetBlock,
            IntegrationCastOptions options);

    DefLoadReport reloadDefs();

    DefLoadReport lastLoadReport();

    Plugin plugin();
}
