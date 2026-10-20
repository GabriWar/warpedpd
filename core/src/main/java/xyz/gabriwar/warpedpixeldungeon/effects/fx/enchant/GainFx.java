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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.items.EquipableItem;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundlable;

/**
 * The moments an item gains, shows or gives away its magic (docs/fx/enchant.md, "Gaining"):
 * Enchanting's look and sting (the ceremony itself is effects/Enchanting), the first pickup of an
 * enchanted item, and identifying revealing a curse or a vestige. Every entry is any thread (it
 * reads the item where it is called, then hands its pictures to the render thread), draws nothing
 * of the game's Random and changes nothing of the game; the host sends each to co-op guests
 * (EnchantNet GAIN and REVEAL), who play it from their own sprites.
 *
 *   lookOf(item)               an item's own look: its first slot's (a curse's once known), else its second's
 *   gainLook(item)             Enchanting's look for it: lookOf, or none (today's picture) where the item
 *                              glows in a colour of no magic of its own (HolyWeapon's, HolyWard's yellow)
 *   collected(owner, item)     Item.collect: a good enchantment or a known curse entering a hero's pack
 *                              the first time (a ring of the last PICKUPS items he held, a stack's by its
 *                              kind; one unequipped or held before is put back, not picked up): a cast
 *                              bloom in its palette at him, three motes of its motif, its slot's aura
 *                              running faster
 *   held(item)                 an item a hero holds took or showed its magic: remembered as held
 *   identified(item, ...)      Item.identify, the item a hero's (a co-op guest's too): a curse revealed (the hex in small, no ghost: six VOID motes
 *                              drawn into the hero and the CURSED sting), a vestige revealed (its
 *                              enchantment's finale at 0.35 on the hero, then VESTIGE_LATE s later its echo:
 *                              the doubled ring at his feet and three ghost motes drawn into him)
 */
public final class GainFx {

	private GainFx(){}

	/** What a REVEAL is: a curse identified, a vestige identified, a first pickup. */
	public static final int CURSE = 0, VESTIGE = 1, PICKUP = 2;

	/** How many items' first pickups are remembered (render thread). */
	public static final int PICKUPS = 64;
	/** A vestige's echo after its enchantment's finale, s; a vestige revealed after a curse, s. */
	public static final float VESTIGE_LATE = 0.4f, AFTER_CURSE = 0.6f;
	/** The finales' scale on a reveal. */
	public static final float REVEAL_SCALE = 0.35f;
	/** A revealed curse's sting. */
	public static final float CURSE_VOL = 0.35f;
	/** The echo's own sign: two thin rings 0.06 s apart at 0.45; its motes. */
	public static final float ECHO_GAP = 0.06f, ECHO_ALPHA = 0.45f, ECHO_RING_LIFE = 0.35f;
	public static final int ECHO_MOTES = 3;
	public static final int[][] ECHO_RING = { FxFrames.RING_9, FxFrames.RING_13, FxFrames.RING_17 };
	/** The motif motes a pickup throws. */
	public static final int PICKUP_MOTES = 3;

	// ------------------------------------------------------------------ the looks

	/** An item's own look (a weapon's or an armour's): its first slot's, a curse's only once known,
	 *  else its second's; null for none or any other item. Reads only. */
	public static EnchantLook lookOf( Item item ){
		if (item instanceof Weapon) return EnchantFx.lookOf( (Weapon) item );
		if (item instanceof Armor){
			Armor a = (Armor) item;
			Armor.Glyph g = shown( a, a.glyph );
			if (g == null) g = shown( a, a.glyph2 );
			return g != null ? EnchantLooks.of( g.getClass() ) : null;
		}
		return null;
	}

	/** The look of an enchantment or a glyph just woven (null for none). */
	public static EnchantLook of( Object magic ){
		return magic != null ? EnchantLooks.of( magic.getClass() ) : null;
	}

	private static Armor.Glyph shown( Armor a, Armor.Glyph g ){
		return g != null && (!g.curse() || a.cursedKnown) ? g : null;
	}

	/** Enchanting's look for an item: its own (lookOf), or null - today's picture - when it shows no
	 *  glow, or one of no magic of its own (HolyWeapon's and HolyWard's yellow, laid over it). */
	public static EnchantLook gainLook( Item item ){
		if (item == null) return null;
		ItemSprite.Glowing g = item.glowing();
		if (g == null) return null;
		EnchantLook l = lookOf( item );
		if (l == null || !ownGlow( item, g )) return null;
		return l;
	}

	//whether a glow is the item's own magic's: one of its classes' glows, or two shown ones blended
	//as Weapon.glowing and Armor.glowing blend them
	private static boolean ownGlow( Item item, ItemSprite.Glowing g ){
		if (EnchantLooks.forGlowing( g ) != null) return true;
		ItemSprite.Glowing a = null, b = null;
		if (item instanceof Weapon){
			Weapon w = (Weapon) item;
			a = glowOf( w.enchantment, w.cursedKnown );
			b = glowOf( w.enchantment2, w.cursedKnown );
		} else if (item instanceof Armor){
			Armor r = (Armor) item;
			a = r.glyph != null && (r.cursedKnown || !r.glyph.curse()) ? r.glyph.glowing() : null;
			b = r.glyph2 != null && (r.cursedKnown || !r.glyph2.curse()) ? r.glyph2.glowing() : null;
		}
		return a != null && b != null && g.color == blend( a.color, b.color );
	}

	private static ItemSprite.Glowing glowOf( Weapon.Enchantment e, boolean cursedKnown ){
		return e != null && (cursedKnown || !e.curse()) ? e.glowing() : null;
	}

	static int blend( int c1, int c2 ){
		int r = (((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF)) / 2;
		int g = (((c1 >> 8) & 0xFF) + ((c2 >> 8) & 0xFF)) / 2;
		int b = ((c1 & 0xFF) + (c2 & 0xFF)) / 2;
		return (r << 16) | (g << 8) | b;
	}

	/** The level of an item's shown look (its first shown slot's), 0 for none. */
	public static int levelOf( Item item ){
		if (item instanceof Weapon){
			Weapon w = (Weapon) item;
			Weapon.Enchantment e = glowOf( w.enchantment, w.cursedKnown ) != null ? w.enchantment
					: glowOf( w.enchantment2, w.cursedKnown ) != null ? w.enchantment2 : null;
			return e != null ? e.level() : 0;
		}
		if (item instanceof Armor){
			Armor a = (Armor) item;
			Armor.Glyph g = shown( a, a.glyph );
			if (g == null) g = shown( a, a.glyph2 );
			return g != null ? g.level() : 0;
		}
		return 0;
	}

	//the look a curse with no class of its own takes (a cursed ring, a cursed wand): the hex is
	//drawn in VOID whatever the row
	private static EnchantLook curseOf( Item item ){
		EnchantLook l = lookOf( item );
		if (l != null && l.curse) return l;
		if (item instanceof Weapon){
			Weapon w = (Weapon) item;
			for (Weapon.Enchantment e : w.enchantments()) if (e.curse()) return EnchantLooks.of( e.getClass() );
		} else if (item instanceof Armor){
			for (Armor.Glyph g : ((Armor) item).glyphs()) if (g.curse()) return EnchantLooks.of( g.getClass() );
			return EnchantLooks.byCode( EnchantLooks.ARMOR_CURSES );
		}
		return EnchantLooks.byCode( EnchantLooks.WEAPON_CURSES );
	}

	private static boolean curseMagic( Item item ){
		if (item instanceof Weapon) return ((Weapon) item).hasCurseEnchant();
		if (item instanceof Armor) return ((Armor) item).hasCurseGlyph();
		return false;
	}

	// ------------------------------------------------------------------ Enchanting's sting

	/** The ceremony's sting at its bind: the look's own sound (a curse's: CURSED at CURSE_VOL);
	 *  none for a look without one. Render thread. */
	public static void sting( EnchantLook l, Char at ){
		if (l == null) return;
		if (l.curse){
			Sting.out.play( Assets.Sounds.CURSED, at, CURSE_VOL, 1f );
		} else if (l.sound != null){
			Sting.out.play( l.sound, at, Sting.volume( l, EnchantFx.MID, false ),
					l.pitch * FxRandom.Float( 1f - Sting.PITCH_SPREAD, 1f + Sting.PITCH_SPREAD ) );
		}
	}

	/** The host's Enchanting going out to co-op guests (EnchantNet GAIN). Any thread. */
	public static void sendGain( Char owner, EnchantLook l, int image, int color ){
		EnchantNet.sendGain( l, owner, image, color );
	}

	//a guest's: the host's Enchanting over his own sprite of its owner
	static void replayGain( CharSprite owner, EnchantLook l, int image, int color ){
		if (owner == null) return;
		Enchanting.play( owner, image, l, color, 0, null );
	}

	// ------------------------------------------------------------------ the first pickup

	//what the hero has held lately (render thread): the identities of the last PICKUPS enchanted
	//items that went into a pack or were seen in one, a stack's by its kind (keyOf)
	private static final int[] picked = new int[PICKUPS];
	private static int pickedNext, pickedCount;

	/**
	 * An item went into a pack (Item.collect): if its owner is a hero on the scene, the item shows
	 * a good enchantment or a known curse, and he has not held it lately (checked on the render
	 * thread), a cast bloom in its palette at him, a few motes of its motif, and its slot's aura
	 * running faster for a second. An item put back rather than picked up shows nothing: one being
	 * unequipped (still in its slot while collect runs), and one he held before, which is why every
	 * call also remembers the other enchanted items in his belongings (a loaded game's, the gear he
	 * wears, a weapon enchanted where it lay). A thrown stack's pieces are new objects, so a stack
	 * is remembered by its kind. A pack filled while the game loads (no sprite yet) shows nothing.
	 * Any thread; reads only.
	 */
	public static void collected( Char owner, final Item item ){
		if (!(owner instanceof Hero) || item == null) return;
		final Hero h = (Hero) owner;
		final CharSprite s = owner.sprite;
		if (s == null) return;
		final EnchantLook l = lookOf( item );
		if (l == null) return;
		if (item instanceof EquipableItem && ((EquipableItem) item).isEquipped( h )) return;
		final int key = keyOf( item, l );
		final int[] held = heldBy( h, item );
		Fx.post( () -> firstPickup( owner, s, item, l, key, held ) );
	}

	//what an item is remembered by: itself, or a stack's kind and look (its thrown pieces split off
	//as new objects)
	static int keyOf( Item item, EnchantLook l ){
		return item.stackable ? 31 * item.getClass().hashCode() + l.code : System.identityHashCode( item );
	}

	//the keys of every other enchanted item a hero holds, worn or carried (the caller's thread,
	//where his belongings change)
	private static int[] heldBy( Hero h, Item except ){
		if (h.belongings == null) return new int[0];
		int n = 0;
		int[] out = new int[8];
		for (Item i : h.belongings){
			if (i == except) continue;
			EnchantLook l = lookOf( i );
			if (l == null) continue;
			if (n == out.length) out = java.util.Arrays.copyOf( out, n * 2 );
			out[n++] = keyOf( i, l );
		}
		return java.util.Arrays.copyOf( out, n );
	}

	private static void firstPickup( Char owner, CharSprite s, Item item, EnchantLook l, int key, int[] held ){
		boolean seen = remembered( key );
		for (int k : held) remember( k );
		remember( key );
		if (seen) return;
		EnchantNet.sendReveal( PICKUP, l, owner );
		EnchantFx.markGained( item );
		if (s.parent == null || !s.visible) return;
		pickup( s, l );
	}

	private static boolean remembered( int key ){
		for (int i = 0; i < pickedCount; i++) if (picked[i] == key) return true;
		return false;
	}

	private static void remember( int key ){
		if (remembered( key )) return;
		picked[pickedNext] = key;
		pickedNext = (pickedNext + 1) % PICKUPS;
		pickedCount = Math.min( PICKUPS, pickedCount + 1 );
	}

	/** An item a hero holds took or showed its magic (Enchanting, an identified curse): remembered,
	 *  so putting it back in his pack later is no first pickup. Any thread; reads only. */
	public static void held( final Item item ){
		if (item == null) return;
		EnchantLook l = lookOf( item );
		if (l == null) return;
		final int key = keyOf( item, l );
		Fx.post( () -> remember( key ) );
	}

	/** Forgets every pickup (a test's fresh start). Render thread. */
	static void forgetPickups(){
		pickedNext = pickedCount = 0;
	}

	//the pickup's picture at a sprite
	private static void pickup( CharSprite s, EnchantLook l ){
		if (Fx.glowAir() == null) return;
		float x = s.x + s.width() / 2f, y = s.y + s.height() / 2f;
		FxRecipes.castBloom( x, y, l.strike );
		MotifFx.burst( l, x, y - 2f, PICKUP_MOTES );
	}

	// ------------------------------------------------------------------ identifying

	/**
	 * An item was identified (Item.identify; what was known before it given): if a hero holds it
	 * (the host's own, or on the host a co-op guest's hero) and its curse just became known, the hex
	 * in small at him; if it is a weapon or armor whose lines (and so its vestige) just became known, the
	 * vestige's reveal - after the curse's, if both. Guests see it at their sprite of that hero.
	 * Any thread; reads only.
	 */
	public static void identified( Item item, boolean knewCurse, boolean knewAll ){
		if (item == null) return;
		EnchantLook curse = null, vestige = null;
		if (!knewCurse && item.cursedKnown && (item.cursed || curseMagic( item ))) curse = curseOf( item );
		if (!knewAll && item.isIdentified()){
			Bundlable v = Quality.vestigeMagic( item );
			if (v != null) vestige = EnchantLooks.of( v.getClass() );
		}
		if (curse == null && vestige == null) return;
		Hero h = holder( item );
		if (h == null) return;
		if (curse != null) held( item );
		reveal( h, h.sprite, curse, vestige, false );
	}

	//the hero holding an item: the game's own, else (on a host) a co-op guest's; null for none
	private static Hero holder( Item item ){
		if (holds( Dungeon.hero, item )) return Dungeon.hero;
		if (!NetManager.isHost()) return null;
		for (Hero h : NetManager.getNetHeroes()) if (holds( h, item )) return h;
		return null;
	}

	private static boolean holds( Hero h, Item item ){
		return h != null && h.isAlive() && h.belongings != null && h.belongings.contains( item );
	}

	private static void reveal( final Char ch, final CharSprite s, final EnchantLook curse, final EnchantLook vestige,
			final boolean guest ){
		if (!guest){
			if (curse != null) EnchantNet.sendReveal( CURSE, curse, ch );
			if (vestige != null) EnchantNet.sendReveal( VESTIGE, vestige, ch );
		}
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible || Fx.glowAir() == null) return;
			if (curse != null) curseReveal( s, curse );
			if (vestige == null) return;
			if (curse == null) vestigeReveal( s, vestige );
			else EnchantFx.later( AFTER_CURSE, () -> {
				if (s.parent != null) vestigeReveal( s, vestige );
			} );
		} );
	}

	//a guest's: what the host revealed, at his own sprite
	static void replayReveal( int what, EnchantLook l, CharSprite s ){
		if (l == null || s == null) return;
		switch (what){
			case CURSE:   reveal( s.ch, s, l, null, true ); break;
			case VESTIGE: reveal( s.ch, s, null, l, true ); break;
			case PICKUP:
				Fx.post( () -> {
					if (s.parent != null && s.visible) pickup( s, l );
				} );
				break;
			default: break;
		}
	}

	//the hex in small at a sprite, no ghost: six VOID motes drawn into him, a ring closing at his
	//feet, the CURSED sting
	private static void curseReveal( CharSprite s, EnchantLook l ){
		float x = s.x + s.width() / 2f, y = s.y + s.height() / 2f;
		EnchantFx.finale( l.curse ? l : EnchantLooks.byCode( EnchantLooks.WEAPON_CURSES ), x, y, 1f );
		Sting.out.play( Assets.Sounds.CURSED, s.ch, CURSE_VOL, 1f );
	}

	//a vestige revealed: its enchantment's finale in small, then a beat later its echo (the
	//doubled ring at the feet, three ghost motes drawn in) with its sting muffled
	private static void vestigeReveal( final CharSprite s, final EnchantLook l ){
		float x = s.x + s.width() / 2f, y = s.y + s.height() / 2f;
		EnchantFx.finale( l, x, y, REVEAL_SCALE );
		EnchantFx.later( VESTIGE_LATE, () -> {
			if (s.parent == null) return;
			echo( s, l );
			if (l.sound != null){
				Sting.out.play( l.sound, s.ch, Sting.volume( l, EnchantFx.MID, true ), l.pitch * Sting.ECHO_PITCH );
			}
		} );
	}

	/** The quads an echo's finale makes: two rings and its motes. */
	public static final int ECHO_CAP = 2 + ECHO_MOTES;

	//the echo's finale, hollow: the doubled thin ring at the feet in the ghost's grey, three motes
	//of it drawn into the middle
	private static void echo( final CharSprite s, EnchantLook l ){
		final float x = s.x + s.width() / 2f, y = s.y + s.height() / 2f, feet = s.y + s.height() - 2f;
		final EnchantFx.Beat b = EnchantFx.Beat.at( l, x, y, ECHO_CAP );
		final int grey = EnchantLook.paled( l.light );
		b.flash( false, true, ECHO_RING, 0f, x, feet, grey, ECHO_ALPHA, ECHO_RING_LIFE );
		final EnchantFx.Beat second = b.split( 1 );
		EnchantFx.later( ECHO_GAP, () -> second.flash( false, true, ECHO_RING, 0f, x, feet, grey, ECHO_ALPHA, ECHO_RING_LIFE ) );
		for (int i = 0; i < ECHO_MOTES; i++){
			float a = (float)(2 * Math.PI * i / ECHO_MOTES) + FxRandom.Float( -0.3f, 0.3f );
			float sx = x + (float)Math.cos( a ) * 10f, sy = y + (float)Math.sin( a ) * 6f;
			FxParticle p = b.lit( sx, sy, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.MOTE_1 ).paint( EnchantLook.paled( l.glow ) ).vel( (x - sx) / 0.35f, (y - sy) / 0.35f )
					.life( 0.35f ).alpha( ECHO_ALPHA, 0.05f, 0.1f ).snap();
		}
	}

	// ------------------------------------------------------------------ restaged (gallery, previews)

	/** A first pickup at a sprite, restaged: nothing sent, nothing remembered. Render thread. */
	public static void stagePickup( CharSprite s, EnchantLook l ){
		if (s != null && l != null) pickup( s, l );
	}

	/** A curse revealed at a sprite, restaged. Render thread. */
	public static void stageCurseReveal( CharSprite s, EnchantLook l ){
		if (s != null && l != null) curseReveal( s, l );
	}

	/** A vestige revealed at a sprite, restaged. Render thread. */
	public static void stageVestigeReveal( CharSprite s, EnchantLook l ){
		if (s != null && l != null) vestigeReveal( s, l );
	}
}
