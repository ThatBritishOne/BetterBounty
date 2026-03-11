package dev.vireon.bounty.listener;

import dev.vireon.bounty.BountyPlugin;
import dev.vireon.bounty.bounty.Bounty;
import dev.vireon.bounty.util.ChatUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@RequiredArgsConstructor
public class DeathListener implements Listener {

    private final BountyPlugin plugin;

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (killer.getUniqueId().equals(player.getUniqueId())) return;

        Bounty bounty = plugin.getBountyManager().getBounty(player.getUniqueId());
        if (bounty == null) return;

        // Calculate after-tax amount using the tax system
        long afterTaxAmount = plugin.getBountyManager().calculateAfterTaxAmount(bounty.getAmount());

        plugin.getBountyManager().removeBounty(player.getUniqueId());
        plugin.getEconomyManager().add(killer, afterTaxAmount);

        ChatUtils.sendMessage(killer, ChatUtils.format(
                plugin.getConfig().getString("messages.bounty-claimed"),
                Placeholder.unparsed("amount", ChatUtils.FORMATTER.format(afterTaxAmount)),
                Placeholder.unparsed("player", bounty.getPlayerName())
        ));

        // Run configurable command on bounty claimed
        String commandTemplate = plugin.getConfig().getString("settings.on-bounty-claimed-command");
        if (commandTemplate != null && !commandTemplate.isEmpty()) {
            String command = commandTemplate
                .replace("{killer}", killer.getName())
                .replace("{personKilled}", player.getName())
                .replace("{amount}", ChatUtils.FORMATTER.format(afterTaxAmount));
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }

        // Drop the killed player's skull at their death location
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(player.getName() + "'s Head");
        skull.setItemMeta(meta);
        player.getWorld().dropItemNaturally(player.getLocation(), skull);

        String soundKey = plugin.getConfig().getString("settings.sounds.claim");
        if (soundKey != null && !soundKey.isEmpty()) {
            plugin.getScheduler().runAtEntity(killer, _task -> killer.playSound(Sound.sound(Key.key(soundKey), Sound.Source.MASTER, 1.0f, 1.0f)));
        }
    }

}
