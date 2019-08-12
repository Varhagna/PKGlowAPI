package com.projectkorra.glowapi;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import com.projectkorra.projectkorra.util.ReflectionHandler;
import com.projectkorra.glowapi.util.TinyProtocol;

import io.netty.channel.Channel;

public class GlowHandler {
	private FileConfiguration config;
	private int trackingRange;

	private Class<?> eDataClass;
	private Field entityData;

	private final ArrayList<LivingEntity> entities = new ArrayList<LivingEntity>();
	private final HashMap<Player, List<LivingEntity>> affectedEntities = new HashMap<Player, List<LivingEntity>>();

	public GlowHandler() {
		trackingRange = getTrackingRange();

		try {
			initializePacketData();
		} catch (Exception e) {
			e.printStackTrace();
		}

		checkGlow();

		runTracker();
	}


	/**
	 * Automatically tracks the distance between two entities in GlowHandler
	 * and removes them if they go beyond entity-tracking-distance.
	 */
	private void runTracker() {
		BukkitScheduler scheduler = PKGlowAPI.plugin.getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(PKGlowAPI.plugin, new Runnable() {
			@Override
			public void run() {
				for (Player p : affectedEntities.keySet()) {
					List<LivingEntity> entities = affectedEntities.get(p);
					for (int i = 0; i < entities.size(); i++) {
						if (p.getLocation().distance(entities.get(i).getLocation()) >= trackingRange) {
							entities.remove(i);
						}
					}
				}
			}
		}, 0L, 20L);
	}

	/**
	 * Gets the entity tracking range. This is used for determining the max distance
	 * of the glow effect. Going beyond this distance and reentering causes issues
	 * with rendering and modifying packets.
	 * 
	 * @return the entity-tracking-range as defined in the spigot.yml.
	 * 
	 */
	public int getTrackingRange() {
		File file = new File(".." + File.separator + "spigot.yml");
		config = YamlConfiguration.loadConfiguration(file);

		return config.getInt("world-settings.default.entity-tracking-range.players");
	}

	private void initializePacketData() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
		eDataClass = ReflectionHandler.PackageType.MINECRAFT_SERVER.getClass("PacketPlayOutEntityMetadata");
		entityData = ReflectionHandler.getField(eDataClass, false, "b");
	}

	/**
	 * Checks and modifies outgoing EntityMetadata packets so only the defined
	 * player can see the glow effect.
	 */
	private void checkGlow() {
		new TinyProtocol(PKGlowAPI.plugin) {
			@Override
			public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
				String type = null;
				try {
					Integer entityId = (Integer) ReflectionHandler.getValue(packet, false, "a");
					List<?> packetData = (List<?>) ReflectionHandler.getValue(packet, false, "b");
				
					if (entityData.getType().getSimpleName().equalsIgnoreCase(packetData.getClass().getSimpleName())) {
						if (packetData != null) {
							Object data = packetData.get(0);
							type = ReflectionHandler.invokeMethod(data, "b").getClass().getSimpleName();

							if (type != null && type.equals("Byte")) {
								byte incoming = (byte) ReflectionHandler.invokeMethod(data, "b");
								byte mask = 0x40;

								if ((mask & incoming) == mask) {
									if (!isGlowReceiver(receiver)) {
										return null;
									} else if (!isGlowEntity(receiver, entityId)) {
										return null;
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
	
				return super.onPacketOutAsync(receiver, channel, packet);
			}
		};
	}

	/** Gets all of the receiving players for the given entity's glow packets.
	 * 
	 * @param entity - a glowing entity
	 * @return the receiving players 
	 */
	public List<Player> getGlowReceivers(LivingEntity entity) {
		if (isGlowEntity(entity)) {
			List<Player> players = new ArrayList<Player>();

			for (Entry<Player, List<LivingEntity>> e : affectedEntities.entrySet()) {
				if (e.getValue().contains(entity)) {
					players.add(e.getKey());
				}
			}

			return players;
		} else {
			return null;
		}
	}

	/** Gets all of the glowing entities for the receiving player
	 * 
	 * @param receiver - the receiving player
	 * @return the glowing entities for receiver.
	 */
	public List<LivingEntity> getGlowEntities(Player receiver) {
		if (isGlowReceiver(receiver)) {
			return affectedEntities.get(receiver);
		}

		return null;
	}

	/** Returns whether the entity is registered in the GlowHandler
	 * 
	 * @param entity - a glowing entity
	 * @return true if the entity is contained in the list of entities, false otherwise.
	 */
	public boolean isGlowEntity(LivingEntity entity) {
		return entities.contains(entity);
	}

	/** Returns whether the entity is registered in the GlowHandler
	 * for the receiving player.
	 * @param receiver - the receiving player
	 * @param entity - a glowing entity
	 * @return true if the entity is contained in the receivers's
	 * list of glowing entities.
	 */
	public boolean isGlowEntity(Player receiver, LivingEntity entity) {
		return isGlowEntity(receiver, entity.getEntityId());
	}

	private boolean isGlowEntity(Player receiver, int eId) {
		if (isGlowReceiver(receiver)) {
			for (LivingEntity e : getGlowEntities(receiver)) {
				if (e.getEntityId() == eId) {
					return true;
				}
			}
		}

		return false;
	}

	/** Returns whether the given player is a receiver of any glow packets.
	 * 
	 * @param receiver - the receiving player
	 * @return true if receiver is contained within the GlowHandler, false otherwise
	 */
	public boolean isGlowReceiver(Player receiver) {
		return affectedEntities.containsKey(receiver);
	}

	/** Adds the given receiver and entities list to the GlowHandler and 
	 * sets the entities to glow.
	 * 
	 * @param receiver - the receiving player
	 * @param entities
	 */
	public void add(Player receiver, List<LivingEntity> entities) {
		this.add(receiver, entities, 60000);
	}

	/** Adds the given receiver and entities list to the GlowHandler and 
	 * sets the entities to glow for the given duration.
	 * 
	 * @param receiver - the receiving player
	 * @param entities - list of glowing entities
	 * @param duration - duration in milliseconds
	 * 
	 * @apiNote duration must be a value divisible by 1000.
	 * Minecraft does not support adding potion effects for fractions of a second.
	 */
	public void add(Player receiver, List<LivingEntity> entities, long duration) {
		if (receiver != null && entities != null && duration % 1000 == 0) {
			for (LivingEntity e : entities) {
				if (e != null && !this.entities.contains(e)) {
					this.entities.add(e);
					e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60000, 0, false, false, false));
					runRemoveGlow(e, duration);
				} 
			}

			affectedEntities.put(receiver, entities);
		}
	}


	/** Runs a synchronised delayed task that removes 
	 * the glow for a given entity within duration
	 * 
	 * @param entity - a glowing entity
	 * @param duration - duration in milliseconds
	 */
	private void runRemoveGlow(LivingEntity entity, long duration) {
		BukkitScheduler scheduler = PKGlowAPI.plugin.getServer().getScheduler();

		scheduler.scheduleSyncDelayedTask(PKGlowAPI.plugin, new Runnable() {
			@Override
			public void run() {
				if (entity.hasPotionEffect(PotionEffectType.GLOWING)) {
					entity.removePotionEffect(PotionEffectType.GLOWING);
				}
				remove(entity);
			}
		}, duration);
	}

	/** Removes the receiving player and all associated entities from GlowHandler
	 * 
	 * @param receiver - the receiving player
	 */
	public void remove(Player receiver) {
		if (isGlowReceiver(receiver)) {
			List<LivingEntity> removals = affectedEntities.get(receiver);

			for (LivingEntity e : removals) {
				e.removePotionEffect(PotionEffectType.GLOWING);
			}

			affectedEntities.remove(receiver);
			this.entities.removeAll(removals);
		}
	}

	/** Removes all instances of this entity from GlowHandler
	 * 
	 * @param entity - a glowing entity
	 * @return true if successfully removed, false otherwise
	 */
	public boolean remove(LivingEntity entity) {
		for (Player p : affectedEntities.keySet()) {
			if (affectedEntities.get(p).contains(entity)) {
				affectedEntities.get(p).remove(entity);
			} 
		}

		if (entity != null) {
			entity.removePotionEffect(PotionEffectType.GLOWING);
		}

		return this.entities.remove(entity);
	}

	/** Removes the instance of entity from the receiver's list of glowing entities.
	 * 
	 * @param receiver - the receiving player
	 * @param entity - a glowing entity
	 * @return true if successfully removed, false otherwise
	 */
	public boolean remove(Player receiver, LivingEntity entity) {
		if (receiver != null && isGlowReceiver(receiver)) {
			if (affectedEntities.get(receiver).contains(entity)) {
				affectedEntities.get(receiver).remove(entity);
			}
		}

		if (entity != null) {
			entity.removePotionEffect(PotionEffectType.GLOWING);
		}

		return this.entities.remove(entity);
	}

	/** Clears the GlowHandler of all entities and receivers. Also unglows all entities.
	 * @apiNote Do not use this to unglow entities as this will affect other moves using the PKGlowHandler, 
	 * Use {@link GlowHandler#remove()} in this circumstance.
	 */
	public void clear() {
		for (int i = 0; i < entities.size(); i++) {
			if (remove(entities.get(i))) {
				i--;
			}
		}

		entities.clear();
		affectedEntities.clear();
	}

}
