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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.audio.WallBreak;
import xyz.gabriwar.warpedpixeldungeon.effects.BuildDrop;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * The homesteads' one door into the world: whether a piece may go up on a cell and why not, putting
 * it up, taking it down, mending it, hurting it (bombs, the pick, fire, storm bolts, monsters), the
 * roofs, and redrawing the cells around a change. Every terrain change goes through Level.set, so
 * a co-op guest's mirror gets it; the pieces' health and the roofs reach the guests through
 * OverworldLevel.sharedHomestead. Everything that changes the world runs on the host: a guest, or
 * a guest's mirror, is told no (Reason.GUEST).
 *
 * Built only on the world's slices (the surface, the mountains, the caves), never in a dungeon
 * level or an interior, and never on a protected cell (OverworldLevel.protectedForBuilding).
 */
public final class Homestead {

	private Homestead(){}

	/** Why a piece may not go up (or come down, or be mended) there; OK when it may. */
	public enum Reason {
		OK,
		NOT_OVERWORLD,
		GUEST,
		PROTECTED,
		OUT_OF_RANGE,
		NOT_IN_SIGHT,
		OCCUPIED,
		WRONG_GROUND,
		ALREADY_THERE,
		UNSUPPORTED,
		NO_MATERIALS,
		NOTHING_THERE,
		UNDAMAGED,
		/** as many of the piece stand as may (BuildPiece.limit: three hearthstones) */
		LIMIT;

		public boolean ok(){
			return this == OK;
		}

		/** What the hero is told (homestead.properties: homestead.homestead.reason_*). */
		public String message(){
			return Messages.get( Homestead.class, "reason_" + name().toLowerCase( java.util.Locale.ENGLISH ) );
		}
	}

	/** How far from the hero he builds, in cells. */
	public static final int RANGE = 4;
	/** How far a roof reaches from what holds it up: a built wall, or the land's own rock. */
	public static final int ROOF_REACH = 4;
	/** The pick's blow on a built piece. */
	public static final int PICK_DAMAGE = 30;
	/** A bomb's blast on a built piece (and on the roof over the cells it reaches). */
	public static final int BOMB_DAMAGE = 60;
	/** A turn in the flames, for a wooden piece. */
	public static final int BURN_DAMAGE = 8;
	/** Anything else that burns a cell away at once (Level.destroy): a wooden piece takes this. */
	public static final int DESTROY_DAMAGE = 20;
	/** A storm's bolt on a roof that does not catch fire. */
	public static final int BOLT_DAMAGE = 25;
	/** A turn alight, for a roof, and how many turns a roof burns once lit. */
	public static final int ROOF_BURN_DAMAGE = 5, ROOF_BURN_TURNS = 8;
	/** How long a door or gate opened by hand stands open before it swings shut on an empty
	 *  doorway, in turns: long enough to step through. */
	public static final float OPEN_GRACE = 2f;

	// ----------------------------------------------------------------- where

	/** Can anything be built on this level: a world slice, the host's own (not a guest's mirror). */
	public static boolean allowedHere( Level level ){
		return level instanceof OverworldLevel && !((OverworldLevel) level).isMirror();
	}

	/** A slice's homestead data, null on any other level. */
	public static HomesteadStore store( Level level ){
		return level instanceof OverworldLevel ? ((OverworldLevel) level).homestead() : null;
	}

	private static int wx( OverworldLevel ow, int cell ){
		return ow.worldX + cell % ow.width();
	}

	private static int wy( OverworldLevel ow, int cell ){
		return ow.worldY + cell / ow.width();
	}

	// ----------------------------------------------------------------- what stands there

	/**
	 * The piece standing on a cell (a terrain or an actor; not the roof), or null. A record whose
	 * terrain was changed by something else since (or whose actor is gone) is forgotten here.
	 */
	public static BuildPiece pieceAt( Level level, int cell ){
		if (!(level instanceof OverworldLevel) || cell < 0 || cell >= level.length()) return null;
		OverworldLevel ow = (OverworldLevel) level;
		HomesteadStore store = ow.homestead();
		HomesteadStore.Built b = store.builtAt( wx( ow, cell ), wy( ow, cell ) );
		if (b == null) return null;
		BuildPiece p = BuildCatalog.byId( b.piece );
		boolean stands = p != null && (p.kind == BuildPiece.Kind.ACTOR
				? actorAt( level, cell ) != null
				: standsAs( p.terrain, level.map[cell] ));
		if (!stands){
			//the mirror only shows what its host sends
			if (!ow.isMirror()) store.clearBuilt( wx( ow, cell ), wy( ow, cell ) );
			return null;
		}
		return p;
	}

	//does a terrain piece still stand on a cell showing `now`: its own terrain, its door or gate
	//swung open, or a change that leaves the piece where it is - farmland watered, a dug channel
	//frozen over by frost (homestead/farming)
	private static boolean standsAs( int pieceTerrain, int now ){
		if (pieceTerrain == BuiltTerrain.closed( now )) return true;
		return (pieceTerrain == Terrain.FARMLAND && now == Terrain.FARMLAND_WET)
				|| (pieceTerrain == Terrain.WATER && now == Terrain.FROZEN_WATER);
	}

	/** The health left in the piece on a cell, 0 when none stands there. */
	public static int hpAt( Level level, int cell ){
		if (pieceAt( level, cell ) == null) return 0;
		OverworldLevel ow = (OverworldLevel) level;
		return ow.homestead().builtAt( wx( ow, cell ), wy( ow, cell ) ).hp;
	}

	/** Does a built piece stand on this cell? */
	public static boolean isBuilt( Level level, int cell ){
		return pieceAt( level, cell ) != null;
	}

	//the homestead actor standing on a cell, or null
	private static Mob actorAt( Level level, int cell ){
		for (Mob m : level.mobs){
			if (m.pos == cell && m instanceof HomesteadActor) return m;
		}
		return null;
	}

	// ----------------------------------------------------------------- roofs

	/** The roof kind over a cell (RoofTiles.THATCH..GLASS), -1 for none. */
	public static int roofAt( Level level, int cell ){
		if (!(level instanceof OverworldLevel) || cell < 0 || cell >= level.length()) return -1;
		OverworldLevel ow = (OverworldLevel) level;
		HomesteadStore.Roof r = ow.homestead().roofAt( wx( ow, cell ), wy( ow, cell ) );
		return r == null ? -1 : r.kind;
	}

	/** Puts a roof of a kind over a cell (full health), replacing any other there. No checks and
	 *  no cost: Homestead.place checks and charges. */
	public static void setRoof( Level level, int cell, int kind ){
		if (!allowedHere( level )) return;
		OverworldLevel ow = (OverworldLevel) level;
		BuildPiece p = BuildCatalog.forRoof( kind );
		ow.homestead().setRoof( wx( ow, cell ), wy( ow, cell ), kind, p != null ? p.maxHp : 1 );
		roofChanged( ow, cell );
	}

	/** Takes the roof off a cell (nothing given back). */
	public static void clearRoof( Level level, int cell ){
		if (!allowedHere( level )) return;
		OverworldLevel ow = (OverworldLevel) level;
		ow.homestead().clearRoof( wx( ow, cell ), wy( ow, cell ) );
		roofChanged( ow, cell );
	}

	//the roof over a cell went on or came off: the rooms round it are worked out again (Rooms), and
	//the roofs on screen
	private static void roofChanged( OverworldLevel ow, int cell ){
		Rooms.changed( ow, cell );
		ow.refreshRoofs( live( ow ) );
	}

	/**
	 * Would a roof on this cell stand: within ROOF_REACH steps of a built wall or window or the
	 * land's own rock, each step to a 4-neighbour that is roofed already (or a wall itself).
	 */
	public static boolean roofSupported( Level level, int cell ){
		return roofSupported( level, cell, -1, null );
	}

	//...counting `ignore` as roofless (a roof just gone), and the cells in `planned` (null: none)
	//as roofed already (the cells before this one of a roof the build bar lays at once)
	private static boolean roofSupported( Level level, int cell, int ignore, Set<Integer> planned ){
		int w = level.width();
		ArrayDeque<int[]> open = new ArrayDeque<>();
		HashSet<Integer> seen = new HashSet<>();
		open.add( new int[]{ cell, 0 } );
		seen.add( cell );
		while (!open.isEmpty()){
			int[] at = open.poll();
			int c = at[0];
			if (holdsRoof( level.map[c] )) return true;
			if (at[1] >= ROOF_REACH) continue;
			for (int n : PathFinder.NEIGHBOURS4){
				int m = c + n;
				if (m < 0 || m >= level.length() || Math.abs( m % w - c % w ) > 1 || seen.contains( m )) continue;
				seen.add( m );
				int t = level.map[m];
				if (holdsRoof( t ) || (m != ignore && (roofAt( level, m ) != -1 || (planned != null && planned.contains( m ))))){
					open.add( new int[]{ m, at[1] + 1 } );
				}
			}
		}
		return false;
	}

	//what a roof rests on: a built wall or window, the land's own rock
	private static boolean holdsRoof( int t ){
		return BuiltTerrain.wallLike( t ) || t == Terrain.WALL || t == Terrain.WALL_DECO;
	}

	//every roof within reach of a cell that has lost its hold falls (a wall wrecked or taken down)
	private static void dropUnsupported( OverworldLevel ow, int cell ){
		int w = ow.width(), r = ROOF_REACH + 1;
		int x0 = cell % w, y0 = cell / w;
		ArrayList<Integer> falling = new ArrayList<>();
		for (int y = Math.max( 0, y0 - r ); y <= Math.min( ow.height() - 1, y0 + r ); y++){
			for (int x = Math.max( 0, x0 - r ); x <= Math.min( w - 1, x0 + r ); x++){
				int c = x + y * w;
				if (roofAt( ow, c ) != -1 && !roofSupported( ow, c )) falling.add( c );
			}
		}
		for (int c : falling) roofFalls( ow, c );
	}

	//a roof cell comes down: nothing given back, splinters or slates in the dust
	private static void roofFalls( OverworldLevel ow, int cell ){
		ow.homestead().clearRoof( wx( ow, cell ), wy( ow, cell ) );
		if (live( ow )){
			PointF p = DungeonTilemap.tileCenterToWorld( cell );
			FxRecipes.dust( p.x, p.y, 6 );
			if (ow.heroFOV != null && ow.heroFOV[cell]) GLog.w( Messages.get( Homestead.class, "roof_fell" ) );
		}
		roofChanged( ow, cell );
	}

	// ----------------------------------------------------------------- may it go up

	/** Whether the hero may put this piece up on this cell now, materials and all. */
	public static Reason canPlace( Hero hero, int cell, BuildPiece piece ){
		return check( hero, cell, piece, true, true, null );
	}

	/** As canPlace, the line of sight left out: a cell of a line or an area the build bar
	 *  (building/BuildPlacer) saw when it planned the shape, which a wall of the same shape put up
	 *  since may hide now. Range, ground, what stands there and the cost are still checked. */
	public static Reason canPlaceUnseen( Hero hero, int cell, BuildPiece piece ){
		return check( hero, cell, piece, true, false, null );
	}

	/** As canPlace, the materials left out: whether the cell would take the piece (a line or an
	 *  area tallies its cost once for all its cells). */
	public static Reason canPlaceSite( Hero hero, int cell, BuildPiece piece ){
		return check( hero, cell, piece, false, true, null );
	}

	/** As canPlaceSite, for a cell of a shape planned whole (building/BuildPlan): the window cells
	 *  in `planned`, the shape's cells to be built before this one, count as roofed already, so
	 *  a roof laid over a whole room at once holds itself up from its walls inward. */
	public static Reason canPlaceSite( Hero hero, int cell, BuildPiece piece, Set<Integer> planned ){
		return check( hero, cell, piece, false, true, planned );
	}

	private static Reason check( Hero hero, int cell, BuildPiece piece, boolean withCost, boolean sight, Set<Integer> planned ){
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel)) return Reason.NOT_OVERWORLD;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.isMirror() || hero != Dungeon.hero) return Reason.GUEST;
		if (cell < 0 || cell >= level.length() || ow.protectedForBuilding( cell )) return Reason.PROTECTED;
		if (level.distance( hero.pos, cell ) > RANGE) return Reason.OUT_OF_RANGE;
		if (sight && (hero.fieldOfView == null || cell >= hero.fieldOfView.length || !hero.fieldOfView[cell])) return Reason.NOT_IN_SIGHT;
		Reason site = site( ow, cell, piece, planned );
		if (!site.ok()) return site;
		if (piece.limit != null && piece.limit.count() >= piece.limit.max()) return Reason.LIMIT;
		if (piece.kind != BuildPiece.Kind.ROOF && (solidPiece( piece ) || piece.kind == BuildPiece.Kind.ACTOR)){
			if (Actor.findChar( cell ) != null || level.heaps.get( cell ) != null) return Reason.OCCUPIED;
		}
		if (withCost && !affords( hero, piece.cost, 1 )) return Reason.NO_MATERIALS;
		return Reason.OK;
	}

	//whether the ground and what stands there take the piece (no hero, no cost)
	private static Reason site( OverworldLevel ow, int cell, BuildPiece piece, Set<Integer> planned ){
		int t = ow.map[cell];
		BuildPiece here = pieceAt( ow, cell );
		switch (piece.kind){
			case ROOF: {
				if (roofAt( ow, cell ) == piece.roofKind) return Reason.ALREADY_THERE;
				//over anything walkable, a built piece, or the furniture inside a room (a statue, a shelf)
				if (ow.pit[cell] || !(ow.passable[cell] || Terrain.homestead( t ) || Rooms.furnishing( t ))) return Reason.WRONG_GROUND;
				if (!roofSupported( ow, cell, -1, planned )) return Reason.UNSUPPORTED;
				return Reason.OK;
			}
			case ACTOR:
				if (here == piece) return Reason.ALREADY_THERE;
				if (here != null && here.kind == BuildPiece.Kind.ACTOR) return Reason.OCCUPIED;
				break;
			default:
				if (here == piece || piece.terrain == BuiltTerrain.closed( t )) return Reason.ALREADY_THERE;
				break;
		}
		//a piece goes up on the land's ground or on a built floor or path, never over another piece
		if (here != null && !BuiltTerrain.floor( here.terrain ) && here.terrain != Terrain.PATH_STONE) return Reason.WRONG_GROUND;
		if (Terrain.homestead( t ) && here == null && !BuiltTerrain.stump( t ) && !BuiltTerrain.sapling( t )
				&& !BuiltTerrain.rubble( t ) && !BuiltTerrain.ground( t )) return Reason.WRONG_GROUND;
		return groundTakes( ow, cell, piece.placement ) ? Reason.OK : Reason.WRONG_GROUND;
	}

	//the placement rules (BuildPiece.Placement)
	private static boolean groundTakes( OverworldLevel ow, int cell, BuildPiece.Placement placement ){
		int t = ow.map[cell];
		switch (placement){
			case WATER:
				return t == Terrain.WATER || t == Terrain.DEEP_WATER;
			case FARMLAND_ONLY:
				return BuiltTerrain.farmland( t );
			case ANY_PASSABLE:
				return ow.passable[cell] && !ow.pit[cell];
			case NEXT_TO_WATER:
				if (!plainGround( ow, cell )) return false;
				for (int n : PathFinder.NEIGHBOURS4){
					int m = cell + n;
					//a well is water too: a channel may be dug off one (homestead/farming)
					if (m >= 0 && m < ow.length() && (ow.map[m] == Terrain.WATER || ow.map[m] == Terrain.DEEP_WATER
							|| ow.map[m] == Terrain.WELL)) return true;
				}
				return false;
			case ROOF:
				return ow.passable[cell] || Terrain.homestead( t ) || Rooms.furnishing( t );
			case OPEN_FRONT: {
				int front = cell + ow.width();
				return plainGround( ow, cell ) && front < ow.length() && !DungeonTileSheet.wallStitcheable( ow.map[front] );
			}
			case GROUND:
			default:
				return plainGround( ow, cell );
		}
	}

	/** Walkable ground that is no water and no ice (it may thaw), nor a farmland bed: the GROUND
	 *  placement (public for PortableChest, which sets its chest on the same ground). */
	public static boolean plainGround( OverworldLevel ow, int cell ){
		int t = ow.map[cell];
		return ow.passable[cell] && !ow.pit[cell] && (Terrain.flags[t] & Terrain.LIQUID) == 0
				&& t != Terrain.FROZEN_WATER && t != Terrain.BRIDGE && t != Terrain.BRIDGE_PLANK
				&& !BuiltTerrain.farmland( t );
	}

	//does the piece stand in the way (a wall, a fence, a shut door, a well): nobody may stand where
	//it goes up
	private static boolean solidPiece( BuildPiece piece ){
		return piece.kind == BuildPiece.Kind.TERRAIN && (Terrain.flags[piece.terrain] & Terrain.PASSABLE) == 0;
	}

	/** Does the hero carry `times` over what a cost asks (gold at the town's rate: Cost.amount)? */
	public static boolean affords( Hero hero, java.util.List<BuildPiece.Cost> cost, int times ){
		for (BuildPiece.Cost c : cost){
			if (Material.count( hero, c.material ) < c.amount() * times) return false;
		}
		return true;
	}

	// ----------------------------------------------------------------- putting it up

	/**
	 * The hero puts a piece up: checked as canPlace, then the materials are spent (the satchel's
	 * first), the hero spends the piece's turns at it, and the piece goes up (build). A built
	 * floor or path under a new piece is taken up first, half of it given back. Returns why not,
	 * having changed nothing, when it may not.
	 */
	public static Reason place( Hero hero, int cell, BuildPiece piece ){
		return place( hero, cell, piece, true );
	}

	/** As place, the line of sight left out (canPlaceUnseen): a cell of a line or an area the
	 *  build bar planned in sight, put up after a wall of the same shape may have hidden it. */
	public static Reason placeUnseen( Hero hero, int cell, BuildPiece piece ){
		return place( hero, cell, piece, false );
	}

	private static Reason place( Hero hero, int cell, BuildPiece piece, boolean sight ){
		Reason r = check( hero, cell, piece, true, sight, null );
		if (!r.ok()) return r;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		//a terrain piece takes up the floor under it; a station or furniture stands on the floor
		//(build gives the floor's record back when it goes: takeDown)
		if (piece.kind == BuildPiece.Kind.TERRAIN){
			BuildPiece under = pieceAt( ow, cell );
			if (under != null) refund( hero, under );
		} else if (piece.kind == BuildPiece.Kind.ROOF && roofAt( ow, cell ) != -1){
			BuildPiece old = BuildCatalog.forRoof( roofAt( ow, cell ) );
			if (old != null) refund( hero, old );
		}
		for (BuildPiece.Cost c : piece.cost) Material.spend( hero, c.material, c.amount() );
		build( ow, cell, piece, hero );
		if (live( ow )){
			//one line a material, each in its own colour (building/MaterialColors), stacked over the cell
			PointF p = DungeonTilemap.raisedTileCenterToWorld( cell );
			for (BuildPiece.Cost c : piece.cost){
				FloatingText.show( p.x, p.y, cell, "-" + c.amount() + " " + Reflection.newInstance( c.material ).name(),
						xyz.gabriwar.warpedpixeldungeon.homestead.building.MaterialColors.of( c.material ) );
			}
			//the guide's pages on what a builder does next: a first piece, a first field
			xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_BUILT );
			if (piece.category == BuildCategory.FARMING) xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_FARM );
		}
		hero.spend( piece.buildTurns );
		hero.busy();
		if (hero.sprite != null) hero.sprite.operate( cell );
		else hero.next();
		return Reason.OK;
	}

	/**
	 * Puts a piece up on a cell of a slice with no checks and no cost: what place does once it
	 * has checked and charged, and what a debug scene or a test lays directly. builder may be null.
	 */
	public static void build( OverworldLevel ow, int cell, BuildPiece piece, Hero builder ){
		if (ow.isMirror()) return;
		int x = wx( ow, cell ), y = wy( ow, cell );
		HomesteadStore store = ow.homestead();
		switch (piece.kind){
			case ROOF:
				store.setRoof( x, y, piece.roofKind, piece.maxHp );
				roofChanged( ow, cell );
				break;
			case ACTOR: {
				store.clearBuilt( x, y );
				Mob m = piece.actor.create();
				m.pos = cell;
				if (live( ow )) GameScene.add( m );
				else ow.mobs.add( m );
				store.setBuilt( x, y, piece.id, piece.maxHp );
				break;
			}
			default:
				Level.set( cell, piece.terrain, ow );
				store.setBuilt( x, y, piece.id, piece.maxHp );
				break;
		}
		if (live( ow )){
			PointF p = DungeonTilemap.tileCenterToWorld( cell );
			FxRecipes.dust( p.x, p.y, 4 );
			SpatialSound.play( stoneWork( piece ) ? Assets.Sounds.STONE_SET : Assets.Sounds.HAMMER, cell );
			//its picture drops into place onto it
			BuildDrop.play( piece, cell, ow.width() );
		}
		if (piece.listener != null) piece.listener.placed( ow, cell, builder );
		cellChanged( ow, cell );
	}

	//laid stone (or brick) rather than nailed wood: by what it costs most of
	private static boolean stoneWork( BuildPiece piece ){
		BuildPiece.Cost main = piece.mainCost();
		return main != null && (main.material == xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone.class
				|| main.material == xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone.class
				|| main.material == xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick.class);
	}

	// ----------------------------------------------------------------- taking it down

	/** Whether the hero may take down what stands on the cell (or, with nothing there, its roof). */
	public static Reason canRemove( Hero hero, int cell ){
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel)) return Reason.NOT_OVERWORLD;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.isMirror() || hero != Dungeon.hero) return Reason.GUEST;
		if (cell < 0 || cell >= level.length()) return Reason.NOTHING_THERE;
		if (level.distance( hero.pos, cell ) > RANGE) return Reason.OUT_OF_RANGE;
		if (hero.fieldOfView == null || !hero.fieldOfView[cell]) return Reason.NOT_IN_SIGHT;
		BuildPiece p = pieceAt( level, cell );
		//rubble is cleared away like a piece taken down, nothing given back
		if (p == null) return roofAt( level, cell ) != -1 || BuiltTerrain.rubble( level.map[cell] ) ? Reason.OK : Reason.NOTHING_THERE;
		//what the ground goes back to must take whoever stands there
		if (p.kind == BuildPiece.Kind.TERRAIN && Actor.findChar( cell ) != null
				&& (Terrain.flags[groundUnder( ow, cell )] & Terrain.PASSABLE) == 0) return Reason.OCCUPIED;
		return Reason.OK;
	}

	/**
	 * The hero takes down the piece on a cell (or, with none there, the roof over it, or clears the
	 * rubble of a wrecked one): half of each material comes back, rounded down, and at least one
	 * of the main one for a piece that cost two or more; the ground goes back to the land's own
	 * (groundUnder). A turn's work.
	 */
	public static Reason remove( Hero hero, int cell ){
		Reason r = canRemove( hero, cell );
		if (!r.ok()) return r;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		BuildPiece p = pieceAt( ow, cell );
		//what comes down, for its sound: the piece, else the roof, else the rubble
		boolean wooden = p != null ? p.flammable
				: roofAt( ow, cell ) != -1 ? BuildCatalog.forRoof( roofAt( ow, cell ) ) != null && BuildCatalog.forRoof( roofAt( ow, cell ) ).flammable
				: ow.map[cell] == Terrain.RUBBLE_WOOD;
		if (p == null && roofAt( ow, cell ) == -1){
			//rubble cleared
			Level.set( cell, groundUnder( ow, cell ), ow );
			cellChanged( ow, cell );
		} else if (p == null){
			BuildPiece roof = BuildCatalog.forRoof( roofAt( ow, cell ) );
			if (roof != null) refund( hero, roof );
			ow.homestead().clearRoof( wx( ow, cell ), wy( ow, cell ) );
			roofChanged( ow, cell );
			dropUnsupported( ow, cell );
		} else {
			refund( hero, p );
			takeDown( ow, cell, p, groundUnder( ow, cell ) );
		}
		if (live( ow )){
			PointF pt = DungeonTilemap.tileCenterToWorld( cell );
			FxRecipes.dust( pt.x, pt.y, 6 );
			//taken apart: boards splinter, stone comes down as a light fall of rock
			if (wooden) SpatialSound.play( Assets.Sounds.WOOD_BREAK, cell );
			else WallBreak.play( cell, 0.6f, 1f );
		}
		hero.spend( Actor.TICK );
		hero.busy();
		if (hero.sprite != null) hero.sprite.operate( cell );
		else hero.next();
		return Reason.OK;
	}

	//a piece leaves its cell, the terrain becoming `ground`; the roof stays (it may fall: dropUnsupported)
	private static void takeDown( OverworldLevel ow, int cell, BuildPiece p, int ground ){
		ow.homestead().clearBuilt( wx( ow, cell ), wy( ow, cell ) );
		if (p.kind == BuildPiece.Kind.ACTOR){
			Mob m = actorAt( ow, cell );
			if (m != null){
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
			}
			//the built floor or path it stood on is the cell's piece again (build gave its record
			//to the actor), so it can be taken up, and mended, like any other
			BuildPiece floor = BuildCatalog.forTerrain( ow.map[cell] );
			if (floor != null && (BuiltTerrain.floor( floor.terrain ) || floor.terrain == Terrain.PATH_STONE)){
				ow.homestead().setBuilt( wx( ow, cell ), wy( ow, cell ), floor.id, floor.maxHp );
			}
		} else {
			Level.set( cell, ground, ow );
			standClear( ow, cell );
		}
		if (p.listener != null) p.listener.removed( ow, cell );
		cellChanged( ow, cell );
		dropUnsupported( ow, cell );
	}

	/**
	 * The land's own ground under a cell, what a piece taken down leaves: the generator's terrain
	 * where it is open ground or water (a bridge's river), grass where a tree or a bush stood (the
	 * dressing draws it as snow while the ground is frozen, WindowGenerator.visualGround, so it
	 * thaws with the season), and broken ground where rock, a boulder or anything else solid did -
	 * a felled tree or a mined wall never comes back.
	 */
	public static int groundUnder( OverworldLevel ow, int cell ){
		int p = ow.pristineAt( cell );
		if (p == Terrain.WATER || p == Terrain.DEEP_WATER) return p;
		if ((Terrain.flags[p] & Terrain.SOLID) == 0 && !Terrain.homestead( p )) return p;
		if (p == Terrain.TREE_OAK || p == Terrain.TREE_PINE || p == Terrain.SHRUB) return Terrain.GRASS;
		return Terrain.EMPTY_DECO;
	}

	//what taking a piece down gives back (refundOf), into the hero's pack or at his feet when full
	private static void refund( Hero hero, BuildPiece piece ){
		for (BuildPiece.Cost c : refundOf( piece )){
			Item item = Reflection.newInstance( c.material );
			if (item == null) continue;
			item.quantity( c.quantity );
			if (!item.collect( hero.belongings.backpack )){
				Heap h = Dungeon.level.drop( item, hero.pos );
				if (h.sprite != null) h.sprite.drop();
			}
		}
	}

	/** What taking down a piece gives back: half of each material, rounded down, and at least one
	 *  of the main one for a piece that cost two or more. Gold paid is never given back. */
	public static ArrayList<BuildPiece.Cost> refundOf( BuildPiece piece ){
		ArrayList<BuildPiece.Cost> out = new ArrayList<>();
		int total = 0;
		for (BuildPiece.Cost c : piece.cost) if (!c.gold()) total += c.quantity;
		BuildPiece.Cost main = piece.mainCost();
		for (BuildPiece.Cost c : piece.cost){
			if (c.gold()) continue;
			int back = c.quantity / 2;
			if (c == main && total >= 2) back = Math.max( 1, back );
			if (back > 0) out.add( new BuildPiece.Cost( c.material, back ) );
		}
		return out;
	}

	// ----------------------------------------------------------------- mending

	/** What mending a piece costs: a quarter of each material, rounded up (never gold). */
	public static ArrayList<BuildPiece.Cost> repairCost( BuildPiece piece ){
		ArrayList<BuildPiece.Cost> out = new ArrayList<>();
		for (BuildPiece.Cost c : piece.cost) if (!c.gold()) out.add( new BuildPiece.Cost( c.material, (c.quantity + 3) / 4 ) );
		return out;
	}

	/** Whether the hero may mend what stands on the cell (or the roof over it). */
	public static Reason canRepair( Hero hero, int cell ){
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel)) return Reason.NOT_OVERWORLD;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.isMirror() || hero != Dungeon.hero) return Reason.GUEST;
		if (cell < 0 || cell >= level.length()) return Reason.NOTHING_THERE;
		if (level.distance( hero.pos, cell ) > RANGE) return Reason.OUT_OF_RANGE;
		if (hero.fieldOfView == null || !hero.fieldOfView[cell]) return Reason.NOT_IN_SIGHT;
		BuildPiece p = repairTarget( ow, cell );
		if (p == null) return pieceAt( ow, cell ) != null || roofAt( ow, cell ) != -1 ? Reason.UNDAMAGED : Reason.NOTHING_THERE;
		return affords( hero, repairCost( p ), 1 ) ? Reason.OK : Reason.NO_MATERIALS;
	}

	/** What mending a cell would mend: the damaged piece on it, else its damaged (or burning)
	 *  roof, else null. Public for the build bar, which shows the cost before the work. */
	public static BuildPiece repairTarget( OverworldLevel ow, int cell ){
		BuildPiece p = pieceAt( ow, cell );
		if (p != null && p.destructible() && hpAt( ow, cell ) < p.maxHp) return p;
		HomesteadStore.Roof r = ow.homestead().roofAt( wx( ow, cell ), wy( ow, cell ) );
		BuildPiece roof = r == null ? null : BuildCatalog.forRoof( r.kind );
		if (roof != null && (r.hp < roof.maxHp || r.burning > 0)) return roof;
		return null;
	}

	/** The hero mends the damaged piece on a cell (or its roof, putting out a fire on it): a
	 *  quarter of its materials, rounded up, and the piece's turns. Back to full health. */
	public static Reason repair( Hero hero, int cell ){
		Reason r = canRepair( hero, cell );
		if (!r.ok()) return r;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		BuildPiece p = repairTarget( ow, cell );
		for (BuildPiece.Cost c : repairCost( p )) Material.spend( hero, c.material, c.quantity );
		int x = wx( ow, cell ), y = wy( ow, cell );
		if (p.kind == BuildPiece.Kind.ROOF){
			HomesteadStore.Roof roof = ow.homestead().roofAt( x, y );
			roof.hp = p.maxHp;
			roof.burning = 0;
		} else {
			ow.homestead().builtAt( x, y ).hp = p.maxHp;
		}
		ow.homestead().changed();
		if (live( ow )){
			PointF pt = DungeonTilemap.tileCenterToWorld( cell );
			FxRecipes.dust( pt.x, pt.y, 3 );
			SpatialSound.play( stoneWork( p ) ? Assets.Sounds.STONE_SET : Assets.Sounds.HAMMER, cell );
		}
		hero.spend( p.buildTurns );
		hero.busy();
		if (hero.sprite != null) hero.sprite.operate( cell );
		else hero.next();
		return Reason.OK;
	}

	// ----------------------------------------------------------------- hurting it

	/** Hurts the built piece on a cell of the shown level; see damage( Level, ... ). */
	public static boolean damage( int cell, int amount, Object src ){
		return damage( Dungeon.level, cell, amount, src );
	}

	/**
	 * The one way a built piece is hurt: bombs, the pick, fire, storm bolts and monsters all come
	 * here. Its health goes down; at none left it is wrecked - a terrain piece becomes rubble
	 * (wooden or stone; a bridge falls into its river), an actor piece is gone - and the roof over
	 * it falls, with any other the wreck leaves without a hold. A piece that cannot be hurt (a
	 * path, a floor) and a tomb's sealed stone take nothing. True when a piece was hit. Host only.
	 */
	public static boolean damage( Level level, int cell, int amount, Object src ){
		if (!allowedHere( level ) || amount <= 0) return false;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.unbreakable( cell )) return false;
		BuildPiece p = pieceAt( ow, cell );
		if (p == null || !p.destructible()) return false;
		HomesteadStore.Built b = ow.homestead().builtAt( wx( ow, cell ), wy( ow, cell ) );
		b.hp -= amount;
		ow.homestead().changed();
		if (b.hp > 0){
			if (live( ow ) && ow.heroFOV != null && ow.heroFOV[cell]){
				PointF pt = DungeonTilemap.tileCenterToWorld( cell );
				FxRecipes.dust( pt.x, pt.y, 2 );
			}
			return true;
		}
		wreck( ow, cell, p );
		return true;
	}

	//a piece with no health left: rubble (or the river back), its roof down, a crash
	private static void wreck( OverworldLevel ow, int cell, BuildPiece p ){
		boolean wood = p.kind == BuildPiece.Kind.TERRAIN ? BuiltTerrain.wooden( p.terrain ) : p.flammable;
		int left = p.kind != BuildPiece.Kind.TERRAIN ? ow.map[cell]
				: p.terrain == Terrain.BRIDGE_PLANK ? groundUnder( ow, cell ) : BuiltTerrain.rubbleOf( p.terrain );
		if (roofAt( ow, cell ) != -1) roofFalls( ow, cell );
		takeDown( ow, cell, p, left );
		if (live( ow )){
			PointF pt = DungeonTilemap.tileCenterToWorld( cell );
			FxRecipes.dust( pt.x, pt.y, 10 );
			if (wood) SpatialSound.play( Assets.Sounds.WOOD_BREAK, cell );
			else WallBreak.play( cell, 1f, 1f );
			if (ow.heroFOV != null && ow.heroFOV[cell]) GLog.w( Messages.get( Homestead.class, "wrecked", p.name() ) );
		}
	}

	/** A monster breaks at a built piece (the threats give monsters the will to): the blow lands
	 *  on the piece on the cell, else on the roof over it. True when anything was hit. */
	public static boolean attackBuilt( Char attacker, int cell, int dmg ){
		Level level = Dungeon.level;
		if (damage( level, cell, dmg, attacker )) return true;
		return damageRoof( level, cell, dmg );
	}

	/** Hurts the roof over a cell; at none left it falls. True when there was one. Host only. */
	public static boolean damageRoof( Level level, int cell, int amount ){
		if (!allowedHere( level ) || amount <= 0) return false;
		OverworldLevel ow = (OverworldLevel) level;
		HomesteadStore.Roof r = ow.homestead().roofAt( wx( ow, cell ), wy( ow, cell ) );
		if (r == null) return false;
		r.hp -= amount;
		ow.homestead().changed();
		if (r.hp <= 0) roofFalls( ow, cell );
		return true;
	}

	/** Sets the roof over a cell alight, when it burns (thatch, planks). True when it caught. */
	public static boolean igniteRoof( Level level, int cell ){
		if (!allowedHere( level )) return false;
		OverworldLevel ow = (OverworldLevel) level;
		HomesteadStore.Roof r = ow.homestead().roofAt( wx( ow, cell ), wy( ow, cell ) );
		BuildPiece p = r == null ? null : BuildCatalog.forRoof( r.kind );
		if (p == null || !p.flammable) return false;
		if (r.burning <= 0 && live( ow ) && ow.heroFOV != null && ow.heroFOV[cell]){
			GLog.w( Messages.get( Homestead.class, "roof_fire" ) );
		}
		r.burning = Math.max( r.burning, ROOF_BURN_TURNS );
		ow.homestead().changed();
		return true;
	}

	/**
	 * Fire.evolve, for a cell in the flames: a wooden piece there loses BURN_DAMAGE a turn instead
	 * of burning away at once, and a burning roof over it catches. True while a built piece still
	 * stands burning there: the fire keeps burning on it.
	 */
	public static boolean burn( int cell ){
		Level level = Dungeon.level;
		if (!allowedHere( level )) return false;
		igniteRoof( level, cell );
		BuildPiece p = pieceAt( level, cell );
		if (!burns( level, cell, p ) || !p.destructible()) return false;
		damage( level, cell, BURN_DAMAGE, null );
		return pieceAt( level, cell ) != null;
	}

	/**
	 * Fire.evolve, for a cell beside the flames that is not flamable ground: does a wooden piece
	 * stand on it all the same - a flammable actor piece (a workbench, a tanning rack) on stone,
	 * dirt or a path - for the fire to spread onto, as onto any flamable cell?
	 */
	public static boolean catches( int cell ){
		Level level = Dungeon.level;
		if (!allowedHere( level )) return false;
		BuildPiece p = pieceAt( level, cell );
		return p != null && p.kind == BuildPiece.Kind.ACTOR && p.flammable && p.destructible();
	}

	//does the piece on a cell burn: a terrain piece by its terrain (a log wall, a plank floor), an
	//actor piece (a station) by its own word, whatever the ground under it
	private static boolean burns( Level level, int cell, BuildPiece p ){
		if (p == null) return false;
		if (p.kind == BuildPiece.Kind.ACTOR) return p.flammable;
		return p.kind == BuildPiece.Kind.TERRAIN && (Terrain.flags[level.map[cell]] & Terrain.FLAMABLE) != 0;
	}

	/** OverworldLevel.destroy: a wooden piece hit by something that burns a cell away at once
	 *  loses DESTROY_DAMAGE instead, and the ground under a wooden station stays while it stands.
	 *  True when it was one (the cell is the homestead's). */
	public static boolean destroyed( Level level, int cell ){
		BuildPiece p = pieceAt( level, cell );
		if (!burns( level, cell, p )) return false;
		damage( level, cell, DESTROY_DAMAGE, null );
		return true;
	}

	/** StormStrikes.strike: a bolt on a roofed cell hits the roof - thatch catches, the rest is
	 *  hurt - and goes no further. True when it did. */
	public static boolean roofStruck( Level level, int cell ){
		if (roofAt( level, cell ) == -1 || !allowedHere( level )) return false;
		if (!igniteRoof( level, cell )) damageRoof( level, cell, BOLT_DAMAGE );
		return true;
	}

	/** A bomb's blast reached these cells: every built piece on them or beside them takes
	 *  BOMB_DAMAGE once, and every roof over them. */
	public static void blast( Level level, java.util.Collection<Integer> reached ){
		if (!allowedHere( level )) return;
		HashSet<Integer> hit = new HashSet<>();
		for (int c : reached){
			for (int n : PathFinder.NEIGHBOURS9){
				int m = c + n;
				if (m >= 0 && m < level.length()) hit.add( m );
			}
			damageRoof( level, c, BOMB_DAMAGE );
		}
		for (int c : hit) damage( level, c, BOMB_DAMAGE, null );
	}

	// ----------------------------------------------------------------- doors and gates

	/** The hero (beside it) or an ally (stepping in) opens a barred door or a gate: it stands
	 *  open until nobody is in it any more, a few turns on (worldTurn). True when it opened. */
	public static boolean openByHand( Char ch, int cell ){
		Level level = Dungeon.level;
		if (!allowedHere( level ) || !BuiltTerrain.handOpened( level.map[cell] )) return false;
		OverworldLevel ow = (OverworldLevel) level;
		int t = level.map[cell];
		Level.set( cell, BuiltTerrain.opened( t ), level );
		ow.homestead().openedAt.put( OverworldLevel.worldKey( wx( ow, cell ), wy( ow, cell ) ), Actor.now() );
		if (live( ow )){
			GameScene.updateMap( cell );
			Dungeon.observe();
			SpatialSound.play( BuiltTerrain.barred( t ) ? Assets.Sounds.DOOR_BAR : Assets.Sounds.OPEN, cell );
		}
		return true;
	}

	/** May this char open hand-opened doors and gates on its own: the hero's allies. */
	public static boolean opensByHand( Char ch ){
		return ch.alignment == Char.Alignment.ALLY && !(ch instanceof Hero);
	}

	/** Char.modifyPassable for an ally: the shut barred doors and gates are its way too (it opens
	 *  them stepping in, Level.pressCell). */
	public static void allyPassable( Char ch, boolean[] passable ){
		Level level = Dungeon.level;
		if (!allowedHere( level ) || !opensByHand( ch )) return;
		OverworldLevel ow = (OverworldLevel) level;
		ow.homestead().forEachBuilt( ow.worldX, ow.worldY, ow.worldX + ow.width(), ow.worldY + ow.height(), (x, y, b) -> {
			int c = ow.localCell( x, y );
			if (c != -1 && c < passable.length && BuiltTerrain.handOpened( ow.map[c] )) passable[c] = true;
		} );
	}

	// ----------------------------------------------------------------- the turn

	/**
	 * Once a world turn on a slice (OverworldLevel's turn, the host's): doors and gates standing
	 * open with nobody in them swing shut (a hand-opened one after OPEN_GRACE), burning roofs
	 * burn on, spread to the burning kinds beside them and go out in the rain, the traits of the
	 * hero's room act on him (RoomTraits), a home he comes back to is settled (AwaySettlers), and
	 * every section takes its turn.
	 */
	public static void worldTurn( OverworldLevel ow ){
		if (ow.isMirror()) return;
		HomesteadStore store = ow.homestead();
		final float now = Actor.now();
		final ArrayList<Integer> shut = new ArrayList<>(), alight = new ArrayList<>();
		int x0 = ow.worldX, y0 = ow.worldY, x1 = x0 + ow.width(), y1 = y0 + ow.height();
		store.forEachBuilt( x0, y0, x1, y1, (x, y, b) -> {
			int c = ow.localCell( x, y );
			if (c == -1 || !BuiltTerrain.open( ow.map[c] )) return;
			Float at = store.openedAt.get( OverworldLevel.worldKey( x, y ) );
			if (at != null && now - at < OPEN_GRACE) return;
			if (Actor.findChar( c ) == null && ow.heaps.get( c ) == null) shut.add( c );
		} );
		for (int c : shut){
			int t = ow.map[c];
			Level.set( c, BuiltTerrain.closed( t ), ow );
			store.openedAt.remove( OverworldLevel.worldKey( wx( ow, c ), wy( ow, c ) ) );
			if (live( ow )){
				GameScene.updateMap( c );
				if (BuiltTerrain.barred( t ) && ow.heroFOV != null && ow.heroFOV[c]) SpatialSound.play( Assets.Sounds.DOOR_BAR, c );
			}
		}
		if (!shut.isEmpty() && live( ow )) Dungeon.observe();

		store.forEachRoof( x0, y0, x1, y1, (x, y, r) -> {
			if (r.burning > 0){
				int c = ow.localCell( x, y );
				if (c != -1) alight.add( c );
			}
		} );
		if (!alight.isEmpty()){
			boolean rain = ow.openSky() && (ClimateManager.localPrecipType() == PrecipType.RAIN
					|| ClimateManager.localPrecipType() == PrecipType.SLEET);
			float douse = rain ? ClimateManager.localPrecipRate() : 0f;
			for (int c : alight){
				HomesteadStore.Roof r = store.roofAt( wx( ow, c ), wy( ow, c ) );
				if (r == null) continue;
				if (Random.Float() < douse){
					r.burning = 0;
					store.changed();
					continue;
				}
				r.burning--;
				if (live( ow ) && ow.heroFOV != null && ow.heroFOV[c]){
					PointF p = DungeonTilemap.raisedTileCenterToWorld( c );
					FxRecipes.embers( p.x, p.y, Element.FIRE, 3 );
				}
				for (int n : PathFinder.NEIGHBOURS4){
					int m = c + n;
					if (m >= 0 && m < ow.length() && Random.Int( 4 ) == 0) igniteRoof( ow, m );
				}
				damageRoof( ow, c, ROOF_BURN_DAMAGE );
			}
		}

		//what the hero's room does for him, and a home he comes back to settled
		RoomTraits.worldTurn( ow );
		AwaySettlers.visit( ow );

		for (StoreSection s : store.sections()) s.worldTurn( ow );
	}

	// ----------------------------------------------------------------- redrawing

	/** Redraws a cell and everything its change reaches; see refreshArea. */
	public static void refresh( Level level, int cell ){
		refreshArea( level, cell, cell );
	}

	/**
	 * After the terrain changed on the cells of the rectangle with corners a and b: the walls'
	 * discoverability around them only (Level.cleanWalls(cell)), the four render layers, the
	 * overworld's dressing and the fog for each and its neighbours (GameScene.updateMap), and a
	 * fresh look. Never the whole level: this is what replaces a full cleanWalls and updateMap
	 * after an edit. Any level; nothing on screen without a scene.
	 */
	public static void refreshArea( Level level, int a, int b ){
		int w = level.width();
		int xa = Math.min( a % w, b % w ), xb = Math.max( a % w, b % w );
		int ya = Math.min( a / w, b / w ), yb = Math.max( a / w, b / w );
		for (int y = ya; y <= yb; y++){
			for (int x = xa; x <= xb; x++) level.cleanWalls( x + y * w );
		}
		if (level != Dungeon.level || !(Game.scene() instanceof GameScene)) return;
		for (int y = Math.max( 0, ya - 1 ); y <= Math.min( level.height() - 1, yb + 1 ); y++){
			for (int x = Math.max( 0, xa - 1 ); x <= Math.min( w - 1, xb + 1 ); x++) GameScene.updateMap( x + y * w );
		}
		if (Dungeon.hero != null) Dungeon.observe();
	}

	//a change through the facade: redrawn, and every section told
	private static void cellChanged( OverworldLevel ow, int cell ){
		refresh( ow, cell );
		for (StoreSection s : ow.homestead().sections()) s.cellChanged( ow, cell );
	}

	//whoever stands on a cell that no longer takes him (a bridge fallen into deep water) steps off
	private static void standClear( OverworldLevel ow, int cell ){
		if (ow.passable[cell]) return;
		Char ch = Actor.findChar( cell );
		if (ch == null) return;
		int w = ow.width();
		for (int r = 1; r <= 3; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = cell % w + dx, y = cell / w + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= ow.height() - 1) continue;
					int c = x + y * w;
					if (ow.passable[c] && Actor.findChar( c ) == null){
						ch.pos = c;
						if (ch.sprite != null) ch.sprite.place( c );
						ow.occupyCell( ch );
						return;
					}
				}
			}
		}
	}

	//is this level the one on screen
	private static boolean live( Level level ){
		return level == Dungeon.level && Game.scene() instanceof GameScene;
	}
}
