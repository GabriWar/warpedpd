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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.watabou.noosa.Game;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.FileUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

/**
 * The game's profiler, started and stopped from the debug menu. While it runs a sampling
 * thread reads the render thread's and the actor thread's stacks every few milliseconds,
 * so the report can say which methods the time actually went to (self and inclusive), per
 * thread, without touching any game code. On top of that it tracks every frame (update,
 * draw, percentiles), the GL work per frame (draw calls, texture binds, shader switches,
 * vertices), heap use, and the hand-timed hot paths and slowest actors the lag detector
 * keeps. On desktop it can also record a Java Flight Recorder file for JDK Mission Control.
 * Off by default; it costs nothing until it is started.
 */
public class Profiler {

	public static volatile boolean running = false;

	private static final int SAMPLE_MS = 1;
	private static final int TOP = 40;

	private static Thread sampler;
	private static volatile Thread renderThread;
	private static long startNanos, stopNanos;

	//per thread: samples by top frame (self) and by any frame (inclusive)
	private static class ThreadStats {
		final String name;
		int samples = 0, busy = 0;
		final HashMap<String, Integer> self = new HashMap<>();
		final HashMap<String, Integer> incl = new HashMap<>();
		final HashMap<String, Integer> buckets = new HashMap<>();
		final StackTree tree = new StackTree();
		ThreadStats( String name ){ this.name = name; }
	}
	private static final HashMap<Long, ThreadStats> threads = new HashMap<>();

	//what a busy render or actor sample was doing, by the packages and classes on its stack
	private static final String[][] BUCKETS = {
			{ "GL / draw calls", "com.badlogic.gdx.graphics", "org.lwjgl", "com.watabou.glwrap", "com.watabou.gltextures" },
			{ "scene draw", "com.watabou.noosa.Group.draw", "com.watabou.noosa.Visual.draw", "com.watabou.noosa.Image.draw", ".draw(" },
			{ "scene update", "com.watabou.noosa.Group.update", ".update(" },
			{ "particles", "com.watabou.noosa.particles" },
			{ "text rendering", "com.watabou.noosa.RenderedText", "com.badlogic.gdx.graphics.g2d.freetype", "BitmapText" },
			{ "textures / loading", "com.watabou.gltextures.TextureCache", "com.badlogic.gdx.graphics.Texture", "com.badlogic.gdx.graphics.Pixmap" },
			{ "audio", "com.watabou.noosa.audio", "com.badlogic.gdx.audio", "com.badlogic.gdx.backends.lwjgl3.audio" },
			{ "actor logic", "actors.Actor.process" },
			{ "field of view", "Dungeon.observe", "Level.updateFieldOfView", "mechanics.ShadowCaster" },
			{ "pathfinding", "com.watabou.utils.PathFinder", "levels.Level.findPath", "Ballistica" },
			{ "fog of war", "FogOfWar", "WallBlockingTilemap" },
			{ "tilemaps", "DungeonTilemap", "tiles." },
			{ "climate / weather", "ClimateManager", "TileTemperature", "WeatherOverlay", "WeatherBlobFX", "DayNightCycle" },
			{ "plants", "PlantGrowthManager", "plants." },
			{ "saving / bundles", "com.watabou.utils.Bundle", "Dungeon.saveAll", "org.json" },
			{ "networking", ".net." },
			{ "garbage collection / JVM", "java.lang.ref", "jdk.internal", "sun." },
	};

	//frames
	private static final ArrayList<Float> frameMs = new ArrayList<>(), updateMs = new ArrayList<>(), drawMs = new ArrayList<>();
	private static long lastFrameNanos = 0;
	private static float pendingUpdate = 0f, pendingDraw = 0f;

	//gl per frame: the interceptor checks glGetError after EVERY call, so it is switched on
	//for one frame in ten rather than left on - left on it halved a zoomed-out frame rate
	private static final int GL_SAMPLE_EVERY = 10;
	private static int glSampleCountdown = 0;
	private static GLProfiler gl;
	private static long glFrames = 0, glCalls = 0, glDraws = 0, glBinds = 0, glShaders = 0;
	private static double glVerts = 0;
	private static int glDrawsWorst = 0;

	//memory
	private static long memMin = Long.MAX_VALUE, memMax = 0, memSum = 0, memSamples = 0, gcDrops = 0, lastUsed = 0;

	private static String lastReport = null;

	// ---------------------------------------------------------------- control

	public static synchronized void start(){
		if (running) return;
		threads.clear();
		frameMs.clear(); updateMs.clear(); drawMs.clear();
		glFrames = glCalls = glDraws = glBinds = glShaders = 0; glVerts = 0; glDrawsWorst = 0;
		memMin = Long.MAX_VALUE; memMax = 0; memSum = 0; memSamples = 0; gcDrops = 0; lastUsed = 0;
		lastFrameNanos = 0;
		startNanos = System.nanoTime();
		running = true;
		//the lag detector's hand-timed sections and actor timings feed the report too
		if (!LagMonitor.enabled) LagMonitor.setEnabled( true );
		try {
			if (Gdx.graphics != null){
				gl = new GLProfiler( Gdx.graphics );
				gl.setListener( com.badlogic.gdx.graphics.profiling.GLErrorListener.THROWING_LISTENER );
				glSampleCountdown = 0;
			}
		} catch (Throwable t){
			gl = null;
		}
		sampler = new Thread( Profiler::sampleLoop, "WPD Profiler" );
		sampler.setDaemon( true );
		sampler.setPriority( Thread.MAX_PRIORITY );
		sampler.start();
		GLog.i( "Profiler started." );
	}

	public static synchronized String stop(){
		if (!running) return lastReport;
		running = false;
		stopNanos = System.nanoTime();
		try { if (sampler != null) sampler.join( 200 ); } catch (InterruptedException ignored){ }
		if (gl != null){
			try { if (gl.isEnabled()) gl.disable(); } catch (Throwable ignored){ }
			gl = null;
		}
		lastReport = buildReport();
		GLog.i( "Profiler stopped: " + String.format( Locale.ROOT, "%.1fs", (stopNanos - startNanos) / 1e9 ) );
		return lastReport;
	}

	public static String lastReport(){
		return lastReport;
	}

	public static float seconds(){
		return running ? (System.nanoTime() - startNanos) / 1e9f : 0f;
	}

	// ---------------------------------------------------------------- hooks (render thread)

	/** every rendered frame, from the game's update(); also pins down which thread renders */
	public static void frame(){
		if (!running) return;
		if (renderThread == null) renderThread = Thread.currentThread();
		long now = System.nanoTime();
		if (lastFrameNanos != 0){
			synchronized (frameMs){
				frameMs.add( (now - lastFrameNanos) / 1_000_000f );
				updateMs.add( pendingUpdate );
				drawMs.add( pendingDraw );
			}
		}
		lastFrameNanos = now;
		pendingUpdate = pendingDraw = 0f;
		if (gl != null){
			try {
				if (gl.isEnabled()){
					//the frame just drawn was a sampled one
					glFrames++;
					glCalls += gl.getCalls();
					int d = gl.getDrawCalls();
					glDraws += d;
					if (d > glDrawsWorst) glDrawsWorst = d;
					glBinds += gl.getTextureBindings();
					glShaders += gl.getShaderSwitches();
					glVerts += gl.getVertexCount().total;
					gl.reset();
					gl.disable();
				}
				if (++glSampleCountdown >= GL_SAMPLE_EVERY){
					glSampleCountdown = 0;
					gl.enable();
				}
			} catch (Throwable ignored){ }
		}
		Runtime rt = Runtime.getRuntime();
		long used = rt.totalMemory() - rt.freeMemory();
		if (lastUsed != 0 && lastUsed - used > (8L << 20)) gcDrops++;
		lastUsed = used;
		memMin = Math.min( memMin, used ); memMax = Math.max( memMax, used ); memSum += used; memSamples++;
	}

	/** the two halves of a frame, from the game class */
	public static void phase( boolean update, float ms ){
		if (!running) return;
		if (update) pendingUpdate = ms; else pendingDraw = ms;
	}

	// ---------------------------------------------------------------- sampling

	private static void sampleLoop(){
		Thread actor = null;
		while (running){
			Thread r = renderThread;
			if (actor == null || !actor.isAlive()) actor = findThread( "WPD Actor Thread" );
			if (r != null) sample( r );
			if (actor != null) sample( actor );
			try { Thread.sleep( SAMPLE_MS ); } catch (InterruptedException e){ return; }
		}
	}

	private static Thread findThread( String name ){
		for (Thread t : Thread.getAllStackTraces().keySet()) if (name.equals( t.getName() )) return t;
		return null;
	}

	private static void sample( Thread t ){
		StackTraceElement[] st = t.getStackTrace();
		Thread.State state = t.getState();
		ThreadStats ts;
		synchronized (threads){
			ts = threads.get( t.getId() );
			if (ts == null) threads.put( t.getId(), ts = new ThreadStats( t.getName() ) );
		}
		synchronized (ts){
			ts.samples++;
			if (state != Thread.State.RUNNABLE || st.length == 0) return;
			//a render thread blocked in the swap or a sleep is idle, not busy
			String top = frame( st[0] );
			if (top.startsWith( "java.lang.Thread.sleep" ) || top.startsWith( "java.lang.Object.wait" )
					|| top.contains( "glfwSwapBuffers" ) || top.contains( "LockSupport.park" )) return;
			ts.busy++;
			ts.tree.add( st );
			bump( ts.self, top );
			HashSet<String> seen = new HashSet<>();
			HashSet<String> bucketsHit = new HashSet<>();
			for (StackTraceElement e : st){
				String f = frame( e );
				if (seen.add( f )) bump( ts.incl, f );
				String full = e.getClassName() + "." + e.getMethodName() + "(";
				for (String[] b : BUCKETS){
					if (bucketsHit.contains( b[0] )) continue;
					for (int i = 1; i < b.length; i++){
						if (full.contains( b[i] )){ bucketsHit.add( b[0] ); break; }
					}
				}
			}
			for (String b : bucketsHit) bump( ts.buckets, b );
		}
	}

	private static String frame( StackTraceElement e ){
		return StackTree.frame( e );
	}

	private static void bump( HashMap<String, Integer> map, String key ){
		Integer v = map.get( key );
		map.put( key, v == null ? 1 : v + 1 );
	}

	// ---------------------------------------------------------------- report

	private static String buildReport(){
		StringBuilder sb = new StringBuilder();
		float secs = (stopNanos - startNanos) / 1e9f;
		sb.append( "Warped PD profile - " ).append( new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss", Locale.ROOT ).format( new Date() ) ).append( '\n' );
		sb.append( String.format( Locale.ROOT, "version %s (%d), %.1fs sampled every %d ms\n", Game.version, Game.versionCode, secs, SAMPLE_MS ) );
		if (Game.scene() != null) sb.append( "scene: " ).append( Game.scene().getClass().getSimpleName() );
		if (Dungeon.level != null) sb.append( String.format( Locale.ROOT, ", level %s depth %d, %dx%d, %d mobs, %d actors",
				Dungeon.level.getClass().getSimpleName(), Dungeon.depth, Dungeon.level.width(), Dungeon.level.height(),
				Dungeon.level.mobs.size(), Actor.all().size() ) );
		sb.append( '\n' );

		//frames
		ArrayList<Float> f;
		float uSum = 0, dSum = 0;
		synchronized (frameMs){
			f = new ArrayList<>( frameMs );
			for (float u : updateMs) uSum += u;
			for (float d : drawMs) dSum += d;
		}
		if (!f.isEmpty()){
			float sum = 0; for (float v : f) sum += v;
			ArrayList<Float> sorted = new ArrayList<>( f ); Collections.sort( sorted );
			sb.append( String.format( Locale.ROOT, "\nFRAMES: %d, avg %.1f ms (%.0f fps), p50 %.1f, p95 %.1f, p99 %.1f, worst %.0f ms\n",
					f.size(), sum / f.size(), 1000f / Math.max( 0.1f, sum / f.size() ),
					pct( sorted, 0.50f ), pct( sorted, 0.95f ), pct( sorted, 0.99f ), sorted.get( sorted.size() - 1 ) ) );
			sb.append( String.format( Locale.ROOT, "  per frame: update %.2f ms, draw %.2f ms, outside the loop (vsync, OS, GC) %.2f ms\n",
					uSum / f.size(), dSum / f.size(), Math.max( 0f, (sum - uSum - dSum) / f.size() ) ) );
			int over16 = 0, over33 = 0, over100 = 0;
			for (float v : f){ if (v > 16.7f) over16++; if (v > 33f) over33++; if (v > 100f) over100++; }
			sb.append( String.format( Locale.ROOT, "  frames over 16.7 ms: %d, over 33 ms: %d, over 100 ms: %d\n", over16, over33, over100 ) );
		}

		//gl
		if (glFrames > 0){
			sb.append( String.format( Locale.ROOT, "\nGL PER FRAME (one frame in %d sampled): %.0f draw calls (worst %d), %.0f texture binds, %.1f shader switches, %.0f GL calls, %.0f vertices\n",
					GL_SAMPLE_EVERY, glDraws / (double) glFrames, glDrawsWorst, glBinds / (double) glFrames, glShaders / (double) glFrames,
					glCalls / (double) glFrames, glVerts / glFrames ) );
		} else {
			sb.append( "\nGL PER FRAME: unavailable on this backend\n" );
		}

		//memory
		if (memSamples > 0){
			sb.append( String.format( Locale.ROOT, "\nHEAP: %d MB avg, %d..%d MB, %d gc-like drops, %d MB max heap\n",
					memSum / memSamples >> 20, memMin >> 20, memMax >> 20, gcDrops, Runtime.getRuntime().maxMemory() >> 20 ) );
		}

		//threads
		ArrayList<ThreadStats> list;
		synchronized (threads){ list = new ArrayList<>( threads.values() ); }
		for (ThreadStats ts : list){
			synchronized (ts){
				sb.append( String.format( Locale.ROOT, "\n=== %s: %d samples, busy %.0f%% of the time ===\n",
						ts.name, ts.samples, ts.samples == 0 ? 0f : 100f * ts.busy / ts.samples ) );
				if (ts.busy == 0) continue;
				sb.append( "where the busy time went (a sample can fall in more than one):\n" );
				for (Map.Entry<String, Integer> e : sorted( ts.buckets, 20 )){
					sb.append( String.format( Locale.ROOT, "  %-28s %5.1f%%\n", e.getKey(), 100f * e.getValue() / ts.busy ) );
				}
				sb.append( "call tree (root first, share of busy samples; straight chains folded with >):\n" );
				sb.append( ts.tree.text( 0.01f, 120 ) );
				sb.append( "top lines by self time (the exact line that was executing):\n" );
				for (Map.Entry<String, Integer> e : sorted( ts.self, TOP )){
					sb.append( String.format( Locale.ROOT, "  %5.1f%%  %s\n", 100f * e.getValue() / ts.busy, e.getKey() ) );
				}
				sb.append( "top lines by inclusive time (that line or anything it called):\n" );
				for (Map.Entry<String, Integer> e : sorted( ts.incl, TOP )){
					sb.append( String.format( Locale.ROOT, "  %5.1f%%  %s\n", 100f * e.getValue() / ts.busy, e.getKey() ) );
				}
			}
		}

		//the lag detector's hand-timed parts
		sb.append( "\n=== hand-timed hot paths and slowest actors (last 5s, from the lag detector) ===\n" );
		sb.append( LagMonitor.sectionsText() );
		return sb.toString();
	}

	private static float pct( ArrayList<Float> sorted, float p ){
		return sorted.get( Math.min( sorted.size() - 1, Math.max( 0, Math.round( p * (sorted.size() - 1) ) ) ) );
	}

	private static ArrayList<Map.Entry<String, Integer>> sorted( HashMap<String, Integer> map, int top ){
		ArrayList<Map.Entry<String, Integer>> l = new ArrayList<>( map.entrySet() );
		Collections.sort( l, ( a, b ) -> b.getValue() - a.getValue() );
		return new ArrayList<>( l.subList( 0, Math.min( top, l.size() ) ) );
	}

	// ---------------------------------------------------------------- flight recorder (desktop)

	private static Object jfr;

	/** the JDK's own recorder, when this JVM has it: method sampling, GC, allocation, locks */
	public static boolean jfrAvailable(){
		if (!DeviceCompat.isDesktop()) return false;
		try { Class.forName( "jdk.jfr.Recording" ); return true; } catch (Throwable t){ return false; }
	}

	public static boolean jfrRunning(){
		return jfr != null;
	}

	public static void jfrStart(){
		if (jfr != null || !jfrAvailable()) return;
		try {
			Class<?> cfgCls = Class.forName( "jdk.jfr.Configuration" );
			Object cfg = cfgCls.getMethod( "getConfiguration", String.class ).invoke( null, "profile" );
			Class<?> recCls = Class.forName( "jdk.jfr.Recording" );
			Object rec = recCls.getConstructor( cfgCls ).newInstance( cfg );
			recCls.getMethod( "setName", String.class ).invoke( rec, "Warped PD" );
			recCls.getMethod( "setToDisk", boolean.class ).invoke( rec, true );
			recCls.getMethod( "start" ).invoke( rec );
			jfr = rec;
			GLog.i( "Flight recording started." );
		} catch (Throwable t){
			GLog.w( "Could not start the flight recorder: " + t );
		}
	}

	/** stops and writes the .jfr next to the saves; open it with JDK Mission Control */
	public static String jfrStop(){
		if (jfr == null) return null;
		try {
			String name = "profile-" + new SimpleDateFormat( "yyyyMMdd-HHmmss", Locale.ROOT ).format( new Date() ) + ".jfr";
			java.io.File out = FileUtils.getFileHandle( name ).file();
			Class<?> recCls = jfr.getClass();
			recCls.getMethod( "stop" ).invoke( jfr );
			recCls.getMethod( "dump", java.nio.file.Path.class ).invoke( jfr, out.toPath() );
			recCls.getMethod( "close" ).invoke( jfr );
			jfr = null;
			GLog.p( "Flight recording saved to " + out.getAbsolutePath() );
			return out.getAbsolutePath();
		} catch (Throwable t){
			jfr = null;
			GLog.w( "Could not save the flight recording: " + t );
			return null;
		}
	}
}
