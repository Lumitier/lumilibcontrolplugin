package com.lumitier.lumilibcontrolplugin;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Baut das Paket, das LumiLib liest.
 *
 * Das Format MUSS Byte fuer Byte zu ServerRules.Payload in LumiLib passen:
 *
 * <pre>
 *   VarInt   Fassung (1)
 *   Boolean  darf dieser Spieler die Regeln aendern?
 *   VarInt   Zahl der Eintraege
 *   je Eintrag:
 *     String   Mod-ID (VarInt-Laenge, dann UTF-8)
 *     Boolean  erlaubt?
 * </pre>
 *
 * Von Hand geschrieben statt ueber eine Bibliothek: Es sind drei
 * Datentypen, und so haengt das Plugin an nichts als der Paper-API.
 */
final class Packets {

	/** Muss mit ServerRules.PROTOCOL in LumiLib uebereinstimmen. */
	static final int PROTOCOL = 1;

	private Packets() {}

	static byte[] rules(boolean admin, Map<String, Boolean> mods) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(16 + mods.size() * 20);
		writeVarInt(out, PROTOCOL);
		out.write(admin ? 1 : 0);
		writeVarInt(out, mods.size());
		for (Map.Entry<String, Boolean> e : mods.entrySet()) {
			writeString(out, e.getKey());
			out.write(e.getValue() ? 1 : 0);
		}
		return out.toByteArray();
	}

	/** Minecrafts VarInt: 7 Bit pro Byte, hoechstes Bit = es folgt noch eins. */
	private static void writeVarInt(ByteArrayOutputStream out, int value) {
		while ((value & ~0x7F) != 0) {
			out.write((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		out.write(value);
	}

	private static void writeString(ByteArrayOutputStream out, String s) {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		writeVarInt(out, bytes.length);
		out.write(bytes, 0, bytes.length);
	}
}
