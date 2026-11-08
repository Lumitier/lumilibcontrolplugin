package com.lumitier.lumilibcontrolplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Aenderungen aus dem Menue von LumiLibControl.
 *
 * DER SERVER ENTSCHEIDET, NICHT DER CLIENT. Das Menue zeigt Schalter nur,
 * wenn der Server "Admin" gemeldet hat - aber ein veraenderter Client
 * koennte trotzdem senden. Deshalb wird hier dieselbe Berechtigung geprueft
 * wie beim Befehl. Ohne sie: Meldung an den Spieler, aktueller Stand
 * zurueck, damit sein Menue wieder stimmt, und nichts geaendert.
 *
 * Format, Byte fuer Byte wie in LumiLibControl:
 * <pre>
 *   VarInt   Fassung (1)
 *   String   Mod-ID (VarInt-Laenge, dann UTF-8)
 *   Boolean  erlauben?
 * </pre>
 */
final class SetRequests implements PluginMessageListener {

	private static final int PROTOCOL = 1;
	private static final int MAX_ID_BYTES = 64 * 3;

	private static final Component NO_PERMISSION = Component.text(
		"You don't have permission to change the mods on this server.", NamedTextColor.RED);

	private final LumiLibControlPlugin plugin;

	SetRequests(LumiLibControlPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!LumiLibControlPlugin.SET_CHANNEL.equals(channel)) {
			return;
		}
		Request req = parse(message);
		if (req == null) {
			this.plugin.getLogger().warning("Ignored a malformed change request from " + player.getName() + ".");
			return;
		}
		// Auf den Haupt-Thread: Config schreiben und an alle senden soll
		// dort passieren, wo auch der Befehl es tut - nie gleichzeitig.
		this.plugin.getServer().getScheduler().runTask(this.plugin, () -> apply(player, req));
	}

	private void apply(Player player, Request req) {
		if (!player.isOnline()) {
			return;
		}
		if (!player.hasPermission(LumiModCommand.PERMISSION)) {
			player.sendMessage(NO_PERMISSION);
			this.plugin.send(player);
			return;
		}
		if (!this.plugin.rules().known(req.id())) {
			this.plugin.send(player);
			return;
		}
		if (this.plugin.rules().set(req.id(), req.allow())) {
			this.plugin.getLogger().info(player.getName() + " " + (req.allow() ? "enabled " : "disabled ")
				+ req.id() + " (from the LumiLibControl menu)");
			this.plugin.broadcast();
		} else {
			// Nichts geaendert - trotzdem den Stand zurueck, falls das Menue
			// des Spielers aus irgendeinem Grund danebenliegt.
			this.plugin.send(player);
		}
	}

	private record Request(String id, boolean allow) {}

	/** Liest die Anfrage - und wirft dabei nie. Kaputt heisst: null. */
	private static Request parse(byte[] data) {
		try {
			int[] pos = {0};
			if (readVarInt(data, pos) != PROTOCOL) {
				return null;
			}
			int len = readVarInt(data, pos);
			if (len < 1 || len > MAX_ID_BYTES || pos[0] + len + 1 > data.length) {
				return null;
			}
			String id = new String(data, pos[0], len, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
			pos[0] += len;
			boolean allow = data[pos[0]] != 0;
			return new Request(id, allow);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static int readVarInt(byte[] data, int[] pos) {
		int value = 0;
		for (int shift = 0; shift < 35; shift += 7) {
			byte b = data[pos[0]++];
			value |= (b & 0x7F) << shift;
			if ((b & 0x80) == 0) {
				return value;
			}
		}
		throw new IllegalArgumentException("VarInt too long");
	}
}
