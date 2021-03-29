package ru.onlymc.machineguard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MachineGuard extends JavaPlugin {
	public static MachineGuard plugin;
	public final MGRListener listener = new MGRListener(this);
	public static List<String> protectedBlocks = new ArrayList<String>(1024);
	public static List<String> protectableBySign = new ArrayList<String>(1024);
	public static List<String> hopperBlocks = new ArrayList<String>(1024);
	static boolean protectOnlyWithNoOwners = false, signProtect = false, protectAllDefinedBlocks = false, additionalProtect = false, uuidChecks = true, forceWritePlayers = false;
	static String msgAccessDenied = "", msgAlreadyProtectedByAnotherSign = "", msgSuccessfullyProtected = "", msgSuccessfullyUnprotected = "", msgWrongHopperPlacing = "";

	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(listener, this);

		Plugin wgPlugin = pm.getPlugin("WorldGuard");
		if (wgPlugin != null) {
			String versionString = wgPlugin.getDescription().getVersion();
            String[] versionPieces = versionString.contains(".") ? versionString.split("\\.") : new String[] { versionString };
            int version = Integer.parseInt(versionPieces[0]);
            if (version > 5) {
                listener.wg = new WorldGuardAPI(wgPlugin, this);
            } else {
                getLogger().warning("[MachineGuard] Only WorldGuard 6+ is supported - please update! (WG version: " + versionString + ")");
                getServer().shutdown();
            }
		}

		try { loadConfiguration(); }
		catch (IOException e) { getLogger().warning("[MachineGuard] Can't load config"); }
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			getLogger().warning("[MachineGuard] Failed to submit the stats");
		}

		plugin = this;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command,	String commandLabel, String[] args) {
		if (command.getName().equalsIgnoreCase("mgr")) {
			if (sender.hasPermission("machineguard.admin")) {
				try {
					loadConfiguration();
					sender.sendMessage("§2[MachineGuard] §aConfiguration has been reloaded");
					return true;
				} catch (IOException e) {
					getLogger().info("§4[MachineGuard] §cUnable to reload config");
					return true;
				}
			} else {
				sender.sendMessage("§4[MachineGuard Reloaded] §cYou don't have permission");
				return true;
			}
		}

		if (command.getName().equalsIgnoreCase("mgid")) {
			if (!(sender instanceof Player) || !sender.hasPermission("machineguard.admin")) return true;
			Player p = (Player) sender;
			Block b = p.getTargetBlock((Set<Material>)null, 10);
			if (b == null) return true;
			ItemStack i = p.getItemInHand();
			String m = "§2[MachineGuard Reloaded] §a"+(args.length > 0 ? b.getTypeId() : b.getType().toString());
			if (b.getData() > 0)
				m = m + "§8:§7" + b.getData();
			if ((i != null) && !(i.getType().toString().equalsIgnoreCase("AIR"))) {
				m = m + "§a / " + (args.length > 0 ? i.getTypeId() : i.getType().toString());
				if (i.getDurability() > 0)
					m = m + "§8:§7" + i.getDurability();
			}

			p.sendMessage(m);
			return true;
		}

		if (command.getName().equalsIgnoreCase("mg")) {
			sender.sendMessage("§eMachineGuard Reloaded §6v" + getDescription().getVersion() + " §eby CaptainChef for §6www.Mine.by");
			sender.sendMessage("§b§nhttp://spigotmc.org/resources/machineguard-reloaded.2845/");
			return true;
		}

		return false;
	}

	public void loadConfiguration() throws IOException {
		File cfgFile = new File(this.getDataFolder() + File.separator + "config.yml");
		String[] temp;
		YamlConfiguration config = YamlConfiguration.loadConfiguration(cfgFile);
		config.addDefault("enable-uuid-check", false);
		config.addDefault("force-write-players", false);
		config.addDefault("additional-protect", false);
		config.addDefault("block-all", false);
		config.addDefault("only-no-owners", false);
		config.addDefault("msg", "&cYou don't have access to open that");
		config.addDefault("msg-sign", "&cThis block is already protected");
		config.addDefault("msg-locked", "&aThis block is protected");
		config.addDefault("msg-unlock", "&cThis block is no longer protected");
		config.addDefault("msg-hopper", "&cYou can't place hoppers under sign protected blocks");
		config.addDefault("sign-block", false);
		temp = new String[] {"DISPENSER", "NOTE_BLOCK", "BED_BLOCK", "CHEST", "FURNACE", "BURNING_FURNACE", "WOODEN_DOOR", "LEVER", "STONE_BUTTON", "JUKEBOX", "DIODE_BLOCK_OFF", "DIODE_BLOCK_ON", "TRAP_DOOR", "FENCE_GATE", "BREWING_STAND", "ENDER_PORTAL_FRAME", "FLOWER_POT", "ANVIL", "HOPPER", "DROPPER"};
		config.addDefault("block-list", Arrays.asList(temp));
		temp = new String[] {"DISPENSER", "NOTE_BLOCK", "BED_BLOCK", "CHEST", "FURNACE", "BURNING_FURNACE", "WOODEN_DOOR", "LEVER", "STONE_BUTTON", "JUKEBOX", "TRAP_DOOR", "FENCE_GATE", "BREWING_STAND", "HOPPER", "DROPPER"};
		config.addDefault("blocked-sign-list", Arrays.asList(temp));
		temp = new String[] {"HOPPER"};
		config.addDefault("hopper-blocks-list", Arrays.asList(temp));
		config.options().copyDefaults(true);
		config.save(cfgFile);
		uuidChecks = config.getBoolean("enable-uuid-check");
		forceWritePlayers = config.getBoolean("force-write-players");
		additionalProtect = config.getBoolean("additional-protect");
		protectedBlocks.clear();
		protectedBlocks = config.getStringList("block-list");
		protectAllDefinedBlocks = config.getBoolean("block-all");
		protectOnlyWithNoOwners = config.getBoolean("only-no-owners");
		msgAccessDenied = colorize(config.getString("msg"));
		msgAlreadyProtectedByAnotherSign = colorize(config.getString("msg-sign"));
		msgSuccessfullyProtected = colorize(config.getString("msg-locked"));
		msgSuccessfullyUnprotected = colorize(config.getString("msg-unlock"));
		msgWrongHopperPlacing = colorize(config.getString("msg-hopper"));
		signProtect = config.getBoolean("sign-block");
		protectableBySign.clear();
		protectableBySign = config.getStringList("blocked-sign-list");
		hopperBlocks.clear();
		hopperBlocks = config.getStringList("hopper-blocks-list");
	}

	private String colorize(String input) {
		return ChatColor.translateAlternateColorCodes('&', input);
	}
}
