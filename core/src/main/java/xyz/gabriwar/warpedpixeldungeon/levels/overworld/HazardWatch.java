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


package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Breathless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CaveIn;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DraftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FiredampParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * The dangers of a world slice, acting (where they lie: LayerHazards). One per OverworldLevel.
 *
 * - Firedamp (deep caves): a hero walking into a pocket smells it and sees its haze; lingering
 *   makes him dizzy; a flame he carries (a torch, or himself on fire) is warned of, and if he
 *   is still in the gas with it a turn later the pocket goes up. A fire on a gas cell (a wand,
 *   a campfire, a burning beast) sets it off too, with a hiss to every hero near first. The
 *   blast hurts everyone in and beside the gas - a hero never more than 35% of his health - and
 *   leaves fire on the gas cells nobody stands on; the pocket is spent for three days.
 * - Rockfall (caves): a blow of the pick on loose or unpropped rock may bring the roof down
 *   (CaveIn): the cells are marked, the dust falls and everyone near is warned, and the rocks
 *   come down after the miner's next action, never on a way or water, always with a free cell
 *   beside him.
 * - Thin ice (the slices, never the surface): stepping on it cracks it (seen, IceCracks; logged); standing on cracked
 *   ice it groans, then gives; stepping back on a crack breaks it at once. The hole is water,
 *   the hero drenched and chilled.
 * - The heights (LayerHazards.HIGH and up): Breathless, and on a ridge in a wind a gust - the
 *   warning first, then after his action a push along the ridge, never into the drop, or a
 *   stagger where there is nowhere to go.
 *
 * Every consequence is a Strike: a one-shot actor after the hero's own action of that turn (or
 * the next), checking its condition again and finding its cells again by world position, so a
 * rebase in between changes nothing. Nothing here changes on a network mirror; the cracks reach
 * a guest's mirror through shared(), the rest through the map, the log and relayed particles.
 * State is keyed by world position: spent pockets and cracks persist in the level's bundle.
 */
public final class HazardWatch {

	private static final long NONE = Long.MIN_VALUE;

	private final OverworldLevel level;

	//the window's pristine firedamp (WindowGenerator.Window.firedamp) and the cells it marks
	private boolean[] mask;
	private int[] pocketCells = new int[0];

	//spent pockets: LayerHazards.pocketKey -> the world turn the pocket fills again at
	private final HashMap<Long, Integer> spent = new HashMap<>();
	//cracked ice: world key -> the world turn it cracked
	private final HashMap<Long, Integer> cracked = new HashMap<>();
	//a pocket a fire is about to set off, and the strike that will (never saved: a load sees the
	//fire and arms it again)
	private final HashMap<Long, Strike> armed = new HashMap<>();
	//per hero (by id), never saved: a load warns again, which is the safe way round
	private final HashMap<Integer, State> states = new HashMap<>();
	//world turns to the next sweep of healed cracks and refilled pockets
	private int sweepIn = 64;

	private static final class State {
		float lastTick = -1f;
		long pocket = NONE;
		int inGas, flameTurns;
		boolean dizzyTold;
		long strainKey = NONE;
		int strain;
		//hero turns before the next gust may come
		int quiet;
		int lastCrackLog = Integer.MIN_VALUE;
	}

	/** The cracks as everyone sees them: an immutable snapshot, rebuilt on every change. Any thread. */
	static final class Cracks {
		static final Cracks EMPTY = new Cracks( new long[0], new byte[0], 0 );
		final long[] keys;
		final byte[] stages;   //1 cracked, 2 giving way
		final int version;
		private final HashMap<Long, Byte> at = new HashMap<>();
		Cracks( long[] keys, byte[] stages, int version ){
			this.keys = keys;
			this.stages = stages;
			this.version = version;
			for (int i = 0; i < keys.length; i++) at.put( keys[i], stages[i] );
		}
		byte stageOf( long key ){
			Byte s = at.get( key );
			return s == null ? 0 : s;
		}
	}

	private volatile Cracks shown = Cracks.EMPTY;
	private int crackVersion = 0;

	HazardWatch( OverworldLevel level ){
		this.level = level;
	}

	// ------------------------------------------------------------- helpers

	private long keyOf( int cell ){
		return OverworldLevel.worldKey( level.worldX + cell % level.width(), level.worldY + cell / level.width() );
	}

	private long pocketKeyOf( int cell ){
		return LayerHazards.pocketKey( level.worldX + cell % level.width(), level.worldY + cell / level.width() );
	}

	private State state( Hero h ){
		State s = states.get( h.id() );
		if (s == null){
			s = new State();
			states.put( h.id(), s );
		}
		return s;
	}

	//halts what the hero was doing: the host's own hero by interrupt, a guest's by dropping his
	//action (interrupt would reset the host's key-hold, see OverworldLevel.reseason). with no
	//scene up there is no key-hold to reset either
	private void stop( Hero h ){
		if (h == Dungeon.hero && live()) h.interrupt();
		else h.curAction = null;
	}

	private static void say( Hero h, String prefix, String key ){
		NetManager.heroLog( h, prefix + Messages.get( HazardWatch.class, key ) );
	}

	//every hero on the level within r of a cell
	private ArrayList<Hero> heroesNear( int cell, int r ){
		ArrayList<Hero> out = new ArrayList<>();
		for (Hero h : OverworldLevel.heroesOn( level )){
			if (level.distance( cell, h.pos ) <= r) out.add( h );
		}
		return out;
	}

	private boolean live(){
		return level.liveScene();
	}

	/**
	 * A consequence that lands after the hero's action: a one-shot actor after the buffs of the
	 * turn it is added on (or `delay` turns on), doing nothing if the level is no longer the one
	 * played. Never saved: on a load the danger is seen and warned of again.
	 */
	static final class Strike extends Actor {
		private final OverworldLevel on;
		private final Runnable run;
		Strike( OverworldLevel on, Runnable run ){
			this.on = on;
			this.run = run;
			actPriority = BUFF_PRIO;
		}
		@Override
		protected boolean act(){
			Actor.remove( this );
			if (Dungeon.level == on) run.run();
			return true;
		}
	}

	private Strike soon( float delay, Runnable r ){
		Strike s = new Strike( level, r );
		Actor.addDelayed( s, delay );
		return s;
	}

	// --------------------------------------------------------------- window

	/** A window was adopted: its firedamp mask, and the cells it marks. One pass per adoption. */
	void windowChanged( boolean[] firedamp ){
		mask = firedamp;
		if (firedamp == null){
			pocketCells = new int[0];
		} else {
			int n = 0;
			for (boolean b : firedamp) if (b) n++;
			int[] cells = new int[n];
			n = 0;
			for (int c = 0; c < firedamp.length; c++) if (firedamp[c]) cells[n++] = c;
			pocketCells = cells;
		}
	}

	/** Is there unspent firedamp on this window cell? */
	public boolean gasAt( int cell ){
		return mask != null && cell >= 0 && cell < mask.length && mask[cell] && level.passable[cell]
				&& !spentNow( pocketKeyOf( cell ) );
	}

	private boolean spentNow( long key ){
		Integer until = spent.get( key );
		return until != null && until > WorldClock.turn();
	}

	/** Is the ice on this window cell cracked? Any thread, mirrors too (the shared snapshot). */
	public boolean crackedAt( int cell ){
		return cell >= 0 && cell < level.length() && shown.stageOf( keyOf( cell ) ) > 0;
	}

	Cracks cracks(){
		return shown;
	}

	//the temperature the ice feels: the open air over that cell (its own biome, whoever's hero
	//the climate is worked out for) plus whatever heat or cold lies on it
	private float ambientAt( int cell ){
		return ClimateManager.airAt( level, cell ) + (level.tileHeat != null ? level.tileHeat[cell] : 0f);
	}

	//the cracks as they are now, for everyone to see (IceCracks, examine, the guests)
	private void publish(){
		long[] keys = new long[cracked.size()];
		byte[] stages = new byte[keys.length];
		int i = 0;
		for (Long k : cracked.keySet()){
			keys[i] = k;
			stages[i] = 1;
			for (State s : states.values()){
				if (s.strainKey == k && s.strain >= LayerHazards.STRAIN_GROAN) stages[i] = 2;
			}
			i++;
		}
		shown = new Cracks( keys, stages, ++crackVersion );
	}

	// ----------------------------------------------------------- the hero

	/** Hero.act, every hero's turn as it begins (host only; a guest's hero acts on the host). */
	public void heroTurn( Hero h ){
		heroTurnAt( h, Actor.now(), Random.Float() );
	}

	void heroTurnAt( Hero h, float now, float roll ){
		if (level.isMirror() || Dungeon.level != level || !h.isAlive()) return;
		State s = state( h );
		//a turn the hero waits in for the player begins again at the same time: it is no new turn
		if (now == s.lastTick) return;
		s.lastTick = now;
		long t = LagMonitor.begin();
		//his turn has begun: rocks marked before it fall once he has taken it
		for (CaveIn c : h.buffs( CaveIn.class )) c.waiting = false;
		firedampTick( h, s );
		iceTick( h, s );
		if (level.altitude() >= LayerHazards.HIGH){
			//resting by a fire lets the breath come back (Breathless eases off and goes): it is not
			//put on again until he is up and about
			if (h.buff( Breathless.class ) == null && !Breathless.sheltered( h )){
				Buff.affect( h, Breathless.class );
				NetManager.heroLog( h, GLog.WARNING + Messages.get( Breathless.class, "onset" ) );
			}
			gustTick( h, s, roll );
		}
		LagMonitor.end( "OW hazards (hero)", t );
	}

	// -------------------------------------------------------------- firedamp

	//an open flame on the hero: a torch, or himself on fire
	private static boolean flame( Hero h ){
		if (h.buff( Burning.class ) != null) return true;
		Light l = h.buff( Light.class );
		return l != null && l.flame;
	}

	//the foul air about the hero: wisps on each gas cell within two steps of him, nearest first
	//(at most HAZE_CELLS), so the pocket's shape shows round him for as long as he is in it
	private void haze( int at, int n ){
		if (!live()) return;
		int w = level.width(), shown = 0;
		for (int r = 0; r <= 2; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r || shown >= HAZE_CELLS) continue;
					int x = at % w + dx, y = at / w + dy;
					if (x < 0 || y < 0 || x >= w || y >= level.height() || !gasAt( x + y * w )) continue;
					CellEmitter.get( x + y * w ).burst( FiredampParticle.FACTORY, n );
					shown++;
				}
			}
		}
	}

	private static final int HAZE_CELLS = 8;

	private void firedampTick( Hero h, State s ){
		int c = h.pos;
		if (!gasAt( c )){
			s.pocket = NONE;
			s.inGas = s.flameTurns = 0;
			s.dizzyTold = false;
			return;
		}
		long key = pocketKeyOf( c );
		if (s.pocket == NONE){
			//he walked in: the smell, and the haze of the foul air about him
			s.pocket = key;
			s.inGas = 0;
			s.flameTurns = 0;
			say( h, GLog.WARNING, "firedamp_smell" );
			stop( h );
			haze( c, 2 );
			if (flame( h )) warnFlame( h, s );
			return;
		}
		s.pocket = key;
		s.inGas++;
		haze( c, 1 );
		if (flame( h )){
			if (s.flameTurns == 0){
				warnFlame( h, s );
			//a turn after the warning it goes up - unless he is held fast (rooted, frozen): then
			//it waits for a turn he could have carried his flame away in
			} else if (canMove( h )){
				final int wx = level.worldX + c % level.width(), wy = level.worldY + c / level.width();
				soon( 0f, () -> torchStrike( h, wx, wy ) );
			}
		} else {
			s.flameTurns = 0;
		}
		if (s.inGas >= LayerHazards.DIZZY_TURNS){
			Buff.prolong( h, Vertigo.class, 3f );
			if (!s.dizzyTold){
				s.dizzyTold = true;
				say( h, GLog.WARNING, "firedamp_dizzy" );
			}
		}
	}

	//his flame in the gas: he is told, and so is anyone near enough to be caught by it
	private void warnFlame( Hero h, State s ){
		s.flameTurns = 1;
		say( h, GLog.WARNING, "firedamp_flame" );
		stop( h );
		for (Hero o : warned( h.pos )){
			if (o != h) say( o, GLog.WARNING, "firedamp_hiss" );
		}
	}

	//the gas joined to a gas cell (any block it lies in): the pocket that goes up with it
	private ArrayList<Integer> pocket( int origin ){
		int w = level.width();
		ArrayList<Integer> cells = new ArrayList<>();
		HashSet<Integer> seen = new HashSet<>();
		ArrayDeque<Integer> todo = new ArrayDeque<>();
		todo.add( origin );
		seen.add( origin );
		while (!todo.isEmpty()){
			int c = todo.poll();
			cells.add( c );
			for (int d : PathFinder.NEIGHBOURS8){
				int n = c + d;
				if (n < 0 || n >= level.length() || Math.abs( n % w - c % w ) > 1 || seen.contains( n )) continue;
				seen.add( n );
				if (gasAt( n )) todo.add( n );
			}
		}
		return cells;
	}

	//what a pocket's blast reaches: its gas and the open cells round it
	private HashSet<Integer> blastOf( ArrayList<Integer> cells ){
		HashSet<Integer> blast = new HashSet<>( cells );
		for (int c : cells){
			for (int d : PathFinder.NEIGHBOURS8){
				int n = c + d;
				if (n >= 0 && n < level.length() && !level.solid[n]) blast.add( n );
			}
		}
		return blast;
	}

	//who hears a pocket about to go up: every hero within earshot of the flame, and every one
	//the blast would reach, however far the pocket runs
	private ArrayList<Hero> warned( int gasCell ){
		ArrayList<Hero> out = heroesNear( gasCell, 12 );
		HashSet<Integer> blast = blastOf( pocket( gasCell ) );
		for (Hero h : OverworldLevel.heroesOn( level )){
			if (!out.contains( h ) && blast.contains( h.pos )) out.add( h );
		}
		return out;
	}

	/** A flame carried into the gas, a turn after it was warned of: up it goes if both are still there. */
	void torchStrike( Hero h, int wx, int wy ){
		int c = level.localCell( wx, wy );
		if (c == -1 || h.pos != c || !h.isAlive() || !gasAt( c ) || !flame( h )) return;
		ignite( wx, wy );
	}

	/** The world's turn on this level (OverworldLevel.hostTurn): fires on the gas, and the sweep. */
	void worldTurn(){
		if (level.isMirror()) return;
		long t = LagMonitor.begin();
		if (pocketCells.length > 0){
			if (level.tileHeat != null){
				for (int c : pocketCells){
					if (level.tileHeat[c] >= LayerHazards.IGNITE_HEAT && gasAt( c )) arm( c );
				}
			}
			for (Mob m : level.mobs.toArray( new Mob[0] )){
				if (m.buff( Burning.class ) != null && gasAt( m.pos )) arm( m.pos );
			}
		}
		if (--sweepIn <= 0){
			sweepIn = 64;
			sweep();
		}
		LagMonitor.end( "OW hazards", t );
	}

	//a fire on the gas: the flames lick, every hero near hears the hiss, and after his next
	//action the pocket goes up if the gas is still there
	private void arm( int c ){
		long key = pocketKeyOf( c );
		Strike pending = armed.get( key );
		if (pending != null && Actor.findById( pending.id() ) != null) return;
		if (live()) CellEmitter.get( c ).burst( FlameParticle.FACTORY, 6 );
		for (Hero h : warned( c )) say( h, GLog.WARNING, "firedamp_hiss" );
		final int wx = level.worldX + c % level.width(), wy = level.worldY + c / level.width();
		armed.put( key, soon( 1f, () -> heatStrike( wx, wy ) ) );
	}

	/** A fire's fuse burnt down: the gas it licked at goes up, if it is still there. */
	void heatStrike( int wx, int wy ){
		int c = level.localCell( wx, wy );
		armed.remove( LayerHazards.pocketKey( wx, wy ) );
		if (c != -1 && gasAt( c )) ignite( wx, wy );
	}

	/**
	 * The pocket the gas on this world cell belongs to goes up: the gas cells joined to it (any
	 * block) are spent for LayerHazards.REFILL world turns, everyone in or beside them is hurt
	 * (a hero by at most 35% of his health), and fire takes the gas cells nobody stands on.
	 */
	void ignite( int wx, int wy ){
		int origin = level.localCell( wx, wy );
		if (origin == -1 || !gasAt( origin )) return;
		ArrayList<Integer> cells = pocket( origin );
		int refill = WorldClock.turn() + LayerHazards.REFILL;
		for (int c : cells) spent.put( pocketKeyOf( c ), refill );

		//the blast: the gas and the cells round it
		HashSet<Integer> blast = blastOf( cells );
		int k = Math.max( 1, -level.altitude() - 3 );
		HashSet<Char> hit = new HashSet<>();
		for (int b : blast){
			Char ch = Actor.findChar( b );
			if (ch == null || !hit.add( ch )) continue;
			int dmg = Random.NormalIntRange( 3 + 2 * k, 8 + 3 * k ) - ch.drRoll();
			if (ch instanceof Hero) dmg = Math.min( dmg, Math.max( 1, Math.round( ch.HT * 0.35f ) ) );
			if (dmg > 0) ch.damage( dmg, new Firedamp() );
		}
		//the flames it leaves: never under anyone, so the blast is the whole of the hit
		for (int c : cells){
			if (!level.water[c] && Actor.findChar( c ) == null) GameScene.add( Blob.seed( c, 2, Fire.class ) );
		}
		if (live()){
			CellEmitter.center( origin ).burst( BlastParticle.FACTORY, 30 );
			for (int i = 0; i < Math.min( 40, cells.size() ); i++){
				CellEmitter.get( cells.get( i ) ).burst( BlastParticle.FACTORY, 3 );
				CellEmitter.get( cells.get( i ) ).burst( SmokeParticle.FACTORY, 2 );
			}
			Sample.INSTANCE.play( Assets.Sounds.BLAST );
			if (OverworldLevel.heroDistance( level, origin ) <= 12) PixelScene.shake( 3, 0.7f );
		}
		OverworldCritters.noise( origin );
		for (Hero h : heroesNear( origin, 12 )) say( h, GLog.NEGATIVE, "firedamp_ignite" );
	}

	// --------------------------------------------------------------- rockfall

	//where a fall may land: open floor, not a way between slices or beside one, no pit, no
	//water, no ice, nothing of a place's (CaveSites: a camp, a tomb's door, a forge) nor a stone
	//nothing breaks
	private boolean canFall( int c ){
		if (!level.insideMap( c ) || !level.passable[c] || level.pit[c] || level.water[c]) return false;
		if (level.inSiteReach( c ) || level.unbreakable( c )) return false;
		int t = level.map[c];
		if (t == Terrain.ENTRANCE || t == Terrain.EXIT || t == Terrain.FROZEN_WATER) return false;
		int w = level.width();
		for (int n : new int[]{ c, c - w, c + 1, c + w, c - 1 }){
			if (level.linkAt( n ) != WindowGenerator.LINK_NONE) return false;
		}
		return true;
	}

	/** Hero.actMine, once the rock is gone (`was` the terrain it was): a careless blow may bring the roof down. */
	public void mined( Hero h, int cell, int was ){
		if (level.isMirror() || Dungeon.level != level || level.altitude() >= 0) return;
		if (was == Terrain.MINE_BOULDER || (Terrain.flags[was] & Terrain.SOLID) == 0) return;
		if (!LayerHazards.openCell( level.map[cell] )) return;
		int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
		boolean loose = LayerHazards.looseRock( level.worldSeed, wx, wy, level.altitude() );
		boolean unsup = LayerHazards.unsupported( level.map, level.width(), cell );
		if (Random.Float() < LayerHazards.collapseChance( loose, unsup, level.altitude() )){
			//a slow miner gets longer to step away (as GnollGeomancer's rockfall does)
			collapse( h, cell, unsup, GameMath.gate( Actor.TICK, (float) Math.ceil( 1f / h.speed() ), 3 * Actor.TICK ) );
		}
	}

	//could he step anywhere at all?
	private static boolean canMove( Char ch ){
		return ch.paralysed <= 0 && ch.buff( Roots.class ) == null && ch.buff( Frost.class ) == null;
	}

	/**
	 * The roof over a freshly mined cell starts to come down: the cell and some of its
	 * neighbours (all of them when it lost its props) are marked, and the rocks fall once the
	 * miner has had his next action - always with a free cell beside him to step to, and never
	 * on him at all when he cannot move. `delay`: the turns until they fall, at least one.
	 */
	void collapse( Hero h, int dst, boolean unsup, float delay ){
		ArrayList<Integer> cells = new ArrayList<>();
		if (canFall( dst )) cells.add( dst );
		for (int d : PathFinder.NEIGHBOURS8){
			int c = dst + d;
			if (canFall( c ) && (unsup || Random.Int( 2 ) == 0)) cells.add( c );
		}
		//a way out, whatever falls
		if (!canMove( h )){
			cells.remove( (Integer) h.pos );
		} else {
			boolean free = false;
			int farthest = -1;
			for (int d : PathFinder.NEIGHBOURS8){
				int c = h.pos + d;
				if (c < 0 || c >= level.length() || !level.passable[c] || level.pit[c] || Actor.findChar( c ) != null) continue;
				if (!cells.contains( c )){
					free = true;
					break;
				}
				if (farthest == -1 || level.trueDistance( c, dst ) > level.trueDistance( farthest, dst )) farthest = c;
			}
			if (!free){
				if (farthest != -1) cells.remove( (Integer) farthest );
				else cells.remove( (Integer) h.pos );
			}
		}
		if (cells.isEmpty()) return;

		CaveIn fall = Buff.append( h, CaveIn.class, delay / 2f );
		fall.rest = delay / 2f;
		fall.depth = Dungeon.depth;
		fall.deep = -level.altitude() - 1;
		fall.setRockPositions( cells );

		//everyone in the way, or near it, is told
		for (Hero o : OverworldLevel.heroesOn( level )){
			boolean under = cells.contains( o.pos ) || level.distance( o.pos, dst ) <= 2;
			if (o == h || under) say( o, GLog.WARNING, "ceiling_groans" );
		}
		if (live()){
			for (int c : cells){
				GameScene.targetedCell( c, delay );
				CellEmitter.get( c ).burst( EarthParticle.FALLING, 4 );
			}
			Sample.INSTANCE.play( Assets.Sounds.ROCKS, 0.5f, 0.7f );
			PixelScene.shake( 1f, 0.6f );
		}
		OverworldCritters.noise( dst );
	}

	/** OverworldLevel.rebase: the rocks about to fall follow their ground. */
	void rebased( int dx, int dy, ArrayList<Hero> netHeroes ){
		ArrayList<Hero> all = new ArrayList<>( netHeroes );
		if (Dungeon.hero != null) all.add( Dungeon.hero );
		for (Hero h : all){
			for (CaveIn c : h.buffs( CaveIn.class )) c.translate( dx, dy, level.width(), level.height() );
		}
	}

	// --------------------------------------------------------------- thin ice

	/** OverworldLevel.occupyCell: a hero set foot on a cell. Thin ice cracks; a crack gives way. */
	public void stepped( Hero h ){
		//a danger of the slices above and below: the surface's rivers and lakes bear anyone
		if (level.isMirror() || Dungeon.level != level || h.flying || level.altitude() == 0) return;
		int c = h.pos;
		//a hot spring's pool never freezes thin, and a place's own cells (MountainSites) bear him
		if (c < 0 || c >= level.length() || level.map[c] != Terrain.FROZEN_WATER || level.inTown( c )
				|| level.hotSpring( c ) || level.inSiteReach( c )) return;
		long key = keyOf( c );
		if (cracked.containsKey( key )){
			breakIce( c, key );
			return;
		}
		if (!LayerHazards.thinIce( level.map, level.width(), c, ambientAt( c ) )) return;
		cracked.put( key, WorldClock.turn() );
		publish();
		if (live()){
			Splash.at( c, 0xE8F4FA, 4 );
			Sample.INSTANCE.play( Assets.Sounds.SHATTER, 0.6f, 0.6f );
		}
		State s = state( h );
		int turn = WorldClock.turn();
		if (s.lastCrackLog == Integer.MIN_VALUE || turn - s.lastCrackLog >= 10 || turn < s.lastCrackLog){
			s.lastCrackLog = turn;
			say( h, GLog.WARNING, "ice_crack" );
			//a march to the map's waypoint keeps going: the log is warning enough
			if (!(h == Dungeon.hero && level.waypointMarching)) stop( h );
		}
	}

	private void iceTick( Hero h, State s ){
		if (level.altitude() == 0) return;
		int c = h.pos;
		long key = keyOf( c );
		if (!h.flying && level.map[c] == Terrain.FROZEN_WATER && cracked.containsKey( key )){
			if (s.strainKey != key){
				s.strainKey = key;
				s.strain = 0;
			}
			//held fast, he could not step off: the ice waits for a turn he could, and says
			//nothing more meanwhile
			if (!canMove( h )) return;
			s.strain++;
			if (s.strain == LayerHazards.STRAIN_GROAN){
				say( h, GLog.WARNING, "ice_groan" );
				stop( h );
				publish();
				//the dark water wells up through the breaks, round him where he stands
				if (live()){
					Splash.at( c, 0x1E3C54, 8 );
					GameScene.ripple( c );
					Sample.INSTANCE.play( Assets.Sounds.SHATTER, 0.4f, 0.5f );
				}
			} else if (s.strain >= LayerHazards.STRAIN_BREAK){
				final int wx = level.worldX + c % level.width(), wy = level.worldY + c / level.width();
				soon( 0f, () -> iceStrike( h, wx, wy ) );
			}
		} else if (s.strainKey != NONE){
			boolean wasGroaning = s.strain >= LayerHazards.STRAIN_GROAN;
			s.strainKey = NONE;
			s.strain = 0;
			if (wasGroaning) publish();
		}
	}

	/** The groaning ice gives, if he still stands on it. */
	void iceStrike( Hero h, int wx, int wy ){
		int c = level.localCell( wx, wy );
		if (c == -1 || h.pos != c || level.map[c] != Terrain.FROZEN_WATER) return;
		long key = OverworldLevel.worldKey( wx, wy );
		if (cracked.containsKey( key )) breakIce( c, key );
	}

	//the ice is gone: open water (a diff, which a hard frost or the season closes again), and
	//whoever stood on it in the freezing water
	private void breakIce( int c, long key ){
		cracked.remove( key );
		for (State s : states.values()){
			if (s.strainKey == key){
				s.strainKey = NONE;
				s.strain = 0;
			}
		}
		publish();
		Level.set( c, Terrain.WATER, level );
		GameScene.updateMap( c );
		if (live()){
			Splash.at( c, 0xCFE3EE, 10 );
			GameScene.ripple( c );
			Sample.INSTANCE.play( Assets.Sounds.WATER );
			Sample.INSTANCE.play( Assets.Sounds.SHATTER, 0.8f, 0.8f );
		}
		Char ch = Actor.findChar( c );
		if (ch == null || ch.flying) return;
		Buff.prolong( ch, Drenched.class, Drenched.DURATION * 2 );
		if (ch instanceof Hero){
			Buff.affect( ch, SoakedShoes.class ).add( SoakedShoes.STEP_DURATION );
			((Hero) ch).coldGrace = 0;
			say( (Hero) ch, GLog.NEGATIVE, "ice_break" );
		}
		if (!Float.isNaN( ch.bodyTemp )) ch.bodyTemp -= LayerHazards.PLUNGE_CHILL;
	}

	//cracks heal and pockets fill again with time; a crack whose ice is gone is forgotten
	private void sweep(){
		int turn = WorldClock.turn();
		boolean changed = false;
		spent.values().removeIf( until -> turn >= until );
		java.util.Iterator<Map.Entry<Long, Integer>> it = cracked.entrySet().iterator();
		while (it.hasNext()){
			Map.Entry<Long, Integer> e = it.next();
			long k = e.getKey();
			boolean standing = false;
			for (State s : states.values()) standing |= s.strainKey == k;
			int c = level.localCell( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) );
			boolean gone = c != -1 && level.map[c] != Terrain.FROZEN_WATER;
			boolean healed = !standing && (turn - e.getValue() >= LayerHazards.CRACK_HEAL || turn < e.getValue());
			if (gone || healed){
				it.remove();
				changed = true;
			}
		}
		if (changed) publish();
	}

	// ----------------------------------------------------------------- gusts

	//hero turns after a gust before the next can come
	static final int GUST_QUIET = 3;

	private void gustTick( Hero h, State s, float roll ){
		//counted in his turns, not by the actor clock (Actor.fixTime turns that back on every save)
		if (s.quiet > 0){
			s.quiet--;
			return;
		}
		//a place of the heights (a waystation, a tower) stands where the wind is broken
		if (!LayerHazards.ridgeCell( level.map, level.width(), h.pos ) || level.inSiteReach( h.pos )) return;
		if (roll >= LayerHazards.gustChance( level.altitude(), ClimateManager.localWindSpeed() )) return;
		final int dir = LayerHazards.windDir( ClimateManager.surfaceWindDir() );
		say( h, GLog.WARNING, "gust_warn" );
		stop( h );
		if (live()){
			int card = ((dir + 1) / 2) % 4;
			CellEmitter.get( h.pos ).burst( DraftParticle.GUSTS[card], 10 );
			int up = h.pos - LayerHazards.DX[dir] - LayerHazards.DY[dir] * level.width();
			if (level.insideMap( up )) CellEmitter.get( up ).burst( DraftParticle.GUSTS[card], 4 );
		}
		s.quiet = GUST_QUIET;
		soon( 0f, () -> gustStrike( h, dir ) );
	}

	/**
	 * The gust warned of hits, after the hero's action: still on the ridge, he is pushed a cell
	 * downwind (or 45 degrees off it) - never into the drop, never onto anyone or onto a crack -
	 * or, with nowhere to go, staggers where he stands. Off the ridge it howls past.
	 */
	void gustStrike( Hero h, int dir ){
		if (level.isMirror() || !h.isAlive() || Dungeon.level != level) return;
		if (!LayerHazards.ridgeCell( level.map, level.width(), h.pos )) return;
		say( h, GLog.WARNING, "gust_hit" );
		//held fast (rooted, frozen) he is not moved at all
		int to = !canMove( h ) ? -1 : LayerHazards.gustTarget( level.map, level.width(), h.pos, dir,
				c -> Actor.findChar( c ) != null || crackedAt( c ) );
		if (to == -1){
			Buff.prolong( h, Daze.class, 2f );
			say( h, GLog.WARNING, "gust_brace" );
		} else {
			int from = h.pos, ox = level.worldX, oy = level.worldY;
			h.move( to, false );
			//the push may have slid the window (occupyCell): then the old cells name other ground
			if (h.sprite != null){
				if (level.worldX == ox && level.worldY == oy) Actor.add( new Pushing( h, from, h.pos ) );
				else h.sprite.place( h.pos );
			}
		}
		stop( h );
	}

	// ----------------------------------------------------------------- saves

	private static final String SPENT_KEYS = "hz_spent_keys", SPENT_UNTIL = "hz_spent_until";
	private static final String CRACK_KEYS = "hz_crack_keys", CRACK_AT = "hz_crack_at";

	void storeInBundle( Bundle bundle ){
		long[] sk = new long[spent.size()];
		int[] su = new int[sk.length];
		int i = 0;
		for (Map.Entry<Long, Integer> e : spent.entrySet()){
			sk[i] = e.getKey();
			su[i++] = e.getValue();
		}
		bundle.put( SPENT_KEYS, sk );
		bundle.put( SPENT_UNTIL, su );
		long[] ck = new long[cracked.size()];
		int[] ca = new int[ck.length];
		i = 0;
		for (Map.Entry<Long, Integer> e : cracked.entrySet()){
			ck[i] = e.getKey();
			ca[i++] = e.getValue();
		}
		bundle.put( CRACK_KEYS, ck );
		bundle.put( CRACK_AT, ca );
	}

	void restoreFromBundle( Bundle bundle ){
		spent.clear();
		cracked.clear();
		if (bundle.contains( SPENT_KEYS ) && bundle.contains( SPENT_UNTIL )){
			long[] k = bundle.getLongArray( SPENT_KEYS );
			int[] v = bundle.getIntArray( SPENT_UNTIL );
			for (int i = 0; i < Math.min( k.length, v.length ); i++) spent.put( k[i], v[i] );
		}
		if (bundle.contains( CRACK_KEYS ) && bundle.contains( CRACK_AT )){
			long[] k = bundle.getLongArray( CRACK_KEYS );
			int[] v = bundle.getIntArray( CRACK_AT );
			for (int i = 0; i < Math.min( k.length, v.length ); i++) cracked.put( k[i], v[i] );
		}
		publish();
	}

	// --------------------------------------------------------- network guests

	/** Co-op host: what a guest's cracks are drawn from changed when this did. Any thread. */
	long sharedSig(){
		return shown.version;
	}

	/** Co-op host: the cracks as his guests see them, a bundle's text. Any thread (the snapshot). */
	String shared(){
		Cracks c = shown;
		Bundle b = new Bundle();
		int[] stages = new int[c.stages.length];
		for (int i = 0; i < stages.length; i++) stages[i] = c.stages[i];
		b.put( "keys", c.keys );
		b.put( "stages", stages );
		b.put( "ver", c.version );
		return b.toString();
	}

	/** A guest's mirror takes over its host's cracks. */
	void adoptShared( Bundle b ){
		long[] keys = b.contains( "keys" ) ? b.getLongArray( "keys" ) : new long[0];
		int[] stages = b.contains( "stages" ) ? b.getIntArray( "stages" ) : new int[0];
		int n = Math.min( keys.length, stages.length );
		byte[] st = new byte[n];
		for (int i = 0; i < n; i++) st[i] = (byte) stages[i];
		shown = new Cracks( java.util.Arrays.copyOf( keys, n ), st, b.contains( "ver" ) ? b.getInt( "ver" ) : shown.version + 1 );
	}
}
