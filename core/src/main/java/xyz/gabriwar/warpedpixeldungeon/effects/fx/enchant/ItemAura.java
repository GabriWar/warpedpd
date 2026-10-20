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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Displacement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirroring;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blooming;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Corrupting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Holy;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Surging;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

import java.util.ArrayList;

/**
 * An enchanted item's motes in a slot (the bag, the quickslots, the inventory pane, the pedestal's
 * slot) or in the info window: the art spec's UI renderings of the thirteen motifs (its 2.2 and
 * 4.3). A fixed set of quads made once - a backlight (GLOW_11, added) behind the item, a dark
 * halo behind a dark look's, up to SLOT_MOTES (PANEL_MOTES in the info window) motes and one echo
 * mote - whose place, frame and alpha are pure functions of the time and the item's own phase: a
 * mote "spawns" as its slot's cycle wraps. No Random of either kind, nothing allocated after it is
 * made, render thread only (ItemSprite steps and draws it).
 *
 * Re-keyed, not rebuilt: a refresh of the same item (a quickslot's every turn) changes nothing;
 * a new look sets each slot's look and sheet once. Runs only while its slot shows (visible, active,
 * the sprite shown). Two looks share the slots, the odd ones the first's and the even the second's;
 * a curse's motes come from 3-4 px out, drawn in; the echo (an identified vestige) one pale mote
 * orbiting at half speed; an item that gained its magic under a second ago runs three times as fast.
 *
 * Some rows twist their motif (the art spec's 6 and 7): Chilling's rime glint, Lucky's clover, the
 * dark looks' rims (Eldritch's eye, Grim's crawl...), Paralysis's ring that stops dead, Vicious's
 * bead splitting as it falls and its serration glinting, Slashing's streak across the blade,
 * Ancient's runes of age (one, and one more for each four kills toward its attunement), Midas's
 * one coin with gold falling off it, and Polarized's backlight split red and blue (the dark halo's
 * quad lends its right half).
 *
 * Every aura's motes come from one UI pool of POOL quads: past STRETCH_PAST in use the motes come
 * STRETCH times slower, and a slot that finds the pool spent shows its backlight alone - but an
 * equipped, quickslotted or selected slot is served first (it takes from the others). At FxBudget
 * MINIMAL (the tier read, nothing kept or spent from the world's budget) the backlight alone.
 *
 * An item with no look of magic shows instead its rarity line's hint (hintOf: warm grip, cooling,
 * rainproof, moonlit by night, fleet - the first it holds), one quad and no backlight, only where it
 * is equipped or quickslotted and in the info window (hinted); served last from the pool (it never
 * takes from another slot), none at MINIMAL. An item that shows a look of magic shows no hint.
 */
public final class ItemAura {

	/** The motes a slot's aura has, and an info window's; the most quads each draws. */
	public static final int SLOT_MOTES = 3, PANEL_MOTES = 5, SLOT_QUADS = 5, PANEL_QUADS = 7;
	/** The UI pool of mote quads, and how many in use stretch the periods. */
	public static final int POOL = 160, STRETCH_PAST = 96;
	public static final float STRETCH = 1.5f;
	/** The info window's rate; a just-gained item's, for how long. */
	public static final float PANEL_RATE = 1.5f, GAINED_RATE = 3f, GAINED_FOR = 1f;
	/** The backlight's size (GLOW_11 scaled), the dark halo's alpha. */
	public static final float BACK_SCALE = 1.6f, HALO_SCALE = 1.9f, HALO_ALPHA = 0.35f;
	/** The echo: its alpha, its turns a second (half an orbit's). */
	public static final float ECHO_ALPHA = 0.45f, ECHO_TURNS = 0.4f;
	/** A claim not used for this long goes back to the pool. */
	public static final float STALE = 0.5f;
	/** The hints' every (s) and life (s), by LineLooks look (WARM_GRIP..FLEET; WINDBREAK has none);
	 *  their colours. */
	public static final float[] HINT_EVERY = { 1.6f, 4f, 1.8f, 2.5f, 0f, 3f }, HINT_LIFE = { 0.8f, 0.3f, 0.7f, 1f, 0f, 0.12f };
	public static final int HINT_WARM = 0xFF9A48, HINT_COOL = 0xBCE0FF, HINT_RAIN = 0xA8D8FF, HINT_FLEET = 0xF0E8D8;

	//the pool: who holds mote quads, and how many are held
	private static final ArrayList<ItemAura> holders = new ArrayList<>( 256 );
	private static int held;

	private final boolean panel;
	private final Quad back, halo, echo, hint;
	private final Quad[] motes;
	//each mote slot's look and its place among that look's slots, and how many slots each look has
	private final EnchantLook[] slotLook;
	private final int[] slotIndex, slotCount;
	//what it was keyed by, and the look the holy override's motes take
	private final ItemLooks.Set key = new ItemLooks.Set();
	private boolean keyed;
	//the mote quads it holds from the pool, whether it is served first, when it last ran
	private int claim;
	private boolean priority;
	private float ranAt = -1;
	//quads drawn this frame (tests)
	private int shown;
	//the item frame a ghost copies, as last seen
	private RectF ghostOf;
	private int seed;

	public ItemAura( boolean panel ){
		this.panel = panel;
		int n = panel ? PANEL_MOTES : SLOT_MOTES;
		back = new Quad( Assets.Effects.FX_LIGHT, true );
		halo = new Quad( Assets.Effects.FX_LIGHT, false );
		back.frame( FxFrames.Light.GLOW_11 );
		halo.frame( FxFrames.Light.GLOW_11 );
		back.behind = halo.behind = true;
		echo = new Quad( Assets.Effects.FX_SHEET, true );
		echo.frame( FxFrames.MOTE_1 );
		hint = new Quad( Assets.Effects.FX_SHEET, true );
		motes = new Quad[n];
		for (int i = 0; i < n; i++) motes[i] = new Quad( Assets.Effects.FX_SHEET, true );
		slotLook = new EnchantLook[n];
		slotIndex = new int[n];
		slotCount = new int[n];
	}

	// ------------------------------------------------------------------ the pool

	/** Mote quads held from the pool now, all auras together. */
	public static int held(){
		return held;
	}

	/** A test's: the pool empty, nobody holding. */
	public static void resetPool(){
		for (ItemAura a : holders) a.claim = 0;
		holders.clear();
		held = 0;
	}

	//up to `want` mote quads held (fewer if the pool is spent); how many it holds now
	private int take( int want, float now ){
		if (claim > want){
			held -= claim - want;
			claim = want;
			if (claim == 0) holders.remove( this );
		}
		if (claim < want){
			int need = want - claim;
			int free = POOL - held;
			if (free < need) free += reclaimStale( now );
			if (free < need && priority) free += steal( need - free );
			int got = Math.min( need, Math.max( 0, free ) );
			if (got > 0){
				if (claim == 0) holders.add( this );
				claim += got;
				held += got;
			}
		}
		return claim;
	}

	/** Its sprite is destroyed: what it holds given back and its quads' GPU buffers freed (they lie
	 *  outside the scene graph, so nothing else frees them). Render thread. */
	public void destroy(){
		release();
		back.destroy();
		halo.destroy();
		echo.destroy();
		hint.destroy();
		for (Quad q : motes) q.destroy();
	}

	/** Gives back what it holds (its slot hidden, its item gone). */
	public void release(){
		if (claim > 0){
			held -= claim;
			claim = 0;
			holders.remove( this );
		}
	}

	//what auras that stopped running hold, back to the pool; how much came back
	private static int reclaimStale( float now ){
		int back = 0;
		for (int i = holders.size() - 1; i >= 0; i--){
			ItemAura a = holders.get( i );
			if (now - a.ranAt > STALE || a.ranAt - now > STALE){
				back += a.claim;
				held -= a.claim;
				a.claim = 0;
				holders.remove( i );
			}
		}
		return back;
	}

	//taken from slots not served first, the newest first; how much was taken
	private static int steal( int need ){
		int got = 0;
		for (int i = holders.size() - 1; i >= 0 && got < need; i--){
			ItemAura a = holders.get( i );
			if (a.priority) continue;
			int k = Math.min( a.claim, need - got );
			a.claim -= k;
			held -= k;
			got += k;
			if (a.claim == 0) holders.remove( i );
		}
		return got;
	}

	// ------------------------------------------------------------------ the frame

	/** Mote quads it holds now (tests). */
	public int claimed(){
		return claim;
	}

	/** The quads it drew this frame (tests): never past SLOT_QUADS (PANEL_QUADS in the window). */
	public int quads(){
		return shown;
	}

	/**
	 * Once a frame, from its sprite's update (render thread): shown while `on` (its slot visible and
	 * active, the sprite shown) and the sprite shows a look; `first` serves it first from the pool.
	 */
	public void update( ItemSprite s, boolean on, boolean first ){
		shown = 0;
		ItemLooks.Set looks = s.looks();
		int line = on && !looks.any() ? hintOf( s.item() ) : -1;
		if (line >= 0 && !hinted( s.item(), panel )) line = -1;
		if (!on || (!looks.any() && line < 0)){
			hide();
			release();
			return;
		}
		float now = Game.timeTotal;
		ranAt = now;
		hint.visible = false;
		if (line >= 0){
			hintAlone( s, line, now );
			return;
		}
		priority = first;
		if (!keyed || !key.same( looks )) rekey( looks, s );

		float alpha = s.alpha();
		float cx = s.x + s.width() / 2f, cy = s.y + s.height() / 2f;

		//the dark halo, then the backlight, behind the item; Polarized's split in two halves
		EnchantLook darkOne = ItemLooks.dark( key.a ) ? key.a : (ItemLooks.dark( key.b ) ? key.b : null);
		EnchantLook pol = polarized( s.lookTime() );
		if (pol != null){
			split( pol, s, cx, cy, alpha );
			shown += 2;
		} else if (darkOne != null){
			halo.whole( FxFrames.Light.GLOW_11 );
			back.whole( FxFrames.Light.GLOW_11 );
			halo.add = false;
			float k = darkOne == key.a ? (key.b == null ? 1f : 1f - ItemLooks.crossB( s.lookTime() )) : ItemLooks.crossB( s.lookTime() );
			halo.hardlight( haloColor( darkOne ) );
			halo.am = HALO_ALPHA * Math.max( 0.35f, k ) * alpha;
			place( halo, cx, cy, HALO_SCALE );
			halo.visible = true;
			shown++;
		} else {
			halo.visible = false;
		}
		if (pol == null){
			back.whole( FxFrames.Light.GLOW_11 );
			back.hardlight( s.backColor() );
			back.am = s.backlight() * alpha;
			place( back, cx, cy, BACK_SCALE );
			back.visible = true;
			shown++;
		}

		//the motes, from the pool
		int slots = 0;
		for (EnchantLook l : slotLook) if (l != null) slots++;
		boolean echoOn = key.echo != null;
		int want = FxBudget.tier() == FxBudget.MINIMAL ? 0 : slots + (echoOn ? 1 : 0);
		int have = take( want, now );
		float rate = (panel ? PANEL_RATE : 1f) * (EnchantFx.gainedAgo( s.item() ) < GAINED_FOR ? GAINED_RATE : 1f);
		float stretch = held > STRETCH_PAST ? STRETCH : 1f;
		float t = s.lookTime();
		//the echo first: a pale mote is the cheapest sign of a vestige
		if (echoOn && have > 0){
			drawEcho( s, t, alpha );
			have--;
		} else {
			echo.visible = false;
		}
		for (int i = 0; i < motes.length; i++){
			Quad q = motes[i];
			EnchantLook l = slotLook[i];
			if (l == null || have <= 0){
				q.visible = false;
				continue;
			}
			have--;
			float period = l == key.b ? key.periodB : key.periodA;
			float spacing = l.motif.periodAt( key.level ) * Math.min( 1f, period ) * stretch / rate;
			q.visible = mote( q, l, slotIndex[i], slotCount[i], t, spacing, s, alpha );
			if (q.visible) shown++;
		}
	}

	//a new look: each slot's look and sheet set once
	private void rekey( ItemLooks.Set looks, ItemSprite s ){
		key.copy( looks );
		keyed = true;
		seed = Math.round( ItemLooks.phase( s.item() ) * 1000f );
		EnchantLook a = key.a, b = key.b;
		if (a == null && key.holy >= 0) a = EnchantLooks.of( Holy.class );
		boolean dark = ItemLooks.dark( a ) || ItemLooks.dark( b );
		int n = motes.length - (dark ? 1 : 0);
		int na = 0, nb = 0;
		for (int i = 0; i < motes.length; i++){
			EnchantLook l = i >= n ? null : (b == null || i % 2 == 0 ? a : b);
			if (l != null && (l == a ? na : nb) >= most( l )) l = null;
			slotLook[i] = l;
			if (l == null) continue;
			slotIndex[i] = l == a ? na++ : nb++;
		}
		for (int i = 0; i < motes.length; i++){
			EnchantLook l = slotLook[i];
			slotCount[i] = l == null ? 0 : (l == a ? na : nb);
			if (l != null) sheet( motes[i], l, slotIndex[i], slotCount[i] );
		}
		ghostOf = null;
	}

	//Polarized when it is the look shown more now (alone, or the heavier of a crossfade), else null
	private EnchantLook polarized( float t ){
		boolean pa = key.a != null && key.a.of == Polarized.class, pb = key.b != null && key.b.of == Polarized.class;
		if (!pa && !pb) return null;
		float k = key.b == null ? 0f : ItemLooks.crossB( t );
		float w = (pa ? 1f - k : 0f) + (pb ? k : 0f);
		return w > 0.5f ? (pa ? key.a : key.b) : null;
	}

	//Polarized's backlight split: its first colour on the left half, its second on the right (the
	//halo's quad, added), meeting under the item's middle
	private void split( EnchantLook l, ItemSprite s, float cx, float cy, float alpha ){
		float level = s.backlight() * alpha;
		back.half( FxFrames.Light.GLOW_11, true );
		halo.half( FxFrames.Light.GLOW_11, false );
		halo.add = true;
		back.hardlight( l.cycle != null ? l.cycle[0] : l.el.light );
		halo.hardlight( l.cycle != null && l.cycle.length > 1 ? l.cycle[1] : l.el.light );
		back.am = halo.am = level;
		back.scale.set( BACK_SCALE );
		halo.scale.set( BACK_SCALE );
		back.origin.set( back.width, back.height / 2f );
		halo.origin.set( 0f, halo.height / 2f );
		back.x = Math.round( cx - back.width );
		halo.x = Math.round( cx );
		back.y = halo.y = Math.round( cy - back.height / 2f );
		back.visible = halo.visible = true;
	}

	//the most slots a look's motif uses: a ghost and a colour walk one each
	private static int most( EnchantLook l ){
		return l.motif == Motif.GHOST || l.motif == Motif.CYCLE ? 1 : Integer.MAX_VALUE;
	}

	//the sheet a slot draws from and whether it is added: set once a key
	private void sheet( Quad q, EnchantLook l, int j, int n ){
		if (twistLight( l, j, n )){
			q.sheet( Assets.Effects.FX_SHEET, true );
			q.behind = l.of == Paralysis.class;
			return;
		}
		boolean rimSlot = ItemLooks.dark( l ) && j == 1;
		if (l.motif == Motif.GHOST){
			q.sheet( Assets.Sprites.ITEMS, true );
		} else if (rimSlot && l.of == Eldritch.class){
			q.sheet( Assets.Effects.FX_LIGHT, true );
		} else {
			q.sheet( Assets.Effects.FX_SHEET, light( l, rimSlot ) );
		}
		q.behind = l.motif == Motif.THROB || l.motif == Motif.GHOST;
	}

	//a twist's slot that draws light where its motif draws matter: Paralysis's ring, Vicious's
	//serration, Slashing's streak, Ancient's runes, Midas's falling gold
	private boolean twistLight( EnchantLook l, int j, int n ){
		Class<?> c = l.of;
		return (c == Paralysis.class && j == 0) || (c == Vicious.class && n > 2 && j == n - 1) || (c == Slashing.class && j == 0)
				|| (c == Ancient.class && j < Math.min( n, key.runes )) || (c == Midas.class && j > 0);
	}

	//whether a slot's motes are added light (else matter, drawn normally)
	private static boolean light( EnchantLook l, boolean rimSlot ){
		if (rimSlot) return l.of != Corrupting.class;
		return MotifFx.light( l );
	}

	private void hide(){
		back.visible = halo.visible = echo.visible = hint.visible = false;
		for (Quad q : motes) q.visible = false;
	}

	// ------------------------------------------------------------------ a line's hint

	/** The rarity line an item hints at in its slot (a LineLooks look), -1 for none: warm grip,
	 *  cooling, rainproof, moonlit by night, fleet, the first it holds. Only on an identified item
	 *  (an unknown one shows no lines). Render thread. */
	public static int hintOf( Item item ){
		Quality q = Quality.of( item );
		if (q == null || q.lines.isEmpty() || !item.isIdentified()) return -1;
		if (q.has( RarityLine.WARM_GRIP )) return LineLooks.WARM_GRIP;
		if (q.has( RarityLine.COOLING )) return LineLooks.COOLING;
		if (q.has( RarityLine.RAINPROOF )) return LineLooks.RAINPROOF;
		if (q.has( RarityLine.MOONLIT ) && DayNightCycle.phase() == DayNightCycle.Phase.NIGHT) return LineLooks.MOONLIT;
		if (q.has( RarityLine.FLEET )) return LineLooks.FLEET;
		return -1;
	}

	/** Whether an item shows its hint where it is: in the info window (`panel`), or equipped or
	 *  quickslotted by the hero - never in a bag, on a heap or as an icon. */
	public static boolean hinted( Item item, boolean panel ){
		if (item == null) return false;
		if (panel) return true;
		return Dungeon.hero != null && (item.isEquipped( Dungeon.hero )
				|| (Dungeon.quickslot != null && Dungeon.quickslot.contains( item )));
	}

	/** Its hint's quad (tests, previews): drawn in front of the item, never in quadsInOrder. */
	public Image hintQuad(){
		return hint;
	}

	//a plain item's hint: no backlight, one quad served last (it takes from no other slot)
	private void hintAlone( ItemSprite s, int line, float now ){
		back.visible = halo.visible = echo.visible = false;
		for (Quad q : motes) q.visible = false;
		keyed = false;
		priority = false;
		if (take( FxBudget.tier() == FxBudget.MINIMAL ? 0 : 1, now ) <= 0) return;
		seed = Math.round( ItemLooks.phase( s.item() ) * 1000f );
		hint.visible = hintMote( line, s.lookTime() * (panel ? PANEL_RATE : 1f), s.x, s.y, s.width(), s.height(), s.alpha() );
		if (hint.visible) shown++;
	}

	//the hint at t (its own clock, the window's quicker); whether it shows now
	private boolean hintMote( int line, float t, float sx, float sy, float w, float h, float alpha ){
		float every = HINT_EVERY[line], life = HINT_LIFE[line];
		if (!spawn( 0, 1, every, life, t )) return false;
		float age = spawnAge, p = age / life;
		int k = spawnK;
		hint.angle = 0;
		hint.scale.set( 1 );
		hint.flipHorizontal = false;
		switch (line){
			case LineLooks.WARM_GRIP:
				//a heat wisp rising off the top edge, wavering a pixel either way
				hint.frame( FxFrames.EMBER_1 );
				hint.hardlight( HINT_WARM );
				at( hint, sx + w * (0.35f + 0.3f * h( k, 40 )) + (float)Math.sin( 2 * Math.PI * 1.5f * age ), sy + 1f - 4f * p );
				hint.scale.set( 1f, 2f );
				hint.am = 0.35f * env( age, life, 0.15f, 0.3f ) * alpha;
				return true;
			case LineLooks.COOLING:
				//a cool mote falling off the bottom edge
				hint.frame( FxFrames.MOTE_1 );
				hint.hardlight( HINT_COOL );
				at( hint, sx + w * (0.35f + 0.3f * h( k, 41 )), sy + h - 1f + 4f * p );
				hint.am = 0.9f * env( age, life, 0.15f, 0.3f ) * alpha;
				return true;
			case LineLooks.RAINPROOF: {
				//a bead forming on the lower edge, rolling 3 px sideways, dropping off
				hint.frame( p < 0.3f ? FxFrames.MOTE_1 : FxFrames.DROP_2 );
				hint.hardlight( HINT_RAIN );
				float x0 = sx + w * (0.3f + 0.2f * h( k, 42 )), y0 = sy + h - 2f;
				float roll = Math.min( 1f, Math.max( 0f, (p - 0.3f) / 0.3f ) ), fall = Math.max( 0f, (p - 0.6f) / 0.4f );
				at( hint, x0 + 3f * roll, y0 + 5f * fall * fall );
				hint.am = 0.9f * env( age, life, 0.3f, 0.25f ) * alpha;
				return true;
			}
			case LineLooks.MOONLIT:
				//a moonlit glint sweeping three pixels of the top-right edge
				hint.frame( FxFrames.PLUS_3 );
				hint.hardlight( LineLooks.MOON );
				at( hint, sx + w - 5f + 3f * p, sy + 4f - 3f * p );
				hint.am = (1f - p * p) * alpha;
				return true;
			case LineLooks.FLEET:
				//a quick streak across the bottom edge, left to right
				hint.frame( FxFrames.MOTE_1 );
				hint.hardlight( HINT_FLEET );
				at( hint, sx + 2f + (w - 4f) * p, sy + h - 1f );
				hint.scale.set( 3f, 1f );
				hint.am = 0.8f * alpha;
				return true;
			default:
				return false;
		}
	}

	// ------------------------------------------------------------------ drawing

	/** The quads behind the item (the halo, the backlight, a ring under it, a ghost, the far half
	 *  of an orbit). From the sprite's draw, before it draws itself. */
	public void drawBack( ItemSprite s ){
		draw( halo, s );
		draw( back, s );
		for (Quad q : motes) if (q.behind) draw( q, s );
		if (echo.behind) draw( echo, s );
	}

	/** The quads over it. From the sprite's draw, after it draws itself. */
	public void drawFront( ItemSprite s ){
		for (Quad q : motes) if (!q.behind) draw( q, s );
		if (!echo.behind) draw( echo, s );
		draw( hint, s );
	}

	private static void draw( Quad q, ItemSprite s ){
		if (!q.visible || q.am <= 0.004f) return;
		q.camera = s.camera();
		q.draw();
	}

	/** The quads it draws, in order (previews). */
	public Image[] quadsInOrder(){
		Image[] out = new Image[3 + motes.length];
		out[0] = halo;
		out[1] = back;
		System.arraycopy( motes, 0, out, 2, motes.length );
		out[out.length - 1] = echo;
		return out;
	}

	// ------------------------------------------------------------------ the motifs

	//a number in [0, 1) for a spawn k and a salt, the same every time
	private float h( int k, int salt ){
		return Profile.hash( k * 31 + salt, seed * 0.001f + salt * 0.37f );
	}

	//the spawn a slot shows at t: k in spawnK, its age in spawnAge; false when it shows none
	private int spawnK;
	private float spawnAge;

	private boolean spawn( int j, int n, float spacing, float life, float t ){
		n = Math.max( 1, n );
		float s = Math.max( spacing, life / n );
		float cycle = n * s;
		float local = t - j * s;
		float c = (float)Math.floor( local / cycle );
		float age = local - c * cycle;
		if (age >= life) return false;
		spawnK = (int)c * n + j;
		spawnAge = age;
		return true;
	}

	//alpha up over `in` s, down over the last `out` s of `life`
	private static float env( float age, float life, float in, float out ){
		float a = in > 0 && age < in ? age / in : 1f;
		if (out > 0 && age > life - out) a = Math.min( a, Math.max( 0f, (life - age) / out ) );
		return a;
	}

	/** The colour a look's light motes show: its glow, or its element's light where the glow is too
	 *  dark to add. */
	static int lightColor( EnchantLook l ){
		int g = l.glow;
		return Math.max( (g >> 16) & 0xFF, Math.max( (g >> 8) & 0xFF, g & 0xFF ) ) < 0x50 ? l.el.light : g;
	}

	private static int haloColor( EnchantLook l ){
		int g = l.glow;
		return Math.max( (g >> 16) & 0xFF, Math.max( (g >> 8) & 0xFF, g & 0xFF ) ) < 0x30 ? 0x0C0614 : Ramp.lerp( g, 0x000000, 0.5f );
	}

	//one mote of a look at t; whether it shows
	private boolean mote( Quad q, EnchantLook l, int j, int n, float t, float spacing, ItemSprite s, float alpha ){
		float sx = s.x, sy = s.y, w = s.width(), h = s.height();
		float cx = sx + w / 2f, cy = sy + h / 2f;
		q.angle = 0;
		q.scale.set( 1 );
		q.flipHorizontal = false;
		if (ItemLooks.dark( l ) && j == 1) return rim( q, l, t, sx, sy, w, h, alpha );
		//the twists that take a slot of their own: Chilling's rime glint, Lucky's clover
		if (l.of == Chilling.class && j == 2 && n > 2){
			if (!spawn( 0, 1, 3f, 0.3f, t )) return false;
			glint( q, 0xFFFFFF, sx, sy, w, h, 0.3f, alpha );
			return true;
		}
		if (l.of == Lucky.class && j == n - 1 && n > 1){
			if (!spawn( 0, 1, 3f, 0.6f, t )) return false;
			q.frame( FxFrames.CLOVER_5 );
			q.hardlight( 0x66FF66 );
			q.am = 0.9f * env( spawnAge, 0.6f, 0.15f, 0.3f ) * alpha;
			at( q, sx + w * (0.3f + 0.4f * h( spawnK, 5 )), sy + 3f );
			return true;
		}
		//the rows that twist their motif: Paralysis, Slashing, Vicious, Midas, Ancient
		if (l.of == Paralysis.class) return j == 0 && stillRing( q, l, t, cx, sy, h, alpha );
		if (l.of == Slashing.class && j == 0) return streak( q, l, t, cx, cy, alpha );
		if (l.of == Vicious.class) return vicious( q, l, j, n, t, spacing, sx, sy, w, h, alpha );
		if (l.of == Midas.class){
			if (j == 0) return orbit( q, l, 0, 1, t, cx, cy, w, h, alpha );
			return goldFall( q, j - 1, n - 1, t, spacing, sx, sy, w, h, alpha );
		}
		if (l.of == Ancient.class){
			int runes = Math.min( n, key.runes );
			if (j < runes) return orbit( q, l, j, runes, t, cx, cy, w, h, alpha );
			j -= runes;
			n -= runes;
		}
		switch (l.motif){
			case ORBIT:
				return orbit( q, l, j, l.of == Lucky.class && n > 1 ? n - 1 : n, t, cx, cy, w, h, alpha );
			case GHOST:
				return ghost( q, l, t, s, alpha );
			case CYCLE:
				if (!spawn( 0, 1, Math.max( 1.6f, spacing ), 0.3f, t )) return false;
				glint( q, l.cycle != null ? Profile.cycleColor( l.cycle, t, ItemLooks.CYCLE_EACH ) : lightColor( l ), sx, sy, w, h, 0.3f, alpha );
				inward( q, l, spawnAge / 0.3f );
				return true;
			case CRACKLE:
				return crackle( q, l, j, t, spacing, sx, sy, w, h, alpha );
			default:
				break;
		}
		float life = l.motif.life;
		if (!spawn( j, n, spacing, life, t )) return false;
		int k = spawnK;
		float age = spawnAge, p = age / life;
		switch (l.motif){
			case RISE: {
				q.frame( l.fam == Family.EMBER ? (h( k, 1 ) < 0.5f ? FxFrames.EMBER_2 : FxFrames.EMBER_1)
						: l.fam == Family.HEART ? FxFrames.MOTE_2 : FxFrames.MOTE_1 );
				Ramp r = l.el.ember != null && l.emissive ? l.el.ember : l.el.ramp;
				q.hardlight( r.at( Math.min( 1f, 0.15f + p * 0.85f ) ) );
				float x = sx + 3f + h( k, 2 ) * (w - 6f) + (float)Math.sin( age * 4 * Math.PI + h( k, 3 ) * 6f );
				float y = sy + 2f + h( k, 4 ) * 3f - (6f + 3f * h( k, 5 )) * age;
				at( q, x, y );
				q.am = 0.9f * env( age, life, 0.1f, 0.4f ) * alpha;
				break;
			}
			case FALL: {
				boolean add = true;
				if (l.fam == Family.RIME){
					q.frame( FxFrames.SNOWFLAKE_5[(int)(age * 3f) % 2] );
					q.hardlight( l.el.ramp.at( 0.1f + 0.3f * p ) );
				} else if (l.fam == Family.FORCE){
					q.frame( FxFrames.PEBBLE_2 );
					q.hardlight( MotifFx.matterColor( l ) );
					add = false;
				} else if (l.fam == Family.VERDANT){
					q.frame( FxFrames.BARK[0] );
					q.hardlight( MotifFx.matterColor( l ) );
					add = false;
				} else {
					q.frame( FxFrames.PLUS_3 );
					q.hardlight( lightColor( l ) );
				}
				float x = sx + 3f + h( k, 2 ) * (w - 6f) + (h( k, 3 ) - 0.5f) * 2f * age;
				float y = sy + h * 0.5f + h( k, 4 ) * 3f + (3f + 2f * h( k, 5 )) * age;
				at( q, x, y );
				float twinkle = add ? 0.75f + 0.25f * (float)Math.sin( 2 * Math.PI * 3f * age + h( k, 6 ) * 6f ) : 1f;
				q.am = 0.8f * twinkle * env( age, life, 0.15f, 0.4f ) * alpha;
				break;
			}
			case DRIP: {
				q.hardlight( MotifFx.matterColor( l ) );
				float x = sx + w * (0.35f + 0.3f * h( k, 2 )), y0 = sy + h * 0.55f + h( k, 3 ) * 2f;
				if (age < 0.5f){
					q.frame( age < 0.25f ? FxFrames.MOTE_1 : FxFrames.DROP_2 );
					at( q, x, y0 );
				} else if (age < 0.9f){
					float f = (age - 0.5f) / 0.4f;
					q.frame( FxFrames.DROP_3 );
					at( q, x, y0 + 6f * f * f );
				} else {
					q.frame( FxFrames.POP_5 );
					at( q, x, y0 + 6f );
				}
				q.am = env( age, life, 0.15f, 0.1f ) * alpha;
				break;
			}
			case SIP: {
				q.hardlight( MotifFx.matterColor( l ) );
				q.frame( p < 0.75f ? FxFrames.DROP_2 : FxFrames.MOTE_1 );
				float x = sx + w * (0.35f + 0.3f * h( k, 2 ));
				at( q, x, sy + h * 0.85f - 7f * p );
				q.am = env( age, life, 0.2f, 0.3f ) * alpha;
				break;
			}
			case BUBBLE: {
				q.hardlight( MotifFx.matterColor( l ) );
				q.frame( p < 0.5f ? FxFrames.BUBBLE_2 : (p < 0.82f ? FxFrames.BUBBLE_3 : FxFrames.POP_5) );
				float x = sx + w * (0.3f + 0.4f * h( k, 2 )) + (float)Math.sin( age * 5f + h( k, 3 ) * 6f ) * 0.8f;
				at( q, x, sy + h * 0.6f - 3f * p );
				q.am = env( age, life, 0.15f, 0.12f ) * alpha;
				break;
			}
			case SEEP: {
				q.frame( FxFrames.INK_WISP[Math.min( 2, (int)(p * 3f) )] );
				q.hardlight( seep( l ) );
				float x = sx + w * (0.3f + 0.4f * h( k, 2 )) + (float)Math.sin( age * 3f + h( k, 3 ) * 6f );
				at( q, x, sy + h * 0.45f - 4f * p );
				q.am = 0.8f * env( age, life, 0.3f, 0.5f ) * alpha;
				break;
			}
			case GLINT: {
				int c = l.of == Hunting.class && (k & 1) == 1 ? 0xFFB040 : lightColor( l );
				glint( q, c, sx, sy, w, h, life, alpha );
				break;
			}
			case SPROUT: {
				q.hardlight( MotifFx.matterColor( l ) );
				if (l.of == Blooming.class && k % 3 == 2){
					//a petal drifting down past it
					q.frame( FxFrames.PETAL_B );
					q.hardlight( l.el.ember != null ? l.el.ember.stop( 1 ) : 0xFFC8DC );
					at( q, sx + w * (0.2f + 0.6f * h( k, 2 )) + (float)Math.sin( age * 4f ) * 1.5f, sy + 3f + 5f * p );
					q.am = env( age, life, 0.2f, 0.4f ) * alpha;
					break;
				}
				boolean seed = l.rune == FxFrames.SEED;
				q.frame( seed ? FxFrames.SEED : FxFrames.BLADE_3 );
				float grow = Math.min( 1f, age / 0.2f );
				float baseX = sx + w * (0.2f + 0.6f * h( k, 2 )), baseY = sy + h - 1f;
				if (seed){
					at( q, baseX, baseY - 1f );
					float beat = Profile.mod( age, 0.8f ) < 0.1f ? 1f : 0.6f;
					q.am = beat * env( age, life, 0.2f, 0.4f ) * alpha;
				} else {
					q.origin.set( 0.5f, 3f );
					q.scale.set( 1f, grow );
					q.x = Math.round( baseX - 0.5f );
					q.y = Math.round( baseY - 3f );
					float droop = p > 0.7f ? (p - 0.7f) / 0.3f * 40f : 0f;
					q.angle = 12f * (float)Math.sin( 2 * Math.PI * 0.8f * age + h( k, 3 ) * 6f ) + droop * (h( k, 4 ) < 0.5f ? -1 : 1);
					q.am = env( age, life, 0.05f, 0.4f ) * alpha;
				}
				break;
			}
			case THROB: {
				q.frame( p < 0.5f ? FxFrames.RING_5 : FxFrames.RING_9 );
				q.hardlight( lightColor( l ) );
				at( q, cx, sy + h - 1f );
				q.am = 0.7f * (1f - p) * alpha;
				break;
			}
			default:
				return false;
		}
		inward( q, l, p );
		return true;
	}

	//a curse's mote: from 3-4 px out, drawn in to where it would be
	private void inward( Quad q, EnchantLook l, float p ){
		if (!l.curse) return;
		float a = h( spawnK, 9 ) * 6.2832f, d = (3f + h( spawnK, 10 )) * (1f - p);
		q.x = Math.round( q.x + (float)Math.cos( a ) * d );
		q.y = Math.round( q.y + (float)Math.sin( a ) * d * 0.7f );
	}

	//a star sweeping 3 px up the blade's edge
	private void glint( Quad q, int color, float sx, float sy, float w, float h, float life, float alpha ){
		float p = spawnAge / life, along = 0.25f + 0.5f * h( spawnK, 7 );
		q.frame( p < 0.6f ? FxFrames.STAR4_5 : FxFrames.PLUS_3 );
		q.hardlight( color );
		at( q, sx + w * along + 3f * p, sy + h * (1f - along) - 3f * p );
		q.am = (1f - p * p) * alpha;
	}

	//two or three motes going round it on a flat ellipse, the far half dimmer and behind it
	private boolean orbit( Quad q, EnchantLook l, int j, int n, float t, float cx, float cy, float w, float h, float alpha ){
		boolean ancient = l.of == Ancient.class;
		float turns = l.of == BuzzSaw.class ? 4f : l.of == Surging.class ? 1.2f : l.fam == Family.WARD ? 0.5f
				: coins( l ) ? 0.6f : ancient ? 0.4f : 0.8f;
		float a = (float)(2 * Math.PI * (turns * t + j / (float)Math.max( 1, n )));
		float sin = (float)Math.sin( a );
		boolean far = sin < 0;
		if (coins( l )){
			q.frame( FxFrames.COIN_6[(int)(t * 8f + j) % FxFrames.COIN_6.length] );
			q.hardlight( MotifFx.matterColor( l ) );
		} else if (l.fam == Family.WARD){
			q.frame( FxFrames.RUNE_3[(j + seed) & 3] );
			q.hardlight( lightColor( l ) );
		} else if (ancient){
			q.frame( FxFrames.RUNE_3[(j + seed) & 3] );
			q.hardlight( Element.GOLD.light );
		} else if (l.fam == Family.CUT){
			q.frame( FxFrames.SPARK_2 );
			q.hardlight( l.el.glint );
		} else if (l.el == Element.PRISM){
			q.frame( FxFrames.STAR4_5 );
			q.hardlight( lightColor( l ) );
		} else {
			q.frame( FxFrames.MOTE_2 );
			q.hardlight( lightColor( l ) );
		}
		q.behind = far;
		at( q, cx + (w / 2f + 2f) * (float)Math.cos( a ), cy + h * 0.2f + 2f * sin );
		q.am = (far ? 0.5f : 1f) * 0.9f * alpha;
		//Ancient's inscription: its runes dim for a moment every INSCRIBE_EVERY s, one after another
		if (ancient && Profile.mod( t - j * 0.1f, INSCRIBE_EVERY ) < 0.1f) q.am *= 0.35f;
		return true;
	}

	static final float INSCRIBE_EVERY = 3f;

	//Paralysis: a ring under the item grows for a quarter second, stops dead for STILL_HOLD s, and
	//is gone; once every 2 s. Stillness, never a pulse
	static final float STILL_GROW = 0.25f, STILL_HOLD = 0.2f, STILL_GONE = 0.08f;

	private boolean stillRing( Quad q, EnchantLook l, float t, float cx, float sy, float h, float alpha ){
		float c = Profile.mod( t, 2f );
		if (c >= STILL_GROW + STILL_HOLD + STILL_GONE) return false;
		float grow = Math.min( 1f, c / STILL_GROW );
		q.frame( FxFrames.RING_5 );
		q.hardlight( lightColor( l ) );
		at( q, cx, sy + h - 1f );
		q.scale.set( 1f + 0.8f * grow );
		float a = c < STILL_GROW ? 0.55f + 0.25f * grow
				: c < STILL_GROW + STILL_HOLD ? 0.8f : 0.8f * (1f - (c - STILL_GROW - STILL_HOLD) / STILL_GONE);
		q.am = a * alpha;
		return true;
	}

	//Slashing: a thin streak flickers across the blade, two frames every 2 s
	private boolean streak( Quad q, EnchantLook l, float t, float cx, float cy, float alpha ){
		float k = Profile.mod( t, 2f );
		if (k >= 0.1f) return false;
		q.frame( FxFrames.STREAK_TAPER );
		q.hardlight( l.glow );
		at( q, cx, cy );
		q.angle = -45f;
		q.scale.set( 0.85f, 0.6f );
		q.am = (k < 0.05f ? 0.9f : 0.45f) * alpha;
		return true;
	}

	//Vicious: its beads split in two as they fall (a slot for each half), and a serration glint
	//sweeping three points of the edge, one at a time, in its last slot of three or more
	private boolean vicious( Quad q, EnchantLook l, int j, int n, float t, float spacing, float sx, float sy, float w, float h, float alpha ){
		if (n > 2 && j == n - 1){
			int win = (int)Math.floor( t / 0.8f );
			float k = t - win * 0.8f;
			if (k >= 0.12f) return false;
			float along = 0.3f + 0.2f * (((win % 3) + 3) % 3);
			q.frame( FxFrames.SPARK_2 );
			q.hardlight( l.el.glint );
			at( q, sx + w * along, sy + h * (1f - along) );
			q.angle = 45f;
			q.am = (1f - k / 0.12f) * alpha;
			return true;
		}
		int slots = n > 2 ? n - 1 : n;
		boolean twin = (j & 1) == 1;
		float life = l.motif.life;
		if (!spawn( j / 2, (slots + 1) / 2, spacing, life, t )) return false;
		float age = spawnAge;
		int k = spawnK;
		float x = sx + w * (0.35f + 0.3f * h( k, 2 )), y0 = sy + h * 0.55f + h( k, 3 ) * 2f;
		q.hardlight( MotifFx.matterColor( l ) );
		if (age < 0.5f){
			if (twin) return false;
			q.frame( age < 0.25f ? FxFrames.MOTE_1 : FxFrames.DROP_2 );
			at( q, x, y0 );
		} else if (age < 0.9f){
			//the bead parts: two halves falling apart
			float f = (age - 0.5f) / 0.4f;
			q.frame( twin ? FxFrames.MOTE_1 : FxFrames.DROP_2 );
			at( q, x + (twin ? 1.5f : -1.5f) * f, y0 + (twin ? 5f : 6f) * f * f );
		} else {
			if (twin) return false;
			q.frame( FxFrames.POP_5 );
			at( q, x - 1.5f, y0 + 6f );
		}
		q.am = env( age, life, 0.15f, 0.1f ) * alpha;
		return true;
	}

	//Midas: gold motes falling off its coin, twinkling
	private boolean goldFall( Quad q, int j, int n, float t, float spacing, float sx, float sy, float w, float h, float alpha ){
		float life = Motif.FALL.life;
		if (n <= 0 || !spawn( j, n, spacing, life, t )) return false;
		float age = spawnAge, p = age / life;
		q.frame( FxFrames.MOTE_1 );
		q.hardlight( Element.GOLD.light );
		at( q, sx + w * (0.3f + 0.4f * h( spawnK, 2 )), sy + h * 0.55f + 4f * p );
		float twinkle = 0.75f + 0.25f * (float)Math.sin( 2 * Math.PI * 3f * age + h( spawnK, 6 ) * 6f );
		q.am = 0.85f * twinkle * env( age, life, 0.15f, 0.4f ) * alpha;
		return true;
	}

	//a copy of the item's own frame in its rim colour, sliding: forward for reach, sideways for a
	//phase, jittering for a displacement, flipped for a mirror
	private boolean ghost( Quad q, EnchantLook l, float t, ItemSprite s, float alpha ){
		RectF f = s.frameRect();
		if (f == null) return false;
		if (f != ghostOf){
			ghostOf = f;
			q.uv.set( f.left, f.top, f.right, f.bottom );
			q.frame( q.uv );
		}
		float dx, dy;
		if (l.of == Projecting.class){
			float d = 1.5f + 1.5f * (float)Math.sin( 2 * Math.PI * 0.6f * t );
			dx = d;
			dy = -d;
		} else if (l.of == Displacing.class || l.of == Displacement.class){
			int k = (int)Math.floor( t / 0.3f );
			dx = Math.round( h( k, 11 ) * 2f - 1f );
			dy = Math.round( h( k, 12 ) * 2f - 1f );
		} else {
			dx = 2.5f * (float)Math.sin( 2 * Math.PI * 0.6f * t );
			dy = 0;
		}
		q.flipHorizontal = l.of == Mirroring.class;
		q.hardlight( l.curse ? Element.VOID.light : l.rim );
		q.x = Math.round( s.x + dx );
		q.y = Math.round( s.y + dy );
		q.am = 0.3f * alpha;
		return true;
	}

	//a jag of static between edge points: two sparks in a zigzag, a frame or two, never closer than
	//0.4 s; a pair's second spark continues its first
	private boolean crackle( Quad q, EnchantLook l, int j, float t, float spacing, float sx, float sy, float w, float h, float alpha ){
		int pair = j / 2;
		boolean second = (j & 1) == 1;
		float span = Math.max( 0.8f, spacing );
		float local = t + pair * span * 0.5f;
		int win = (int)Math.floor( local / span );
		float age = local - win * span - h( win, 20 + pair ) * 0.5f * span;
		if (age < 0 || age >= 0.05f) return false;
		//a point on the item's edge
		float e = h( win, 21 + pair ) * 4f;
		float px, py;
		if (e < 1f){ px = sx + 2f + e * (w - 4f); py = sy + 2f; }
		else if (e < 2f){ px = sx + w - 2f; py = sy + 2f + (e - 1f) * (h - 4f); }
		else if (e < 3f){ px = sx + 2f + (e - 2f) * (w - 4f); py = sy + h - 2f; }
		else { px = sx + 2f; py = sy + 2f + (e - 3f) * (h - 4f); }
		float a1 = h( win, 22 + pair ) * 180f;
		float ang = a1;
		if (second){
			double r = Math.toRadians( a1 );
			px += (float)Math.cos( r ) * 2.5f;
			py += (float)Math.sin( r ) * 2.5f;
			ang = a1 + (h( win, 23 + pair ) < 0.5f ? 50f : -50f);
		}
		q.frame( FxFrames.SPARK_3 );
		q.hardlight( l.el.glint );
		at( q, px, py );
		q.angle = ang + 90f;
		q.am = alpha;
		return true;
	}

	//a dark look's second mote: the edge of its darkness - Eldritch's red eye opening, Grim's pale
	//glint crawling the edge, Corrupting's violet drop, Jupiter's lilac spark, else a glint of its rim
	private boolean rim( Quad q, EnchantLook l, float t, float sx, float sy, float w, float h, float alpha ){
		if (l.of == Eldritch.class){
			float k = Profile.mod( t, 3.5f );
			if (k >= 0.5f) return false;
			q.frame( FxFrames.Light.EYE_7[k < 0.1f || k > 0.4f ? 0 : 1] );
			q.hardlight( 0xFF4060 );
			at( q, sx + w / 2f, sy + h * 0.35f );
			q.am = 0.85f * alpha;
		} else if (l.of == Grim.class){
			float k = Profile.mod( t, 2.4f );
			if (k >= 1.2f) return false;
			float p = k / 1.2f;
			q.frame( FxFrames.MOTE_1 );
			q.hardlight( EnchantLook.GHOST );
			at( q, sx + 3f + p * (w - 6f), sy + h - 3f - p * (h - 6f) );
			q.am = (float)Math.sin( Math.PI * p ) * alpha;
		} else if (l.of == Corrupting.class){
			float k = Profile.mod( t, 2f );
			if (k >= 0.8f) return false;
			float p = k / 0.8f;
			q.frame( FxFrames.DROP_2 );
			q.hardlight( l.rim );
			at( q, sx + w * 0.55f, sy + h * 0.5f + 5f * p * p );
			q.am = env( k, 0.8f, 0.2f, 0.2f ) * alpha;
		} else if (l.of == JupitersHorror.class){
			float k = Profile.mod( t, 2f );
			if (k >= 0.08f) return false;
			int win = (int)Math.floor( t / 2f );
			q.frame( FxFrames.SPARK_2 );
			q.hardlight( l.rim );
			at( q, sx + 3f + h( win, 30 ) * (w - 6f), sy + 3f + h( win, 31 ) * (h - 6f) );
			q.angle = h( win, 32 ) * 180f;
			q.am = alpha;
		} else {
			float k = Profile.mod( t, 1.4f );
			if (k >= 0.3f) return false;
			int win = (int)Math.floor( t / 1.4f );
			q.frame( FxFrames.PLUS_3 );
			q.hardlight( l.rim );
			float e = h( win, 33 );
			at( q, sx + 2f + e * (w - 4f), sy + 2f + (1f - e) * (h - 4f) );
			q.am = (float)Math.sin( Math.PI * k / 0.3f ) * 0.8f * alpha;
		}
		return true;
	}

	private void drawEcho( ItemSprite s, float t, float alpha ){
		float cx = s.x + s.width() / 2f, cy = s.y + s.height() / 2f;
		float a = (float)(2 * Math.PI * ECHO_TURNS * t + Math.PI);
		float sin = (float)Math.sin( a );
		echo.hardlight( EnchantLook.paled( key.echo.el.light ) );
		echo.behind = sin < 0;
		at( echo, cx + (s.width() / 2f + 2f) * (float)Math.cos( a ), cy + s.height() * 0.2f + 2f * sin );
		echo.am = ECHO_ALPHA * (sin < 0 ? 0.5f : 1f) * alpha;
		echo.visible = true;
		shown++;
	}

	private static boolean coins( EnchantLook l ){
		return l.fam == Family.FORTUNE && l.rune == FxFrames.COIN_6[0];
	}

	//an ink wisp's colour: its element's smoke drawn toward its rim, so it reads on a dark slot
	private static int seep( EnchantLook l ){
		return l.el.smoke != null ? Ramp.lerp( l.el.smoke.stop( 0 ), l.rim, 0.35f ) : MotifFx.matterColor( l );
	}

	//its middle at (x, y), whole pixels
	private static void at( Quad q, float x, float y ){
		q.origin.set( q.width / 2f, q.height / 2f );
		q.x = Math.round( x - q.width / 2f );
		q.y = Math.round( y - q.height / 2f );
	}

	private static void place( Quad q, float cx, float cy, float scale ){
		q.scale.set( scale );
		q.origin.set( q.width / 2f, q.height / 2f );
		q.x = Math.round( cx - q.width / 2f );
		q.y = Math.round( cy - q.height / 2f );
	}

	//the halves a quad may show (marks, never drawn)
	private static final int[] HALF_L = new int[0], HALF_R = new int[0];

	/** One quad: a frame of a sheet, added as light or drawn normally. */
	static final class Quad extends Image {

		final RectF uv = new RectF();
		boolean add, behind;
		private int[] shownFrame;
		private String sheet;

		Quad( String sheet, boolean add ){
			super( sheet );
			this.sheet = sheet;
			this.add = add;
			visible = false;
		}

		//its sheet, set once a key (a ghost's is the items')
		void sheet( String s, boolean add ){
			this.add = add;
			if (!s.equals( sheet )){
				sheet = s;
				texture( s );
				shownFrame = null;
			}
		}

		//the backlight's whole frame again after a split (nothing done when it shows it)
		void whole( int[] f ){
			frame( f );
		}

		//the left or right half of a frame { x, y, w, h } (Polarized's split); nothing done when
		//it is the half shown
		void half( int[] f, boolean left ){
			int[] key = left ? HALF_L : HALF_R;
			if (shownFrame == key && halfOf == f) return;
			float tw = texture.width, th = texture.height, hw = f[2] / 2f;
			float x0 = left ? f[0] : f[0] + hw;
			uv.set( x0 / tw, f[1] / th, (x0 + hw) / tw, (f[1] + f[3]) / th );
			super.frame( uv );
			shownFrame = key;
			halfOf = f;
		}

		private int[] halfOf;

		//a frame { x, y, w, h } of its sheet; nothing is made when it is the one shown
		void frame( int[] f ){
			if (f == shownFrame) return;
			shownFrame = f;
			float tw = texture.width, th = texture.height;
			uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
			frame( uv );
		}

		@Override
		public void frame( RectF frame ){
			if (frame != uv) shownFrame = null;
			super.frame( frame );
		}

		@Override
		public void draw(){
			if (add){
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			} else {
				super.draw();
			}
		}
	}
}
