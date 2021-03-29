package ru.onlymc.machineguard;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class MachineGuardAPI {
	//private static MachineGuard plugin;

	/*static void init(MachineGuard p) {
		plugin = p;
	}*/

	public static Sign checkIsSignExists(Block b) {
		Sign s = null;
		s = searchSignAroundBlock(b);
		if (s == null)
			s = searchSignAroundBlock(MachineGuardAPI.getDoublePartOfBlock(b));
		if (s != null) return s;
		return null;
	}

	public static Sign searchSignAroundBlock(Block sb) {
		if (sb == null) return null;
		Block side = null;
		side = sb.getRelative(1, 0, 0);
		if (isSign(side)) return (Sign) side.getState();

		side = sb.getRelative(0, 0, 1);
		if (isSign(side)) return (Sign) side.getState();

		side = sb.getRelative(-1, 0, 0);
		if (isSign(side)) return (Sign) side.getState();

		side = sb.getRelative(0, 0, -1);
		if (isSign(side)) return (Sign) side.getState();

		side = sb.getRelative(0, -2, 0);
		if (isSign(side)) return (Sign) side.getState();

		return null;
	}

	public static boolean isSign(Block b) {
		if ((b == null) || (b.getType() != Material.WALL_SIGN)) return false;
		BlockState state = b.getState();
		if ((state == null) || !(state instanceof Sign)) return false;
		Sign sign = (Sign) state;
		if (getSignProtectType(sign) == null) return false;
		return true;
	}

	public static SignType getSignProtectType(Sign s) {
		if (s.getLine(0).equalsIgnoreCase(ChatColor.DARK_AQUA+"[Private]")) return SignType.PRIVATE;
		if (s.getLine(0).equalsIgnoreCase(ChatColor.DARK_AQUA+"[Public]")) return SignType.PUBLIC;
		if (s.getLine(0).equalsIgnoreCase(ChatColor.DARK_AQUA+"[Dispenser]")) return SignType.DISPENSER;
		return null;
	}

	public static boolean accessDenied(OfflinePlayer p, Sign s) {
		if (MachineGuard.uuidChecks) {
			for (int i = 1; i < s.getLines().length; i++) {
				UUID ru = readUUID(s, i);
				if (ru != null && ru.equals(p.getUniqueId())) {
					if (!s.getLine(i).equalsIgnoreCase(p.getName())) {
						s.setLine(i, p.getName());
						s.update();
					}

					return false;
				}
			}
		} else {
			for (int i = 1; i < s.getLines().length; i++) {
				if (s.getLine(i).trim().equalsIgnoreCase(p.getName())) return false;
			}
		}

		return true;
	}

	public static Block getDoublePartOfBlock(Block sb) {
		Block side;
		side = sb.getRelative(0, 1, 0);
		if (side.getType() == sb.getType()) return side;

		side = sb.getRelative(0, -1, 0);
		if (side.getType() == sb.getType()) return side;

		side = sb.getRelative(1, 0, 0);
		if (side.getType() == sb.getType()) return side;

		side = sb.getRelative(0, 0, 1);
		if (side.getType() == sb.getType()) return side;

		side = sb.getRelative(-1, 0, 0);
		if (side.getType() == sb.getType()) return side;

		side = sb.getRelative(0, 0, -1);
		if (side.getType() == sb.getType()) return side;

		return null;
	}

	@SuppressWarnings("deprecation")
	public static Block getProtectedBlockBehindSign(Block b) {
		if (b.getType() != Material.WALL_SIGN) return null;
		Block bl;
		switch (b.getData()) {
			case 2:
				bl = b.getRelative(BlockFace.SOUTH);
				if (isBlocked(bl.getType().toString(), bl.getData())) return bl;
				break;
			case 3:
				bl = b.getRelative(BlockFace.NORTH);
				if (isBlocked(bl.getType().toString(), bl.getData())) return bl;
				break;
			case 4:
				bl = b.getRelative(BlockFace.EAST);
				if (isBlocked(bl.getType().toString(), bl.getData())) return bl;
				break;
			case 5:
				bl = b.getRelative(BlockFace.WEST);
				if (isBlocked(bl.getType().toString(), bl.getData())) return bl;
				break;
			default: break;
		}

		bl = b.getLocation().add(0, 2, 0).getBlock();
		if (isBlocked(bl.getType().toString(), bl.getData())) return bl;
		return null;
	}

	public static boolean isBlocked(String id, int meta) {
		if (MachineGuard.protectedBlocks.contains(id)) return true;
	    return MachineGuard.protectedBlocks.contains(id + ":" + meta);
	}

	public static boolean canSign(String id, int meta) {
		if (MachineGuard.protectableBySign.contains(id)) return true;
	    return MachineGuard.protectableBySign.contains(id + ":" + meta);
	}

	public static boolean isHopper(String id, int meta) {
		if (MachineGuard.hopperBlocks.contains(id)) return true;
	    return MachineGuard.hopperBlocks.contains(id + ":" + meta);
	}

	public static boolean signsEnabled() {
		return MachineGuard.signProtect;
	}

	public static boolean wholeServerProtectEnabled() {
		return MachineGuard.protectAllDefinedBlocks;
	}

	public static boolean onlyNoOwnersEnabled() {
		return MachineGuard.protectOnlyWithNoOwners;
	}

	public static boolean UUIDsEnabled() {
		return MachineGuard.uuidChecks;
	}

	public static boolean nonMojangUUIDsEnabled() {
		return MachineGuard.forceWritePlayers;
	}

	static void writeUUID(Sign s, int l, OfflinePlayer p) {
		if (MachineGuard.uuidChecks) {
			UUID[] uuids = null;
			if (s.hasMetadata("MGR")) {
				List<MetadataValue> list = s.getMetadata("MGR");
				uuids = (UUID[])((MetadataValue)list.get(0)).value();
			} else uuids = new UUID[3];
			uuids[(l - 1)] = (p != null) ? p.getUniqueId() : null;
			s.setMetadata("MGR", new FixedMetadataValue(MachineGuard.plugin, uuids));
		}

		s.setLine(l, (p != null) ? p.getName() : "-");
		s.update();
	}

	static UUID readUUID(Sign s, int l) {
		if (!s.hasMetadata("MGR")) return null;
		List<MetadataValue> uuids = s.getMetadata("MGR");
		return ((UUID[])((MetadataValue)uuids.get(0)).value())[(l - 1)];
	}

	static void removeUUID(Sign s) {
		if (s.hasMetadata("MGR"))
			s.removeMetadata("MGR", MachineGuard.plugin);
	}
}
