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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndCraft;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A station the hero builds (homestead/pieces/StationPieces): furniture that works. It stands where
 * it was built, nothing moves it, it takes no buffs, and no blow hurts it - a built station is
 * hurt only through the homesteads' own damage (Homestead.damage: bombs, monsters breaking in,
 * fire on a wooden one), against its piece's health, and dropped where it stood when it breaks or
 * is taken down: what it holds lands on its cell.
 *
 * Tapped, it opens its window (WndCraft): the hero queues up to MAX_JOBS jobs of its recipes
 * (StationRecipes), each up to MAX_BATCH batches, everything for them taken at once; the station
 * works through them in order, a batch at a time, and puts what each batch makes in its output for
 * the hero to collect. Work runs on the global clock (FarmCrop.clock), so a station goes on
 * working while the hero is away - on another slice, down in the dungeon, or with the window moved
 * off it - and catches up the moment it acts again (catchUp). In a WORKSHOP (StationRooms: a
 * workbench and another station in one room) work goes WORKSHOP_SPEED times as fast.
 *
 * Host only: a co-op guest who taps it is told so; what he sees of it (hard at work, its fire lit)
 * reaches him with the mob (StateSerializer's "wk"). The world's stations are kept for good
 * (HomesteadActor) and saved with their slice, jobs, output and all.
 */
public abstract class HomesteadStation extends NPC implements HomesteadActor {

	//each station names its own sprite (StationSprite's kinds), so a co-op guest, who is sent only
	//the sprite's name, draws the right one
	{
		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	/** Jobs queued at once, at most. */
	public static final int MAX_JOBS = 4;
	/** Batches in one job, at most. */
	public static final int MAX_BATCH = 20;
	/** How much faster the stations of a workshop work. */
	public static final float WORKSHOP_SPEED = 1.25f;

	private final ArrayList<StationJob> jobs = new ArrayList<>();
	private final ArrayList<Item> output = new ArrayList<>();
	//the global clock when the station last caught up
	private float lastClock = FarmCrop.clock();
	//what its sprite shows: hard at work (a fire lit, steam over the pot, a hide on the rack)
	protected volatile boolean busy;
	//taken down or wrecked: nothing more goes into it (an open window may still point at it)
	private volatile boolean gone;
	//the workshop's speed, kept while the room and the register's stations are as they were
	private Rooms.Room speedRoom;
	private int speedChanges = -1;
	private float speedKept = 1f;

	/** Why a job could not be queued; OK when it was. */
	public enum Craft {
		OK, QUEUE_FULL, BAD_BATCH, NOT_HERE, NO_MATERIALS, GONE;

		public String message(){
			return Messages.get( HomesteadStation.class, "craft_" + name().toLowerCase( java.util.Locale.ENGLISH ) );
		}
	}

	// ----------------------------------------------------------------- its look

	/** Its frame on sprites/homestead.png (HomesteadSprites) at rest: its window's picture (its
	 *  sprite, StationSprite's kind for it, draws the same frame). */
	public abstract int frame();

	/** Is it hard at work, as its sprite shows: read on the render thread. */
	public boolean busy(){
		return busy;
	}

	/** What its sprite shows as work: by default, a job it is getting on with. */
	protected boolean looksBusy(){
		return !jobs.isEmpty() && canWork( jobs.get( 0 ).recipe() );
	}

	// ----------------------------------------------------------------- what it makes

	/** Its recipes, in the window's order. */
	public List<StationRecipe> recipes(){
		return StationRecipes.forStation( this );
	}

	/** Can it get on with this recipe now (a recipe that needs heat waits for a lit fire). */
	protected boolean canWork( StationRecipe r ){
		return r != null && !r.needsHeat;
	}

	/** The heat it gives the room it stands in, C (StationHeat). */
	public float roomHeat(){
		return 0f;
	}

	/** Does it anchor a home now, no wild animal set down near it (StationAnchors)? */
	public boolean anchorsHome(){
		return false;
	}

	/** Until when on the global clock (FarmCrop.clock) its roomHeat and anchorsHome hold without it
	 *  acting again: for good, unless a fire gives them (HeatStation: until its fuel is burnt). */
	public float givesUntil(){
		return Float.POSITIVE_INFINITY;
	}

	/** Does it still stand on the level shown, to be worked? False once it was taken down or
	 *  wrecked, even while a window opened on it is still up. */
	public boolean standing(){
		return !gone && Dungeon.level != null && Dungeon.level.mobs.contains( this );
	}

	/** The jobs queued, in order (the first is the one in hand). */
	public synchronized List<StationJob> jobs(){
		return new ArrayList<>( jobs );
	}

	/** What it has made and holds for the hero. */
	public synchronized List<Item> output(){
		return new ArrayList<>( output );
	}

	/** Is it getting on with a job now? */
	public synchronized boolean working(){
		return !jobs.isEmpty() && canWork( jobs.get( 0 ).recipe() );
	}

	/** Has it any work queued? */
	public synchronized boolean hasJobs(){
		return !jobs.isEmpty();
	}

	/**
	 * The hero queues `batches` batches of a recipe: everything for them is taken from him now
	 * (`chosen` is the class he picked for a chosen input, null for none). Returns why not, having
	 * taken nothing, when it cannot.
	 */
	public synchronized Craft queue( Hero hero, StationRecipe r, int batches, Class<? extends Item> chosen ){
		if (!standing()) return Craft.GONE;
		if (r == null || !r.station.isInstance( this )) return Craft.NOT_HERE;
		if (batches < 1 || batches > MAX_BATCH) return Craft.BAD_BATCH;
		if (jobs.size() >= MAX_JOBS) return Craft.QUEUE_FULL;
		if (r.chosenInput() != null && chosen == null) return Craft.NO_MATERIALS;
		ArrayList<Item> held = r.take( hero, batches, chosen );
		if (held == null) return Craft.NO_MATERIALS;
		jobs.add( new StationJob( r.id, batches, held ) );
		busy = looksBusy();
		return Craft.OK;
	}

	/** The hero calls off a queued job: what it still holds goes back to him (the work done on
	 *  the batch in hand is lost). False when there is no such job. */
	public synchronized boolean cancel( int index, Hero hero ){
		if (index < 0 || index >= jobs.size()) return false;
		StationJob j = jobs.remove( index );
		for (Item i : j.held) give( hero, i );
		busy = looksBusy();
		return true;
	}

	/** The hero takes everything it has made; how many stacks. */
	public synchronized int takeAll( Hero hero ){
		int n = output.size();
		for (Item i : output) give( hero, i );
		output.clear();
		return n;
	}

	//an item into the hero's pack, or at his feet when it is full
	static void give( Hero hero, Item item ){
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
	}

	// ----------------------------------------------------------------- the work

	@Override
	protected boolean act(){
		Level l = Dungeon.level;
		if (l instanceof OverworldLevel && !((OverworldLevel) l).isMirror() && l.mobs.contains( this )){
			OverworldLevel ow = (OverworldLevel) l;
			catchUp( ow );
			liveTurn( ow );
			//a fire on its cell may have taken it (liveTurn)
			if (!ow.mobs.contains( this )) return true;
			sync( ow );
		}
		spend( TICK );
		return true;
	}

	/** The global clock (FarmCrop.clock) the station has caught up to. */
	protected final float caughtUpTo(){
		return lastClock;
	}

	/**
	 * Brings the station up to the global clock: every turn since it last did, the hero there or
	 * not (OverworldLevel's window off it, another slice, the dungeon) - its fuel burnt and its jobs
	 * worked through, at the speed its room gives it now.
	 */
	public final void catchUp( OverworldLevel level ){
		float now = FarmCrop.clock();
		float dt = now - lastClock;
		lastClock = now;
		if (dt <= 0f) return;
		//the room is looked at only for work to speed up (outside the lock: the room's traits
		//read the slice's register)
		float speed = hasJobs() ? speed( level ) : 1f;
		synchronized (this){
			advance( level, dt, speed );
			busy = looksBusy();
		}
	}

	/** `dt` turns pass at the station, its work at `speed`. A station with a fire burns it here
	 *  (HeatStation). Holds the station's lock. */
	protected void advance( OverworldLevel level, float dt, float speed ){
		progress( level, dt, false, speed );
	}

	/**
	 * `time` turns of work on the queue, in order, `heated` when the station's fire burned all
	 * through them: a batch that is done uses its share of its job's held items and puts what it
	 * makes in the output; a job that needs heat holds up the queue while there is none.
	 */
	protected final void progress( OverworldLevel level, float time, boolean heated, float speed ){
		float work = time * speed;
		//the batches finished, told once a recipe however many there were (a long absence caught up)
		LinkedHashMap<StationRecipe, Integer> finished = new LinkedHashMap<>();
		while (work > 0f && !jobs.isEmpty()){
			StationJob j = jobs.get( 0 );
			StationRecipe r = j.recipe();
			if (r == null){
				//a recipe gone from the game: what the job held is the hero's again
				drop( jobs.remove( 0 ) );
				continue;
			}
			if (r.needsHeat && !heated) break;
			float need = r.turns - j.progress;
			if (work < need){
				j.progress += work;
				break;
			}
			work -= need;
			j.progress = 0f;
			ArrayList<Item> used = r.useBatch( j.held );
			if (used == null){
				//it holds too little for a batch (nothing should take from it): given back
				drop( jobs.remove( 0 ) );
				continue;
			}
			for (Item made : r.produce( used )){
				made( level, made );
				store( made );
			}
			j.batches--;
			Integer n = finished.get( r );
			finished.put( r, n == null ? 1 : n + 1 );
			if (j.batches <= 0) drop( jobs.remove( 0 ) );
		}
		if (finished.isEmpty() || !seen( level )) return;
		for (Map.Entry<StationRecipe, Integer> e : finished.entrySet()){
			GLog.p( e.getValue() == 1 ? Messages.get( HomesteadStation.class, "made", name(), e.getKey().name() )
					: Messages.get( HomesteadStation.class, "made_many", name(), e.getKey().name(), e.getValue() ) );
		}
	}

	/** A batch has just made this, before it goes into the output (the cooking pot's kitchen).
	 *  Holds the station's lock. */
	protected void made( OverworldLevel level, Item item ){}

	//what a finished or broken job still holds goes to the output
	private void drop( StationJob j ){
		for (Item i : j.held) store( i );
		j.held.clear();
	}

	//an item into the output, stacked with its like
	private void store( Item item ){
		if (item == null || item.quantity() <= 0) return;
		if (item.stackable){
			for (Item o : output){
				if (o.isSimilar( item )){
					o.quantity( o.quantity() + item.quantity() );
					return;
				}
			}
		}
		output.add( item );
	}

	/**
	 * The station's speed: WORKSHOP_SPEED in a workshop (StationRooms: the register's word for the
	 * room, StationSlice.workshop), 1 elsewhere. Asked each turn it has work, it is worked out again
	 * only when its room (Rooms.at hands out a new one whenever the room changed, the window moved
	 * or it was forgotten) or the register's stations (StationSlice.changes) are not as they were.
	 */
	public float speed( OverworldLevel level ){
		Rooms.Room room = Rooms.at( level, pos );
		StationSlice s = StationSlice.of( level );
		int changes = s == null ? 0 : s.changes();
		if (speedChanges != -1 && room == speedRoom && changes == speedChanges) return speedKept;
		speedRoom = room;
		speedChanges = changes;
		speedKept = room != null && s != null && s.workshop( level, room ) ? WORKSHOP_SPEED : 1f;
		return speedKept;
	}

	/** Once a turn while the hero is on its slice: what the station does to the world round it.
	 *  (A wooden one in the flames burns down through Homestead.burn, as the wooden walls do.) */
	protected void liveTurn( OverworldLevel level ){}

	//its place in the slice's register of stations (StationSlice): the room's heat, the anchors, the workshop
	protected final void sync( OverworldLevel level ){
		StationSlice s = StationSlice.of( level );
		if (s != null) s.put( level, this );
	}

	/** As sync, on the level shown (after the hero lights or puts out its fire). */
	protected final void syncHere(){
		if (Dungeon.level instanceof OverworldLevel && Dungeon.level.mobs.contains( this )) sync( (OverworldLevel) Dungeon.level );
	}

	/** Does the hero see it now? */
	protected final boolean seen( Level level ){
		return level == Dungeon.level && level.heroFOV != null && pos >= 0 && pos < level.heroFOV.length && level.heroFOV[pos];
	}

	/** Everything it holds: the output, what the jobs hold (a heat station adds its fuel). */
	protected synchronized ArrayList<Item> contents(){
		ArrayList<Item> all = new ArrayList<>( output );
		for (StationJob j : jobs) all.addAll( j.held );
		return all;
	}

	/** Empties it: everything it held is now the caller's (not Actor.clear). */
	protected synchronized void emptyOut(){
		output.clear();
		jobs.clear();
	}

	//taken down: nothing more goes into it (once whatever is putting something in now is done:
	//queue and addFuel ask standing under the station's lock)
	private synchronized void retire(){
		gone = true;
	}

	// ----------------------------------------------------------------- what it is

	@Override
	public void destroy(){
		//taken down or wrecked (Homestead.takeDown): what it held lands where it stood
		Level l = Dungeon.level;
		if (l != null && l.mobs.contains( this ) && !(l instanceof OverworldLevel && ((OverworldLevel) l).isMirror())){
			retire();
			for (Item i : contents()){
				Heap h = l.drop( i, pos );
				if (h.sprite != null) h.sprite.drop();
			}
			emptyOut();
			if (l instanceof OverworldLevel){
				StationSlice s = StationSlice.of( l );
				if (s != null) s.remove( (OverworldLevel) l, pos );
			}
		}
		super.destroy();
	}

	@Override
	public void damage( int dmg, Object src ){
		//no blow hurts it: a built station takes damage only as a built piece (Homestead.damage)
	}

	@Override
	public void die( Object cause ){
		//nor does anything kill it: it comes down only through Homestead (taken down, wrecked)
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public boolean interact( Char c ){
		if (NetDialogs.handleNetHero( c, Messages.get( HomesteadStation.class, "net_host_only" ) )) return true;
		if (c != Dungeon.hero) return true;
		if (Dungeon.level instanceof OverworldLevel) catchUp( (OverworldLevel) Dungeon.level );
		Game.runOnRenderThread( () -> GameScene.show( new WndCraft( this ) ) );
		return true;
	}

	@Override
	public String description(){
		return Messages.get( this, "desc" ) + "\n\n" + status();
	}

	/** What it is doing, for a look at it: its job in hand and what waits to be collected. */
	public synchronized String status(){
		StringBuilder s = new StringBuilder();
		if (jobs.isEmpty()){
			s.append( Messages.get( HomesteadStation.class, "status_idle" ) );
		} else {
			StationJob j = jobs.get( 0 );
			StationRecipe r = j.recipe();
			s.append( Messages.get( HomesteadStation.class, canWork( r ) ? "status_working" : "status_waiting",
					r == null ? j.recipe : r.name(), j.batches, Math.round( j.fraction() * 100 ) ) );
		}
		if (!output.isEmpty()){
			s.append( '\n' ).append( Messages.get( HomesteadStation.class, "status_ready", output.size() ) );
		}
		return s.toString();
	}

	private static final String JOBS   = "jobs";
	private static final String OUTPUT = "output";
	private static final String CLOCK  = "clock";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		synchronized (this){
			bundle.put( JOBS, jobs );
			bundle.put( OUTPUT, output );
		}
		bundle.put( CLOCK, lastClock );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		synchronized (this){
			jobs.clear();
			for (Bundlable b : bundle.getCollection( JOBS )){
				if (b instanceof StationJob) jobs.add( (StationJob) b );
			}
			output.clear();
			for (Bundlable b : bundle.getCollection( OUTPUT )){
				if (b instanceof Item) output.add( (Item) b );
			}
			busy = looksBusy();
		}
		if (bundle.contains( CLOCK )) lastClock = bundle.getFloat( CLOCK );
	}
}
