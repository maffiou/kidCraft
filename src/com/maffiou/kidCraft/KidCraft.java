package com.maffiou.kidCraft;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class KidCraft extends JavaPlugin implements Listener, Runnable {

	static final int UNKNOWN = 0;
	static final int NEW = 1;
	static final int BANNED = 2;
	static final int SUSPENDED = 3;
	static final int ACTIVE = 4;

	FileConfiguration config = getConfig();

	static String webpage = "Empty";
	static Logger myLog;

	BukkitScheduler bs;
	PlayerManager pm;
	HtmlGen ws;

	public void run() {
		ArrayList<String> activePlayer = pm.getPlayerListByStatus(ACTIVE);

		if(activePlayer != null) {
			for(String player: activePlayer) {
				pm.decrementTime(player);
				/* Check if anything should happen */
				myLog.info(player+": "+pm.getPlayTime(player));
				switch(pm.getPlayTime(player)) {
					default:
						break;
					case 15*60:
					case 5*60:
					case 1*60:
						Bukkit.getPlayerExact(player).sendMessage("This is your "+pm.getPlayTime(player)/60+" minutes warning!");
						break;
					case 0:
						pm.setPlayerStatus(player, SUSPENDED);
						try {
							// This call will fail if active status is set but the player is not currently on the server (ie set active from the website)
							Bukkit.getPlayerExact(player).kickPlayer("You've run out of time, go do something else!");
						} catch (Exception e) {
							e.printStackTrace();
						}
						break;
				}
			}
		}
	}

	@Override
	public void onEnable() {
		bs = this.getServer().getScheduler();
		bs.scheduleSyncRepeatingTask(this, this, 0, 20);

		config.addDefault("UserList", "");

		pm = new PlayerManager(config);

		ws = new HtmlGen(this);

		myLog = Bukkit.getLogger();

		getServer().getPluginManager().registerEvents(this, this);

		ArrayList<String> uL = pm.getPlayerList();

		if(uL!=null) {
			for(String user: uL) {
				myLog.info("user: "+user);
			}
		}
	}

	@Override
	public void onDisable() {
		ws.stop();
		config.options().copyDefaults(true);
		saveConfig();
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		if(pm.getPlayerStatus(playerName)==ACTIVE) {
			pm.setPlayerStatus(playerName, SUSPENDED);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		String playerName = player.getName();

		switch (pm.getPlayerStatus(playerName)) {
		default:
		case UNKNOWN:
			pm.addPlayer(playerName,NEW,0,false);
		case BANNED:
			player.kickPlayer("Not allowed on this server! Contact the admin if needed...");
			break;
		case NEW:
			player.kickPlayer("Not allowed on this server yet! Contact the admin if needed...");
			break;
		case SUSPENDED:
			if(pm.getPlayTime(playerName) != 0) {

				String msg ="Welcome, " + playerName + "!\nYou can play for "+(int)(pm.getPlayTime(playerName)/60)+" minutes\n";
				if(pm.getGiftState(playerName)) {
					msg+="And you have a gift waiting for you (reclaim with @gift)\n";
				}
				msg+="Enjoy!";

				event.setJoinMessage(msg);
				pm.setPlayerStatus(playerName, ACTIVE);
				Command.broadcastCommandMessage(event.getPlayer(), "There are now " + pm.getPlayerListByStatus(ACTIVE).size() + " players on this server...");
			} else {
				player.kickPlayer("You've run out of playtime for the time being... Come back later...");
			}
			break;

		}
	}

	@EventHandler
	public void onChatreceived(AsyncPlayerChatEvent event)
	{
		String message = event.getMessage();
		Player source = event.getPlayer();

		if(message.startsWith("@tp ")) {
			Player target = Bukkit.getPlayerExact(message.split(" ")[1]);

			if(target==null) {
				source.sendMessage("Sorry, "+source.getName()+", I can't find that player");
			} else {
				source.teleport(target, TeleportCause.COMMAND);
				Command.broadcastCommandMessage(source, "I just teleported to " + target.getName());
			}
			event.setCancelled(true);

		} else if(message.startsWith("@gift ")) {
			if(pm.getGiftState(source.getName())==true) {

				PlayerInventory sourceInventory = source.getInventory();

				Material material = Material.AIR;
				int materialQuantity=0;

				String[] parameters = message.split(" ");

				if(parameters.length>=2) {

					String materialString = parameters[1];

					if(materialString.equalsIgnoreCase("wood")) {
						material=Material.WOOD;
						materialQuantity = 50;
					} else if (materialString.equalsIgnoreCase("coal")) {
						material=Material.COAL;
						materialQuantity = 20;
					} else if (materialString.equalsIgnoreCase("diamond")) {
						material=Material.DIAMOND;
						materialQuantity = 3;
					}

					if(materialQuantity!=0) {
						ItemStack item = new ItemStack(material,materialQuantity);
						int slot = sourceInventory.firstEmpty();
						if(slot!=-1) {
							sourceInventory.setItem(slot, item);
							pm.setGiftState(source.getName(),false);
						} else {
							source.sendMessage("No more room to store your gift");
						}
					}
				} else {
					source.sendMessage("Syntax is @gift <material> (wood, coal)");
				}
			} else {
				source.sendMessage("Syntax is @gift <material> (wood, coal)");
			}
			event.setCancelled(true);

		}
	}

	public void updatePlayerStatus(String player, int newStatus) {
		/* Not all transitions are acceptable */
		switch(newStatus) {
		case BANNED:
		case SUSPENDED:
			if(pm.getPlayerStatus(player)==ACTIVE) {
				bs.runTask(this, new MyKicker(player));
			}
			pm.setPlayerStatus(player, newStatus);
			break;
		}
	}

	class MyKicker implements Runnable {
		String playerToKick;
		
		MyKicker(String player) {
			playerToKick = player;
		}

		@Override
		public void run() {
			try {
				Bukkit.getPlayerExact(playerToKick).kickPlayer("You've run out of time, go do something else!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}


