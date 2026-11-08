package com.lumitier.lumilibcontrolplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /lumimod list | enable &lt;mod&gt; | disable &lt;mod&gt; | reload
 *
 * Geht in der Server-Konsole und fuer Spieler mit lumimod.admin - das haben
 * OPs von selbst, und LuckPerms kann es jedem geben. LuckPerms braucht dafuer
 * keinen eigenen Code: Es haengt sich in die normale Rechtepruefung von
 * Bukkit ein.
 *
 * "list" darf jeder: Welche Mods ein Server abschaltet, ist kein Geheimnis -
 * die Spieler merken es ohnehin.
 */
public final class LumiModCommand implements TabExecutor {

	public static final String PERMISSION = "lumimod.admin";

	private static final Component NO_PERMISSION = Component.text(
		"You don't have permission to change the mods on this server.", NamedTextColor.RED);

	private static final List<String> SUBCOMMANDS = List.of("list", "enable", "disable", "reload");

	private final LumiLibControlPlugin plugin;

	LumiModCommand(LumiLibControlPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			usage(sender, label);
			return true;
		}
		String sub = args[0].toLowerCase(Locale.ROOT);

		if (sub.equals("list")) {
			list(sender);
			return true;
		}

		if (!sender.hasPermission(PERMISSION)) {
			sender.sendMessage(NO_PERMISSION);
			return true;
		}

		switch (sub) {
			case "reload" -> {
				this.plugin.rules().load();
				this.plugin.broadcast();
				sender.sendMessage(Component.text("Reloaded config.yml and sent it to all players.",
					NamedTextColor.GREEN));
				list(sender);
			}
			case "enable", "disable" -> {
				if (args.length < 2) {
					usage(sender, label);
					return true;
				}
				String id = args[1].toLowerCase(Locale.ROOT);
				if (!this.plugin.rules().known(id)) {
					sender.sendMessage(Component.text("Unknown mod: " + id
						+ ". Known: " + String.join(", ", this.plugin.rules().ids()), NamedTextColor.RED));
					return true;
				}
				boolean allow = sub.equals("enable");
				if (this.plugin.rules().set(id, allow)) {
					this.plugin.broadcast();
					this.plugin.getLogger().info(sender.getName() + " " + (allow ? "enabled " : "disabled ") + id);
				}
				sender.sendMessage(Component.text(id + " is now " + (allow ? "allowed" : "disabled")
					+ " on this server.", allow ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
			}
			default -> usage(sender, label);
		}
		return true;
	}

	private void list(CommandSender sender) {
		sender.sendMessage(Component.text("Lumitier's mods on this server:", NamedTextColor.GRAY));
		for (Map.Entry<String, Boolean> e : this.plugin.rules().all().entrySet()) {
			boolean on = e.getValue();
			sender.sendMessage(Component.text(" " + e.getKey() + ": ", NamedTextColor.GRAY)
				.append(Component.text(on ? "allowed" : "disabled",
					on ? NamedTextColor.GREEN : NamedTextColor.RED)));
		}
	}

	private static void usage(CommandSender sender, String label) {
		sender.sendMessage(Component.text("/" + label + " <list|enable|disable|reload> [mod]",
			NamedTextColor.GRAY));
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> out = new ArrayList<>();
		if (args.length == 1) {
			for (String s : SUBCOMMANDS) {
				if ((s.equals("list") || sender.hasPermission(PERMISSION))
						&& s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
					out.add(s);
				}
			}
		} else if (args.length == 2 && sender.hasPermission(PERMISSION)) {
			String sub = args[0].toLowerCase(Locale.ROOT);
			if (sub.equals("enable") || sub.equals("disable")) {
				for (String id : this.plugin.rules().ids()) {
					if (id.startsWith(args[1].toLowerCase(Locale.ROOT))) {
						out.add(id);
					}
				}
			}
		}
		return out;
	}
}
