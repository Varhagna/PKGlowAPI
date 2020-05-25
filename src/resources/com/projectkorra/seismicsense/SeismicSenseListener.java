package com.projectkorra.seismicsense;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;

public class SeismicSenseListener implements Listener {

	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();

		if (player.isSneaking()) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

			if (bPlayer.canBend(CoreAbility.getAbility("SeismicSense"))) {
				new SeismicSense(player);
			}
		}
	}

}
