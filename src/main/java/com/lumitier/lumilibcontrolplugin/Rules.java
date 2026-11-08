package com.lumitier.lumilibcontrolplugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Die Regeln dieses Servers: Mod-ID zu erlaubt/verboten.
 *
 * Steht in plugins/LumiLibControlPlugin/config.yml unter "mods". Datei,
 * Befehl und spaeter das Menue in LumiLibControl aendern alle diesen einen
 * Stand - es gibt keine zweite Quelle, die abweichen koennte.
 */
public final class Rules {

	/**
	 * Mods, die immer in der Datei stehen, auch wenn jemand sie geloescht
	 * hat. Weitere IDs darf man von Hand eintragen - fuer Mods, die es beim
	 * Schreiben dieses Plugins noch nicht gab.
	 *
	 * NICHT DABEI: ipaddresshider. Die Mod schuetzt die Privatsphaere des
	 * Spielers. Ein Server, der sie abschalten kann, koennte genau das
	 * aushebeln, wofuer sie da ist.
	 */
	public static final List<String> KNOWN = List.of(
		"shadowtweaks", "perpetualsnow", "itemdroptweaks", "xporbtweaks");

	/** Gueltige Mod-ID nach Fabric-Regeln - alles andere wird ignoriert. */
	private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_-]{1,63}");

	/** LumiLib nimmt hoechstens so viele Eintraege an. */
	public static final int MAX = 64;

	private final LumiLibControlPlugin plugin;
	private final Map<String, Boolean> mods = new LinkedHashMap<>();

	public Rules(LumiLibControlPlugin plugin) {
		this.plugin = plugin;
	}

	/** Liest config.yml (neu) ein und ergaenzt fehlende bekannte Mods. */
	public void load() {
		this.plugin.reloadConfig();
		FileConfiguration cfg = this.plugin.getConfig();
		this.mods.clear();

		ConfigurationSection section = cfg.getConfigurationSection("mods");
		if (section != null) {
			for (String key : section.getKeys(false)) {
				String id = key.toLowerCase(Locale.ROOT);
				if (!ID.matcher(id).matches()) {
					this.plugin.getLogger().warning("Ignoring invalid mod id in config.yml: " + key);
					continue;
				}
				if (this.mods.size() >= MAX) {
					this.plugin.getLogger().warning("More than " + MAX + " mods in config.yml - ignoring the rest.");
					break;
				}
				this.mods.put(id, section.getBoolean(key, true));
			}
		}

		boolean added = false;
		for (String id : KNOWN) {
			if (!this.mods.containsKey(id)) {
				this.mods.put(id, true);
				added = true;
			}
		}
		if (added) {
			save();
		}
	}

	public void save() {
		FileConfiguration cfg = this.plugin.getConfig();
		cfg.set("mods", null);
		for (Map.Entry<String, Boolean> e : this.mods.entrySet()) {
			cfg.set("mods." + e.getKey(), e.getValue());
		}
		this.plugin.saveConfig();
	}

	public boolean known(String id) {
		return this.mods.containsKey(id);
	}

	/** @return true, wenn sich etwas geaendert hat */
	public boolean set(String id, boolean allowed) {
		Boolean old = this.mods.get(id);
		if (old == null || old == allowed) {
			return false;
		}
		this.mods.put(id, allowed);
		save();
		return true;
	}

	public Map<String, Boolean> all() {
		return Collections.unmodifiableMap(this.mods);
	}

	public List<String> ids() {
		return new ArrayList<>(this.mods.keySet());
	}

	public String disabledList() {
		List<String> off = new ArrayList<>();
		for (Map.Entry<String, Boolean> e : this.mods.entrySet()) {
			if (!e.getValue()) {
				off.add(e.getKey());
			}
		}
		return off.isEmpty() ? "none" : String.join(", ", off);
	}
}
