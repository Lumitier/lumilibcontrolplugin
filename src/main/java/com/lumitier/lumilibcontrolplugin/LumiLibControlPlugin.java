package com.lumitier.lumilibcontrolplugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-Seite von LumiLibControl.
 *
 * Legt fest, welche von Lumitiers Client-Mods auf diesem Server erlaubt
 * sind, und schickt das jedem Spieler, der LumiLib installiert hat. LumiLib
 * schaltet die betroffenen Mods dann fuer die Dauer der Verbindung ab.
 *
 * EHRLICH ZUR REICHWEITE: Das ist eine Bitte, keine Durchsetzung. Der
 * Server sieht nicht, welche Mods ein Spieler hat, und kann sie nicht
 * abschalten. Er sieht nur, ob der Client den Kanal von LumiLib angemeldet
 * hat - und schickt dann seine Regeln.
 */
public final class LumiLibControlPlugin extends JavaPlugin implements Listener {

	/** Muss mit ServerRules.CHANNEL in LumiLib uebereinstimmen. */
	public static final String CHANNEL = "lumilib:control";

	/**
	 * Gegenrichtung: Das Menue in LumiLibControl schickt hierauf eine
	 * Aenderung. Muss mit LumiLibControl uebereinstimmen.
	 */
	public static final String SET_CHANNEL = "lumilib:control_set";

	private Rules rules;

	/**
	 * Was jeder Spieler zuletzt als "darf aendern" bekommen hat.
	 *
	 * Bukkit meldet nicht, wenn sich Rechte aendern - weder bei /op noch bei
	 * LuckPerms. Deshalb wird einmal pro Sekunde nachgesehen und nur bei
	 * einer Abweichung neu gesendet. Das kostet pro Spieler eine
	 * Rechteabfrage, die ohnehin zwischengespeichert ist.
	 */
	private final Map<UUID, Boolean> sentAdmin = new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		this.rules = new Rules(this);
		this.rules.load();

		getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
		getServer().getMessenger().registerIncomingPluginChannel(this, SET_CHANNEL,
			new SetRequests(this));
		getServer().getPluginManager().registerEvents(this, this);

		LumiModCommand command = new LumiModCommand(this);
		PluginCommand cmd = getCommand("lumimod");
		if (cmd != null) {
			cmd.setExecutor(command);
			cmd.setTabCompleter(command);
		} else {
			getLogger().severe("Command /lumimod is missing from plugin.yml.");
		}

		// Rechte beobachten - siehe sentAdmin.
		getServer().getScheduler().runTaskTimer(this, this::checkPermissions, 20L, 20L);

		// Wird das Plugin bei laufendem Server (neu) geladen, sind Spieler
		// schon online. Die bekommen den Stand sofort, nicht erst beim
		// naechsten Betreten.
		broadcast();

		getLogger().info("Ready. Disabled mods: " + this.rules.disabledList());
	}

	@Override
	public void onDisable() {
		getServer().getMessenger().unregisterOutgoingPluginChannel(this);
		getServer().getMessenger().unregisterIncomingPluginChannel(this);
	}

	public Rules rules() {
		return this.rules;
	}

	// ------------------------------------------------------------------
	// Versand
	//
	// Bukkit verwirft eine Nachricht stillschweigend, wenn der Client den
	// Kanal nicht angemeldet hat. Deshalb wird nicht beim Beitreten blind
	// gesendet, sondern dann, wenn der Client den Kanal anmeldet - das ist
	// genau der Moment, ab dem er zuhoert.
	// ------------------------------------------------------------------

	/** Der Client meldet den Kanal an: Ab jetzt hoert LumiLib zu. */
	@EventHandler
	public void onRegister(PlayerRegisterChannelEvent event) {
		if (CHANNEL.equals(event.getChannel())) {
			send(event.getPlayer());
		}
	}

	/**
	 * Zur Sicherheit auch einen Tick nach dem Beitreten.
	 *
	 * Meldet der Client seinen Kanal schon in der Konfigurationsphase an,
	 * gibt es zu dem Zeitpunkt noch keinen Spieler, und das Ereignis oben
	 * kommt womoeglich nicht an. Doppelt gesendet schadet nicht: Das Paket
	 * enthaelt jedes Mal den vollstaendigen Stand.
	 */
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		getServer().getScheduler().runTaskLater(this, () -> {
			if (player.isOnline()) {
				send(player);
			}
		}, 1L);
	}

	/** Schickt dem Spieler die Regeln - nur, wenn er LumiLib hat. */
	public void send(Player player) {
		if (!player.getListeningPluginChannels().contains(CHANNEL)) {
			return;
		}
		boolean admin = player.hasPermission(LumiModCommand.PERMISSION);
		this.sentAdmin.put(player.getUniqueId(), admin);
		player.sendPluginMessage(this, CHANNEL, Packets.rules(admin, this.rules.all()));
	}

	/** Rechte geaendert (op, deop, LuckPerms)? Dann neuen Stand schicken. */
	private void checkPermissions() {
		for (Player player : getServer().getOnlinePlayers()) {
			Boolean last = this.sentAdmin.get(player.getUniqueId());
			if (last != null && last != player.hasPermission(LumiModCommand.PERMISSION)) {
				send(player);
			}
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.sentAdmin.remove(event.getPlayer().getUniqueId());
	}

	/** Allen Spielern mit LumiLib den neuen Stand schicken - sofort, ohne Kick. */
	public void broadcast() {
		for (Player player : getServer().getOnlinePlayers()) {
			send(player);
		}
	}
}
