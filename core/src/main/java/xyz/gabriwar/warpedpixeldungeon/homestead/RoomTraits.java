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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * What a room BECOMES from what is in it: a bed makes a bedroom, a hearth a kitchen, two chests a
 * storehouse. Each PROVIDER looks at a room (Rooms.Room: its cells, walls, roofs, what stands
 * there) and adds to its Traits:
 *   - LABELS, an id and the name shown for it ("bedroom", "Bedroom"): the first label added names
 *     the room (Rooms.describe), and any part asks Traits.is(id) - the bed's sleep asks "bedroom";
 *   - LINES, what a look at the room tells of them (Rooms.describe adds them);
 *   - EFFECTS, run once a world turn for the hero standing in the room (on the host:
 *     Homestead.worldTurn), each with the room it is for.
 * Nothing is kept: the traits are worked out whenever they are asked for, so they always follow
 * what is in the room. A provider must be quick about it (what stands on the room's own cells).
 *
 * Providers are looked up by class name from PROVIDERS (a missing one is skipped), each a public
 * class with a public no-argument constructor implementing Provider; one made in code (a test, a
 * debug scene) is added with register.
 *
 * <pre>
 * public class RoomKinds implements RoomTraits.Provider {
 *     public void describe( OverworldLevel level, Rooms.Room room, RoomTraits.Traits out ){
 *         if (any bed stands on room.cells){
 *             out.label( "bedroom", Messages.get( this, "bedroom" ) );
 *             out.line( Messages.get( this, "bedroom_line" ) );
 *             out.effect( (l, r, hero) -> ... );
 *         }
 *     }
 * }
 * </pre>
 */
public final class RoomTraits {

	private RoomTraits(){}

	/** A part of the homesteads that gives rooms their traits. */
	public interface Provider {
		void describe( OverworldLevel level, Rooms.Room room, Traits out );
	}

	/** What a trait does for the hero in its room, once a world turn (host only). */
	public interface Effect {
		void apply( OverworldLevel level, Rooms.Room room, Hero hero );
	}

	/** One room's traits, as the providers gave them. */
	public static final class Traits {
		private final LinkedHashMap<String, String> labels = new LinkedHashMap<>();
		private final ArrayList<String> lines = new ArrayList<>();
		private final ArrayList<Effect> effects = new ArrayList<>();
		private final boolean text;

		Traits( boolean text ){
			this.text = text;
		}

		/** False when only the effects are asked for (the world turn, every turn the hero spends in
		 *  a room): a provider that gives no effects may return at once, and one that does may
		 *  leave its labels and lines out. */
		public boolean wantsText(){
			return text;
		}

		/** Gives the room a label (an id other parts ask by, and the name shown); the first names it. */
		public void label( String id, String name ){
			if (!labels.containsKey( id )) labels.put( id, name );
		}

		/** A line for a look at the room. */
		public void line( String text ){
			lines.add( text );
		}

		/** Something the room does for the hero in it, every world turn. */
		public void effect( Effect e ){
			effects.add( e );
		}

		public boolean is( String id ){
			return labels.containsKey( id );
		}

		/** The label ids, in the order given. */
		public List<String> labels(){
			return Collections.unmodifiableList( new ArrayList<>( labels.keySet() ) );
		}

		/** The room's name: its first label's, or null for a plain room. */
		public String name(){
			return labels.isEmpty() ? null : labels.values().iterator().next();
		}

		public List<String> lines(){
			return Collections.unmodifiableList( lines );
		}

		public List<Effect> effects(){
			return Collections.unmodifiableList( effects );
		}
	}

	private static final String PKG = "xyz.gabriwar.warpedpixeldungeon.homestead.";

	/** The providers, by class name, in the order they are asked (the first label names a room). */
	public static final String[] PROVIDERS = {
			PKG + "rooms.RoomKinds",        //rooms: bedroom, kitchen, storehouse, shrine, comfort
			PKG + "stations.StationRooms",  //stations: the workshop
			PKG + "farming.FarmRooms"       //farming: the greenhouse
	};

	private static final Providers<Provider> providers = new Providers<>( Provider.class, PROVIDERS );

	/** Adds a provider made in code (a test, a debug scene). */
	public static void register( Provider p ){
		providers.add( p );
	}

	public static void unregister( Provider p ){
		providers.remove( p );
	}

	/** A room's traits, as every provider sees it now. */
	public static Traits of( OverworldLevel level, Rooms.Room room ){
		return of( level, room, true );
	}

	private static Traits of( OverworldLevel level, Rooms.Room room, boolean text ){
		Traits t = new Traits( text );
		for (Provider p : providers.all()) p.describe( level, room, t );
		return t;
	}

	/** Is the room labelled so by any provider? */
	public static boolean is( OverworldLevel level, Rooms.Room room, String id ){
		return of( level, room ).is( id );
	}

	/** Once a world turn on a slice (Homestead.worldTurn, the host's): the traits of the room the
	 *  hero stands in do what they do for him. */
	public static void worldTurn( OverworldLevel level ){
		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.level != level || !hero.isAlive()) return;
		Rooms.Room room = Rooms.at( level, hero.pos );
		if (room == null) return;
		for (Effect e : of( level, room, false ).effects()) e.apply( level, room, hero );
	}
}
