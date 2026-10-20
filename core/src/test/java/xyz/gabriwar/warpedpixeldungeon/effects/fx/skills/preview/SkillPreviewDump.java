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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcImage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.LightFlash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The skills' preview dump (tools/fxpreview/README.md), the enchantments' dump made for skills: with
 * the environment variable FXPREVIEW set (else it returns at once), every entry of the skill
 * registries is played on a headless stage - an 11 x 9 room of floor, the real Dungeon.hero and
 * his HeroSprite at (3, 4), stand-in rats where the entry puts them, stand-in layers, FxRandom
 * seeded - by day, at night (the game's tint held at 0.54) and with the whole room under shallow
 * water (the hero a warrior unless the entry says otherwise), and every live quad (its texture, frame, place, scale, angle, colour, blend, layer) is
 * written at the beats 0, 0.04, 0.1, 0.3, 0.6 and 1.2 s (and every frame at 30 fps for an entry
 * with a GIF) to build/fxpreview/<name>.json, in EnchantPreviewDump's schema, with the kit's live
 * counts beside each frame (particles, lights, decals, rings, flares, afterimages, spectacles,
 * lances). tools/fxpreview/paint.py paints them; tools/fxpreview/merge.py joins a "before" dump
 * and an "after" dump made in two checkouts.
 *
 * The registries are classes found by name, each with a static register(Registry): SkillPreviews
 * Kit, Dread, Martial, Holy and Hunt for the new code ("after"), and the same names ending in
 * Before for today's ("before"), then the second pass's RfxKit, RfxRunes, RfxLinks, RfxMeaning and
 * RfxStars, then the review fixes' RfxFixes (a later registry's entry of a name an earlier one has replaces that side); a missing
 * class is skipped. FXPREVIEW_SIDE=before or after dumps that side only (the before side is dumped
 * from a checkout of the old code, which has no after registries and so cannot call the new code);
 * FXPREVIEW_REGISTRIES=a,b loads those keys only, in that order (a 72b40617b dump of the restored
 * looks: Holy,RfxKit72b).
 *
 * What is drawn outside an Image's own draw is dumped as the game would draw it: a picture carried
 * over the night's tint (a Lit with a live carrier in the light layer: a beam, a bolt of light, a
 * legacy flare) in the LIGHTS layer, added; the legacy Flare's painted star, glow and core (its
 * ArcImages, which it builds on its first draw) as its draw would pose them this frame.
 *
 * A registry plays each moment as the gallery would (the skill's own visual calls, restaged):
 * never a real cast, and nothing of the game's state. CellEmitter needs the game's scene, which a
 * headless stage has none of: a registry bursts through Scene.get/center/bottom/floor instead, the
 * same emitters placed the same way on the stand-in layers.
 */
public class SkillPreviewDump {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	/** The stage's room: 11 x 9 cells of floor in a level this wide. */
	public static final int W = 16, ROOM_W = 11, ROOM_H = 9;

	public static int at( int x, int y ){
		return x + y * W;
	}

	/** One moment, played on a scene (render thread). */
	public interface Play {
		void play( Scene s );
	}

	/** Where an entry plays: the hero and his sprite, the dummies in the order the entry gave them,
	 *  whether it is under water or at night. */
	public static final class Scene {
		public EnchantFxTest.Rig rig;
		public Hero hero;
		public CharSprite h;
		public CharSprite[] d;
		public boolean water, night;
		final ArrayList<Object[]> later = new ArrayList<>();
		float time;

		public int cell( int x, int y ){
			return at( x, y );
		}

		/** CellEmitter.get( cell )'s emitter, on the stand-in effects layer. */
		public Emitter get( int cell ){
			return emitter( GameScene.Layer.EFFECTS, cell % W * 16f, cell / W * 16f, 16, 16 );
		}

		/** CellEmitter.center( cell )'s. */
		public Emitter center( int cell ){
			return emitter( GameScene.Layer.EFFECTS, cell % W * 16f + 8, cell / W * 16f + 8, 0, 0 );
		}

		/** CellEmitter.bottom( cell )'s. */
		public Emitter bottom( int cell ){
			return emitter( GameScene.Layer.EFFECTS, cell % W * 16f, cell / W * 16f + 16, 16, 0 );
		}

		/** CellEmitter.floor( cell )'s. */
		public Emitter floor( int cell ){
			return emitter( GameScene.Layer.FLOOR, cell % W * 16f, cell / W * 16f, 16, 16 );
		}

		private Emitter emitter( GameScene.Layer layer, float x, float y, float w, float h ){
			Emitter e = (Emitter) rig.layers[layer.ordinal()].recycle( Emitter.class );
			e.revive();
			e.pos( x, y, w, h );
			return e;
		}

		/** Runs r `seconds` into the moment (the dump's clock). */
		public void later( float seconds, Runnable r ){
			later.add( new Object[]{ time + seconds, r } );
		}

		//what is due by now
		void run(){
			for (int i = 0; i < later.size(); i++){
				Object[] l = later.get( i );
				if ((Float) l[0] <= time + 0.0001f){
					later.remove( i-- );
					((Runnable) l[1]).run();
				}
			}
		}
	}

	/** An entry's stage and its moment for one side. */
	public static final class Entry {
		final String name;
		final Play play;
		boolean gif = true;
		float seconds = LONG;
		int[] view = { 0, 1, 9, 7 };
		int heroX = 3, heroY = 4;
		HeroClass heroClass = HeroClass.WARRIOR;
		final ArrayList<int[]> rats = new ArrayList<>();

		Entry( String name, Play play ){
			this.name = name;
			this.play = play;
		}

		/** Every frame dumped (a GIF), or the beats only. */
		public Entry gif( boolean g ){
			gif = g;
			return this;
		}

		/** How long it plays, s (at most MAX). */
		public Entry seconds( float s ){
			seconds = Math.min( MAX, s );
			return this;
		}

		/** The view: its top-left cell and its size in cells. */
		public Entry view( int x, int y, int w, int h ){
			view = new int[]{ x, y, w, h };
			return this;
		}

		/** Where the hero stands (3, 4 unless said). */
		public Entry hero( int x, int y ){
			heroX = x;
			heroY = y;
			return this;
		}

		/** The hero's class (a warrior unless said): his sprite, his class's colour. */
		public Entry heroClass( HeroClass c ){
			heroClass = c;
			return this;
		}

		/** A dummy rat at a cell, facing the hero (Scene.d in the order given). */
		public Entry rat( int x, int y ){
			rats.add( new int[]{ x, y } );
			return this;
		}
	}

	/** What a registry adds its entries to: one side's (before or after). */
	public static final class Registry {
		final boolean before;

		Registry( boolean before ){
			this.before = before;
		}

		public Entry entry( String name, Play play ){
			Entry e = new Entry( name, play );
			Entry[] sides = ENTRIES.get( name );
			if (sides == null) ENTRIES.put( name, sides = new Entry[2] );
			sides[before ? 0 : 1] = e;
			return e;
		}
	}

	/** The registries: name -> { before, after } (either may be missing). */
	static final LinkedHashMap<String, Entry[]> ENTRIES = new LinkedHashMap<>();

	/** The registries' keys, each SkillPreviews<key> (after) and SkillPreviews<key>Before. */
	public static final String[] REGISTRIES = { "Kit", "Dread", "Martial", "Holy", "Hunt",
			"RfxKit", "RfxRunes", "RfxLinks", "RfxMeaning", "RfxStars", "RfxFixes", "Rfx2" };

	static void load( boolean before, boolean after ) throws Exception {
		ENTRIES.clear();
		String keys = System.getenv( "FXPREVIEW_REGISTRIES" );
		for (String key : keys != null && !keys.isEmpty() ? keys.split( "," ) : REGISTRIES){
			for (int side = 0; side < 2; side++){
				if (side == 0 && !before || side == 1 && !after) continue;
				String name = SkillPreviewDump.class.getPackage().getName() + ".SkillPreviews" + key + (side == 0 ? "Before" : "");
				Class<?> c;
				try {
					c = Class.forName( name );
				} catch (ClassNotFoundException e){
					continue;
				}
				c.getMethod( "register", Registry.class ).invoke( null, new Registry( side == 0 ) );
			}
		}
	}

	// ------------------------------------------------------------------ the dump

	static final float[] BEATS = { 0f, 0.04f, 0.1f, 0.3f, 0.6f, 1.2f };
	static final float FPS = 30f, LONG = 1.25f, MAX = 2.5f;

	@Test
	public void dump() throws Exception {
		if (System.getenv( "FXPREVIEW" ) == null) return;
		String side = System.getenv( "FXPREVIEW_SIDE" );
		boolean before = side == null || side.equals( "before" ), after = side == null || side.equals( "after" );
		load( before, after );
		String only = System.getenv( "FXPREVIEW_ONLY" );
		File dir = new File( "build/fxpreview" );
		dir.mkdirs();
		for (Map.Entry<String, Entry[]> e : ENTRIES.entrySet()){
			if (only != null && !only.isEmpty() && !Arrays.asList( only.split( "," ) ).contains( e.getKey() )) continue;
			Entry[] sides = e.getValue();
			Entry any = sides[1] != null ? sides[1] : sides[0];
			JSONObject out = new JSONObject();
			out.put( "name", e.getKey() );
			out.put( "fps", FPS );
			out.put( "beats", new JSONArray( BEATS ) );
			out.put( "gif", any.gif );
			out.put( "cell", DungeonTilemap.SIZE );
			JSONObject variants = new JSONObject();
			if (sides[0] != null) variants.put( "before", variant( sides[0] ) );
			if (sides[1] != null) variants.put( "after", variant( sides[1] ) );
			out.put( "variants", variants );
			try (Writer w = new FileWriter( new File( dir, e.getKey() + ".json" ) )){
				w.write( out.toString() );
			}
		}
	}

	//a moment's three rows: dry by day, dry at night, under water by day
	private JSONObject variant( Entry e ) throws Exception {
		JSONObject v = new JSONObject();
		v.put( "day", run( e, false, false ) );
		v.put( "night", run( e, false, true ) );
		v.put( "water", run( e, true, false ) );
		return v;
	}

	private JSONObject run( Entry e, boolean water, boolean night ) throws Exception {
		EnchantFxTest.Rig rig = new EnchantFxTest.Rig();
		rig.up();
		tint( night ? 0.54f : 0f );
		PixelFlare.reset();
		resetCounted();
		try {
			Dungeon.level = room( water );
			FxRandom.seedForTests( 11 );
			Scene s = new Scene();
			s.rig = rig;
			s.water = water;
			s.night = night;
			s.hero = new Hero();
			s.hero.HP = s.hero.HT = 20;
			s.hero.heroClass = e.heroClass;
			s.hero.pos = at( e.heroX, e.heroY );
			Dungeon.hero = s.hero;
			s.h = new HeroSprite( s.hero );
			rig.stage.add( s.h );
			s.h.place( s.hero.pos );
			s.h.visible = true;
			s.d = new CharSprite[e.rats.size()];
			for (int i = 0; i < s.d.length; i++){
				int[] p = e.rats.get( i );
				s.d[i] = rig.rat( at( p[0], p[1] ) );
				//facing the hero
				s.d[i].flipHorizontal = p[0] > e.heroX;
			}
			s.h.flipHorizontal = false;
			JSONObject run = new JSONObject();
			run.put( "view", new JSONArray( e.view ) );
			JSONArray wet = new JSONArray();
			for (int cy = e.view[1]; cy < e.view[1] + e.view[3]; cy++){
				for (int cx = e.view[0]; cx < e.view[0] + e.view[2]; cx++){
					if (cx < 0 || cy < 0 || cx >= W || cy >= W) continue;
					if (Dungeon.level.map[at( cx, cy )] == Terrain.WATER) wet.put( new JSONArray( new int[]{ cx, cy } ) );
				}
			}
			run.put( "water", wet );
			JSONArray frames = new JSONArray();
			if (e.play != null) e.play.play( s );
			int n = Math.round( e.seconds * FPS );
			int beat = 0;
			for (int f = 0; f <= n; f++){
				float t = f / FPS;
				s.time = t;
				s.run();
				boolean onBeat = beat < BEATS.length && t + 0.5f / FPS >= BEATS[beat];
				if (onBeat) beat++;
				if (e.gif || onBeat){
					JSONObject fr = new JSONObject();
					fr.put( "t", t );
					fr.put( "beat", onBeat );
					JSONArray q = quads( rig );
					fr.put( "quads", q );
					fr.put( "counts", counts( q.length() ) );
					frames.put( fr );
				}
				Game.elapsed = 1f / FPS;
				rig.frames( 1, true );
			}
			run.put( "frames", frames );
			return run;
		} finally {
			tint( 0f );
			Dungeon.hero = null;
			rig.down();
		}
	}

	//an 11 x 9 room of floor (or of shallow water) in a level W wide, all of it seen
	static Level room( boolean water ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public String waterTex(){ return Assets.Environment.WATER_CAVES; }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.WALL );
		for (int y = 0; y < ROOM_H; y++) for (int x = 0; x < ROOM_W; x++) l.map[at( x, y )] = water ? Terrain.WATER : Terrain.EMPTY;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	private static void tint( float a ) throws Exception {
		Field f = GameScene.class.getDeclaredField( "curTintA" );
		f.setAccessible( true );
		f.setFloat( null, a );
	}

	// ------------------------------------------------------------------ the counts

	//the counters the kit keeps outside the layers' reach, put back as a new scene would (the
	//kit's own are reset by the rig; these are the skills' and may not exist in an older checkout)
	private static final String[] COUNTED = {
			"xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX",
			"xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX",
			"xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX" };

	private static void resetCounted(){
		for (String c : COUNTED){
			try {
				Class.forName( c ).getMethod( "reset" ).invoke( null );
			} catch (ReflectiveOperationException ignored){
				//not in this checkout
			}
		}
	}

	private static JSONObject counts( int quads ) throws Exception {
		JSONObject o = new JSONObject();
		o.put( "quads", quads );
		o.put( "particles", FxBudget.live( FxBudget.PARTICLES ) );
		o.put( "lights", FxLight.live() );
		o.put( "decals", FxDecal.live() );
		o.put( "rings", FxRing.live() );
		o.put( "flares", PixelFlare.live() );
		o.put( "afterimages", Afterimage.live() );
		o.put( "spectacles", live( "xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX" ) );
		o.put( "lances", live( "xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX" ) );
		return o;
	}

	private static int live( String cls ){
		try {
			Method m = Class.forName( cls ).getMethod( "live" );
			return (Integer) m.invoke( null );
		} catch (ReflectiveOperationException ignored){
			return -1;
		}
	}

	// ------------------------------------------------------------------ the quads

	//the layers in the order the game's scene draws them; the stage (the sprites, and what is
	//added to the hero's layer) after the floor
	private static JSONArray quads( EnchantFxTest.Rig rig ) throws Exception {
		JSONArray out = new JSONArray();
		GameScene.Layer[] order = { GameScene.Layer.SUBSURFACE, GameScene.Layer.SHEEN, GameScene.Layer.SURFACE, GameScene.Layer.FLOOR };
		for (GameScene.Layer l : order) walk( rig.layers[l.ordinal()], l.name(), false, out );
		walk( rig.stage, "STAGE", false, out );
		walk( rig.layers[GameScene.Layer.EFFECTS.ordinal()], "EFFECTS", false, out );
		walk( rig.layers[GameScene.Layer.LIGHTS.ordinal()], "LIGHTS", false, out );
		return out;
	}

	private static void walk( Group g, String layer, boolean light, JSONArray out ) throws Exception {
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive || !m.visible) continue;
			//carried over the night's tint: drawn in the light layer
			String at = carried( m ) ? "LIGHTS" : layer;
			if (m instanceof Emitter){
				walk( (Group) m, at, light || lightMode( (Emitter) m ), out );
			} else if (m instanceof Group){
				walk( (Group) m, at, light, out );
			} else if (m instanceof Image){
				Image i = (Image) m;
				if (i.am <= 0.004f || i.texture == null) continue;
				out.put( quad( i, at, light || additive( i ) ) );
			} else if (m instanceof Flare){
				flare( (Flare) m, at, out );
			}
		}
	}

	//a Lit whose stand-in in the light layer is alive: its draw is the carrier's (Beam, MagicMissile,
	//Flare, and any later Lit keeping its carrier in a field named so)
	private static boolean carried( Gizmo m ) throws Exception {
		for (Class<?> c = m.getClass(); c != null && c != Gizmo.class; c = c.getSuperclass()){
			Field f;
			try {
				f = c.getDeclaredField( "carrier" );
			} catch (NoSuchFieldException e){
				continue;
			}
			if (!Gizmo.class.isAssignableFrom( f.getType() )) return false;
			f.setAccessible( true );
			Gizmo carrier = (Gizmo) f.get( m );
			return carrier != null && carrier.exists && carrier.parent != null;
		}
		return false;
	}

	//the legacy Flare: its painted parts (built on its first draw, so built here when it has not
	//been drawn yet) posed for this frame as its draw would, under-glow, star, inner star, core
	private static void flare( Flare f, String layer, JSONArray out ) throws Exception {
		Field rays = Flare.class.getDeclaredField( "rays" );
		rays.setAccessible( true );
		if (rays.get( f ) == null){
			Method build = Flare.class.getDeclaredMethod( "build" );
			build.setAccessible( true );
			build.invoke( f );
		}
		Method pose = Flare.class.getDeclaredMethod( "pose" );
		pose.setAccessible( true );
		pose.invoke( f );
		Field mode = Flare.class.getDeclaredField( "lightMode" );
		mode.setAccessible( true );
		boolean light = mode.getBoolean( f );
		for (String part : new String[]{ "under", "rays", "inner", "core" }){
			Field p = Flare.class.getDeclaredField( part );
			p.setAccessible( true );
			Image i = (Image) p.get( f );
			if (i == null || !i.visible || i.am <= 0.004f || i.texture == null) continue;
			out.put( quad( i, layer, light ) );
		}
	}

	private static boolean lightMode( Emitter e ) throws Exception {
		Field f = Emitter.class.getDeclaredField( "lightMode" );
		f.setAccessible( true );
		return f.getBoolean( e );
	}

	private static boolean additive( Image i ) throws Exception {
		if (i instanceof FxPic) return ((FxPic) i).additive;
		if (i instanceof ArcImage) return ((ArcImage) i).additive;
		if (i instanceof EnchantFx.Flash) return ((EnchantFx.Flash) i).additive();
		if (i instanceof FxRing){
			Field f = FxRing.class.getDeclaredField( "additive" );
			f.setAccessible( true );
			return f.getBoolean( i );
		}
		return i instanceof FxLight || i instanceof LightFlash || i instanceof Afterimage || i instanceof Beam
				|| i instanceof xyz.gabriwar.warpedpixeldungeon.effects.fx.ItemGhost;
	}

	private static JSONArray quad( Image i, String layer, boolean light ) throws Exception {
		SmartTexture t = i.texture;
		RectF f = i.frame();
		JSONArray q = new JSONArray();
		q.put( layer );
		q.put( key( t ) );
		q.put( Math.round( f.left * t.width ) );
		q.put( Math.round( f.top * t.height ) );
		q.put( Math.round( (f.right - f.left) * t.width ) );
		q.put( Math.round( (f.bottom - f.top) * t.height ) );
		for (float v : new float[]{ i.x, i.y, i.origin.x, i.origin.y, i.scale.x, i.scale.y, i.angle,
				i.rm, i.gm, i.bm, i.am, i.ra, i.ga, i.ba, i.aa }){
			q.put( (double) v );
		}
		q.put( i.flipHorizontal );
		q.put( i.flipVertical );
		q.put( light );
		return q;
	}

	//every texture the game knows by its asset's name, found by the one in use
	private static final IdentityHashMap<SmartTexture, String> KEYS = new IdentityHashMap<>();

	private static String key( SmartTexture t ) throws Exception {
		String k = KEYS.get( t );
		if (k != null) return k;
		for (Class<?> c : new Class<?>[]{ Assets.Effects.class, Assets.Sprites.class, Assets.Environment.class, Assets.Interfaces.class }){
			for (Field f : c.getFields()){
				if (!Modifier.isStatic( f.getModifiers() ) || f.getType() != String.class) continue;
				String asset = (String) f.get( null );
				if (TextureCache.contains( asset ) && TextureCache.get( asset ) == t){
					KEYS.put( t, asset );
					return asset;
				}
			}
		}
		//the skills' sheets made by path
		for (String asset : new String[]{ "effects/skill_fields.png", "effects/skill_spectacles.png", "effects/warrior_impact.png", "effects/sanctum.png" }){
			if (TextureCache.contains( asset ) && TextureCache.get( asset ) == t){
				KEYS.put( t, asset );
				return asset;
			}
		}
		return "?";
	}
}
