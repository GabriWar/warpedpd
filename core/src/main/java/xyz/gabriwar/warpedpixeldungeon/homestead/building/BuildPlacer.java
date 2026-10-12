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

package xyz.gabriwar.warpedpixeldungeon.homestead.building;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.BuildGhost;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildBar;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildIcons;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBuild;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBuildInspect;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.badlogic.gdx.Input;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Scene;
import com.watabou.utils.PointF;

import java.util.List;
import java.util.Objects;

/**
 * Placing mode: the build bar (ui/BuildBar) in place of the toolbar, the ghosts over the map
 * (effects/BuildGhost), and what a tap on the map does while they are up (BuildSelection) -
 * build one piece, lay a line or a rectangle a piece at a time, take something down, mend it, or
 * look at it. One session at a time, on the local hero's screen (the host's: a guest never gets
 * here, BuildersTool).
 *
 * Every piece goes up through Homestead.place, so each spends its materials and the hero's turns
 * as building one by hand does. A line or a rectangle is built in order from where it was started,
 * a piece each time the hero is free again; it stops when a monster comes into view or the hero is
 * hurt, a tap on the map stops it too, and the log sums up what was built and what was not.
 *
 * A tap beyond reach (and not finishing a line or an area) walks the hero there as it would with
 * the bar down: on a phone it is the only way to move while building.
 *
 * Placing ends with DONE, Esc or back, and by itself when the hero dies, leaves the level, the
 * world's window moves under him (a rebase: the cells picked would point elsewhere) or any window
 * but the bar's own opens; the toolbar comes back as it was (GameScene.hideBuildBar). A game screen
 * torn down under it (a phone turned, a new game, the stairs) forgets it with everything on that
 * screen (forget).
 */
public final class BuildPlacer {

	/** A window the bar opens over itself (the menu, the inspect window, a confirmation): placing
	 *  goes on under it. Any other window ends placing. */
	public interface Owned {}

	private static BuildPlacer current;

	private final BuildersTool tool;
	private final OverworldLevel level;
	private final Scene scene;
	private final int window;
	private final BuildSelection sel = new BuildSelection();
	private final BuildBar bar;
	private final BuildGhost ghost = new BuildGhost();

	private BuildPiece piece;
	//the shape being built, a piece each time the hero is free
	private Job job;
	//the cell the pointer is over, or the last one tapped (a phone has no pointer)
	private int aim = -1;
	private PointF hover;
	//what the ghosts were laid for (all but the aim), and whether the aim moved since; the shape's
	//plan as they show it; the cells tinted, worked out once for what they were laid for
	private int laidFor;
	private boolean dirty = true, aimMoved;
	private BuildPlan plan;
	private int[] valid;

	private BuildPlacer( BuildersTool tool, OverworldLevel level ){
		this.tool = tool;
		this.level = level;
		this.scene = Game.scene();
		this.window = level.windowVersion();
		bar = new BuildBar( this );
	}

	// ----------------------------------------------------------------- the session

	/**
	 * Puts the bar up (or keeps the one up) with a piece and a mode. A null piece keeps the one
	 * chosen; a placing mode with no piece at all opens the menu to choose one. A mode the piece
	 * can't be laid in (a line of doors) falls back to one at a time.
	 */
	public static void start( BuildersTool tool, BuildPiece piece, BuildSelection.Mode mode ){
		if (!(Game.scene() instanceof GameScene) || !Homestead.allowedHere( Dungeon.level )) return;
		if (!active()){
			BuildPlacer p = new BuildPlacer( tool, (OverworldLevel) Dungeon.level );
			if (!GameScene.showBuildBar( p.bar )){
				//never shown: its key listener goes with it
				p.bar.destroy();
				return;
			}
			GameScene.effectOverFog( p.ghost );
			current = p;
		}
		if (piece != null) current.piece( piece );
		current.mode( mode );
	}

	/** Is placing on now (on this screen, this level). A session left over from another screen
	 *  is ended here. */
	public static boolean active(){
		if (current != null && !current.alive()){
			current.dispose();
			current = null;
		}
		return current != null;
	}

	/** The session on now, or null. */
	public static BuildPlacer current(){
		return active() ? current : null;
	}

	/** Ends placing. */
	public static void stop(){
		if (current != null){
			BuildPlacer p = current;
			current = null;
			p.dispose();
		}
	}

	/** GameScene.destroy: the game screen is going, the bar and the ghosts with it. Placing is
	 *  forgotten without a word (any shape being built just stops); nothing of that screen is
	 *  touched again. */
	public static void forget(){
		current = null;
	}

	/** CellSelector, for a tap on the map: true when the bar took it (it is up). right: a right
	 *  click, which lets go of the cells picked. False for a tap that walks the hero (walks). */
	public static boolean takesTap( int cell, boolean right ){
		if (!active()) return false;
		if (!right && current.walks( cell )) return false;
		current.tap( cell, right );
		return true;
	}

	private boolean alive(){
		return Game.scene() == scene && Dungeon.level == level && bar.parent != null;
	}

	private void dispose(){
		//a screen already gone (its gizmos destroyed with it): nothing of it to take down or tell
		boolean live = Game.scene() == scene && ghost.parent != null;
		if (live){
			if (job != null) finishJob( "stopped" );
			GameScene.hideBuildBar( bar );
			ghost.reset();
			ghost.killAndErase();
			ghost.destroy();
		} else {
			job = null;
		}
	}

	// ----------------------------------------------------------------- what the bar reads

	public BuildPiece piece(){
		return piece;
	}

	public BuildSelection.Mode mode(){
		return sel.mode();
	}

	public BuildersTool tool(){
		return tool;
	}

	/** A line or a rectangle shown, or a cell picked, waiting for the tap (or the Build button)
	 *  that does it. */
	public boolean pending(){
		return job == null && (sel.previewing() || sel.target() != -1);
	}

	/** Anything picked that Cancel lets go of. */
	public boolean picked(){
		return sel.anchor() != -1 || sel.target() != -1;
	}

	/** Is a line or a rectangle being built now. */
	public boolean building(){
		return job != null;
	}

	/** The bar's line of what to do next, or of what is going on. */
	public String status(){
		Hero hero = Dungeon.hero;
		int w = level.width();
		if (job != null) return Messages.get( this, "status_job", job.built, job.cells.length );
		BuildSelection.Mode m = sel.mode();
		if (m.places() && piece == null) return Messages.get( this, "status_choose" );
		switch (m){
			case SINGLE:
				return Messages.get( this, "status_single" );
			case LINE:
			case AREA: {
				String what = m == BuildSelection.Mode.LINE ? "line" : "area";
				if (sel.anchor() == -1) return Messages.get( this, "status_" + what + "_start" );
				if (!sel.previewing()) return Messages.get( this, "status_" + what + "_end" );
				if (sel.tooBig( w )) return Messages.get( this, "status_too_big", sel.count( sel.end(), w ), BuildShapes.MAX_CELLS );
				BuildPlan p = plan != null ? plan : BuildPlan.of( hero, piece, sel.cells( w, level.height() ) );
				if (p.affordable == 0) return refusal( p ).message();
				return Messages.get( this, "status_plan", p.affordable, p.valid );
			}
			case DEMOLISH: {
				if (sel.target() == -1) return Messages.get( this, "status_demolish" );
				BuildPiece p = Homestead.pieceAt( level, sel.target() );
				BuildPiece roof = BuildCatalog.forRoof( Homestead.roofAt( level, sel.target() ) );
				if (p == null && roof == null) return Messages.get( this, "status_rubble" );
				BuildPiece what = p != null ? p : roof;
				List<BuildPiece.Cost> back = Homestead.refundOf( what );
				return back.isEmpty() ? Messages.get( this, "status_demolish_none", what.name() )
						: Messages.get( this, "status_demolish_target", what.name(), BuildInfo.costs( back ) );
			}
			case REPAIR: {
				if (sel.target() == -1) return Messages.get( this, "status_repair" );
				BuildPiece what = Homestead.repairTarget( level, sel.target() );
				if (what == null) return Messages.get( this, "status_repair" );
				return Messages.get( this, "status_repair_target", what.name(), BuildInfo.costs( Homestead.repairCost( what ) ) );
			}
			default:
				return Messages.get( this, "status_inspect" );
		}
	}

	/** How many cells of the line or area shown take the piece (its cost is shown for all of
	 *  them); 0 with none shown. */
	public int planned(){
		return sel.previewing() && plan != null ? plan.valid : 0;
	}

	/** How many of the chosen piece the hero's materials pay for (Integer.MAX_VALUE: it is free). */
	public int affordable(){
		return piece == null ? 0 : BuildPlan.affordableCount( piece, mat -> Material.count( Dungeon.hero, mat ) );
	}

	// ----------------------------------------------------------------- what the bar does

	/** Chooses the piece to build; a mode it can't be laid in becomes one at a time. */
	public void piece( BuildPiece p ){
		piece = p;
		sel.outline( BuildShapes.areaIsOutline( p ) );
		BuildSelection.Mode m = sel.mode();
		if (!m.places() || (m == BuildSelection.Mode.LINE && !p.canLine) || (m == BuildSelection.Mode.AREA && !p.canArea)){
			sel.mode( BuildSelection.Mode.SINGLE );
		} else {
			sel.clear();
		}
		dirty();
	}

	/** Switches mode. A placing mode with no piece chosen opens the menu instead; a line or an
	 *  area of a piece that can't be laid so is refused with a word. */
	public void mode( BuildSelection.Mode m ){
		if (m == null) return;
		if (job != null) finishJob( "stopped" );
		if (m.places() && piece == null){
			menu();
			return;
		}
		if ((m == BuildSelection.Mode.LINE && !piece.canLine) || (m == BuildSelection.Mode.AREA && !piece.canArea)){
			GLog.w( Messages.get( this, m == BuildSelection.Mode.LINE ? "no_line" : "no_area", piece.name() ) );
			return;
		}
		sel.mode( m );
		dirty();
	}

	/** Opens the build menu over the bar. */
	public void menu(){
		GameScene.show( new WndBuild( tool ) );
	}

	/** Builds the shape shown, or takes down / mends the cell picked (the bar's Build button). */
	public void confirm(){
		if (job != null) return;
		if (sel.previewing()) startJob();
		else if (sel.target() != -1) act( sel.target() );
	}

	/** Lets go of what was picked (the bar's Cancel button); stops a shape being built. */
	public void cancel(){
		if (job != null) finishJob( "stopped" );
		sel.clear();
		dirty();
	}

	/** A key on the bar: 1 to 8 the Recent pieces, L line, A area, D demolish, R repair, I
	 *  inspect, Enter to build what is shown, Esc (or the back key) to stop. True when taken. */
	public boolean key( KeyEvent event ){
		if (!event.pressed) return false;
		if (KeyBindings.getActionForKey( event ) == WPDAction.BACK){
			//on the next frame, out of the key's own dispatch to the bar
			Game.runOnRenderThread( BuildPlacer::stop );
			return true;
		}
		int code = event.code;
		if (code >= Input.Keys.NUM_1 && code <= Input.Keys.NUM_8){
			List<BuildPiece> recent = BuildRecent.pieces( tool.recent );
			int i = code - Input.Keys.NUM_1;
			if (i < recent.size()){
				piece( recent.get( i ) );
				mode( BuildSelection.Mode.SINGLE );
			}
			return true;
		}
		switch (code){
			case Input.Keys.L: mode( BuildSelection.Mode.LINE ); return true;
			case Input.Keys.A: mode( BuildSelection.Mode.AREA ); return true;
			case Input.Keys.D: mode( BuildSelection.Mode.DEMOLISH ); return true;
			case Input.Keys.R: mode( BuildSelection.Mode.REPAIR ); return true;
			case Input.Keys.I: mode( BuildSelection.Mode.INSPECT ); return true;
			case Input.Keys.ENTER:
			case Input.Keys.NUMPAD_ENTER:
				confirm();
				return true;
			default:
				return false;
		}
	}

	// ----------------------------------------------------------------- taps

	/**
	 * Does a tap on this cell walk the hero there instead (left to the map's own listener): it is
	 * beyond Homestead.RANGE, nothing is being built, and it neither finishes a line or an area
	 * already started nor is a look (INSPECT looks anywhere). A cell picked to take down or mend
	 * is let go of.
	 */
	boolean walks( int cell ){
		Hero hero = Dungeon.hero;
		if (job != null || hero == null || level.distance( hero.pos, cell ) <= Homestead.RANGE) return false;
		BuildSelection.Mode m = sel.mode();
		if (m == BuildSelection.Mode.INSPECT || (m.shaped() && sel.anchor() != -1)) return false;
		if (sel.target() != -1){
			sel.clear();
			dirty();
		}
		return true;
	}

	private void tap( int cell, boolean right ){
		if (right){
			cancel();
			return;
		}
		if (job != null){
			finishJob( "stopped" );
			return;
		}
		aim = cell;
		if (sel.mode().places() && piece == null){
			menu();
			return;
		}
		switch (sel.tap( cell, level.width(), level.height() )){
			case PLACE:
				placeOne( cell );
				break;
			case CONFIRM:
				confirm();
				break;
			case TARGET:
				target( cell );
				break;
			case INSPECT:
				GameScene.show( new WndBuildInspect( cell ) );
				break;
			default:
				break;
		}
		dirty();
	}

	//one piece on a cell, now
	private void placeOne( int cell ){
		Hero hero = Dungeon.hero;
		if (!hero.ready) return;
		Homestead.Reason r = Homestead.place( hero, cell, piece );
		if (r.ok()) tool.used( piece.id );
		else toast( cell, r.message() );
	}

	//a cell picked to take down or mend: refused at once when it can't be, its cost told otherwise
	private void target( int cell ){
		Hero hero = Dungeon.hero;
		Homestead.Reason r = sel.mode() == BuildSelection.Mode.DEMOLISH
				? Homestead.canRemove( hero, cell ) : Homestead.canRepair( hero, cell );
		if (!r.ok()){
			toast( cell, r.message() );
			sel.clear();
		}
	}

	//takes down or mends the cell picked; a station (and whatever holds things) asks first
	private void act( int cell ){
		Hero hero = Dungeon.hero;
		if (!hero.ready) return;
		if (sel.mode() == BuildSelection.Mode.REPAIR){
			Homestead.Reason r = Homestead.repair( hero, cell );
			if (!r.ok()) toast( cell, r.message() );
			sel.clear();
			dirty();
			return;
		}
		Homestead.Reason r = Homestead.canRemove( hero, cell );
		if (!r.ok()){
			toast( cell, r.message() );
			sel.clear();
			dirty();
			return;
		}
		BuildPiece p = Homestead.pieceAt( level, cell );
		if (p != null && (p.kind == BuildPiece.Kind.ACTOR || p.confirmRemoval)){
			GameScene.show( new ConfirmDemolish( cell, p ) );
			return;
		}
		remove( cell );
	}

	private void remove( int cell ){
		Hero hero = Dungeon.hero;
		if (!hero.ready) return;
		Homestead.Reason r = Homestead.remove( hero, cell );
		if (!r.ok()) toast( cell, r.message() );
		sel.clear();
		dirty();
	}

	//asks before a station or a store comes down, its contents with it; a piece with a warning of
	//its own (BuildPiece.confirmRemoval: a hearthstone, its home going with it) asks that instead
	private class ConfirmDemolish extends WndOptions implements Owned {

		private final int cell;

		ConfirmDemolish( int cell, BuildPiece p ){
			super( BuildIcons.DEMOLISH.get(), Messages.titleCase( p.name() ),
					p.confirmRemoval ? p.removalWarning() : Messages.get( BuildPlacer.class, "confirm_station", p.name() ),
					Messages.get( BuildPlacer.class, "confirm_yes" ),
					Messages.get( BuildPlacer.class, "confirm_no" ) );
			this.cell = cell;
		}

		@Override
		protected void onSelect( int index ){
			if (index == 0 && current == BuildPlacer.this) BuildPlacer.this.remove( cell );
			else {
				sel.clear();
				dirty();
			}
		}
	}

	// ----------------------------------------------------------------- a shape, a piece at a time

	private static final class Job {
		final BuildPiece piece;
		final int[] cells;
		final int enemies;
		//where the hero stood when it was planned: every cell was in his sight from there
		final int from;
		int at, built, skipped, shortOf, hp;

		Job( BuildPiece piece, int[] cells, int skipped, int shortOf, int enemies, int hp, int from ){
			this.piece = piece;
			this.cells = cells;
			this.skipped = skipped;
			this.shortOf = shortOf;
			this.enemies = enemies;
			this.hp = hp;
			this.from = from;
		}
	}

	private void startJob(){
		Hero hero = Dungeon.hero;
		if (!hero.ready) return;
		int w = level.width();
		if (sel.tooBig( w )){
			toast( sel.end(), Messages.get( this, "status_too_big", sel.count( sel.end(), w ), BuildShapes.MAX_CELLS ) );
			return;
		}
		BuildPlan p = BuildPlan.of( hero, piece, sel.cells( w, level.height() ) );
		if (p.affordable == 0){
			toast( sel.end(), refusal( p ).message() );
			return;
		}
		job = new Job( piece, p.buildableCells(), p.skipped(), p.valid - p.affordable, hero.visibleEnemies(), hero.HP, hero.pos );
		sel.clear();
		stepJob();
		dirty();
	}

	//why none of a plan's cells is built: no cell takes the piece (the first one's reason), or the
	//materials are short
	private static Homestead.Reason refusal( BuildPlan p ){
		if (p.valid > 0) return Homestead.Reason.NO_MATERIALS;
		for (Homestead.Reason r : p.reasons) if (!r.ok()) return r;
		return Homestead.Reason.NO_MATERIALS;
	}

	//the next piece of the shape, the hero free: stopped by danger, skipping cells that no longer
	//take it, ended when the materials run out. Every cell was in sight when the shape was planned:
	//while the hero stands where he planned it, a wall of the shape put up since that hides the
	//cells behind it does not stop them (Homestead.canPlaceUnseen); moved, sight counts again
	private void stepJob(){
		Hero hero = Dungeon.hero;
		if (hero.visibleEnemies() > job.enemies || hero.HP < job.hp){
			finishJob( "danger" );
			return;
		}
		while (job.at < job.cells.length){
			int c = job.cells[job.at++];
			boolean seen = hero.pos == job.from;
			Homestead.Reason r = seen ? Homestead.canPlaceUnseen( hero, c, job.piece ) : Homestead.canPlace( hero, c, job.piece );
			if (r == Homestead.Reason.NO_MATERIALS){
				job.shortOf += job.cells.length - job.at + 1;
				job.at = job.cells.length;
				break;
			}
			if (!r.ok()){
				job.skipped++;
				continue;
			}
			if (seen) Homestead.placeUnseen( hero, c, job.piece );
			else Homestead.place( hero, c, job.piece );
			tool.used( job.piece.id );
			job.built++;
			job.hp = hero.HP;
			bar.refresh();
			return;
		}
		finishJob( null );
	}

	//the shape is done (or stopped: why, as a key): the log sums it up
	private void finishJob( String stoppedBy ){
		Job j = job;
		job = null;
		if (j == null) return;
		if (stoppedBy != null) GLog.w( Messages.get( this, "job_" + stoppedBy ) );
		StringBuilder s = new StringBuilder( Messages.get( this, "job_built", j.built, j.piece.name() ) );
		if (j.skipped > 0) s.append( ' ' ).append( Messages.get( this, "job_skipped", j.skipped ) );
		if (j.shortOf > 0) s.append( ' ' ).append( Messages.get( this, "job_short", j.shortOf ) );
		if (j.built > 0) GLog.p( s.toString() );
		else GLog.w( s.toString() );
		dirty();
	}

	// ----------------------------------------------------------------- each frame

	/** The bar's frame: ends placing when it should, builds the next piece of a shape when the
	 *  hero is free, follows the pointer, and lays the ghosts again when what they show changed. */
	public void update(){
		Hero hero = Dungeon.hero;
		if (current != this || hero == null || !hero.isAlive() || Dungeon.level != level
				|| level.windowVersion() != window || foreignWindow()){
			if (current == this) stop();
			return;
		}
		if (job != null && hero.ready && !GameScene.interfaceBlockingHero()) stepJob();
		follow();
		if (hero.ready){
			//the pointer moving only moves the ghosts: the tint and the bar's words don't hang on it
			int key = Objects.hash( sel.mode(), piece == null ? null : piece.id, sel.anchor(), sel.end(), sel.target(),
					hero.pos, level.homestead().version(), Float.floatToIntBits( Actor.now() ), job != null );
			if (dirty || key != laidFor){
				laidFor = key;
				dirty = aimMoved = false;
				valid = null;
				lay();
				bar.refresh();
			} else if (aimMoved){
				aimMoved = false;
				lay();
			}
		}
	}

	private void dirty(){
		dirty = true;
		bar.refresh();
	}

	//any window up that is not the bar's own
	private boolean foreignWindow(){
		if (!GameScene.showingWindow()) return false;
		for (Gizmo g : scene.membersView()){
			if (g instanceof xyz.gabriwar.warpedpixeldungeon.ui.Window && !(g instanceof Owned)) return true;
		}
		return false;
	}

	//the pointer's cell, on a screen that has one: a phone's stays where it was last tapped
	private void follow(){
		PointF p = PointerEvent.currentHoverPos();
		if (hover == null){
			hover = p;
			return;
		}
		if (p.equals( hover )) return;
		hover = p;
		PointF ui = PixelScene.uiCamera.screenToCamera( (int) p.x, (int) p.y );
		if (bar.inside( ui.x, ui.y )) return;
		PointF world = Camera.main.screenToCamera( (int) p.x, (int) p.y );
		int x = (int) Math.floor( world.x / DungeonTilemap.SIZE ), y = (int) Math.floor( world.y / DungeonTilemap.SIZE );
		int cell = x >= 0 && y >= 0 && x < level.width() && y < level.height() ? x + y * level.width() : -1;
		if (cell != aim){
			aim = cell;
			aimMoved = true;
		}
	}

	//the tint of the cells that take the piece, and the ghosts of what the taps aim at
	private void lay(){
		Hero hero = Dungeon.hero;
		int w = level.width(), h = level.height();
		ghost.reset();
		plan = null;
		BuildSelection.Mode m = sel.mode();
		if (job != null){
			if (valid == null || valid.length > 0) ghost.tint( valid = new int[0], w );
			for (int i = job.at; i < job.cells.length; i++) ghost.piece( job.cells[i], w, job.piece, BuildGhost.VALID );
			return;
		}
		if (valid == null) ghost.tint( valid = BuildPlan.validAround( hero, level, m, piece ), w );
		switch (m){
			case SINGLE:
				if (aim != -1 && piece != null) ghost.piece( aim, w, piece, colour( Homestead.canPlace( hero, aim, piece ) ) );
				break;
			case LINE:
			case AREA: {
				if (piece == null) break;
				int[] cells;
				int to = sel.previewing() ? sel.end() : aim;
				if (sel.anchor() == -1){
					cells = aim != -1 ? new int[]{ aim } : new int[0];
				} else if (sel.tooBigTo( to, w )){
					//too big to build: its bounds alone, in red (the bar says how big)
					ghost.box( sel.anchor(), to, w, BuildGhost.INVALID );
					break;
				} else {
					cells = sel.cellsTo( to, w, h );
				}
				BuildPlan p = BuildPlan.of( hero, piece, cells );
				if (sel.previewing()) plan = p;
				//the plan's own order (a roof's cells from its walls in), each with its verdict
				for (int i = 0; i < p.cells.length; i++){
					int colour = p.buildable( i ) ? BuildGhost.VALID
							: p.reasons[i].ok() ? BuildGhost.SHORT : BuildGhost.INVALID;
					ghost.piece( p.cells[i], w, piece, colour );
				}
				break;
			}
			case DEMOLISH:
				if (sel.target() != -1) ghost.icon( sel.target(), w, BuildIcons.DEMOLISH, BuildGhost.INVALID );
				else if (aim != -1) ghost.frame( aim, w, BuildGhost.LOOK );
				break;
			case REPAIR:
				if (sel.target() != -1) ghost.icon( sel.target(), w, BuildIcons.REPAIR, BuildGhost.VALID );
				else if (aim != -1) ghost.frame( aim, w, BuildGhost.LOOK );
				break;
			case INSPECT:
				if (aim != -1) ghost.frame( aim, w, BuildGhost.LOOK );
				break;
		}
	}

	private static int colour( Homestead.Reason r ){
		return r.ok() ? BuildGhost.VALID : r == Homestead.Reason.NO_MATERIALS ? BuildGhost.SHORT : BuildGhost.INVALID;
	}

	//a short word floating up off a cell
	private static void toast( int cell, String text ){
		if (cell < 0) return;
		PointF p = DungeonTilemap.raisedTileCenterToWorld( cell );
		FloatingText.show( p.x, p.y, cell, text, CharSprite.WARNING );
	}
}
