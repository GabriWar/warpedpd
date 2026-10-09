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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Debug-only lag detector. Off by default; switched on from the debug menu. While on it
 * times every frame and every actor's act(), and when a frame spikes or the frame rate
 * sags below the recent baseline it captures a profile (what level, how many mobs,
 * actors and blobs, the slowest actors of the last seconds, memory) and asks whether to
 * save it. Reports can be saved to a file and copied to the clipboard.
 */
public class LagMonitor {

	public static boolean enabled = false;

	//a frame at or above this is a spike, regardless of the baseline
	private static final float SPIKE_MS = 60f;
	//and this many seconds under half the baseline fps is a sag
	private static final float SAG_SECONDS = 1f;
	private static final float PROMPT_COOLDOWN = 15f;
	private static final int WINDOW = 300;   //frames kept for the profile

	private static long lastFrameNanos = 0;
	private static final float[] frames = new float[WINDOW];
	private static int frameIdx = 0, frameCount = 0;
	private static float baselineFps = 0f;
	private static float sagTime = 0f, sinceCapture = PROMPT_COOLDOWN;
	private static boolean prompting = false;

	private static class ActorTime { String name; long total; long worst; int calls; }
	private static final HashMap<String, ActorTime> actorTimes = new HashMap<>();
	private static final HashMap<String, ActorTime> sectionTimes = new HashMap<>();
	private static long actorWindowStart = 0;

	//per-frame detail: total, the update (logic + scene) and draw halves, memory, gc mark
	private static final int TIMELINE = 120;
	private static final float[] tlTotal = new float[TIMELINE], tlUpdate = new float[TIMELINE], tlDraw = new float[TIMELINE];
	private static final boolean[] tlGc = new boolean[TIMELINE];
	private static int tlIdx = 0, tlCount = 0;
	private static float pendingUpdate = 0f, pendingDraw = 0f;
	private static long lastUsedMem = 0;
	private static int gcCount = 0;

	private static final ArrayList<String> reports = new ArrayList<>();
	private static String lastSpikeSamples = null;

	public static void setEnabled( boolean on ){
		enabled = on;
		reset();
		if (on) startSampler(); else renderThread = null;
	}

	private static void reset(){
		lastFrameNanos = 0;
		frameIdx = frameCount = 0;
		baselineFps = 0f;
		sagTime = 0f;
		sinceCapture = PROMPT_COOLDOWN;
		actorTimes.clear();
		synchronized (sectionTimes){ sectionTimes.clear(); }
		actorWindowStart = System.nanoTime();
		tlIdx = tlCount = 0;
		gcCount = 0;
		lastUsedMem = 0;
	}

	/** start of a timed section; pair with {@link #end} */
	public static long begin(){
		return enabled ? System.nanoTime() : 0L;
	}

	/** end of a timed section named for the report; a no-op when the detector is off */
	public static void end( String name, long start ){
		if (!enabled || start == 0L) return;
		long nanos = System.nanoTime() - start;
		synchronized (sectionTimes){
			ActorTime t = sectionTimes.get( name );
			if (t == null){
				t = new ActorTime();
				t.name = name;
				sectionTimes.put( name, t );
			}
			t.total += nanos;
			t.calls++;
			if (nanos > t.worst) t.worst = nanos;
		}
	}

	/** the render thread's two halves of a frame, timed by the game class */
	public static void framePhase( boolean update, float ms ){
		Profiler.phase( update, ms );
		if (!enabled) return;
		if (update) pendingUpdate = ms; else pendingDraw = ms;
	}

	// ---------------------------------------------------------------- stack samples
	//while the detector is on, a background thread snapshots the render thread's and the actor
	//thread's full stacks every millisecond and keeps the last few seconds; a spike report can
	//then replay the slow frame sample by sample, fold it into a call tree with line numbers,
	//and list the exact lines that were executing

	private static final int SAMPLE_MS = 1;
	private static final int SAMPLES = 4096;
	private static final long[] sampleTime = new long[SAMPLES];
	private static final StackTraceElement[][] sampleStack = new StackTraceElement[SAMPLES][];
	private static final byte[] sampleWho = new byte[SAMPLES];   //0 render, 1 actor
	private static int sampleIdx = 0;
	private static volatile Thread renderThread;
	private static Thread samplerThread;

	private static void startSampler(){
		if (samplerThread != null && samplerThread.isAlive()) return;
		samplerThread = new Thread( () -> {
			Thread actor = null;
			int tick = 0;
			while (enabled){
				if ((tick++ & 255) == 0 || (actor != null && !actor.isAlive())){
					actor = null;
					for (Thread t : Thread.getAllStackTraces().keySet()) if ("WPD Actor Thread".equals( t.getName() )) actor = t;
				}
				record( renderThread, (byte) 0 );
				record( actor, (byte) 1 );
				try { Thread.sleep( SAMPLE_MS ); } catch (InterruptedException e){ return; }
			}
		}, "WPD Lag Sampler" );
		samplerThread.setDaemon( true );
		samplerThread.setPriority( Thread.MAX_PRIORITY );
		samplerThread.start();
	}

	private static void record( Thread t, byte who ){
		if (t == null || t.getState() != Thread.State.RUNNABLE) return;
		StackTraceElement[] st = t.getStackTrace();
		if (st.length == 0) return;
		//idle waits are not work: the swap, a sleep or a park, wherever the JNI trampoline
		//puts them in the stack
		for (int i = 0; i < st.length && i < 6; i++){
			String f = st[i].getClassName() + "." + st[i].getMethodName();
			if (f.contains( "glfwSwapBuffers" ) || f.contains( "glfwWaitEvents" ) || f.contains( "LockSupport.park" )
					|| f.startsWith( "java.lang.Object.wait" ) || f.startsWith( "java.lang.Thread.sleep" )
					|| f.contains( "Lwjgl3Application.loop" ) && i > 0 && st[0].getClassName().contains( "JNI" )) return;
		}
		synchronized (sampleTime){
			sampleTime[sampleIdx] = System.nanoTime();
			sampleStack[sampleIdx] = st;
			sampleWho[sampleIdx] = who;
			sampleIdx = (sampleIdx + 1) % SAMPLES;
		}
	}

	private static final String[] WHO = { "render thread", "actor thread" };

	/** everything both threads did during the last {@code ms} milliseconds: a sample-by-sample
	 *  timeline, then the call tree and the hot lines of each thread */
	private static String samplesText( float ms ){
		long now = System.nanoTime();
		long from = now - (long)(ms * 1_000_000L);
		ArrayList<Integer> idx = new ArrayList<>();
		synchronized (sampleTime){
			for (int k = 1; k <= SAMPLES; k++){
				int i = ((sampleIdx - k) % SAMPLES + SAMPLES) % SAMPLES;
				if (sampleStack[i] == null || sampleTime[i] < from) break;
				idx.add( 0, i );
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append( String.format( Locale.ROOT, "=== the last %.0f ms, sampled every %d ms: %d busy samples ===\n", ms, SAMPLE_MS, idx.size() ) );
		if (idx.isEmpty()){
			sb.append( "  both threads were idle or waiting the whole time\n" );
			return sb.toString();
		}
		//timeline: consecutive identical samples are folded into one line with a duration
		sb.append( "timeline (ms from the start of the window, thread, what was running, innermost first):\n" );
		String last = null; long runStart = 0, runEnd = 0; int runs = 0;
		synchronized (sampleTime){
			for (int i : idx){
				StackTraceElement[] st = sampleStack[i];
				StringBuilder line = new StringBuilder( WHO[sampleWho[i]] ).append( "  " );
				for (int f = 0, shown = 0; f < st.length && shown < 4; f++){
					String c = st[f].getClassName();
					if (c.startsWith( "com.badlogic.gdx.backends" ) || c.startsWith( "org.lwjgl" )) continue;
					if (shown > 0) line.append( " < " );
					line.append( StackTree.frame( st[f] ) );
					shown++;
				}
				String cur = line.toString();
				if (cur.equals( last )){
					runEnd = sampleTime[i];
					continue;
				}
				if (last != null && runs < 80){
					sb.append( String.format( Locale.ROOT, "  %6.1f  %5.1f ms  %s\n", (runStart - from) / 1e6, Math.max( SAMPLE_MS, (runEnd - runStart) / 1e6 + SAMPLE_MS ), last ) );
					runs++;
				}
				last = cur; runStart = runEnd = sampleTime[i];
			}
			if (last != null && runs < 80) sb.append( String.format( Locale.ROOT, "  %6.1f  %5.1f ms  %s\n", (runStart - from) / 1e6, Math.max( SAMPLE_MS, (runEnd - runStart) / 1e6 + SAMPLE_MS ), last ) );
			if (runs >= 80) sb.append( "  ...\n" );
		}
		//per thread: tree and hot lines
		for (byte who = 0; who < 2; who++){
			StackTree tree = new StackTree();
			synchronized (sampleTime){
				for (int i : idx) if (sampleWho[i] == who) tree.add( sampleStack[i] );
			}
			if (tree.samples() == 0) continue;
			sb.append( String.format( Locale.ROOT, "\n--- %s: %d samples ---\ncall tree (root first, share of this thread's samples):\n", WHO[who], tree.samples() ) );
			sb.append( tree.text( 0.02f, 70 ) );
			sb.append( "hottest lines (the exact line executing):\n" );
			sb.append( tree.selfText( 15 ) );
		}
		return sb.toString();
	}

	// ---------------------------------------------------------------- hooks

	/** every rendered frame, from the game's update() */
	public static void frame(){
		if (!enabled) return;
		if (renderThread == null) renderThread = Thread.currentThread();
		long now = System.nanoTime();
		if (lastFrameNanos == 0){ lastFrameNanos = now; return; }
		float ms = (now - lastFrameNanos) / 1_000_000f;
		lastFrameNanos = now;

		frames[frameIdx] = ms;
		frameIdx = (frameIdx + 1) % WINDOW;
		if (frameCount < WINDOW) frameCount++;

		//timeline entry for this frame; a sudden drop in used memory marks a collection
		Runtime rt = Runtime.getRuntime();
		long used = rt.totalMemory() - rt.freeMemory();
		boolean gc = lastUsedMem != 0 && lastUsedMem - used > (8L << 20);
		if (gc) gcCount++;
		lastUsedMem = used;
		tlTotal[tlIdx] = ms;
		tlUpdate[tlIdx] = pendingUpdate;
		tlDraw[tlIdx] = pendingDraw;
		tlGc[tlIdx] = gc;
		tlIdx = (tlIdx + 1) % TIMELINE;
		if (tlCount < TIMELINE) tlCount++;
		pendingUpdate = pendingDraw = 0f;

		//the baseline is a slow average of ordinary frames, so a sag is measured against
		//what this device normally does rather than a fixed number
		float fps = 1000f / Math.max( 1f, ms );
		if (ms < SPIKE_MS){
			baselineFps = baselineFps == 0f ? fps : baselineFps * 0.995f + fps * 0.005f;
		}
		sinceCapture += ms / 1000f;
		//actor timings only matter for the last few seconds
		if (now - actorWindowStart > 5_000_000_000L){
			actorTimes.clear();
			synchronized (sectionTimes){ sectionTimes.clear(); }
			actorWindowStart = now;
		}

		if (prompting || sinceCapture < PROMPT_COOLDOWN || frameCount < 60) return;

		String why = null;
		if (ms >= SPIKE_MS){
			why = String.format( Locale.ROOT, "frame spike: %.0f ms (baseline %.0f fps)", ms, baselineFps );
		} else if (baselineFps > 20f && fps < baselineFps * 0.75f){
			sagTime += ms / 1000f;
			if (sagTime >= SAG_SECONDS){
				why = String.format( Locale.ROOT, "frame rate sag: %.0f fps for %.1fs (baseline %.0f fps)", fps, sagTime, baselineFps );
			}
		} else {
			sagTime = 0f;
		}
		if (why != null){
			//the slow frame just ended: keep its samples before anything else overwrites them
			lastSpikeSamples = samplesText( Math.max( ms, 50f ) + 2f );
			capture( why, true );
		}
	}

	/** wraps one actor's act(): called by Actor.process with the time it took */
	public static void actorActed( Actor actor, long nanos ){
		if (!enabled || actor == null) return;
		String name = actor.getClass().getSimpleName();
		if (name.isEmpty()) name = actor.getClass().getName();
		ActorTime t = actorTimes.get( name );
		if (t == null){
			t = new ActorTime();
			t.name = name;
			actorTimes.put( name, t );
		}
		t.total += nanos;
		t.calls++;
		if (nanos > t.worst) t.worst = nanos;
	}

	// ---------------------------------------------------------------- reports

	/** takes a profile now; from the debug menu or from a detected lag */
	public static String capture( String why, boolean ask ){
		sinceCapture = 0f;
		sagTime = 0f;
		final String report = buildReport( why );
		reports.add( 0, report );
		while (reports.size() > 5) reports.remove( reports.size() - 1 );
		if (ask && Game.scene() instanceof GameScene){
			prompting = true;
			Game.runOnRenderThread( () -> GameScene.show( new WndOptions( "Lag detected",
					why + "\n\nA profile of what was running was captured. Save it?",
					"Save to file + clipboard", "Show it", "Ignore", "Stop the detector" ){
				@Override
				protected void onSelect( int index ){
					prompting = false;
					if (index == 0) save( report );
					else if (index == 1) GameScene.show( new xyz.gabriwar.warpedpixeldungeon.windows.WndDebugReport( "Lag profile", report ) );
					else if (index == 3) setEnabled( false );
				}
				@Override
				public void onBackPressed(){ prompting = false; super.onBackPressed(); }
			} ) );
		}
		return report;
	}

	public static String lastReport(){
		return reports.isEmpty() ? null : reports.get( 0 );
	}

	private static String buildReport( String why ){
		StringBuilder sb = new StringBuilder();
		sb.append( "Warped PD lag profile - " ).append( new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss", Locale.ROOT ).format( new Date() ) ).append( '\n' );
		sb.append( "reason: " ).append( why ).append( '\n' );
		sb.append( "version: " ).append( Game.version ).append( " (" ).append( Game.versionCode ).append( ")\n" );

		//frames
		int n = frameCount;
		float sum = 0, worst = 0; int over50 = 0;
		for (int i = 0; i < n; i++){ float f = frames[i]; sum += f; if (f > worst) worst = f; if (f > 50f) over50++; }
		if (n > 0){
			sb.append( String.format( Locale.ROOT, "frames: last %d avg %.1f ms (%.0f fps), worst %.0f ms, %d frames over 50 ms, baseline %.0f fps\n",
					n, sum / n, 1000f / Math.max( 0.1f, sum / n ), worst, over50, baselineFps ) );
		}
		Runtime rt = Runtime.getRuntime();
		sb.append( String.format( Locale.ROOT, "memory: %d MB used of %d MB\n", (rt.totalMemory() - rt.freeMemory()) >> 20, rt.totalMemory() >> 20 ) );

		//where
		sb.append( "scene: " ).append( Game.scene() == null ? "none" : Game.scene().getClass().getSimpleName() ).append( '\n' );
		if (Dungeon.level != null){
			sb.append( String.format( Locale.ROOT, "level: %s depth %d branch %d, %dx%d = %d cells\n",
					Dungeon.level.getClass().getSimpleName(), Dungeon.depth, Dungeon.branch,
					Dungeon.level.width(), Dungeon.level.height(), Dungeon.level.length() ) );
			int mobs = Dungeon.level.mobs.size(), hunting = 0;
			for (Mob m : Dungeon.level.mobs) if (m.state == m.HUNTING) hunting++;
			sb.append( String.format( Locale.ROOT, "mobs: %d (%d hunting), heaps: %d, plants: %d, actors: %d\n",
					mobs, hunting, Dungeon.level.heaps.valueList().size(), Dungeon.level.plants.valueList().size(), Actor.all().size() ) );
			StringBuilder blobs = new StringBuilder();
			for (Blob b : Dungeon.level.blobs.values()){
				if (b.volume > 0) blobs.append( b.getClass().getSimpleName() ).append( '=' ).append( b.volume ).append( ' ' );
			}
			sb.append( "blobs: " ).append( blobs.length() == 0 ? "none" : blobs ).append( '\n' );
			if (Dungeon.hero != null){
				sb.append( String.format( Locale.ROOT, "hero: pos %d, action %s, ready %b, buffs %d\n", Dungeon.hero.pos,
						Dungeon.hero.curAction == null ? "none" : Dungeon.hero.curAction.getClass().getSimpleName(),
						Dungeon.hero.ready, Dungeon.hero.buffs().size() ) );
			}
		}
		if (Game.scene() instanceof PixelScene){
			sb.append( "camera zoom: " ).append( com.watabou.noosa.Camera.main == null ? "?" : Float.toString( com.watabou.noosa.Camera.main.zoom ) ).append( '\n' );
		}

		sb.append( sectionsText() );

		//the stack samples of the frame that tripped the detector, or of the last second on a manual snapshot
		sb.append( '\n' ).append( lastSpikeSamples != null ? lastSpikeSamples : samplesText( 2000f ) );
		lastSpikeSamples = null;

		//the last frames one by one: total (update/draw), with the collections marked
		sb.append( String.format( Locale.ROOT, "gc-like memory drops since the detector started: %d\n", gcCount ) );
		sb.append( "last frames, ms total (update/draw), * = memory drop:\n  " );
		int shown = Math.min( tlCount, 40 );
		float worstT = -1; int worstI = -1;
		for (int k = shown; k >= 1; k--){
			int i = ((tlIdx - k) % TIMELINE + TIMELINE) % TIMELINE;
			sb.append( String.format( Locale.ROOT, "%.0f(%.0f/%.0f)%s ", tlTotal[i], tlUpdate[i], tlDraw[i], tlGc[i] ? "*" : "" ) );
			if (tlTotal[i] > worstT){ worstT = tlTotal[i]; worstI = i; }
		}
		sb.append( '\n' );
		if (worstI >= 0){
			float u = tlUpdate[worstI], d = tlDraw[worstI], other = Math.max( 0f, worstT - u - d );
			sb.append( String.format( Locale.ROOT, "worst of those: %.0f ms = update %.0f + draw %.0f + outside the loop %.0f (vsync, GC, OS)%s\n",
					worstT, u, d, other, tlGc[worstI] ? ", memory dropped that frame" : "" ) );
		}

		return sb.toString();
	}

	/** the hand-timed hot paths and the slowest actors, for this report and the profiler's */
	public static String sectionsText(){
		StringBuilder sb = new StringBuilder();
		//sections: the game's own hot paths, timed on whichever thread ran them
		ArrayList<ActorTime> secs;
		synchronized (sectionTimes){ secs = new ArrayList<>( sectionTimes.values() ); }
		Collections.sort( secs, ( a, b ) -> Long.compare( b.total, a.total ) );
		sb.append( "hot paths in the last 5s (total ms / worst ms / calls):\n" );
		if (secs.isEmpty()) sb.append( "  nothing timed\n" );
		for (ActorTime t : secs){
			sb.append( String.format( Locale.ROOT, "  %-28s %7.1f / %6.1f / %d\n", t.name, t.total / 1e6, t.worst / 1e6, t.calls ) );
		}

		//who was slow
		ArrayList<ActorTime> slow = new ArrayList<>( actorTimes.values() );
		Collections.sort( slow, ( a, b ) -> Long.compare( b.total, a.total ) );
		sb.append( "slowest actors in the last 5s (total ms / worst ms / calls):\n" );
		if (slow.isEmpty()) sb.append( "  no actor ran\n" );
		for (int i = 0; i < slow.size() && i < 10; i++){
			ActorTime t = slow.get( i );
			sb.append( String.format( Locale.ROOT, "  %-28s %7.1f / %6.1f / %d\n", t.name, t.total / 1e6, t.worst / 1e6, t.calls ) );
		}
		return sb.toString();
	}

	public static void save( String report ){
		save( "lag-profile", report );
	}

	public static void save( String prefix, String report ){
		String name = prefix + "-" + new SimpleDateFormat( "yyyyMMdd-HHmmss", Locale.ROOT ).format( new Date() ) + ".txt";
		String fileResult;
		try {
			FileHandle file = FileUtils.getFileHandle( name );
			file.writeString( report, false, "UTF-8" );
			fileResult = "Saved to:\n" + file.file().getAbsolutePath();
		} catch (Exception e){
			fileResult = "Could not save the file: " + e.getMessage();
		}
		String clipResult = copyToClipboard( report ) ? "Copied to the clipboard." : "Could not copy to the clipboard.";
		GLog.i( fileResult.replace( '\n', ' ' ) + " " + clipResult );
		final String message = fileResult + "\n\n" + clipResult;
		Game.runOnRenderThread( () -> {
			if (Game.scene() instanceof PixelScene){
				Game.scene().addToFront( new WndTitledMessage( xyz.gabriwar.warpedpixeldungeon.ui.Icons.get( xyz.gabriwar.warpedpixeldungeon.ui.Icons.WARNING ),
						prefix.equals( "profile" ) ? "Profile saved" : "Lag profile saved", message ) );
			}
		} );
	}

	/** the game's clipboard does nothing under Wayland, so on desktop the system clipboard
	 *  tools are tried first; the result is checked, never assumed */
	private static boolean copyToClipboard( String text ){
		if (com.watabou.utils.DeviceCompat.isDesktop()){
			String[][] tools = {
					{ "wl-copy" },
					{ "xclip", "-selection", "clipboard" },
					{ "xsel", "--clipboard", "--input" },
					{ "pbcopy" },
					{ "clip" } };
			for (String[] tool : tools){
				try {
					Process p = new ProcessBuilder( tool ).redirectErrorStream( true ).start();
					try (java.io.OutputStream out = p.getOutputStream()){
						out.write( text.getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
					}
					//wl-copy and xclip stay alive serving the selection; a quick exit code 0
					//or a live process both mean the copy took
					if (!p.waitFor( 500, java.util.concurrent.TimeUnit.MILLISECONDS ) || p.exitValue() == 0) return true;
				} catch (Exception ignored){
					//tool not installed: try the next one
				}
			}
		}
		try {
			if (Gdx.app != null && Gdx.app.getClipboard() != null){
				Gdx.app.getClipboard().setContents( text );
				return text.equals( Gdx.app.getClipboard().getContents() );
			}
		} catch (Exception ignored){
			//no clipboard on this backend
		}
		return false;
	}
}
