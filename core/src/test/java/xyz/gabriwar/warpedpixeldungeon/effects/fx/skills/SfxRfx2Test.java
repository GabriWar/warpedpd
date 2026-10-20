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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HealingPrayer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Transcendence;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.Sanctum;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Round 2 of the skills' looks (the owner's review of 10981):
 *
 *  - Transcendence looks as at f0783eacd again: its pass a plain gold stroke with his afterimages,
 *    its split lance three gold strokes, never a SkillRay;
 *  - Charged Shot's release is f0783eacd's thin stroke of nature white again, and SkillRay keeps
 *    only the looks something still uses (HOLY, HEAL);
 *  - Sanctuary's ground is one still seal of holy light (Sanctum), never a glyph a cell: nothing of
 *    it ever changes frame, turns, scales or moves; its light only breathes and a slow reading light
 *    goes round its runes, so no alpha jumps between two frames; it opens, inscribes its runes and
 *    fades smoothly; its cast lies on no glyph and its rising runes keep their glyphs; none of it
 *    draws the game's Random or touches the GPU while made; the mend's crosses and greens stay
 *    (SfxRfxMeaningTest).
 */
public class SfxRfx2Test {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private DreadShadowProcs t;

	@Before
	public void setUp() throws Exception {
		t = new DreadShadowProcs();
		t.up();
		Game.elapsed = 1 / 60f;
		RuneFX.reset();
		SkillRay.reset();
		SkillFieldFX.clearCap();
	}

	@After
	public void tearDown() throws Exception {
		t.down();
	}

	private Group layer( GameScene.Layer l ){
		return t.rig.layers[l.ordinal()];
	}

	// ------------------------------------------------------------------ Transcendence, the rays

	@Test
	public void chargedShotsReleaseIsItsThinStrokeAgain() throws Exception {
		assertEquals( 0xDDEFD0, ChargedShot.STROKE );
		ChargedShot.release( Dungeon.hero.sprite, t.at( 12, 4 ) );
		int strokes = 0;
		for (int f = 0; f < 30; f++){
			t.rig.frames( 1, true );
			assertEquals( "no ray", 0, count( layer( GameScene.Layer.LIGHTS ), SkillRay.class.getName() + "$Ray" ) );
			strokes = Math.max( strokes, count( layer( GameScene.Layer.LIGHTS ), StreakFX.class.getName() ) );
		}
		assertEquals( "its stroke", 1, strokes );
		String src = code( SfxDreadShadowTest.source( "actors/hero/skills/ChargedShot.java" ) );
		assertTrue( src.contains( "StreakFX.show( hand, DungeonTilemap.raisedTileCenterToWorld( far ), STROKE, 0.5f, 0.3f );" ) );
		assertFalse( src.contains( "SkillRay" ) );
	}

	@Test
	public void transcendenceDrawsWhatF0783Drew() throws Exception {
		//f0783eacd's two calls, and nothing of the second pass's
		String src = code( SfxDreadShadowTest.source( "actors/hero/skills/Transcendence.java" ) );
		assertTrue( src.contains( "StreakFX.dash( from, land, 0xF1D99C, 0.45f, 0.25f, true );" ) );
		assertTrue( src.contains( "StreakFX.show( land, end, 0xF1D99C, 0.35f, 0.2f );" ) );
		assertFalse( src.contains( "SkillRay" ) );
		assertFalse( src.contains( "MOTES" ) );
		//its split lance played (+3, three ways): three strokes, no ray
		Transcendence tr = DreadShadowProcs.skill( new Transcendence() );
		t.rat( t.at( 4, 4 ) );
		t.rat( t.at( 5, 5 ) );
		t.rat( t.at( 5, 3 ) );
		Method split = Transcendence.class.getDeclaredMethod( "splitLance", Hero.class, int.class, int.class, int.class, int.class );
		split.setAccessible( true );
		split.invoke( tr, t.hero, t.hero.pos, 1, 5, 10 );
		int strokes = 0;
		for (int f = 0; f < 30; f++){
			t.rig.frames( 1, true );
			assertEquals( "no ray", 0, count( layer( GameScene.Layer.LIGHTS ), SkillRay.class.getName() + "$Ray" ) );
			strokes = Math.max( strokes, count( layer( GameScene.Layer.LIGHTS ), StreakFX.class.getName() ) );
		}
		assertEquals( "three strokes", 3, strokes );
	}

	@Test
	public void theNatureAndSteelRaysAreGone(){
		assertEquals( 2, SkillRay.LOOKS );
		assertEquals( 0, SkillRay.HOLY );
		assertEquals( 1, SkillRay.HEAL );
		for (String gone : new String[]{ "NATURE", "STEEL", "STREAM_NATURE", "STREAM_STEEL" }){
			try {
				SkillRay.class.getField( gone );
				throw new AssertionError( "SkillRay." + gone + " is still there" );
			} catch (NoSuchFieldException expected){
				//gone
			}
		}
	}

	// ------------------------------------------------------------------ Sanctuary's ground

	//a sanctuary of reach r round the hero, fresh, its picture on the floor
	private SkillFieldFX sanctuary( int reach ){
		List<Integer> cells = new ArrayList<>();
		int hx = t.hero.pos % DreadShadowProcs.W, hy = t.hero.pos / DreadShadowProcs.W;
		for (int dy = -reach; dy <= reach; dy++) for (int dx = -reach; dx <= reach; dx++) cells.add( t.at( hx + dx, hy + dy ) );
		SkillField.place( t.hero, SkillField.SANCTUARY, 1, 5, cells );
		t.rig.frames( 1, true );
		SkillFieldFX fx = field( layer( GameScene.Layer.FLOOR ) );
		assertNotNull( "its picture", fx );
		return fx;
	}

	@Test
	public void sanctuarysGroundIsOneSealNeverAGlyphACell(){
		Object glyphs = TextureCache.get( "effects/skill_fields.png" ), seal = TextureCache.get( Sanctum.TEXTURE );
		for (int reach = 1; reach <= 2; reach++){
			SkillFieldFX fx = sanctuary( reach );
			int cuts = 0, runes = 0;
			for (Image p : pics( fx )){
				assertFalse( "no glyph a cell", p.texture == glyphs );
				if (p.texture == seal){
					cuts++;
					assertEquals( 16, p.width, 0.01f );
					assertEquals( 16, p.height, 0.01f );
				} else {
					runes++;
					assertEquals( "a RUNE_5", 5, p.width, 0.01f );
				}
			}
			int side = 2 * reach + 1;
			assertEquals( "a cut a cell", side * side, cuts );
			assertEquals( "its runes", Sanctum.RUNES[reach - 1], runes );
			for (SkillField f : t.hero.buffs( SkillField.class )) f.detach();
			t.rig.frames( 60, true );
		}
	}

	@Test
	public void sanctuarysGroundNeverFlickersFlipsOrMoves() throws Exception {
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			for (int reach = 1; reach <= 2; reach++){
				SkillFieldFX fx = sanctuary( reach );
				List<Image> pics = pics( fx );
				IdentityHashMap<Image, float[]> last = new IdentityHashMap<>();
				IdentityHashMap<Image, RectF> frames = new IdentityHashMap<>();
				float most = 0, mostSettled = 0;
				int n = Math.round( 6f / dt );
				for (int f = 0; f < n; f++){
					t.rig.frames( 1, true );
					float age = (f + 1) * dt;
					for (Image p : pics){
						RectF fr = frame( p );
						float[] now = { p.x, p.y, p.angle, p.scale.x, p.scale.y, p.am, p.visible ? 1 : 0 };
						if (frames.containsKey( p )){
							RectF was = frames.get( p );
							assertTrue( "its frame never changes", was.left == fr.left && was.top == fr.top && was.right == fr.right && was.bottom == fr.bottom );
						} else frames.put( p, new RectF( fr.left, fr.top, fr.right, fr.bottom ) );
						assertEquals( "never turned", 0, p.angle, 0 );
						assertEquals( "never scaled", 1, p.scale.x, 0 );
						assertEquals( "never scaled", 1, p.scale.y, 0 );
						float[] was = last.get( p );
						if (was != null){
							assertEquals( "never moves", was[0], now[0], 0 );
							assertEquals( "never moves", was[1], now[1], 0 );
							//alpha only ever slides: a share of a second's worth, never a jump
							float step = Math.abs( now[5] - was[5] ) / dt;
							most = Math.max( most, step );
							//once open and inscribed, it never blinks off and on
							if (age > 1f){
								mostSettled = Math.max( mostSettled, step );
								assertEquals( "never blinks", was[6], now[6], 0 );
							}
						}
						last.put( p, now );
					}
				}
				//(about 5.4 a second while a rune is inscribed: 0.09 a frame at 60 fps)
				assertTrue( "dt " + dt + ": its alpha slides at most " + most + " a second", most <= 7f );
				//(breath and the reading light: under 1 a second, a sixtieth of it a frame)
				assertTrue( "dt " + dt + ": settled, at most " + mostSettled + " a second", mostSettled <= 1.2f );
				for (SkillField fi : t.hero.buffs( SkillField.class )) fi.detach();
				t.rig.frames( 200, true );
			}
		}
	}

	@Test
	public void sanctumsLightIsSmoothAndItsRunesAreReadInTurn(){
		for (int r = 0; r < 2; r++){
			int n = Sanctum.RUNES[r];
			float h = 1 / 240f;
			for (int k = 0; k < n; k++){
				float prev = Sanctum.runeAlpha( k, n, 0, 0, true ), lit = 0;
				for (float s = h; s < 12f; s += h){
					float a = Sanctum.runeAlpha( k, n, s, s, true );
					assertTrue( "rune " + k + " at " + s, Math.abs( a - prev ) / h <= 7f );
					assertTrue( Sanctum.read( k, n, s ) >= 0 && Sanctum.read( k, n, s ) <= 1 );
					if (s > 1f) lit = Math.max( lit, Sanctum.read( k, n, s ) );
					prev = a;
				}
				//every rune is read in its turn: the light passes over each, full
				assertTrue( "rune " + k + " read " + lit, lit > 0.95f );
			}
			//inscribed one after another round the band, each in full by the end
			for (int k = 1; k < n; k++){
				assertTrue( Sanctum.inscribed( k, n, 0.2f, true ) <= Sanctum.inscribed( k - 1, n, 0.2f, true ) );
				assertEquals( 1f, Sanctum.inscribed( k, n, Sanctum.INSCRIBE_AT + Sanctum.INSCRIBE + Sanctum.RUNE_IN, true ), 0 );
			}
			//shown again (a stairway, a save), it is all there at once
			assertEquals( 1f, Sanctum.inscribed( 0, n, 0, false ), 0 );
		}
		float prev = Sanctum.sealAlpha( 0, 0, true );
		for (float s = 1 / 240f; s < 8f; s += 1 / 240f){
			float a = Sanctum.sealAlpha( s, s, true );
			assertTrue( Math.abs( a - prev ) * 240 <= 4f );
			assertTrue( a <= 1f );
			prev = a;
		}
	}

	@Test
	public void sanctuaryLetGoOfFadesAndGoes(){
		SkillFieldFX fx = sanctuary( 1 );
		t.rig.frames( 90, true );
		float before = brightest( fx );
		assertTrue( before > 0.5f );
		for (SkillField f : t.hero.buffs( SkillField.class )) f.detach();
		t.rig.frames( 2, true );
		assertNotNull( "still fading", field( layer( GameScene.Layer.FLOOR ) ) );
		t.rig.frames( Math.round( Sanctum.FADE / 2 * 60 ), true );
		float half = brightest( fx );
		assertTrue( "half faded " + half + " of " + before, half < before * 0.75f && half > 0.05f );
		t.rig.frames( Math.round( Sanctum.FADE * 60 ) + 5, true );
		assertNull( "gone", field( layer( GameScene.Layer.FLOOR ) ) );
	}

	@Test
	public void sanctuarysCastLiesOnNoGlyphAndItsRunesKeepTheirGlyphs() throws Exception {
		Sanctuary s = new Sanctuary();
		s.level = 1;
		assertFalse( SkillCastFX.holyGlyph( s ) );
		HealingPrayer hp = new HealingPrayer();
		hp.level = 1;
		assertTrue( "the other holy casts keep theirs", SkillCastFX.holyGlyph( hp ) );
		Object glyphs = TextureCache.get( "effects/skill_fields.png" );
		SkillCastFX.play( s, t.hero );
		Sanctuary.raised( t.hero.sprite );
		IdentityHashMap<Image, RectF> frames = new IdentityHashMap<>();
		int runes = 0;
		for (int f = 0; f < 40; f++){
			t.rig.frames( 1, true );
			for (Group g : t.rig.layers) for (Image p : allPics( g )) assertFalse( "no glyph under the feet", p.texture == glyphs );
			for (Image p : allPics( layer( GameScene.Layer.LIGHTS ) )){
				if (!(p.parent instanceof RuneFX.Runes) || !p.visible) continue;
				RectF fr = frame( p );
				RectF was = frames.get( p );
				if (was == null){
					frames.put( p, new RectF( fr.left, fr.top, fr.right, fr.bottom ) );
					runes++;
				} else assertTrue( "a rising rune keeps its glyph", was.left == fr.left && was.top == fr.top );
			}
		}
		assertEquals( Sanctuary.RISE, runes );
	}

	@Test
	public void sanctuarysGroundDrawsNothingOfTheGamesRandomNorTouchesTheGpu(){
		assertEquals( 0, EnchantFxTest.draws( () -> {
			SkillFieldFX fx = sanctuary( 2 );
			t.rig.frames( 240, true );
			for (SkillField f : t.hero.buffs( SkillField.class )) f.detach();
			t.rig.frames( 60, true );
		} ) );
		GL20 saved = Gdx.gl;
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, args) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
		try {
			new Sanctum( new int[]{ t.at( 3, 4 ), t.at( 4, 4 ), t.at( 3, 5 ) }, t.at( 3, 4 ), true );
			new SkillFieldFX( SkillField.place( t.hero, SkillField.SANCTUARY, 1, 5, java.util.Collections.singletonList( t.hero.pos ) ) );
		} finally {
			Gdx.gl = Gdx.gl20 = saved;
		}
	}

	@Test
	public void aSealCutByWallsKeepsToItsCells(){
		//the hero against a wall: the field lacks the row above, the seal has no cut there and no
		//rune there either
		List<Integer> cells = new ArrayList<>();
		int hx = t.hero.pos % DreadShadowProcs.W, hy = t.hero.pos / DreadShadowProcs.W;
		for (int dy = 0; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) cells.add( t.at( hx + dx, hy + dy ) );
		SkillField.place( t.hero, SkillField.SANCTUARY, 1, 5, cells );
		t.rig.frames( 90, true );
		SkillFieldFX fx = field( layer( GameScene.Layer.FLOOR ) );
		Object seal = TextureCache.get( Sanctum.TEXTURE );
		int cuts = 0;
		float top = SkillVfx.cy( t.hero.pos ) - 8;
		for (Image p : pics( fx )){
			if (p.texture == seal) cuts++;
			else if (p.visible) assertTrue( "no rune over the wall", p.y >= top - 1 );
		}
		assertEquals( 6, cuts );
		//its middle is the hero's cell, so the seal round him is cut, never shifted
		assertEquals( t.hero.pos, new Sanctum( toArray( cells ), t.hero.pos, false ).middle );
	}

	// ------------------------------------------------------------------ helpers

	private static int[] toArray( List<Integer> l ){
		int[] a = new int[l.size()];
		for (int i = 0; i < a.length; i++) a[i] = l.get( i );
		return a;
	}

	private static float brightest( SkillFieldFX fx ){
		float a = 0;
		for (Image p : pics( fx )) if (p.visible) a = Math.max( a, p.am );
		return a;
	}

	private static SkillFieldFX field( Group g ){
		if (g == null) return null;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof SkillFieldFX) return (SkillFieldFX) m;
			if (m instanceof Group){
				SkillFieldFX f = field( (Group) m );
				if (f != null) return f;
			}
		}
		return null;
	}

	private static List<Image> pics( Group fx ){
		List<Image> out = new ArrayList<>();
		for (Gizmo m : fx.membersView()) if (m instanceof Image && m.exists) out.add( (Image) m );
		return out;
	}

	private static List<Image> allPics( Group g ){
		List<Image> out = new ArrayList<>();
		if (g == null) return out;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof Image) out.add( (Image) m );
			else if (m instanceof Group) out.addAll( allPics( (Group) m ) );
		}
		return out;
	}

	private static RectF frame( Image p ){
		try {
			Field f = Image.class.getDeclaredField( "frame" );
			f.setAccessible( true );
			return (RectF) f.get( p );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	private static int count( Group g, String name ){
		if (g == null) return 0;
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m.getClass().getName().equals( name )) n++;
			if (m instanceof Group) n += count( (Group) m, name );
		}
		return n;
	}

	private static String code( String src ){
		return src.replaceAll( "(?s)/\\*.*?\\*/", "" ).replaceAll( "//[^\n]*", "" );
	}
}
