package ru.onlymc.machineguard;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MGRListener implements Listener {
	MachineGuard plugin;
	WorldGuardAPI wg = null;

	MGRListener(MachineGuard instance) {
		plugin = instance;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	private void onLock(SignChangeEvent e) {
		String title = e.getLine(0).trim();
		if ((title.equalsIgnoreCase("[Private]")) || (title.equalsIgnoreCase("[Public]"))) {
			if (!(plugin.signProtect)) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(plugin.msgAlreadyProtectedByAnotherSign);
			} else {
				Player p = e.getPlayer();
				Sign s = (Sign)e.getBlock().getState();
				Block b = MachineGuardAPI.getProtectedBlockBehindSign(e.getBlock());
				Sign cip = MachineGuardAPI.checkIsSignExists(b);
				if (cip != null && MachineGuardAPI.getSignProtectType(cip) != null) {
					e.setCancelled(true);
					p.sendMessage(plugin.msgAlreadyProtectedByAnotherSign);
					return;
				}

				if (title.equalsIgnoreCase("[Private]")) {
					boolean isempty = true;
					for (int i = 1; i < 4; i++) {
						String line = e.getLine(i).trim();
						if ((e.getLine(i) != null) && (!line.equals("")) && (line.length() > 2)) {
							isempty = false;
							OfflinePlayer sp = Bukkit.getOfflinePlayer(line);
							if (sp.hasPlayedBefore() || plugin.forceWritePlayers) MachineGuardAPI.writeUUID(s, i, sp);
							else e.setLine(i, "-");
						}
					}

					if (isempty) {
						MachineGuardAPI.writeUUID(s, 1, p);
						e.setLine(1, p.getName());
					}
				}

				e.setLine(0, ChatColor.DARK_AQUA+title);
				p.sendMessage(plugin.msgSuccessfullyProtected);
			}
		} else if (title.equalsIgnoreCase("[Dispenser]")) {
			Player p = e.getPlayer();
			Block b = e.getBlock();
			if (b.getType() == Material.WALL_SIGN) {
				b = MachineGuardAPI.getProtectedBlockBehindSign(b);
				if ((b == null) || (b.getType() != Material.DISPENSER) || !(p.hasPermission("machineguard.infdis")))
					e.setCancelled(true);
				else
					e.setLine(0, ChatColor.DARK_AQUA+title);
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	private void onUnlock(BlockBreakEvent e) {
		Block b = e.getBlock();
		String id = b.getType().toString();
		@SuppressWarnings("deprecation")
		byte me = b.getData();
		Player p = e.getPlayer();
		if (b.getType() == Material.WALL_SIGN) {
			Sign s = (Sign) b.getState();
			if (MachineGuardAPI.getSignProtectType(s) == SignType.PRIVATE) {
				if ((MachineGuardAPI.accessDenied(p, s)) && !(p.hasPermission("machineguard.bypass.sign"))) {
					e.setCancelled(true);
					p.sendMessage(plugin.msgAccessDenied);
				} else {
					MachineGuardAPI.removeUUID(s);
					p.sendMessage(plugin.msgSuccessfullyUnprotected);
				}
			}
		} else if ((MachineGuardAPI.isBlocked(id, me)) && (plugin.signProtect) && (MachineGuardAPI.canSign(id, me))) {
			Sign s = MachineGuardAPI.checkIsSignExists(b);
			if (s != null && MachineGuardAPI.getSignProtectType(s) != null) {
				if (MachineGuardAPI.getSignProtectType(s) == SignType.PRIVATE) {
					if ((MachineGuardAPI.accessDenied(p, s)) && !(p.hasPermission("machineguard.bypass.sign"))) {
						e.setCancelled(true);
						p.sendMessage(plugin.msgAccessDenied);
						return;
					}
				}

				if (!e.isCancelled()) {
					MachineGuardAPI.removeUUID(s);
					s.getWorld().dropItemNaturally(s.getLocation(), new ItemStack(Material.SIGN, 1));
					s.getBlock().setType(Material.AIR);
					p.sendMessage(plugin.msgSuccessfullyUnprotected);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	private void onClick(PlayerInteractEvent e) {
		if (!e.hasBlock()) return;
		Block b = e.getClickedBlock();
		Player p = e.getPlayer();
		String id = b.getType().toString();
		byte me = b.getData();
		if (MachineGuardAPI.isBlocked(id, me)) {
			if ((plugin.signProtect) && (MachineGuardAPI.canSign(id, me))) {
				Sign s = MachineGuardAPI.checkIsSignExists(b);
				if (s != null) {
					if (MachineGuardAPI.getSignProtectType(s) == SignType.PRIVATE) {
						if (plugin.uuidChecks && !s.hasMetadata("MGR")) {
							// Конвертация всех ников в UUID
							for (int i = 1; i < 4; i++) {
								String line = s.getLine(i).trim();
								if ((s.getLine(i) != null) && (!line.equals("")) && (line.length() > 2)) {
									OfflinePlayer sp = Bukkit.getOfflinePlayer(s.getLine(i).trim());
									if (sp.hasPlayedBefore() || plugin.forceWritePlayers) MachineGuardAPI.writeUUID(s, i, sp);
									else {
										s.setLine(i, "-");
										s.update();
									}
								}
							}
						}

						if ((MachineGuardAPI.accessDenied(p, s)) && !(p.hasPermission("machineguard.bypass.sign"))) {
							e.setCancelled(true);
							if (plugin.additionalProtect) {
								final Player pl = p;
								Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
									@Override
									public void run() { if (pl.getOpenInventory() != null) pl.closeInventory(); }
								}, 1L);
							}

							p.sendMessage(plugin.msgAccessDenied);
							return;
						} else {
							e.setCancelled(false);
							return;
						}
					} else if (MachineGuardAPI.getSignProtectType(s) == SignType.PUBLIC) {
						e.setCancelled(false);
						return;
					}
				}
			}

			if (((plugin.protectAllDefinedBlocks) || !(canBuild(p, b.getLocation()))) && !(p.hasPermission("machineguard.bypass.build")) && !(p.hasPermission("machineguard.bypass.build." + id))) {
				e.setCancelled(true);
				if (plugin.additionalProtect) {
					final Player pl = p;
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run() { if (pl.getOpenInventory() != null) pl.closeInventory(); }
					}, 1L);
				}

				p.sendMessage(plugin.msgAccessDenied);
				return;
			}
		}

		if ((id.equalsIgnoreCase("COMMAND")) && (me == 3) && !(MachineGuardAPI.isBlocked(id, me)) && !(p.hasPermission("machineguard.cmdblock"))) {
			e.setCancelled(true);
			p.sendMessage("§cYou don't have access to command block");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	private void onItemDispense(final BlockDispenseEvent e) {
		Block b = e.getBlock();
		if (b.getType() == Material.DISPENSER) {
			Sign s = MachineGuardAPI.searchSignAroundBlock(b);
			if ((s != null) && (MachineGuardAPI.getSignProtectType(s) == SignType.DISPENSER)) {
				plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable(){
					@Override
					public void run() {
						Inventory i = ((org.bukkit.block.Dispenser) e.getBlock().getState()).getInventory();
						i.addItem(new ItemStack[] { e.getItem() });
					}
				}, 1L);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	private void onPlace(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		Block b = e.getBlock();
		if (MachineGuardAPI.isHopper(b.getType().toString(), b.getData())) {
			if (plugin.signProtect && p.hasPermission("machineguard.bypass.sign")) {
				Sign s = MachineGuardAPI.checkIsSignExists(b.getRelative(0, 1, 0));
				if (s != null && MachineGuardAPI.getSignProtectType(s) == SignType.PRIVATE && MachineGuardAPI.accessDenied(p, s)) {
					e.setCancelled(true);
					p.sendMessage(plugin.msgWrongHopperPlacing);
				}
			}
		}
	}

	/*@SuppressWarnings("deprecation")
	@EventHandler
	private void onTreeGrow(StructureGrowEvent e) {
		Block pb = null;
		Iterator<BlockState> bsi = e.getBlocks().iterator();
		while (bsi.hasNext()) {
			BlockState bs = bsi.next();
			pb = bs.getLocation().getBlock();
			if (pb != null && MachineGuardAPI.isBlocked(pb.getType().toString(), pb.getData())) {
				pb.getWorld().dropItem(pb.getLocation(), new ItemStack(pb.getType(), 1, pb.getData()));
				bsi.remove();
			}
		}
	}*/

	/*private boolean canBuild(Player p, Location l) {
		if (ono) {
			ApplicableRegionSet ars = wg.getRegionManager(l.getWorld()).getApplicableRegions(l);
			for (ProtectedRegion pr : ars.getRegions())
				if (pr.getOwners().size() == 0) return false;
			return true;
		} else return wg.canBuild(p, l);
	}*/

	private boolean canBuild(Player p, Location l) {
		if (MachineGuard.protectOnlyWithNoOwners) {
			ApplicableRegionSet ars = wg.getRegionsAt(l);
			for (ProtectedRegion pr : ars.getRegions())
				if (pr.getOwners().size() == 0) return false;
			return true;
		} else return wg.canBuild(p, l);
	}

	/*private boolean canBuild(Player p, Location l) {
		if (ono) {
			ApplicableRegionSet ars = wg.getRegionManager(l.getWorld()).getApplicableRegions(l);
			for (ProtectedRegion pr : ars.getRegions())
				if (pr.getOwners().size() == 0) return false;
			return true;
		} else {
	        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
	        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(l);
	        if (!hasBypass(p, l)) {
	            return query.testState(loc, WorldGuardPlugin.inst().wrapPlayer(p), Flags.BUILD);
	        } else {
	            return true;
	        }
		}
	}

    public boolean hasBypass(Player p, Location l) {
        return WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(getLocalPlayer(p), getLocalWorld(l.getWorld()));
    }*/
}
