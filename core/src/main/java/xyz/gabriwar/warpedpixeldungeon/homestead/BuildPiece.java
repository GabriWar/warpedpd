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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One thing the hero can build (BuildCatalog lists them all): what it is called, which group of
 * the build menu it sits in, what it costs and how long it takes, how much punishment it takes,
 * where it may stand, and what it makes - a terrain (a wall, a door, a floor), a roof over a cell,
 * or an actor (a station or a piece of furniture, a mob made by its factory).
 *
 * Made with the builder: {@code BuildPiece.terrain( "wall_log", BuildCategory.WALLS,
 * Terrain.BUILT_WALL_LOG ).cost( Log.class, 2 ).turns( 3 ).hp( 40 ).line().area().done()}, then
 * handed to BuildCatalog.register by a provider class (see BuildCatalog). Its name and description
 * are the message keys homestead.piece.ID.name and .desc.
 */
public final class BuildPiece {

	/** Where a piece may stand (Homestead.canPlace checks it, WRONG_GROUND when it fails). */
	public enum Placement {
		/** walkable ground that is no water: the land's own, or a built floor or path */
		GROUND,
		/** open water, shallow or deep: a bridge */
		WATER,
		/** tilled farmland only */
		FARMLAND_ONLY,
		/** ground (as GROUND) with water (or a well) on one of its four sides */
		NEXT_TO_WATER,
		/** anything that can be walked on, water and built ground included */
		ANY_PASSABLE,
		/** a roof: over anything walkable or a built wall, near enough to a wall to hold it up */
		ROOF,
		/** ground (as GROUND) with open ground in front of it, on the cell below it (south): a
		 *  piece drawn as a wall with a face (a bookshelf) would show none there, only a wall's top */
		OPEN_FRONT
	}

	/** What a piece makes. */
	public enum Kind { TERRAIN, ROOF, ACTOR }

	/** Makes the actor of an actor-backed piece: a fresh one, not yet on any level. */
	public interface ActorFactory {
		Mob create();
	}

	/** Told when a piece goes up or comes down, on the host (a hearthstone founding a home, a
	 *  station dropping what it held). Both run after the world has changed. */
	public interface Listener {
		void placed( OverworldLevel level, int cell, Hero builder );
		void removed( OverworldLevel level, int cell );
	}

	/** How many of a piece may stand at once, across the whole world (a hearthstone: a home each,
	 *  three at most). Homestead.canPlace says LIMIT once count() has reached max(). */
	public interface Limit {
		int max();
		int count();
	}

	/** One material and how many of it. Gold (items.Gold) is a cost too: its quantity is the base
	 *  price, and what is paid steps up with the town's prices (amount()). */
	public static final class Cost {
		public final Class<? extends Item> material;
		/** The quantity as registered: for gold the BASE price, not what is paid. Whatever shows,
		 *  checks or charges a cost (a cost chip's have/need, a plan's totals and shortfall, the
		 *  affordable count) reads amount(). */
		public final int quantity;

		public Cost( Class<? extends Item> material, int quantity ){
			this.material = material;
			this.quantity = quantity;
		}

		/** Is it paid in gold (from the hero's purse, never the pack)? */
		public boolean gold(){
			return material == xyz.gabriwar.warpedpixeldungeon.items.Gold.class;
		}

		/** How many are paid now: the quantity, or for gold the base price at the town's rate
		 *  (TownLedger.scaled: it steps up with the deepest floor reached). */
		public int amount(){
			return gold() ? xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger.scaled( quantity ) : quantity;
		}
	}

	public final String id;
	public final BuildCategory category;
	public final Kind kind;
	public final List<Cost> cost;
	public final int buildTurns;
	/** 0: it cannot be damaged (a path, a floor). */
	public final int maxHp;
	public final boolean flammable;
	public final Placement placement;
	public final boolean canLine, canArea;
	/** Terrain.*, for a TERRAIN piece; -1 otherwise. */
	public final int terrain;
	/** RoofTiles.THATCH..GLASS, for a ROOF piece; -1 otherwise. */
	public final int roofKind;
	/** For an ACTOR piece; null otherwise. */
	public final ActorFactory actor;
	public final Listener listener;
	/** How many may stand at once (null: no limit). */
	public final Limit limit;
	/** The build menu asks before taking it down (a hearthstone: the home goes with it), with
	 *  removalWarning() as its question. */
	public final boolean confirmRemoval;

	private final String nameKey, descKey;
	private final Object previewTexture;
	private final int previewFrame;

	private BuildPiece( Builder b ){
		id = b.id;
		category = b.category;
		kind = b.kind;
		cost = Collections.unmodifiableList( new ArrayList<>( b.cost ) );
		buildTurns = b.turns;
		maxHp = b.hp;
		flammable = b.flammable;
		placement = b.placement;
		canLine = b.line;
		canArea = b.area;
		terrain = b.terrain;
		roofKind = b.roofKind;
		actor = b.actor;
		listener = b.listener;
		limit = b.limit;
		confirmRemoval = b.confirmRemoval;
		nameKey = "homestead.piece." + id + ".name";
		descKey = "homestead.piece." + id + ".desc";
		previewTexture = b.previewTexture;
		previewFrame = b.previewFrame;
	}

	public String name(){
		return Messages.get( nameKey );
	}

	public String desc(){
		return Messages.get( descKey );
	}

	/** What the build menu asks before taking down a piece that confirmRemoval, from the key
	 *  homestead.piece.ID.remove_warning; null for any other piece. */
	public String removalWarning(){
		return confirmRemoval ? Messages.get( "homestead.piece." + id + ".remove_warning" ) : null;
	}

	/** Can it be hurt at all (a path and a floor cannot). */
	public boolean destructible(){
		return maxHp > 0;
	}

	/** The material it costs most of (the first named on a tie): what a refund always gives back.
	 *  Gold is never it while the piece costs any material (gold is not given back). */
	public Cost mainCost(){
		Cost best = null;
		for (Cost c : cost){
			if (c.gold()) continue;
			if (best == null || c.quantity > best.quantity) best = c;
		}
		if (best == null && !cost.isEmpty()) best = cost.get( 0 );
		return best;
	}

	/** A picture of the piece for the build menu: its tile, its roof, or its station's frame. */
	public Image preview(){
		Image img = new Image( previewTexture );
		//the stations' sheet holds the watchtower's 16x32 frame under its own id
		TextureFilm film = HomesteadSprites.TEXTURE.equals( previewTexture )
				? HomesteadSprites.film() : new TextureFilm( previewTexture, 16, 16 );
		img.frame( film.get( previewFrame ) );
		return img;
	}

	// ----------------------------------------------------------------- the builder

	/** A piece that lays a terrain. Its preview is the terrain's own picture: a homestead terrain's
	 *  from its sheet, one of the land's own (a rug, a statue, the alchemy pot) as the surface's tile
	 *  sheet draws it (HomesteadVisuals.landFrame). A terrain with no fitting picture there (water dug
	 *  for a channel, a well) is given one with {@link Builder#preview}, which overrides either. */
	public static Builder terrain( String id, BuildCategory category, int terrain ){
		Builder b = new Builder( id, category, Kind.TERRAIN );
		b.terrain = terrain;
		if (Terrain.homestead( terrain )){
			b.previewTexture = HomesteadVisuals.previewTexture( terrain );
			b.previewFrame = HomesteadVisuals.previewFrame( terrain );
		} else {
			b.previewTexture = Assets.Environment.TILES_OVERWORLD;
			b.previewFrame = HomesteadVisuals.landFrame( terrain );
		}
		return b;
	}

	/** A roof of a kind (RoofTiles.THATCH..GLASS), laid over one cell. */
	public static Builder roof( String id, int roofKind ){
		Builder b = new Builder( id, BuildCategory.ROOFS, Kind.ROOF );
		b.roofKind = roofKind;
		b.placement = Placement.ROOF;
		b.previewTexture = RoofTiles.TEXTURE;
		b.previewFrame = RoofTiles.index( roofKind, 0 );
		return b;
	}

	/** A station or a piece of furniture: an actor the factory makes, shown by a frame of
	 *  sprites/homestead.png (HomesteadSprites). */
	public static Builder actor( String id, BuildCategory category, ActorFactory factory, int frame ){
		Builder b = new Builder( id, category, Kind.ACTOR );
		b.actor = factory;
		b.previewTexture = HomesteadSprites.TEXTURE;
		b.previewFrame = frame;
		return b;
	}

	public static final class Builder {
		private final String id;
		private final BuildCategory category;
		private final Kind kind;
		private final ArrayList<Cost> cost = new ArrayList<>();
		private int turns = 1, hp = 0;
		private boolean flammable, line, area;
		private Placement placement = Placement.GROUND;
		private int terrain = -1, roofKind = -1;
		private ActorFactory actor;
		private Listener listener;
		private Limit limit;
		private boolean confirmRemoval;
		private Object previewTexture;
		private int previewFrame;

		private Builder( String id, BuildCategory category, Kind kind ){
			if (category.menuOnly()) throw new IllegalArgumentException( "no piece is in " + category );
			this.id = id;
			this.category = category;
			this.kind = kind;
		}

		public Builder cost( Class<? extends Item> material, int quantity ){
			cost.add( new Cost( material, quantity ) );
			return this;
		}

		public Builder turns( int turns ){
			this.turns = turns;
			return this;
		}

		public Builder hp( int hp ){
			this.hp = hp;
			return this;
		}

		public Builder flammable(){
			flammable = true;
			return this;
		}

		public Builder placement( Placement placement ){
			this.placement = placement;
			return this;
		}

		/** May be laid along a line in one go. */
		public Builder line(){
			line = true;
			return this;
		}

		/** May be laid over a rectangle in one go. */
		public Builder area(){
			area = true;
			return this;
		}

		public Builder listener( Listener listener ){
			this.listener = listener;
			return this;
		}

		/** Shows the piece in the menu by this frame (16x16) of this texture instead: an actor whose
		 *  look is a terrain's (the enchanting pedestal stands on the pedestal tile). */
		public Builder preview( Object texture, int frame ){
			previewTexture = texture;
			previewFrame = frame;
			return this;
		}

		/** At most limit.max() of it standing at once (Homestead.Reason.LIMIT). */
		public Builder limit( Limit limit ){
			this.limit = limit;
			return this;
		}

		/** The build menu asks first before taking it down (removalWarning). */
		public Builder confirmRemoval(){
			confirmRemoval = true;
			return this;
		}

		public BuildPiece done(){
			//no picture given for a terrain the homesteads did not draw: HomesteadVisuals' own
			if (previewTexture == null && kind == Kind.TERRAIN){
				previewTexture = HomesteadVisuals.previewTexture( terrain );
				previewFrame = HomesteadVisuals.previewFrame( terrain );
			}
			return new BuildPiece( this );
		}
	}
}
