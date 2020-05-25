package com.projectkorra.seismicsense;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.projectkorra.glowapi.GlowHandler;
import com.projectkorra.glowapi.PKGlowAPI;
import com.projectkorra.pathfinder.Pathfinder;
import com.projectkorra.pathfinder.Pathfinder.Path;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;

public class SeismicSense extends EarthAbility implements AddonAbility {

	private List<Material> materials;
	private List<LivingEntity> entities;
	private List<LivingEntity> notglow;

	private GlowHandler g;
	private Pathfinder p;

	private long cooldown = 1000L;
	private int radius = 20;
	private long duration = 1000L;




	private Location start;


	public SeismicSense(Player player) {
		super(player);
		// TODO Auto-generated constructor stub
		g = PKGlowAPI.plugin.getGlowHandler();

		materials = new ArrayList<Material>();

		for (String s : getEarthbendableBlocks()) {
			materials.add(Material.getMaterial(s));
		}

		for (String s : getMetalbendableBlocks()) {
			materials.add(Material.getMaterial(s));
		}

		for (String s : getSandbendableBlocks()) {
			materials.add(Material.getMaterial(s));
		}

		entities = new ArrayList<LivingEntity>();
		notglow = new ArrayList<LivingEntity>();

		cooldown = 6000L;
		duration = 2000L;

		
		
		start = player.getLocation().clone().subtract(0, .5, 0);

		radius = isSand(start.getBlock().getType()) ? 10 : 20;
		
		System.out.println("starting");
		if(bPlayer.canBend(this)) {
			bPlayer.addCooldown(this);
			start();
		}
	}

	@Override
	public long getCooldown() {
		// TODO Auto-generated method stub
		return cooldown;
	}

	@Override
	public Location getLocation() {
		// TODO Auto-generated method stub
		return start;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "SeismicSense";
	}

	@Override
	public boolean isHarmlessAbility() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void progress() {
		// TODO Auto-generated method stub


		Entity e = getNearestEntity();
		if(e != null) {
			if(!notglow.contains(e)) {
				entities.add((LivingEntity) e);
				g.add(player, entities, 1000);
				duration += 50;
			}

		}


		if(this.getStartTime() + duration < System.currentTimeMillis()) {
			remove();
			return;
		}
	}

	private Entity getNearestEntity() {
		int distance = radius;
		Entity entity = null;
		for(Entity e : GeneralMethods.getEntitiesAroundPoint(start, radius)) {
			if(!entities.contains(e) && e instanceof LivingEntity) {
				if(!e.equals(player)) {
					p = new Pathfinder(start, e.getLocation().clone().subtract(0, .5, 0), materials);
					Path path = p.calculate(50, false);
					if(path == null) {
						notglow.add((LivingEntity) e);
						continue;
					}

					if(distance >= (int) start.distance(e.getLocation())) {
						entity = e;
						distance = (int) start.distance(e.getLocation());
					}

				}
			}
		}
		return entity;
	}

	@Override
	public String getAuthor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void load() {
		// TODO Auto-generated method stub
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new SeismicSenseListener(),
				ProjectKorra.plugin);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove() {
		g.clear();

		super.remove();
	}

}
