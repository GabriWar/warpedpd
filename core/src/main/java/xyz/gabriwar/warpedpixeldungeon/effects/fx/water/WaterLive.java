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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.particles.Emitter;

/**
 * The water's live surface (docs/fx/water-life.md, "A live surface"), one a scene, put in the
 * scene's sheen layer by the water's life (WaterLifeFx.sceneCreated).
 *
 * The palette cycle: the level's water texture swapped every frame of its liquid's cycle
 * (Element.Liquid.cycleFps) for the next of eight painted frames (tools/water_live_gen.py), so a
 * crest of light runs across the water - the same plane, the same scroll, no draw more. The
 * frames are sliced off their strip into eight 32x32 textures that repeat, made on the render
 * thread the first time they are wanted (and again after the texture cache was cleared, as a
 * region change does); the plane's texture field is set, never texture(), which would reset its
 * scroll. The liquid also sets how fast the plane scrolls (the lava crawls at 2 px/s, the city's
 * oil at 3.5). Frozen with every emitter (the hourglass).
 *
 * The sheens: two added pictures that tile at 64 px laid over the water (tools/water_live_gen.py)
 * - A, dashes of light along gentle crests, drifting (+3, +1) px/s; B, a sparse caustic net,
 * drifting (-2, +2.5) - in the liquid's glint by day and the moon's D8E4FF at night (leaning to the
 * aurora's green under one), at the liquid's alphas (the caves' dimmer, the city's oil in its own
 * two colours, none on the lava), dimmed by clouds over open water (to half), underground (0.8),
 * at night (0.6) and by a rain beating the water (to 0.3 in a downpour), breathing +-12% at 0.25 Hz half a cycle apart. Each is cut every frame to
 * the box of the water cells in view (a row scan, no allocation) and anchored to the world, so it
 * stays put as the view moves and over the overworld's slides (64 divides the window's 512); the
 * shore's tiles mask it where the box takes in dry ground. Hidden with no water in view, and as
 * the budget's governor asks (B off from REDUCED, A at MINIMAL). In a wind of 4 m/s or more under
 * an open sky a darker band 24 px wide (cat's paws: A at 60%) runs over the water with the wind,
 * at its gusting pace - A is drawn in three pieces while one crosses. Over the open sea they
 * drift half again as fast. On the lava, in their place, its glow (lava_glow.png) added in FF7A20,
 * swelling from 0.10 to 0.25 and back over 3.5 s, crawling with it. At most four quads (and the
 * rain's pocks), all of them clipped.
 *
 * Render thread only: built and run from the scene's update.
 */
public class WaterLive extends Group {

	private static final float SIZE = DungeonTilemap.SIZE;

	/** Debug only (the gallery's liquid carousel): this liquid shown instead of the level's. */
	public static volatile Element.Liquid debugLiquid;

	/** The liquid the water shows: the debug carousel's, else the level's (WaterFX.liquid()). */
	public static Element.Liquid liquid(){
		Element.Liquid d = debugLiquid;
		return d != null ? d : WaterFX.liquid();
	}

	/** A liquid's water texture (FROZEN's included), the one its cycle was painted from. */
	public static String baseOf( Element.Liquid l ){
		switch (l){
			case PRISON: return Assets.Environment.WATER_PRISON;
			case CAVE:   return Assets.Environment.WATER_CAVES;
			case CITY:   return Assets.Environment.WATER_CITY;
			case LAVA:   return Assets.Environment.WATER_HALLS;
			case FROZEN: return Assets.Environment.WATER_FROZEN;
			case TEMPLE: return Assets.Environment.WATER_TEMPLE;
			default:     return Assets.Environment.WATER_SEWERS;
		}
	}

	/** A liquid's cycle (a strip of eight frames), or null for one that keeps still (the ice). */
	public static String cycleOf( Element.Liquid l ){
		switch (l){
			case SEWER:  return Assets.Environment.WATER_SEWERS_CYCLE;
			case PRISON: return Assets.Environment.WATER_PRISON_CYCLE;
			case CAVE:   return Assets.Environment.WATER_CAVES_CYCLE;
			case CITY:   return Assets.Environment.WATER_CITY_CYCLE;
			case LAVA:   return Assets.Environment.WATER_HALLS_CYCLE;
			case TEMPLE: return Assets.Environment.WATER_TEMPLE_CYCLE;
			default:     return null;
		}
	}

	/**
	 * The `n` square frames of `size` px of a strip, each a texture of its own that repeats
	 * (keyed "<strip>#k" in the texture cache, so a scene finds the last one's), drawn NEAREST.
	 * Made from the strip's bitmap on the CPU: the GPU first sees them when they are drawn.
	 * Render thread.
	 */
	public static SmartTexture[] slices( String strip, int n, int size ){
		SmartTexture[] out = new SmartTexture[n];
		SmartTexture src = null;
		for (int k = 0; k < n; k++){
			String key = strip + "#" + k;
			if (TextureCache.contains( key )){
				out[k] = TextureCache.get( key );
				continue;
			}
			if (src == null) src = TextureCache.get( strip );
			SmartTexture t = TextureCache.create( key, size, size );
			Pixmap pm = t.bitmap;
			pm.setBlending( Pixmap.Blending.None );
			pm.drawPixmap( src.bitmap, 0, 0, k * size, 0, size, size );
			t.filter( Texture.NEAREST, Texture.NEAREST );
			t.wrap( Texture.REPEAT, Texture.REPEAT );
			out[k] = t;
		}
		return out;
	}

	//the liquid shown, its cycle's frames (null: the plane keeps its own texture), the frame on
	//show and the cycle's clock
	private Element.Liquid shown;
	private String shownTex;
	private SmartTexture[] cycle;
	private String cycleKey;
	private int frame = -1;
	private float cycleT;
	//the plane's own texture, as the scene made it
	private SmartTexture own;

	/** Test only: the water's plane while there is no game scene. */
	static SkinnedBlock planeForTests;

	static SkinnedBlock plane(){
		SkinnedBlock p = GameScene.waterPlane();
		return p != null ? p : planeForTests;
	}

	@Override
	public void update(){
		super.update();
		SkinnedBlock plane = plane();
		Level level = Dungeon.level;
		if (plane == null || level == null) return;
		if (own == null) own = plane.texture;
		Element.Liquid l = liquid();
		GameScene.waterScroll = l.scroll;
		cycle( plane, level, l );

		float dt = Emitter.freezeEmitters ? 0f : Game.elapsed;
		t += dt;
		WaterWalk.update( dt );
		WaterVoices.update( dt );
		if ((slowIn -= Game.elapsed) <= 0f){
			slowIn = 1f;
			look( level );
		}
		//the overworld's window mid-slide: its map is not the one drawn; keep where things are
		if (level.fogHeld()) return;
		water = view( level );
		sheens( l, dt );
		lavaGlow( l, dt );
		pocks( l, dt );
		ambience.update( level, l, dt, vx0, vy0, vx1, vy1 );
	}

	//glints, lap foam, whitecaps, the swamp's gas
	private final WaterAmbience ambience = new WaterAmbience();

	/** The lava's banks lit now (tests). */
	int banksLitForTests(){
		return ambience.banksLit();
	}

	/** The water cells in view the hero saw at the live surface's last look. */
	public static int waterInView(){
		return WaterAmbience.seen;
	}

	/** A new scene: nothing of the last one's water in view. */
	public static void sceneCreated(){
		debugLiquid = null;
		WaterAmbience.seen = 0;
		Marks.reset();
		WaterLava.reset();
		WaterFalls.reset();
		WaterRain.reset();
		WaterWalk.reset();
		WaterVoices.reset();
		current = null;
	}

	/** The water at a cell laps its banks now (a LAP voice heard from it). Render thread. */
	public static void lap( int cell ){
		WaterLive c = current;
		Level level = Dungeon.level;
		if (c == null || level == null || cell < 0 || cell >= level.length() || !level.water[cell]) return;
		Element.Liquid l = liquid();
		if (l != Element.Liquid.LAVA) c.ambience.lapAt( level, l, cell );
	}

	//the scene's live surface
	private static WaterLive current;

	public WaterLive(){
		super();
		current = this;
	}

	/** The scene's live surface while it stands, to keep pictures that go with the scene but are
	 *  drawn by something else (a wader's waterline); null off the scene. */
	static Group keeper(){
		WaterLive c = current;
		return c != null && c.parent != null ? c : null;
	}

	// ------------------------------------------------------------------ the lava's glow

	/** The lava's glow over the whole of it: added in GLOW, its alpha swelling from GLOW_MIN to
	 *  GLOW_MAX and back every GLOW_PERIOD s, riding the lava's crawl. */
	static final int GLOW = 0xFF7A20;
	static final float GLOW_MIN = 0.10f, GLOW_MAX = 0.25f, GLOW_PERIOD = 3.5f;

	private Plane glow;
	private float glowY;

	/** The lava glow's alpha `t` s in: 0.10 to 0.25 and back over 3.5 s. */
	static float glowAlpha( float t ){
		float k = 0.5f - 0.5f * (float)Math.cos( 2 * Math.PI * t / GLOW_PERIOD );
		return GLOW_MIN + (GLOW_MAX - GLOW_MIN) * k;
	}

	//in place of the sheens on the lava (off with sheen A as the governor asks)
	private void lavaGlow( Element.Liquid l, float dt ){
		if (!water || l != Element.Liquid.LAVA || !FxBudget.sheenA()){
			hide( glow );
			return;
		}
		if (glow == null) glow = (Plane) add( new Plane( Assets.Effects.LAVA_GLOW, true ) );
		glowY = wrap( glowY + l.scroll * dt );
		glow.cut( boxX, boxY, boxW, boxH, 0f, glowY, GLOW, Math.min( SHEEN_CAP, glowAlpha( t ) ) );
	}

	// ------------------------------------------------------------------ the rain's pocks

	/** The pocks' frames swap this often, s, each at a new offset so their loop never shows; at
	 *  night they are lifted by POCK_NIGHT_LIFT (under the night's tint), never past POCK_CAP. */
	static final float POCK_SWAP = 0.08f, POCK_NIGHT_LIFT = 0.4f, POCK_CAP = 0.6f;

	private Plane pocks;
	private SmartTexture[] pockFrames;
	private int pockFrame;
	private float pockIn, pockX, pockY;

	//past 0.4 of rain or sleet, the rain's landing all over the water at once
	private void pocks( Element.Liquid l, float dt ){
		float a = Math.min( POCK_CAP, WaterGeom.pockAlpha( WaterAmbience.rain() )
				* WaterGeom.nightLift( GameScene.nightTintAlpha(), POCK_NIGHT_LIFT ) );
		if (!water || a <= 0f || l == Element.Liquid.LAVA || !FxBudget.sheenA()){
			if (pocks != null) pocks.visible = false;
			return;
		}
		if (pockFrames == null || !TextureCache.contains( Assets.Effects.WATER_POCKS + "#3" )){
			pockFrames = slices( Assets.Effects.WATER_POCKS, 4, WaterGeom.SHEEN_TILE );
			if (pocks != null) pocks.texture = pockFrames[pockFrame];
		}
		if (pocks == null) pocks = (Plane) add( new Plane( pockFrames[0], false ) );
		if ((pockIn -= dt) <= 0f){
			pockIn = POCK_SWAP;
			pockFrame = (pockFrame + 1) % pockFrames.length;
			pocks.texture = pockFrames[pockFrame];
			pockX = FxRandom.Int( WaterGeom.SHEEN_TILE );
			pockY = FxRandom.Int( WaterGeom.SHEEN_TILE );
		}
		pocks.cut( boxX, boxY, boxW, boxH, pockX, pockY, l.foam, a );
	}

	//the plane's texture: the cycle's frame now, or the liquid's own still texture
	private void cycle( SkinnedBlock plane, Level level, Element.Liquid l ){
		String tex = level.waterTex();
		boolean debug = debugLiquid != null;
		if (l != shown || tex != shownTex || (cycle != null && !TextureCache.contains( cycleKey ))){
			shown = l;
			shownTex = tex;
			frame = -1;
			String strip = cycleOf( l );
			//a level's water cycles only when it shows its liquid's own texture
			if (strip != null && (debug || baseOf( l ).equals( tex ))){
				cycle = slices( strip, WaterGeom.CYCLE_FRAMES, WaterGeom.CYCLE_TILE );
				cycleKey = strip + "#" + (WaterGeom.CYCLE_FRAMES - 1);
			} else {
				cycle = null;
				plane.texture = debug ? repeating( baseOf( l ) ) : own;
			}
		}
		if (cycle == null) return;
		if (!Emitter.freezeEmitters) cycleT += Game.elapsed;
		int f = WaterGeom.cycleFrame( cycleT, l.cycleFps );
		if (f != frame || plane.texture != cycle[f]){
			frame = f;
			plane.texture = cycle[f];
		}
	}

	// ------------------------------------------------------------------ the water in view

	//the water cells in view (cells, inclusive), and as world px; whether there are any
	private final int[] box = new int[4];
	private boolean water;
	private float boxX, boxY, boxW, boxH;
	//the cells in view, and one round them
	private int vx0, vy0, vx1, vy1;

	/** The water cells within the camera's view and a cell round it, cut to their box; false
	 *  for none. The whole level off the game's scene (a test's). */
	private boolean view( Level level ){
		Camera c = Camera.main;
		vx0 = 0;
		vy0 = 0;
		vx1 = level.width() - 1;
		vy1 = level.height() - 1;
		if (c != null && GameScene.waterPlane() != null){
			vx0 = (int)Math.floor( c.scroll.x / SIZE ) - 1;
			vy0 = (int)Math.floor( c.scroll.y / SIZE ) - 1;
			vx1 = (int)Math.floor( (c.scroll.x + c.width) / SIZE ) + 1;
			vy1 = (int)Math.floor( (c.scroll.y + c.height) / SIZE ) + 1;
		}
		if (!WaterGeom.waterBox( level.water, level.width(), level.height(), vx0, vy0, vx1, vy1, box )) return false;
		boxX = box[0] * SIZE;
		boxY = box[1] * SIZE;
		boxW = (box[2] - box[0] + 1) * SIZE;
		boxH = (box[3] - box[1] + 1) * SIZE;
		return true;
	}

	//once a second: the open sea, the aurora
	private float slowIn;
	private boolean ocean, aurora;

	private void look( Level level ){
		ocean = level instanceof OverworldLevel && Dungeon.hero != null && Dungeon.hero.pos >= 0
				&& ((OverworldLevel) level).biomeAtCell( Dungeon.hero.pos ) == WorldModel.Biome.OCEAN;
		aurora = ClimateManager.ambientType() == ClimateManager.WeatherOverlayAmbient.AURORA;
	}

	// ------------------------------------------------------------------ the sheens

	/** The sheens' drift, px/s: A (+3, +1), B (-2, +2.5); over the open sea half again as fast. */
	static final float A_VX = 3f, A_VY = 1f, B_VX = -2f, B_VY = 2.5f, OCEAN_PACE = 1.5f;
	/** The moon's tint the sheens take at night, and the aurora's they lean toward under it. */
	static final int MOON = 0xD8E4FF, AURORA = 0x40E888;
	static final float AURORA_SHARE = 0.15f;
	/** No sheen past this alpha: an added layer over the whole water. */
	static final float SHEEN_CAP = 0.30f;
	/** The cat's paws: under an open sky in a wind of PAWS_WIND m/s or more, a band PAWS_W px wide
	 *  where sheen A shows at PAWS_DIM runs over the water with the wind, at its gusting pace. */
	static final float PAWS_WIND = 4f, PAWS_DIM = 0.6f;
	static final int PAWS_W = 24;

	//sheen A in up to three pieces (before, in and after a cat's paw), and sheen B
	private Plane a0, a1, a2, b;
	private float driftAX, driftAY, driftBX, driftBY, t;
	//the cat's paw's left edge, world px (NaN: none), and the wait for the next
	private float pawX = Float.NaN, pawWait;

	private void sheens( Element.Liquid l, float dt ){
		boolean showA = water && l.sheenA > 0 && FxBudget.sheenA();
		boolean showB = water && l.sheenB > 0 && FxBudget.sheenB();
		if (!showA && !showB){
			hide( a0 );
			hide( a1 );
			hide( a2 );
			hide( b );
			pawX = Float.NaN;
			return;
		}
		if (b == null){
			b = (Plane) add( new Plane( Assets.Effects.WATER_SHEEN_B, true ) );
			a0 = (Plane) add( new Plane( Assets.Effects.WATER_SHEEN_A, true ) );
			a1 = (Plane) add( new Plane( Assets.Effects.WATER_SHEEN_A, true ) );
			a2 = (Plane) add( new Plane( Assets.Effects.WATER_SHEEN_A, true ) );
		}
		float pace = (ocean ? OCEAN_PACE : 1f) * dt;
		driftAX = wrap( driftAX + A_VX * pace );
		driftAY = wrap( driftAY + A_VY * pace );
		driftBX = wrap( driftBX + B_VX * pace );
		driftBY = wrap( driftBY + B_VY * pace );

		float tint = GameScene.nightTintAlpha();
		float n = WaterGeom.night( tint );
		boolean open = Fx.openSky();
		float cover = ClimateManager.cloudCover();
		//a rain beating the water breaks its calm shine
		float rain = WaterGeom.rainDim( WaterAmbience.rain() );

		if (showB){
			int c = sheenColor( l.sheenTintB != Element.NONE ? l.sheenTintB : l.glint, n );
			float a = Math.min( SHEEN_CAP, sheenB( l ) * rain * WaterGeom.sheenShare( open, cover, tint, t, (float)Math.PI ) );
			b.cut( boxX, boxY, boxW, boxH, driftBX, driftBY, c, a );
		} else {
			hide( b );
		}

		if (!showA){
			hide( a0 );
			hide( a1 );
			hide( a2 );
			return;
		}
		int c = sheenColor( l.sheenTintA != Element.NONE ? l.sheenTintA : l.glint, n );
		float a = Math.min( SHEEN_CAP, sheenA( l ) * rain * WaterGeom.sheenShare( open, cover, tint, t, 0f ) );
		float right = boxX + boxW;
		if (paws( open, dt ) && pawX < right && pawX + PAWS_W > boxX){
			float l0 = Math.max( boxX, pawX ), r0 = Math.min( right, pawX + PAWS_W );
			cutOrHide( a0, boxX, l0 - boxX, c, a );
			cutOrHide( a1, l0, r0 - l0, c, a * PAWS_DIM );
			cutOrHide( a2, r0, right - r0, c, a );
		} else {
			a0.cut( boxX, boxY, boxW, boxH, driftAX, driftAY, c, a );
			hide( a1 );
			hide( a2 );
		}
	}

	private void cutOrHide( Plane p, float x, float w, int color, float alpha ){
		if (w < 1f) hide( p );
		else p.cut( x, boxY, w, boxH, driftAX, driftAY, color, alpha );
	}

	//the cat's paw under way: true while one crosses the water
	private boolean paws( boolean open, float dt ){
		if (!open || ClimateManager.localWindSpeed() < PAWS_WIND){
			pawX = Float.NaN;
			return false;
		}
		float wind = Fx.windX();
		float dir = wind >= 0 ? 1f : -1f, speed = Math.max( PAWS_WIND, Math.abs( wind ) );
		if (Float.isNaN( pawX )){
			if ((pawWait -= dt) > 0f) return false;
			pawX = dir > 0 ? boxX - PAWS_W : boxX + boxW;
		}
		pawX += dir * speed * dt;
		if (dir > 0 ? pawX > boxX + boxW : pawX + PAWS_W < boxX){
			pawX = Float.NaN;
			pawWait = FxRandom.Float( 1f, 4f );
			return false;
		}
		return true;
	}

	/** The caves' sheens, brighter than their liquid's own 0.12 and 0.08 (Element.Liquid.CAVE, the
	 *  kit's: a kit request to take these): on their dark water under a roof the dimmer ones barely
	 *  showed. */
	static final float CAVE_SHEEN_A = 0.18f, CAVE_SHEEN_B = 0.12f;

	static float sheenA( Element.Liquid l ){
		return l == Element.Liquid.CAVE ? CAVE_SHEEN_A : l.sheenA;
	}

	static float sheenB( Element.Liquid l ){
		return l == Element.Liquid.CAVE ? CAVE_SHEEN_B : l.sheenB;
	}

	//the sheen's colour: its own by day, the moon's at night, leaning to the aurora's under one
	private int sheenColor( int day, float night ){
		int c = Ramp.lerp( day, MOON, night );
		return aurora && night > 0f ? Ramp.lerp( c, AURORA, AURORA_SHARE * night ) : c;
	}

	private static float wrap( float v ){
		v %= WaterGeom.SHEEN_TILE;
		return v < 0 ? v + WaterGeom.SHEEN_TILE : v;
	}

	private static void hide( Plane p ){
		if (p != null) p.visible = false;
	}

	/**
	 * A picture that tiles over the water (a sheen, the pocks, the lava's glow): cut to a box each
	 * frame and anchored to the world, so the same world point always shows the same texel, its
	 * drift aside. Added (light) or laid on (matter) in one colour at one alpha.
	 */
	static final class Plane extends SkinnedBlock {

		final boolean additive;

		Plane( Object texture, boolean additive ){
			super( 1, 1, texture );
			this.additive = additive;
			visible = false;
		}

		/** Over the box at (x, y), w x h world px, the texture drifted (dx, dy) px, in `color` at `alpha`. */
		void cut( float x, float y, float w, float h, float dx, float dy, int color, float alpha ){
			if (alpha <= 0.004f){
				visible = false;
				return;
			}
			this.x = x;
			this.y = y;
			if (w != width || h != height) size( w, h );
			offsetTo( WaterGeom.anchor( x, -dx, texture.width ), WaterGeom.anchor( y, -dy, texture.height ) );
			hardlight( color );
			am = alpha;
			visible = true;
		}

		@Override
		public void draw(){
			if (additive){
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			} else {
				super.draw();
			}
		}
	}

	//a still water texture that repeats, for the debug carousel
	private static SmartTexture repeating( String asset ){
		SmartTexture t = TextureCache.get( asset );
		t.wrap( Texture.REPEAT, Texture.REPEAT );
		return t;
	}
}
