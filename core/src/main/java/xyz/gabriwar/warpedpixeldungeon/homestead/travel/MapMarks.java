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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the homesteads pin on the world map (WndWorldMap): every HOME (a house, its name beside it in
 * the homes' gold), every WAYSTONE the hero has attuned (bright) or only seen (dim; one never seen is
 * not shown, and on the surface a waystone's settlement is already named on the map, so there it
 * carries no name of its own), and whatever the PROVIDERS add - the threats' part marks a home under
 * threat. A home or a waystone on another slice than the map's is pinned faded, its slice in its
 * name. A tap on a mark of the map's own slice sets the march's waypoint on it.
 *
 * A provider is a public class with a public no-argument constructor implementing Provider, named in
 * PROVIDERS (looked up by name, a missing one skipped); one made in code is added with register.
 *
 * <pre>
 * public class ThreatMarks implements MapMarks.Provider {
 *     public void marks( int depth, List&lt;MapMarks.Mark&gt; out ){
 *         for (HomesteadWorld.Home h : threatened()) out.add( new MapMarks.Mark( MapMarks.Icon.DOT, h.depth,
 *                 h.wx, h.wy, 0xFF3333, Messages.get( this, "under_threat" ), true, 1f, true ) );
 *     }
 * }
 * </pre>
 */
public final class MapMarks {

	private MapMarks(){}

	/** What a mark is drawn as: a dot in its colour, a house (BuildIcons.ROOFS), or a waystone
	 *  (BuildIcons.TRAVEL). */
	public enum Icon { DOT, HOME, WAYSTONE }

	/** One mark on the map. */
	public static final class Mark {
		public final Icon icon;
		/** The slice it is on (WorldLayers.depthOf) and its world cell. */
		public final int depth, wx, wy;
		/** Its colour (0xRRGGBB): a dot's own, a picture's tint and its name's. */
		public final int color;
		/** What it is called (the waypoint's message names it). */
		public final String name;
		/** Is its name written beside it? */
		public final boolean labelled;
		/** How plainly it shows, 0..1. */
		public final float alpha;
		/** Does it blink (a home under threat)? */
		public final boolean blink;

		public Mark( Icon icon, int depth, int wx, int wy, int color, String name, boolean labelled, float alpha, boolean blink ){
			this.icon = icon;
			this.depth = depth;
			this.wx = wx;
			this.wy = wy;
			this.color = color;
			this.name = name;
			this.labelled = labelled;
			this.alpha = alpha;
			this.blink = blink;
		}
	}

	/** A part of the homesteads with marks for the map. */
	public interface Provider {
		/** Adds its marks for a map shown from a slice (its depth). */
		void marks( int depth, List<Mark> out );
	}

	/** The providers, by class name. */
	public static final String[] PROVIDERS = {
			"xyz.gabriwar.warpedpixeldungeon.homestead.threats.ThreatMarks"   //threats: a home under threat
	};

	/** A home's colour on the map. */
	public static final int HOME = 0xFFD27F;
	/** A waystone's. */
	public static final int STONE = Travel.RUNE;
	/** How plainly a waystone only seen shows, and a home on another slice. */
	public static final float DIM = 0.45f, OTHER_SLICE = 0.65f;

	private static List<Provider> named;
	private static final ArrayList<Provider> added = new ArrayList<>();

	public static synchronized void register( Provider p ){
		if (!added.contains( p )) added.add( p );
	}

	public static synchronized void unregister( Provider p ){
		added.remove( p );
	}

	private static synchronized List<Provider> providers(){
		if (named == null){
			ArrayList<Provider> out = new ArrayList<>();
			for (String name : PROVIDERS){
				Class<?> cls;
				try {
					cls = Class.forName( name );
				} catch (ClassNotFoundException e){
					continue;
				}
				try {
					out.add( (Provider) cls.getConstructor().newInstance() );
				} catch (ReflectiveOperationException | ClassCastException e){
					throw new IllegalStateException( name + " is not a public MapMarks.Provider", e );
				}
			}
			named = Collections.unmodifiableList( out );
		}
		ArrayList<Provider> all = new ArrayList<>( named );
		all.addAll( added );
		return all;
	}

	/** Every mark for a map shown from a slice (its depth): the homes, the waystones known, then
	 *  the providers'. On a co-op guest's screen the homes and waystones are the host's (TravelShare). */
	public static List<Mark> all( int depth ){
		ArrayList<Mark> out = new ArrayList<>();
		for (HomesteadWorld.Home h : TravelShare.homes()){
			boolean here = h.depth == depth;
			String name = here ? h.title() : Messages.get( MapMarks.class, "elsewhere", h.title(), WndTravel.slice( h.depth ) );
			out.add( new Mark( Icon.HOME, h.depth, h.wx, h.wy, HOME, name, true, here ? 1f : OTHER_SLICE, false ) );
		}
		//the waystones all stand on the surface: seen from another slice they are faded and named
		//with where they are, as a home elsewhere is (on the surface their settlement names them)
		boolean surface = depth == WorldLayers.SURFACE_DEPTH;
		for (TravelWorld.Stone s : TravelShare.stones()){
			float alpha = (s.attuned ? 1f : DIM) * (surface ? 1f : OTHER_SLICE);
			String name = surface ? s.name
					: Messages.get( MapMarks.class, "elsewhere", s.name, WndTravel.slice( WorldLayers.SURFACE_DEPTH ) );
			out.add( new Mark( Icon.WAYSTONE, WorldLayers.SURFACE_DEPTH, s.wx, s.wy, STONE, name, !surface, alpha, false ) );
		}
		for (Provider p : providers()) p.marks( depth, out );
		return out;
	}
}
