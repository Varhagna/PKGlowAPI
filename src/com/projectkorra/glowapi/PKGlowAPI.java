package com.projectkorra.glowapi;


import org.bukkit.plugin.java.JavaPlugin;

public class PKGlowAPI extends JavaPlugin {

	public static PKGlowAPI plugin;
	public static final String BRAND = "[PKGLOWAPI]";
	private GlowHandler glowHandler;

	
	@Override
	public void onEnable() {
		
		plugin = this;
		
		sendMessage("Getting ready to brighten up your day!");

		sendMessage("Creating GlowHandler");
		glowHandler = new GlowHandler();
		
		sendMessage("Registering PKGlowListener");
		
		PKGlowAPI.plugin.getServer().getPluginManager().registerEvents(new PKGlowListener(), PKGlowAPI.plugin);
		
	}

	public GlowHandler getGlowHandler() {
		return glowHandler;
	}

	private void sendMessage(String string) {
		// TODO Auto-generated method stub
		System.out.println(BRAND + ": " + string);
	}

	@Override
	public void onDisable() {
		glowHandler.clear();
		sendMessage("Turning off the lights");
	}

}
