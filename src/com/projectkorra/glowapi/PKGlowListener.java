package com.projectkorra.glowapi;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PKGlowListener implements Listener {
	GlowHandler g = PKGlowAPI.plugin.getGlowHandler();

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();

		if(g.isGlowEntity(p)) {
			g.remove((LivingEntity) p);
		} else if (g.isGlowReceiver(p)) {
			g.remove(p);
		}
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Player p = event.getPlayer();

		if(g.isGlowEntity(p)) {
			g.remove((LivingEntity) p);
		} else if (g.isGlowReceiver(p)) {
			g.remove(p);
		}
	}
}
