/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredStatue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Sentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModules;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;

/**
 * What the enchantments' pictures keep for one sprite, in its fxSlots[FxModules.ENCHANT]: the blow
 * landing on it now (how many procs joined it, whether its death accent was drawn), the tint a
 * swing of an enchanted weapon left on it, the one sting its blow will play and the last it
 * played, when it last answered a step, and what its wielder holds (weaponCodes: up to two looks
 * of the weapon in hand, polled every POLL_EVERY s on the host). Made once a sprite first needs
 * it; nothing in it is allocated again.
 *
 * Its fields are written on the render thread (EnchantFx's posted work, the module's afterDraw),
 * but the weapon poll, which reads the character from wherever the swing came.
 */
public final class EnchantState {

	/** How often the weapon in hand is looked at again, s. */
	public static final float POLL_EVERY = 0.25f;
	/** The most weapon looks kept. */
	public static final int WEAPON_LOOKS = 2;

	// ------------------------------------------------------------------ the blow

	/** This proc's place on the blow landing now (0 the first, which owns the stamp). */
	public int blowIndex;
	/** Until when the blow's death accent counts as drawn (a finisher, Game.timeTotal). */
	public float accentUntil = -1f;
	/** The look a non-proc blow of an enchanted weapon draws its star and crescent toward, and
	 *  until when (EnchantFx.swing writes it right before the blow's HitFx reads it). */
	public EnchantLook swingLook;
	public float swingUntil = -1f;

	// ------------------------------------------------------------------ the blows on it (effects/fx/combat/Blow)

	/** Its live hit card (HitCard), and the small cards of what flowed back to it as a wielder (a
	 *  heal, mana, a shield); dead or another sprite's once recycled, so read with care. */
	public HitCard card;
	public final HitCard[] gains = new HitCard[3];
	/** When its last red X was drawn (PieceFx: one a frame, so pellets draw one). */
	public float critAt = -1f;
	/** When its stun stars drop in (Looks.Stunned, Game.timeTotal), -1 none: a stun put on it inside
	 *  a blow holds them (Looks.stunning sets it a while ahead) till the blow's stun beat sets it to
	 *  its own time. Written on whichever thread the blow runs, read on the render thread. */
	public volatile float stunAt = -1f;

	// ------------------------------------------------------------------ the sting

	//the sting this blow will play (the strongest asked for), whether it is an echo's, for whom,
	//whether it is waiting for its frame
	EnchantLook stingLook;
	int stingTier;
	boolean stingEcho, stingWaiting;
	Char stingAt;
	//the last sample played on this character and when
	String lastSample;
	float lastAt = -1f;

	// ------------------------------------------------------------------ steps

	/** When it last answered a step (EnchantFx.passive's STEP, once per STEP_GAP). */
	float stepAt = -1f;

	// ------------------------------------------------------------------ the weapon in hand

	/** The looks' codes of the weapon its character wields, the primary first (-1: none), as many
	 *  as weaponCount; and the weapon's enchant level. */
	public final int[] weaponCodes = { -1, -1 };
	public int weaponCount, weaponLevel;
	private float polledAt = -1f;

	/** The sprite's state, made if it has none. Any thread. */
	public static EnchantState of( CharSprite s ){
		Object o = s.fxSlots[FxModules.ENCHANT];
		if (o == null){
			synchronized (s.fxSlots){
				o = s.fxSlots[FxModules.ENCHANT];
				if (o == null) s.fxSlots[FxModules.ENCHANT] = o = new EnchantState();
			}
		}
		return (EnchantState) o;
	}

	/** The sprite's state, null if it has none yet. */
	public static EnchantState get( CharSprite s ){
		return s == null ? null : (EnchantState) s.fxSlots[FxModules.ENCHANT];
	}

	/** The weapon in the character's hand looked at again if POLL_EVERY s have passed (the host's
	 *  hero, statues and sentinels; a co-op guest's are left to what the host sends). */
	public void poll( Char ch ){
		float now = Game.timeTotal;
		if (polledAt >= 0 && now - polledAt < POLL_EVERY && now >= polledAt) return;
		polledAt = now;
		if (NetManager.isNetClient()) return;
		Weapon w = wielded( ch );
		weaponCount = 0;
		weaponLevel = 0;
		weaponCodes[0] = weaponCodes[1] = -1;
		if (w != null){
			add( w, w.enchantment );
			add( w, w.enchantment2 );
		}
		carried( ch, w );
	}

	private void add( Weapon w, Weapon.Enchantment e ){
		if (e == null || weaponCount >= WEAPON_LOOKS) return;
		if (e.curse() && !w.cursedKnown) return;
		EnchantLook l = EnchantLooks.of( e.getClass() );
		if (l == null) return;
		if (weaponCount == 0) weaponLevel = e.level();
		weaponCodes[weaponCount++] = l.code;
	}

	// ------------------------------------------------------------------ what it carries (ench-item-looks)

	/** The looks' codes of the armour its character wears, as many as armorCount (-1: none); the
	 *  weapon in hand's echo (its identified vestige's code, -1: none); how charged a Kinetic weapon
	 *  is, 0-3 (its stored damage under 10%, 30%, 60% of the wielder's health, or past). Polled with
	 *  the weapon; a co-op guest's come from the host (EnchantNet's WIELD). */
	public final int[] armorCodes = { -1, -1 };
	public int armorCount, echoCode = -1, kineticBin;
	/** The Kinetic bins' edges, shares of the wielder's health. */
	public static final float[] KINETIC_BINS = { 0.10f, 0.30f, 0.60f };
	/** What a co-op guest is sent again after this long, while there is anything (a guest joining
	 *  late learns it). */
	public static final float WIELD_RESEND = 4f;

	//what was last sent to the guests and when
	private int sentSig = SIG_NONE;
	private float sentAt = -1e9f;
	/** WieldLook's own: when each channel (two weapon looks, two glyphs, the echo) is next due, and
	 *  the times and lives of its last two motes each. */
	final float[] wieldNext = new float[5];
	final float[] wieldAt = new float[10], wieldLife = new float[10];
	/** LineLooks' own: until when its look on this sprite lives and how many quads it holds, counted
	 *  by WieldLook.live against its ceiling. */
	float lineUntil;
	int lineQuads;

	private void carried( Char ch, Weapon w ){
		armorCount = 0;
		armorCodes[0] = armorCodes[1] = -1;
		echoCode = -1;
		kineticBin = 0;
		Armor a = worn( ch );
		if (a != null){
			glyph( a, a.glyph );
			glyph( a, a.glyph2 );
		}
		if (w != null && w.isIdentified()){
			Weapon.Enchantment v = Quality.vestigeOf( w );
			EnchantLook l = v != null ? EnchantLooks.of( v.getClass() ) : null;
			if (l != null) echoCode = l.code;
		}
		if (wields( KINETIC ) && ch.HT > 0){
			Kinetic.ConservedDamage k = ch.buff( Kinetic.ConservedDamage.class );
			float share = k == null ? 0f : k.damageBonus() / (float)ch.HT;
			for (float edge : KINETIC_BINS) if (share >= edge) kineticBin++;
		}
		if (NetManager.isHost()){
			int sig = sig();
			float now = Game.timeTotal;
			if (sig != sentSig || (sig != SIG_NONE && (now - sentAt > WIELD_RESEND || now < sentAt))){
				sentSig = sig;
				sentAt = now;
				EnchantNet.sendWield( ch, this );
			}
		}
	}

	private void glyph( Armor a, Armor.Glyph g ){
		if (g == null || armorCount >= WEAPON_LOOKS) return;
		if (g.curse() && !a.cursedKnown) return;
		EnchantLook l = EnchantLooks.of( g.getClass() );
		if (l != null) armorCodes[armorCount++] = l.code;
	}

	private static final int KINETIC = EnchantLooks.of( Kinetic.class ).code;

	/** Whether the weapon in hand shows this look's code. */
	public boolean wields( int code ){
		for (int i = 0; i < weaponCount; i++) if (weaponCodes[i] == code) return true;
		return false;
	}

	//what it carries as one number: what a guest was last told
	private static final int SIG_NONE = 0x5EED;

	private int sig(){
		int h = SIG_NONE;
		h = h * 31 + weaponCodes[0];
		h = h * 31 + weaponCodes[1];
		h = h * 31 + weaponLevel;
		h = h * 31 + armorCodes[0];
		h = h * 31 + armorCodes[1];
		h = h * 31 + echoCode;
		h = h * 31 + kineticBin;
		return weaponCount == 0 && armorCount == 0 && echoCode < 0 ? SIG_NONE : h;
	}

	/** What the host sent (EnchantNet's WIELD: weapon codes, level, armour codes, echo, Kinetic's bin),
	 *  as a guest holds it. Codes off the table count as none. */
	void wielded( int w0, int w1, int level, int a0, int a1, int echo, int bin ){
		weaponCount = 0;
		weaponCodes[0] = weaponCodes[1] = -1;
		if (EnchantLooks.byCode( w0 ) != null) weaponCodes[weaponCount++] = w0;
		if (EnchantLooks.byCode( w1 ) != null) weaponCodes[weaponCount++] = w1;
		weaponLevel = Math.max( 0, level );
		armorCount = 0;
		armorCodes[0] = armorCodes[1] = -1;
		if (EnchantLooks.byCode( a0 ) != null) armorCodes[armorCount++] = a0;
		if (EnchantLooks.byCode( a1 ) != null) armorCodes[armorCount++] = a1;
		echoCode = EnchantLooks.byCode( echo ) != null ? echo : -1;
		kineticBin = Math.max( 0, Math.min( KINETIC_BINS.length, bin ) );
	}

	/** The armour a character wears: the hero's, an armoured statue's. Null for anyone else. */
	public static Armor worn( Char ch ){
		if (ch instanceof Hero){
			Hero h = (Hero) ch;
			return h.belongings != null ? h.belongings.armor() : null;
		}
		if (ch instanceof ArmoredStatue) return ((ArmoredStatue) ch).armor();
		return null;
	}

	/** The weapon a character wields: the hero's in hand, a statue's, a sentinel's. Null for anyone
	 *  else. */
	public static Weapon wielded( Char ch ){
		if (ch instanceof Hero){
			Hero h = (Hero) ch;
			KindOfWeapon k = h.belongings != null ? h.belongings.weapon() : null;
			return k instanceof Weapon ? (Weapon) k : null;
		}
		if (ch instanceof Statue) return ((Statue) ch).weapon();
		if (ch instanceof Sentinel) return ((Sentinel) ch).weapon();
		return null;
	}
}
