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
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.DiscardedItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

/**
 * An enchanted heap on the floor (ItemSprite in HEAP mode): the item's own glow profile (ItemSprite),
 * and this - a GLOW_11 backlight in the light layer for an emissive family, so its embers glow at
 * night; its motif's motes (MotifFx, P3, thinned by FxBudget as world particles are) at most
 * PER_HEAP alive, from only the MOST_EMIT heaps nearest the hero in his view (ranked every
 * RANK_EVERY s); remembered heaps keep the glow and shed nothing; the legendary and exotic emissive
 * heaps among the LIT nearest a lasting GLOW_7 light at LIGHT_LEVEL (asking FxLight.room first);
 * a Venomous heap's drips leave at most WET_DOTS wet dots; a Midas heap's coin hops and flips
 * every HOP_EVERY s. A hidden heap (drawn as a faint hint) sheds nothing and plays no first sight.
 *
 * And its first sight: the first frame the heap lies in the hero's view, landed, with an enchanted
 * top item (a good enchantment, or a curse once known), its motif bursts (FIRST_BURST), a pool of
 * its light shows for half a second and a star winks; a legendary or exotic one stings at 0.2. Once
 * a discovery: not again for an item the hero has had in a slot or thrown, nor for a place (the
 * level, the cell, the look) already seen - a co-op guest's heaps are made anew with every change
 * and a level come back to lays its heaps again - remembering the last SEEN of each.
 *
 * It is the backlight quad itself, in the light layer, and steers itself from its heap's sprite
 * each frame (render thread): it lets everything go and retires once the sprite is gone (killed,
 * or taken off the scene: a guest's heaps cleared and made again), linked to another heap, or
 * shows no look. A chasm's falling item (DiscardedItemSprite) gets none. Pictures use FxRandom
 * only (MotifFx); no game Random.
 */
public class HeapLook extends Image {

	/** How many heaps shed motes; how many may hold a lasting light; motes a heap at once. */
	public static final int MOST_EMIT = 8, LIT = 2, PER_HEAP = 4, WET_DOTS = 4;
	/** How often the heaps in view are ranked by distance, s. */
	public static final float RANK_EVERY = 0.25f;
	/** The lasting light's level; the wet dots' alpha and how long one lasts. */
	public static final float LIGHT_LEVEL = 0.15f, WET_ALPHA = 0.3f, WET_HOLD = 2f, WET_FADE = 1f;
	/** First sight: the burst, the pool's life and level, the star's life, the sting's volume. */
	public static final int FIRST_BURST = 6;
	public static final float FIRST_POOL = 0.5f, FIRST_POOL_LEVEL = 0.5f, FIRST_STAR = 0.35f, FIRST_STING = 0.2f;
	/** First sights remembered: items and places, each. */
	public static final int SEEN = 128;
	/** A Midas heap's coin hops this often, s. */
	public static final float HOP_EVERY = 5f;

	/** Where a first sight's sting goes: SpatialSound in the game, a test's ear in a test. */
	interface Ear {
		void play( String sample, int cell, float volume, float pitch );
	}

	static Ear ear = SpatialSound::play;

	private ItemSprite owner;
	private Heap heap;
	//when each of its last PER_HEAP motes was shed (and so whether it still lives), the next due,
	//whose turn it is
	private final float[] shedAt = new float[PER_HEAP];
	private final float[] shedFor = new float[PER_HEAP];
	private float nextShed, nextHop;
	private int sheds;
	//its wet dots' times
	private final float[] wetAt = new float[WET_DOTS];
	//its place among the heaps in view as last ranked, and in which window
	private int rank = -1, rankedIn = -1;
	//its lasting light and the setups it had when handed over
	private FxLight light;
	private int lightSetups;
	private boolean sighted;
	private final RectF uv = new RectF();

	public HeapLook(){
		super( Assets.Effects.FX_LIGHT );
		int[] f = FxFrames.Light.GLOW_11;
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
	}

	/** A heap sprite's look, made (or recycled) in the light layer; null off the scene, and for an
	 *  item falling into a chasm. Render thread. */
	public static HeapLook attach( ItemSprite s ){
		if (s instanceof DiscardedItemSprite) return null;
		HeapLook h = GameScene.recycle( GameScene.Layer.LIGHTS, HeapLook.class );
		if (h == null) return null;
		h.setup( s );
		return h;
	}

	private void setup( ItemSprite s ){
		owner = s;
		heap = s.heap;
		for (int i = 0; i < PER_HEAP; i++) shedAt[i] = shedFor[i] = -1f;
		for (int i = 0; i < WET_DOTS; i++) wetAt[i] = -1e9f;
		nextShed = Game.timeTotal;
		//the coin's first hop somewhere in its first HOP_EVERY s, apart from its neighbours'
		nextHop = Game.timeTotal + 1f + (s.heap.pos * 7 % 40) / 10f;
		sheds = 0;
		rank = rankedIn = -1;
		light = null;
		sighted = false;
		resetColor();
		scale.set( ItemAura.BACK_SCALE );
		origin.set( width / 2f, height / 2f );
		am = 0;
		visible = false;
		revive();
	}

	/** Whether it is this sprite's look still. */
	public boolean owns( ItemSprite s ){
		return alive && owner == s && heap == s.heap;
	}

	/** Its place among the heaps in view as last ranked (0 the nearest), -1 when it is not among
	 *  them (tests). */
	public int rank(){
		return rankedIn == window - 1 || rankedIn == window ? rank : -1;
	}

	/** Its motes alive now. */
	public int livingMotes(){
		float now = Game.timeTotal;
		int n = 0;
		for (int i = 0; i < PER_HEAP; i++) if (shedAt[i] >= 0 && now - shedAt[i] < shedFor[i]) n++;
		return n;
	}

	/** Whether it holds its lasting light now. */
	public boolean lit(){
		return light != null && light.alive && light.setups() == lightSetups;
	}

	/** Whether its first sight has played. */
	public boolean sighted(){
		return sighted;
	}

	@Override
	public void update(){
		super.update();
		ItemSprite s = owner;
		if (s == null || !s.alive || !s.exists || s.parent == null || s.heap != heap || heap == null
				|| s.mode() != ItemSprite.Mode.HEAP){
			retire();
			return;
		}
		ItemLooks.Set looks = s.looks();
		EnchantLook a = looks.a, b = looks.b;
		if (a == null){
			retire();
			return;
		}
		float now = Game.timeTotal;
		float t = s.lookTime();
		EnchantLook l = b != null && ItemLooks.crossB( t ) > 0.5f ? b : a;
		float cx = s.x + s.width() / 2f, cy = s.y + s.height() / 2f;
		//a hidden heap (a faint hint of loot) gives nothing away
		boolean inView = s.visible && heap.seen && !heap.hidden && inFov( heap.pos );

		//the backlight in the light layer: an emissive family's glows through the night
		boolean emissive = a.emissive || (b != null && b.emissive);
		if (emissive && s.visible){
			hardlight( s.backColor() );
			am = s.backlight() * s.alpha() * Fx.nightMul( Fx.DAY_PERSISTENT );
			x = Math.round( cx - width / 2f );
			y = Math.round( cy - height / 2f );
			visible = am > 0.004f;
		} else {
			visible = false;
		}

		rank( this, inView, now );

		//its first sight: once, landed, in view, and only a discovery
		if (!sighted && inView && !s.falling()){
			sighted = true;
			if (discovered( heap.peek(), heap.pos, a )) firstSight( l, cx, cy );
		}

		//its motes: the nearest heaps in view only, PER_HEAP at most
		int r = rank();
		if (inView && r >= 0 && r < MOST_EMIT && now >= nextShed){
			float period = l == b ? looks.periodB : looks.periodA;
			nextShed = now + l.motif.periodAt( looks.level ) * Math.min( 1f, period );
			if (livingMotes() < PER_HEAP){
				EnchantLook which = b != null && (sheds & 1) == 1 ? b : a;
				sheds++;
				if (MotifFx.heap( which, cx, cy ) != null){
					int i = oldest();
					shedAt[i] = now;
					shedFor[i] = which.motif.life;
					if (which.of == Venomous.class) wet( which, cx, s.y + s.height(), now );
				}
			}
		}

		//a Midas heap's coin hops and flips
		EnchantLook midas = a.of == Midas.class ? a : (b != null && b.of == Midas.class ? b : null);
		if (midas != null && inView && r >= 0 && r < MOST_EMIT && now >= nextHop){
			nextHop = now + HOP_EVERY;
			if (livingMotes() < PER_HEAP && hop( midas, cx, s.y + s.height() - 3f )){
				int i = oldest();
				shedAt[i] = now;
				shedFor[i] = HOP_LIFE;
			}
		}

		//a legendary or exotic emissive heap among the nearest holds a lasting light
		boolean wantLight = inView && r >= 0 && r < LIT && emissive && legendary( heap.peek() );
		if (wantLight && !lit()){
			if (FxLight.room( 1, 1 )){
				EnchantLook e = a.emissive ? a : b;
				light = FxLight.glow( cx, cy, 7, e.light, LIGHT_LEVEL ).persistent();
				light.follow( s, 0, 0 );
				lightSetups = light.setups();
			}
		} else if (!wantLight && lit()){
			light.putOut( 0.3f );
			light = null;
		}
	}

	private int oldest(){
		int best = 0;
		for (int i = 1; i < PER_HEAP; i++) if (shedAt[i] < shedAt[best]) best = i;
		return best;
	}

	private static final float HOP_LIFE = 0.9f;

	//a coin off the top of the heap: up, a flip as it turns, down onto the heap with a small bounce
	private static boolean hop( EnchantLook l, float x, float ground ){
		FxEmitter e = Fx.air();
		if (e == null || !FxBudget.keep( FxBudget.P3, x, ground )) return false;
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( x, ground ).light( false ).priority( FxBudget.P3 );
		p.look( FxFrames.COIN_6, 14f ).paint( MotifFx.matterColor( l ) ).vel( FxRandom.Float( -4f, 4f ), -26f ).gravity( 90f )
				.ground( ground, FxParticle.BOUNCE, 0.3f ).life( HOP_LIFE ).alpha( 1f, 0f, 0.2f ).snap();
		return true;
	}

	//a wet dot under the heap, WET_DOTS at most at once
	private void wet( EnchantLook l, float x, float ground, float now ){
		int free = -1;
		for (int i = 0; i < WET_DOTS; i++){
			if (now - wetAt[i] >= WET_HOLD + WET_FADE){
				free = i;
				break;
			}
		}
		if (free < 0) return;
		FxDecal d = FxDecal.at( x + (sheds % 3 - 1) * 2f, ground - 1f, FxFrames.WET_5, MotifFx.matterColor( l ), WET_ALPHA,
				WET_HOLD, WET_FADE, false );
		if (d != null) wetAt[free] = now;
	}

	//the heap's first sight: a burst of its motif, a pool of its light, a star winking; a legendary
	//or exotic one's sting
	private void firstSight( EnchantLook l, float cx, float cy ){
		MotifFx.burst( l, cx, cy, FIRST_BURST );
		FxLight pool = FxLight.pool( cx, owner.y + owner.height() - 1f, 23, l.light, FIRST_POOL_LEVEL );
		pool.life( 0.05f, 0.15f, FIRST_POOL - 0.2f );
		EnchantFx.Flash.make( true, true, EnchantFx.STAR_WINK, 0f, cx, cy - 2f, l.curse ? Element.VOID.glint : 0xFFFFFF,
				0.85f, FIRST_STAR );
		if (legendary( heap.peek() ) && l.sound != null) ear.play( l.sound, heap.pos, FIRST_STING, l.pitch );
	}

	//legendary or exotic: the item's own rarity (its launcher's for a shot), or what the host sent
	//for a guest's stand-in
	private static boolean legendary( Item item ){
		Item i = item instanceof Weapon ? ItemLooks.launcherOf( (Weapon) item ) : item;
		Quality q = Quality.of( i );
		Rarity r = q != null ? q.rarity : ItemLooks.signedRarity( item );
		return r != null && r.ordinal() >= Rarity.LEGENDARY.ordinal();
	}

	// ------------------------------------------------------------------ discoveries

	//the first sights already played, render thread: the items' identities, and the places' keys
	//(the level, the cell, the first look) - each a ring of the last SEEN
	private static final int[] seenItems = new int[SEEN], seenPlaces = new int[SEEN];
	private static int itemsNext, itemsCount, placesNext, placesCount;

	/** An item the hero has had before him as itself (in a slot, the info window, a throw): its
	 *  landing in a heap is no discovery. Render thread. */
	public static void known( Item item ){
		if (item == null) return;
		int id = System.identityHashCode( item );
		if (has( seenItems, itemsCount, id )) return;
		seenItems[itemsNext] = id;
		itemsNext = (itemsNext + 1) % SEEN;
		itemsCount = Math.min( SEEN, itemsCount + 1 );
	}

	//whether a heap's top item showing its look at a cell is a discovery; if it is, it is one no more
	private static boolean discovered( Item item, int cell, EnchantLook l ){
		int place = place( cell, l );
		if (has( seenPlaces, placesCount, place ) || (item != null && has( seenItems, itemsCount, System.identityHashCode( item ) ))){
			return false;
		}
		seenPlaces[placesNext] = place;
		placesNext = (placesNext + 1) % SEEN;
		placesCount = Math.min( SEEN, placesCount + 1 );
		known( item );
		return true;
	}

	private static int place( int cell, EnchantLook l ){
		int k = Dungeon.depth;
		k = k * 31 + Dungeon.branch;
		k = k * 1_000_003 + cell;
		return k * 131 + (l != null ? l.code : -1);
	}

	private static boolean has( int[] ring, int count, int v ){
		for (int i = 0; i < count; i++) if (ring[i] == v) return true;
		return false;
	}

	/** Every first sight forgotten: a gallery's or debug scene's heaps flare again as they are laid
	 *  anew (and a test's). */
	public static void forgetSights(){
		itemsNext = itemsCount = placesNext = placesCount = 0;
	}

	private static boolean inFov( int cell ){
		Level l = Dungeon.level;
		return l != null && l.heroFOV != null && cell >= 0 && cell < l.heroFOV.length && l.heroFOV[cell];
	}

	private void retire(){
		if (lit()) light.putOut( 0.3f );
		light = null;
		owner = null;
		heap = null;
		kill();
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	// ------------------------------------------------------------------ the ranking

	//the heaps in view this window, and their distances from the hero; the window's number
	private static final HeapLook[] cands = new HeapLook[64];
	private static final int[] dists = new int[64];
	private static int candCount, window;
	private static float windowAt = -1e9f;

	//a heap in view joins this window's ranking; as a window ends, its heaps are ranked by distance
	private static void rank( HeapLook h, boolean inView, float now ){
		if (now - windowAt >= RANK_EVERY || now < windowAt){
			for (int i = 0; i < candCount; i++){
				int r = 0;
				for (int k = 0; k < candCount; k++){
					if (dists[k] < dists[i] || (dists[k] == dists[i] && k < i)) r++;
				}
				cands[i].rank = r;
				cands[i].rankedIn = window;
				cands[i] = null;
			}
			candCount = 0;
			window++;
			windowAt = now;
		}
		if (!inView) return;
		for (int i = 0; i < candCount; i++) if (cands[i] == h) return;
		int d = distance( h.heap.pos );
		if (candCount < cands.length){
			cands[candCount] = h;
			dists[candCount++] = d;
		} else {
			int far = 0;
			for (int i = 1; i < candCount; i++) if (dists[i] > dists[far]) far = i;
			if (dists[far] > d){
				cands[far] = h;
				dists[far] = d;
			}
		}
	}

	private static int distance( int cell ){
		Level l = Dungeon.level;
		if (l == null || Dungeon.hero == null || Dungeon.hero.pos < 0) return 0;
		return l.distance( Dungeon.hero.pos, cell );
	}

	/** A new scene (EnchantArea.sceneCreated): no ranking of the last one's heaps, nor any of them
	 *  held. What was discovered stays discovered. */
	public static void reset(){
		for (int i = 0; i < candCount; i++) cands[i] = null;
		candCount = 0;
		windowAt = -1e9f;
	}
}
