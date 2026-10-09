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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Writes the kit's numbers to build/fx-constants.json (core/build/ from the repo's root) for the
 * preview harness's kitsim, whose Python ports of the curves, ramps, rings, budget, flash gate and
 * particles check themselves against it: the constants as they stand, and what the code makes of
 * them at fixed points - curves sampled, a ramp read, rings stepped frame by frame, the budget's
 * factor at loads and tiers, the governor through slow and fast frames, flashes asked for along a
 * timeline, particles flown, the effects' random numbers from a seed. Then reads the file back.
 */
public class FxConstantsTest {

	/** Where the harness looks for it, from the core module (the tests' working directory). */
	static final String OUT = "build/fx-constants.json";

	private GL20 savedGl, savedGl20;
	private float savedElapsed, savedScale;
	private LongSupplier savedClock;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedElapsed = Game.elapsed;
		savedScale = FxBudget.scale;
		savedClock = FlashGate.clock;
		//nothing here draws: any call to the GPU is a mistake
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call: " + m.getName() ); } );
		FxBudget.scale = 1f;
		FxBudget.reset();
		FxBudget.resetGovernor();
		FxRing.reset();
		FlashGate.reset();
		Emitter.freezeEmitters = false;
	}

	@After
	public void tearDown(){
		FxBudget.reset();
		FxBudget.resetGovernor();
		FxBudget.scale = savedScale;
		FxRing.reset();
		FlashGate.clock = savedClock;
		FlashGate.reset();
		Game.elapsed = savedElapsed;
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
	}

	@Test
	public void writesTheNumbersTheHarnessMirrors() throws IOException {
		Map<String, Object> all = new LinkedHashMap<>();
		all.put( "budget", budget() );
		all.put( "curves", curves() );
		all.put( "ramp", ramp() );
		all.put( "rings", rings() );
		all.put( "light", light() );
		all.put( "flash", flash() );
		all.put( "random", random() );
		all.put( "particles", particles() );

		StringBuilder sb = new StringBuilder();
		write( sb, all, 0 );
		File out = new File( OUT );
		File dir = out.getAbsoluteFile().getParentFile();
		assertTrue( dir.isDirectory() || dir.mkdirs() );
		Files.write( out.toPath(), sb.toString().getBytes( StandardCharsets.UTF_8 ) );

		String back = new String( Files.readAllBytes( out.toPath() ), StandardCharsets.UTF_8 );
		assertEquals( sb.toString(), back );
		for (String k : all.keySet()) assertTrue( k, back.contains( "\"" + k + "\"" ) );
	}

	// ------------------------------------------------------------------ the sections

	private static Map<String, Object> budget(){
		Map<String, Object> b = new LinkedHashMap<>();
		b.put( "SOFT", FxBudget.SOFT );
		b.put( "HARD", FxBudget.HARD );
		b.put( "SPAWNS", FxBudget.SPAWNS );
		b.put( "LIGHTS_CAP", FxBudget.LIGHTS_CAP );
		b.put( "LIGHTS_PERSISTENT", FxBudget.LIGHTS_PERSISTENT );
		b.put( "LIGHTS_REDUCED", FxBudget.LIGHTS_REDUCED );
		b.put( "LIGHTS_MINIMAL", FxBudget.LIGHTS_MINIMAL );
		b.put( "DECALS_CAP", FxBudget.DECALS_CAP );
		b.put( "DECALS_REDUCED", FxBudget.DECALS_REDUCED );
		b.put( "DECALS_MINIMAL", FxBudget.DECALS_MINIMAL );
		b.put( "RINGS_CAP", FxBudget.RINGS_CAP );
		b.put( "DROPLETS_CAP", FxBudget.DROPLETS_CAP );
		b.put( "AFTERIMAGES_CAP", FxBudget.AFTERIMAGES_CAP );
		b.put( "EMA", FxBudget.EMA );
		b.put( "SLOW", FxBudget.SLOW );
		b.put( "FAST", FxBudget.FAST );
		b.put( "DOWN_AFTER", FxBudget.DOWN_AFTER );
		b.put( "UP_AFTER", FxBudget.UP_AFTER );
		b.put( "LOAD_FULL", FxBudget.LOAD_FULL );
		b.put( "LOAD_LOW", FxBudget.LOAD_LOW );
		b.put( "AT_LOW", FxBudget.AT_LOW );
		b.put( "FLOOR", FxBudget.FLOOR );
		b.put( "TIER_FACTOR", FxBudget.TIER_FACTOR );
		b.put( "NEAR", FxBudget.NEAR );
		b.put( "MID", FxBudget.MID );
		b.put( "MID_FACTOR", FxBudget.MID_FACTOR );
		b.put( "FAR_FACTOR", FxBudget.FAR_FACTOR );

		//the factor of each priority at loads, in each tier
		int[] loads = { 0, 100, 287, 288, 300, 360, 400, 468, 500, 519, 520, 600 };
		List<Object> factors = new ArrayList<>();
		for (int tier = FxBudget.FULL; tier <= FxBudget.MINIMAL; tier++){
			toTier( tier );
			for (int l : loads){
				setLive( l );
				Map<String, Object> f = new LinkedHashMap<>();
				f.put( "tier", tier );
				f.put( "live", l );
				f.put( "f", new float[]{ FxBudget.factor( FxBudget.P0 ), FxBudget.factor( FxBudget.P1 ),
						FxBudget.factor( FxBudget.P2 ), FxBudget.factor( FxBudget.P3 ) } );
				factors.add( f );
			}
		}
		setLive( 0 );
		b.put( "factor", factors );

		//the governor through smooth, slow, fast frames and hitches: its average and tier after each
		FxBudget.resetGovernor();
		float[] dts = new float[60 + 100 + 300 + 10];
		int i = 0;
		for (int k = 0; k < 60; k++) dts[i++] = 1 / 60f;
		for (int k = 0; k < 100; k++) dts[i++] = 1 / 25f;
		for (int k = 0; k < 300; k++) dts[i++] = 1 / 70f;
		for (int k = 0; k < 10; k++) dts[i++] = 0.4f;
		float[] ema = new float[dts.length];
		int[] tiers = new int[dts.length];
		for (int k = 0; k < dts.length; k++){
			FxBudget.frame( dts[k] );
			ema[k] = FxBudget.frameTime();
			tiers[k] = FxBudget.tier();
		}
		Map<String, Object> g = new LinkedHashMap<>();
		g.put( "dt", dts );
		g.put( "ema", ema );
		g.put( "tier", tiers );
		b.put( "governor", g );
		FxBudget.resetGovernor();
		return b;
	}

	//the governor stepped down to a tier by slow frames
	private static void toTier( int tier ){
		FxBudget.resetGovernor();
		for (int k = 0; k < 1000 && FxBudget.tier() < tier; k++) FxBudget.frame( 0.05f );
		assertEquals( tier, FxBudget.tier() );
	}

	private static void setLive( int n ){
		FxBudget.add( FxBudget.PARTICLES, n - FxBudget.live( FxBudget.PARTICLES ) );
	}

	private static Map<String, Object> curves(){
		Map<String, Object> c = new LinkedHashMap<>();
		float[] ts = { -0.2f, 0f, 0.05f, 0.1f, 0.2f, 0.25f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.75f, 0.8f, 0.9f, 0.95f, 1f, 1.2f };
		c.put( "t", ts );
		float[] eo = new float[ts.length], eo3 = new float[ts.length], ei = new float[ts.length], sm = new float[ts.length],
				bo = new float[ts.length], tri = new float[ts.length], st = new float[ts.length];
		for (int i = 0; i < ts.length; i++){
			eo[i] = FxCurves.easeOut( ts[i] );
			eo3[i] = FxCurves.easeOut3( ts[i] );
			ei[i] = FxCurves.easeIn( ts[i] );
			sm[i] = FxCurves.smooth( ts[i] );
			bo[i] = FxCurves.backOut( ts[i], 1.15f );
			tri[i] = FxCurves.tri( ts[i] );
			st[i] = FxCurves.step( ts[i], 4 );
		}
		c.put( "easeOut", eo );
		c.put( "easeOut3", eo3 );
		c.put( "easeIn", ei );
		c.put( "smooth", sm );
		c.put( "backOut115", bo );
		c.put( "tri", tri );
		c.put( "step4", st );

		float[] at = { -0.1f, 0f, 0.02f, 0.05f, 0.1f, 0.2f, 0.35f, 0.5f };
		float[] av = new float[at.length];
		for (int i = 0; i < at.length; i++) av[i] = FxCurves.attackRelease( at[i], 0.05f, 0.3f );
		c.put( "attackRelease", entry( "args", new float[]{ 0.05f, 0.3f }, "t", at, "v", av ) );

		float[] kt = { -0.1f, 0f, 0.02f, 0.04f, 0.06f, 0.1f, 0.13f, 0.17f, 0.25f, 0.3f, 0.5f };
		float[] kv = new float[kt.length];
		for (int i = 0; i < kt.length; i++) kv[i] = FxCurves.keyed( FxLight.STRIKE_KEYS, kt[i] );
		c.put( "keyed", entry( "keys", FxLight.STRIKE_KEYS, "t", kt, "v", kv ) );

		float[] ht = { 0f, 0.5f, 1f, 2.5f, 7.3f };
		float[] hv = new float[ht.length];
		for (int i = 0; i < ht.length; i++) hv[i] = FxCurves.hearth( ht[i], 0.3f, 1.1f, 2.0f );
		c.put( "hearth", entry( "abc", new float[]{ 0.3f, 1.1f, 2.0f }, "t", ht, "v", hv ) );

		int[] seeds = { 0, 7, 12345, -3 };
		float[] nt = { -1.5f, -0.2f, 0f, 0.3f, 1f, 2.7f, 10.5f, 100.25f };
		float[][] nv = new float[seeds.length][nt.length];
		for (int s = 0; s < seeds.length; s++) for (int i = 0; i < nt.length; i++) nv[s][i] = FxCurves.noise1( seeds[s], nt[i] );
		c.put( "noise1", entry( "seeds", seeds, "t", nt, "v", nv ) );
		return c;
	}

	private static Map<String, Object> ramp(){
		Ramp r = Element.FIRE.ramp;
		int[] stops = new int[r.size()];
		for (int i = 0; i < stops.length; i++) stops[i] = r.stop( i );
		float[] ps = { -0.1f, 0f, 0.1f, 0.18f, 0.2f, 0.33f, 0.45f, 0.5f, 0.66f, 0.7f, 0.83f, 0.99f, 1f, 1.1f };
		float[] keyedAt = { 0f, 0.18f, 0.45f, 0.7f, 1f };
		Ramp k = r.keyed( keyedAt );
		int[] at = new int[ps.length], step = new int[ps.length], kat = new int[ps.length], kstep = new int[ps.length];
		for (int i = 0; i < ps.length; i++){
			at[i] = r.at( ps[i] );
			step[i] = r.step( ps[i] );
			kat[i] = k.at( ps[i] );
			kstep[i] = k.step( ps[i] );
		}
		float[] lk = { 0f, 0.25f, 0.5f, 1f };
		int[] lerp = new int[lk.length];
		for (int i = 0; i < lk.length; i++) lerp[i] = Ramp.lerp( 0xFF0000, 0x0000FF, lk[i] );
		Map<String, Object> m = new LinkedHashMap<>();
		m.put( "stops", stops );
		m.put( "p", ps );
		m.put( "at", at );
		m.put( "step", step );
		m.put( "keyedPositions", keyedAt );
		m.put( "keyedAt", kat );
		m.put( "keyedStep", kstep );
		m.put( "lerpRedBlue", entry( "k", lk, "v", lerp ) );
		return m;
	}

	private static Map<String, Object> rings(){
		Map<String, Object> m = new LinkedHashMap<>();
		m.put( "RING_WIDTHS", FxFrames.RING_WIDTHS );
		m.put( "FROM", WaterFX.FROM );
		m.put( "TO", WaterFX.TO );
		m.put( "SECOND_FROM", WaterFX.SECOND_FROM );
		m.put( "SECOND_TO", WaterFX.SECOND_TO );
		m.put( "LIFE", WaterFX.LIFE );
		m.put( "ALPHA", WaterFX.ALPHA );
		m.put( "DISTURB", WaterFX.DISTURB );
		m.put( "SECOND_AFTER", WaterFX.SECOND_AFTER );
		m.put( "RINGS_A_FRAME", WaterFX.RINGS_A_FRAME );
		m.put( "BUS", WaterFX.BUS );
		m.put( "DROPLETS_MIN", WaterFX.DROPLETS_MIN );
		m.put( "DROPLETS_MAX", WaterFX.DROPLETS_MAX );
		m.put( "CROWN_LIFE", WaterFX.CROWN_LIFE );
		m.put( "WASHES", WaterFX.WASHES );

		//each size's ring and its second, stepped at 30 frames a second: its frame's width, alpha, shown
		Game.elapsed = 1 / 30f;
		List<Object> steps = new ArrayList<>();
		Group g = new Group();
		for (int s = WaterFX.S; s <= WaterFX.L; s++){
			steps.add( stepped( s, false, FxRing.ground( g, 50, 50, WaterFX.FROM[s], WaterFX.TO[s], WaterFX.LIFE[s],
					0xFFFFFF, WaterFX.ALPHA[s], false ).mirrorY() ) );
			if (s > WaterFX.S){
				steps.add( stepped( s, true, FxRing.ground( g, 50, 50, WaterFX.SECOND_FROM[s], WaterFX.SECOND_TO[s], WaterFX.LIFE[s],
						0xFFFFFF, WaterFX.ALPHA[s], false ).mirrorY().delay( WaterFX.SECOND_AFTER ) ) );
			}
		}
		m.put( "dt", Game.elapsed );
		m.put( "steps", steps );
		return m;
	}

	private static Map<String, Object> stepped( int size, boolean second, FxRing r ){
		ArrayList<Float> w = new ArrayList<>(), a = new ArrayList<>();
		ArrayList<Boolean> v = new ArrayList<>();
		for (int k = 0; k < 120 && r.alive; k++){
			r.update();
			if (!r.alive) break;
			w.add( r.width );
			a.add( r.am );
			v.add( r.visible );
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put( "size", size );
		m.put( "second", second );
		m.put( "width", w );
		m.put( "am", a );
		m.put( "visible", v );
		return m;
	}

	private static Map<String, Object> light(){
		Map<String, Object> m = new LinkedHashMap<>();
		m.put( "CORE_CAP", FxLight.CORE_CAP );
		m.put( "GLOW_CAP", FxLight.GLOW_CAP );
		m.put( "POOL_CAP", FxLight.POOL_CAP );
		m.put( "REFLECTIONS", FxLight.REFLECTIONS );
		m.put( "STRIKE_KEYS", FxLight.STRIKE_KEYS );
		m.put( "GLOW_SIZES", FxFrames.Light.GLOW_SIZES );
		m.put( "POOL_SIZES", FxFrames.Light.POOL_SIZES );
		m.put( "DAY_PERSISTENT", Fx.DAY_PERSISTENT );
		m.put( "DAY_TRANSIENT", Fx.DAY_TRANSIENT );
		float[] shares = { Fx.DAY_PERSISTENT, Fx.DAY_TRANSIENT };
		float[] tints = { 0f, 0.1f, 0.2f, 0.33f, 0.4f, 0.5f, 0.54f, 0.7f };
		float[][] v = new float[shares.length][tints.length];
		for (int s = 0; s < shares.length; s++) for (int i = 0; i < tints.length; i++) v[s][i] = Fx.nightMul( shares[s], tints[i] );
		m.put( "nightMul", entry( "dayShare", shares, "tint", tints, "v", v ) );
		return m;
	}

	private static Map<String, Object> flash(){
		Map<String, Object> m = new LinkedHashMap<>();
		m.put( "STORM_GAP", FlashGate.STORM_GAP );
		m.put( "GAME_GAP", FlashGate.GAME_GAP );
		m.put( "DAMAGE_GAP", FlashGate.DAMAGE_GAP );
		m.put( "WINDOW_MAX", FlashGate.WINDOW_MAX );
		m.put( "WINDOW", FlashGate.WINDOW );
		m.put( "PEAK_STORM", FlashGate.PEAK_STORM );
		m.put( "PEAK_GAME", FlashGate.PEAK_GAME );

		//flashes asked for along a timeline (storm, game, damage), and which went
		ArrayList<float[]> asks = new ArrayList<>();
		for (int k = 0; k <= 15; k++) asks.add( new float[]{ 0.3f * k, 0 } );
		for (float t : new float[]{ 11f, 11.5f, 11.8f, 12.5f, 13f, 14f, 21.5f }) asks.add( new float[]{ t, 0 } );
		for (float t : new float[]{ 0.5f, 1f, 2.6f, 2.7f, 5f, 5.2f, 7f, 9.5f, 12f, 12.1f, 15f, 20f }) asks.add( new float[]{ t, 1 } );
		for (float t : new float[]{ 0.1f, 0.3f, 0.45f, 0.5f, 0.9f, 1f, 1.2f, 8f, 8.3f, 8.36f }) asks.add( new float[]{ t, 2 } );
		asks.sort( (x, y) -> x[0] != y[0] ? Float.compare( x[0], y[0] ) : Float.compare( x[1], y[1] ) );
		final double[] now = { 0 };
		FlashGate.clock = () -> Math.round( now[0] * 1e9 );
		FlashGate.reset();
		float[] t = new float[asks.size()];
		int[] kind = new int[asks.size()];
		boolean[] went = new boolean[asks.size()];
		for (int i = 0; i < asks.size(); i++){
			t[i] = asks.get( i )[0];
			kind[i] = (int) asks.get( i )[1];
			now[0] = t[i];
			went[i] = kind[i] == 0 ? FlashGate.storm() : kind[i] == 1 ? FlashGate.game() : FlashGate.damage();
		}
		m.put( "timeline", entry( "t", t, "kind", kind, "went", went ) );
		return m;
	}

	private static Map<String, Object> random(){
		Map<String, Object> m = new LinkedHashMap<>();
		FxRandom.seedForTests( 42 );
		float[] f = new float[6];
		for (int i = 0; i < f.length; i++) f[i] = FxRandom.Float();
		FxRandom.seedForTests( 42 );
		int[] n = new int[6];
		for (int i = 0; i < n.length; i++) n[i] = FxRandom.Int( 10 );
		FxRandom.seedForTests( 7 );
		float[] nf = new float[4];
		for (int i = 0; i < nf.length; i++) nf[i] = FxRandom.NormalFloat( 0f, 10f );
		m.put( "seed", 42 );
		m.put( "float", f );
		m.put( "int10", n );
		m.put( "normalSeed", 7 );
		m.put( "normal0to10", nf );
		return m;
	}

	private static List<Object> particles(){
		List<Object> out = new ArrayList<>();
		Game.elapsed = 1 / 30f;
		out.add( flown( "drop: vel (10, -40), gravity 240, a bounce of 0.3 on y 60, life 2",
				new FxParticle().reset( 50, 20 ).paint( 0xD8EEE2 ).life( 2f ).vel( 10, -40 ).gravity( 240 )
						.ground( 60, FxParticle.BOUNCE, 0.3f ) ) );
		out.add( flown( "puff: matter, alpha 0.55 in 0.2 out 0.5, vel (0, -12), drag 0.08, life 1",
				new FxParticle().reset( 0, 0 ).paint( 0x8A847C ).life( 1f ).alpha( 0.55f, 0.2f, 0.5f ).vel( 0, -12 ).drag( 0.08f ) ) );
		FxRandom.seedForTests( 42 );
		out.add( flown( "ember: light, seed 42, alpha 1 out 0.4, vel (0, -20), gravity -6, drag 0.8, sway 4 at 3, life 1.5",
				new FxParticle().reset( 0, 0 ).light( true ).paint( 0xFFC060 ).life( 1.5f ).alpha( 1f, 0f, 0.4f )
						.vel( 0, -20 ).gravity( -6 ).drag( 0.8f ).sway( 4f, 3f ) ) );
		return out;
	}

	private static Map<String, Object> flown( String what, FxParticle p ){
		ArrayList<Float> px = new ArrayList<>(), py = new ArrayList<>(), am = new ArrayList<>();
		for (int k = 0; k < 120 && p.alive; k++){
			p.update();
			if (!p.alive) break;
			px.add( p.px );
			py.add( p.py );
			am.add( p.am );
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put( "what", what );
		m.put( "dt", Game.elapsed );
		m.put( "px", px );
		m.put( "py", py );
		m.put( "am", am );
		return m;
	}

	// ------------------------------------------------------------------ a little JSON

	private static Map<String, Object> entry( Object... kv ){
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) m.put( (String) kv[i], kv[i + 1] );
		return m;
	}

	private static void write( StringBuilder sb, Object o, int depth ){
		if (o instanceof Map){
			sb.append( "{" );
			boolean first = true;
			for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()){
				sb.append( first ? "\n" : ",\n" );
				first = false;
				indent( sb, depth + 1 );
				sb.append( '"' ).append( e.getKey() ).append( "\": " );
				write( sb, e.getValue(), depth + 1 );
			}
			sb.append( "\n" );
			indent( sb, depth );
			sb.append( "}" );
		} else if (o instanceof List){
			List<?> l = (List<?>) o;
			boolean nested = !l.isEmpty() && (l.get( 0 ) instanceof Map);
			sb.append( "[" );
			for (int i = 0; i < l.size(); i++){
				if (i > 0) sb.append( "," );
				if (nested){
					sb.append( "\n" );
					indent( sb, depth + 1 );
				}
				write( sb, l.get( i ), depth + 1 );
			}
			if (nested){
				sb.append( "\n" );
				indent( sb, depth );
			}
			sb.append( "]" );
		} else if (o instanceof float[]){
			float[] a = (float[]) o;
			ArrayList<Object> l = new ArrayList<>();
			for (float f : a) l.add( f );
			write( sb, l, depth );
		} else if (o instanceof int[]){
			int[] a = (int[]) o;
			ArrayList<Object> l = new ArrayList<>();
			for (int n : a) l.add( n );
			write( sb, l, depth );
		} else if (o instanceof boolean[]){
			boolean[] a = (boolean[]) o;
			ArrayList<Object> l = new ArrayList<>();
			for (boolean b : a) l.add( b );
			write( sb, l, depth );
		} else if (o instanceof float[][]){
			write( sb, new ArrayList<Object>( Arrays.asList( (Object[]) o ) ), depth );
		} else if (o instanceof Float){
			float f = (Float) o;
			if (Float.isNaN( f ) || Float.isInfinite( f )) throw new IllegalArgumentException( "not a JSON number: " + f );
			sb.append( f );
		} else if (o instanceof String){
			sb.append( '"' ).append( ((String) o).replace( "\\", "\\\\" ).replace( "\"", "\\\"" ) ).append( '"' );
		} else {
			sb.append( o );
		}
	}

	private static void indent( StringBuilder sb, int depth ){
		for (int i = 0; i < depth; i++) sb.append( "  " );
	}
}
