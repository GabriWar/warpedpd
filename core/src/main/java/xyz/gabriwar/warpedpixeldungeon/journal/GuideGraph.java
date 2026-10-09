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

package xyz.gabriwar.warpedpixeldungeon.journal;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrabKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM300;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonLord;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKingTomb;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShadowYog;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderQueen;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ThiefKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.items.journal.DescentPage;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

//The Descent Guide as a tree: chapters (regions), the pages in them, and the boss
//chapters. Every node's words live in messages/guide: guide.title.<id>, guide.sub.<id>
//and, for a page, guide.<key>. Building the tree touches no strings, so it is cheap and
//can be walked headlessly (GuideGraphTest).
//
//How a page is earned:
//  - floor pages lie on a main-dungeon floor (floor(...)), one floor below the one
//    they describe so the guide never reads ahead of you;
//  - branch pages lie on a floor of a side branch (branch(...));
//  - discovered pages write themselves when you first reach a place or see a thing
//    (tag(...)): the overworld is streamed, nothing can lie on its floor;
//  - boss chapters write themselves the first time you face the boss.
public class GuideGraph {

	public static class Node {
		public final String id;
		public int color;
		public Icons icon = null;
		public Class<? extends Mob> mob = null;
		//key into the guide messages bundle ("guide.<key>"), null = a chapter
		public String key = null;

		//how the page is earned (see the class comment); a chapter is open once any page
		//inside it is, and an always-open node (the root, the topics) never locks
		public int[] depths = null;
		public int branch = 0;
		public String place = null;
		public String tag = null;
		public boolean open = false;

		public Node parent = null;
		public final ArrayList<Node> children = new ArrayList<>();
		public boolean expanded = false;

		//layout, in graph pixels, written by GuideLayout: top-left corner and card size
		public int lx, ly, lw, lh;
		//the layout depth (0 = root)
		public int level;

		public Node( String id, int color ){
			this.id = id;
			this.color = color;
		}

		public Node icon( Icons icon ){
			this.icon = icon;
			return this;
		}

		public Node mob( Class<? extends Mob> mob ){
			this.mob = mob;
			return this;
		}

		public Node key( String key ){
			this.key = key;
			return this;
		}

		//a page on these main-dungeon floors
		public Node floor( int... depths ){
			this.depths = depths;
			return this;
		}

		//a page lying on these floors of a side branch
		public Node branch( int branch, String place, int... depths ){
			this.branch = branch;
			this.place = place;
			this.depths = depths;
			return this;
		}

		//a page that writes itself when the tagged discovery happens
		public Node tag( String tag ){
			this.tag = tag;
			return this;
		}

		public Node open(){
			this.open = true;
			return this;
		}

		public Node child( Node c ){
			c.parent = this;
			children.add( c );
			return this;
		}

		public boolean isPage(){
			return key != null;
		}

		public String title(){
			return Messages.get( "guide.title." + id );
		}

		/** The node's subtitle, or null when it has none. */
		public String subtitle(){
			String s = Messages.get( "guide.sub." + id );
			return s.equals( Messages.NO_TEXT_FOUND ) ? null : s;
		}

		/** The page text, or null when there is none. */
		public String text(){
			if (key == null) return null;
			String s = Messages.get( "guide." + key );
			return s.equals( Messages.NO_TEXT_FOUND ) ? null : s;
		}
	}

	//region accents, roughly matching the guide's tileset-derived palette
	private static final int SEWERS   = 0x59B848;
	private static final int PRISON   = 0xE0A54C;
	private static final int NEST     = 0xB8C060;
	private static final int CAVES    = 0xB07048;
	private static final int TEMPLE   = 0x64BB4C;
	private static final int CITY     = 0x6AA8C8;
	private static final int HALLS    = 0xC04848;
	private static final int FROZEN   = 0x88C8F0;
	private static final int POSTGAME = 0x9AC46A;
	private static final int DENS     = 0xB05888;
	private static final int SOKOBAN  = 0x8878C8;
	private static final int TOWN     = 0x68C8A8;
	private static final int ZOT      = 0xE8E848;
	private static final int SURFACE  = 0x84CC5C;
	private static final int PEAKS    = 0xC8D4E0;
	private static final int DEEP     = 0x9C8CE0;
	private static final int BARROW   = 0x50C0B0;
	private static final int TOPIC    = 0xCCCCCC;

	//--- discoveries: what reaching a place or seeing a thing writes into the guide ---

	public static final String TAG_SURFACE      = "surface";       //first steps on the surface
	public static final String TAG_TOWN_INSIDE  = "town_inside";   //through a door of the town square
	public static final String TAG_VILLAGE      = "village";       //through a village door
	public static final String TAG_WORLD_MAP    = "world_map";     //the world map, opened
	public static final String TAG_WORLD_EVENT  = "world_event";   //a star, a market, a raid or a hunt heard of
	public static final String TAG_BARROW       = "barrow";        //down a barrow's stairs
	public static final String TAG_MOUNTAINS    = "mountains";     //up onto the mountains
	public static final String TAG_HEIGHTS      = "heights";       //up where the air thins (+7 and up)
	public static final String TAG_PEAK_SITE    = "peak_site";     //a place of the heights, found
	public static final String TAG_CAVES        = "caves";         //down into the caves under the world
	public static final String TAG_CAVES_DEEP   = "caves_deep";    //down where the gas pools (-4 and down)
	public static final String TAG_CAVE_SITE    = "cave_site";     //a place of the caves, found

	public static final List<String> ALL_TAGS = Collections.unmodifiableList( Arrays.asList(
			TAG_SURFACE, TAG_TOWN_INSIDE, TAG_VILLAGE, TAG_WORLD_MAP, TAG_WORLD_EVENT, TAG_BARROW,
			TAG_MOUNTAINS, TAG_HEIGHTS, TAG_PEAK_SITE, TAG_CAVES, TAG_CAVES_DEEP, TAG_CAVE_SITE ) );

	//the save numbering of the places (see Dungeon.newLevel, WorldLayers, Delves)
	static final int SURFACE_DEPTH = 97;
	static final int TOP_PEAK = 87, HEIGHTS_FROM = 90, FIRST_PEAK = 96;
	static final int FIRST_CAVE = 101, DEEP_CAVES_FROM = 104, LAST_CAVE = 112;
	static final int TOWN_INTERIORS = 6, VILLAGE_HOUSE = 7, BARROWS_FROM = 100;

	/** The discoveries arriving on a level makes. */
	public static ArrayList<String> arrivalTags( int depth, int branch ){
		ArrayList<String> tags = new ArrayList<>();
		if (branch == 0){
			if (depth == SURFACE_DEPTH) tags.add( TAG_SURFACE );
			if (depth >= TOP_PEAK && depth <= FIRST_PEAK) tags.add( TAG_MOUNTAINS );
			if (depth >= TOP_PEAK && depth <= HEIGHTS_FROM) tags.add( TAG_HEIGHTS );
			if (depth >= FIRST_CAVE && depth <= LAST_CAVE) tags.add( TAG_CAVES );
			if (depth >= DEEP_CAVES_FROM && depth <= LAST_CAVE) tags.add( TAG_CAVES_DEEP );
		} else if (branch == TOWN_INTERIORS){
			tags.add( TAG_TOWN_INSIDE );
		} else if (branch == VILLAGE_HOUSE){
			tags.add( TAG_VILLAGE );
		} else if (branch >= BARROWS_FROM){
			tags.add( TAG_BARROW );
		}
		return tags;
	}

	/** Records every unfound page the discovery writes. Returns the keys it wrote. */
	public static ArrayList<String> discover( String tag ){
		ArrayList<String> found = new ArrayList<>();
		discover( build(), tag, found );
		return found;
	}

	private static void discover( Node n, String tag, ArrayList<String> out ){
		if (n.key != null && tag.equals( n.tag ) && GuideProgress.findPage( n.key )){
			out.add( n.key );
		}
		for (Node c : n.children) discover( c, tag, out );
	}

	/** discover(), then tells the player: a line in the log and the journal button flashing. */
	public static void reveal( String tag ){
		for (String key : discover( tag )){
			DescentPage.announce( key );
		}
	}

	/** Reveals what arriving on this level discovers. */
	public static void onArrive( int depth, int branch ){
		for (String tag : arrivalTags( depth, branch )){
			reveal( tag );
		}
	}

	/** A boss was faced (its health bar came up) or fell: its chapter writes itself, once. */
	public static void faced( Class<?> mob ){
		String key = keyForMob( mob );
		if (key != null && GuideProgress.findPage( key )){
			DescentPage.announce( key );
		}
	}

	//--- unlocking ---

	public static boolean unlocked( Node n ){
		if (n.open) return true;
		//bosses: facing the creature is the knowledge. Tracked by the guide
		// itself (faced(): its health bar coming up, or its kill) so old bestiary
		// data doesn't pre-unlock.
		if (n.key != null){
			if (n.mob != null || n.depths != null || n.tag != null){
				return GuideProgress.pageFound( n.key );
			}
			//a page with no way to earn it is common knowledge
			return true;
		}
		//chapters: visible once any part is known
		for (Node c : n.children){
			if (unlocked( c )) return true;
		}
		return false;
	}

	/** Unlocked and not yet opened in the guide. */
	public static boolean isNew( Node n ){
		return n.key != null && unlocked( n ) && !GuideProgress.isRead( n.key );
	}

	/** Pages in this subtree: {unlocked, total, new}. */
	public static int[] progress( Node n ){
		int[] out = new int[3];
		progress( n, out );
		return out;
	}

	private static void progress( Node n, int[] out ){
		if (n.key != null){
			out[1]++;
			if (unlocked( n )){
				out[0]++;
				if (!GuideProgress.isRead( n.key )) out[2]++;
			}
		}
		for (Node c : n.children) progress( c, out );
	}

	public static String lockHint( Node n ){
		if (n.mob != null){
			return Messages.get( "guide.hint.boss" );
		}
		if (n.key == null){
			return Messages.get( "guide.hint.chapter" );
		}
		if (n.tag != null){
			return Messages.get( "guide.hint.tag_" + n.tag );
		}
		if (n.place != null){
			return Messages.get( "guide.hint.place_" + n.place );
		}
		if (n.depths != null){
			int min = Integer.MAX_VALUE;
			for (int d : n.depths) min = Math.min( min, d );
			if (shiftedPage( n )) min = spawnDepthFor( min );
			return Messages.get( "guide.hint.floor", min );
		}
		return "";
	}

	//floor pages describe the floor ABOVE where they drop: you find the page
	// of depth N while on depth N+1, so the guide never reads ahead of you.
	// Overviews (region-wide) and topics (general systems) drop unshifted.
	static boolean shiftedPage( Node n ){
		return n.key != null && n.mob == null && n.place == null && n.tag == null
				&& !n.key.startsWith( "topic_" ) && !n.key.endsWith( "_overview" );
	}

	//linear stretches read one floor behind you; teleport specials
	// (postgame zones, boss dens, sokoban dimensions) and the last floor
	// of each chain keep their page on the floor itself
	static int spawnDepthFor( int described ){
		if (described >= 1 && described <= 25) return described + 1;   //sewers..halls, ends at 26
		if (described >= 56 && described <= 64) return described + 1;  //the town's mines, ends at 65
		return described;
	}

	/** The floor (main dungeon, or the page's own branch) a page lies on, or -1 for a page
	 *  that is never lying anywhere. */
	public static int spawnDepth( Node n ){
		if (n.key == null || n.mob != null || n.tag != null || n.depths == null) return -1;
		int min = Integer.MAX_VALUE;
		for (int d : n.depths) min = Math.min( min, shiftedPage( n ) ? spawnDepthFor( d ) : d );
		return min;
	}

	//keys of pages that should be lying on the floor of this level:
	// nodes whose page hasn't been found and whose home matches depth+branch
	public static ArrayList<String> pagesToSpawn( int depth, int branch ){
		ArrayList<String> keys = new ArrayList<>();
		collectPages( build(), depth, branch, keys );
		return keys;
	}

	private static void collectPages( Node n, int depth, int branch, ArrayList<String> keys ){
		if (n.key != null && n.mob == null && n.tag == null && n.depths != null
				&& n.branch == branch && !GuideProgress.pageFound( n.key )){
			boolean shifted = shiftedPage( n );
			for (int d : n.depths){
				if ((shifted ? spawnDepthFor( d ) : d) == depth){
					keys.add( n.key );
					break;
				}
			}
		}
		for (Node c : n.children){
			collectPages( c, depth, branch, keys );
		}
	}

	//--- lookups ---

	//guide key for a mob class, if any boss node covers it
	private static HashMap<Class<?>, String> mobKeys = null;

	public static String keyForMob( Class<?> cls ){
		if (mobKeys == null){
			HashMap<Class<?>, String> keys = new HashMap<>();
			collectMobKeys( build(), keys );
			mobKeys = keys;
		}
		return mobKeys.get( cls );
	}

	private static void collectMobKeys( Node n, HashMap<Class<?>, String> out ){
		if (n.mob != null && n.key != null){
			out.put( n.mob, n.key );
		}
		for (Node c : n.children){
			collectMobKeys( c, out );
		}
	}

	//every guide key in the tree (pages + boss chapters), for debug fills
	public static ArrayList<String> allKeys(){
		ArrayList<String> keys = new ArrayList<>();
		collectKeys( build(), keys );
		return keys;
	}

	private static void collectKeys( Node n, ArrayList<String> out ){
		if (n.key != null) out.add( n.key );
		for (Node c : n.children){
			collectKeys( c, out );
		}
	}

	/** Every node of the tree, depth first. */
	public static ArrayList<Node> all( Node root ){
		ArrayList<Node> out = new ArrayList<>();
		collectAll( root, out );
		return out;
	}

	private static void collectAll( Node n, ArrayList<Node> out ){
		out.add( n );
		for (Node c : n.children) collectAll( c, out );
	}

	public static Node find( Node root, String key ){
		if (key == null) return null;
		if (key.equals( root.key ) || key.equals( root.id )) return root;
		for (Node c : root.children){
			Node f = find( c, key );
			if (f != null) return f;
		}
		return null;
	}

	//title of the node a page belongs to, for item descriptions and the log
	public static String titleForKey( String key ){
		Node n = find( build(), key );
		return n == null ? null : fullTitle( n );
	}

	/** A node's title, with its chapter's in front where the title alone says little (every
	 *  chapter has an "Overview"). */
	public static String fullTitle( Node n ){
		if (n.parent != null && n.parent.parent != null && n.id.endsWith( "_overview" )){
			return n.parent.title() + " - " + n.title();
		}
		return n.title();
	}

	//--- the tree ---

	private static Node page( String key, int color, Icons icon ){
		return new Node( key, color ).key( key ).icon( icon );
	}

	private static Node boss( String key, int color, Class<? extends Mob> mob ){
		return new Node( key, color ).key( key ).mob( mob );
	}

	public static Node build(){

		Node root = new Node( "guide", 0xFFFFFF ).icon( Icons.WPD ).open();
		root.expanded = true;

		//the world above: every run starts in the town on its surface
		Node surface = new Node( "ch_surface", SURFACE ).icon( Icons.STAIRS_GRASS );
		surface.child( page( "surface_overview", SURFACE, Icons.INFO ).tag( TAG_SURFACE ) );
		surface.child( page( "surface_villages", SURFACE, Icons.DEPTH_GRASS ).tag( TAG_VILLAGE ) );
		surface.child( page( "surface_wilds", SURFACE, Icons.COMPASS ).tag( TAG_WORLD_MAP ) );
		surface.child( page( "surface_events", SURFACE, Icons.ALERT ).tag( TAG_WORLD_EVENT ) );
		surface.child( page( "surface_barrows", BARROW, Icons.STAIRS_TRAPS ).tag( TAG_BARROW ) );
		surface.child( boss( "boss_dragon", SURFACE, OverworldDragon.class ) );
		Node peaks = new Node( "ch_peaks", PEAKS ).icon( Icons.STAIRS_CHASM );
		peaks.child( page( "peaks_overview", PEAKS, Icons.INFO ).tag( TAG_MOUNTAINS ) );
		peaks.child( page( "peaks_places", PEAKS, Icons.DEPTH_CHASM ).tag( TAG_PEAK_SITE ) );
		peaks.child( page( "peaks_heights", PEAKS, Icons.WARNING ).tag( TAG_HEIGHTS ) );
		surface.child( peaks );
		Node deep = new Node( "ch_deep", DEEP ).icon( Icons.STAIRS_DARK );
		deep.child( page( "deep_overview", DEEP, Icons.INFO ).tag( TAG_CAVES ) );
		deep.child( page( "deep_places", DEEP, Icons.DEPTH_DARK ).tag( TAG_CAVE_SITE ) );
		deep.child( page( "deep_hazards", DEEP, Icons.WARNING ).tag( TAG_CAVES_DEEP ) );
		surface.child( deep );
		root.child( surface );

		Node town = new Node( "ch_town", TOWN ).icon( Icons.STAIRS_GRASS );
		town.child( page( "town_overview", TOWN, Icons.INFO ).tag( TAG_SURFACE ) );
		town.child( page( "town_dolyahaven", TOWN, Icons.DEPTH_GRASS ).tag( TAG_TOWN_INSIDE ) );
		Node mines = new Node( "ch_mines", TOWN ).icon( Icons.STAIRS );
		for (int i = 1; i <= 9; i++){
			mines.child( page( "mines_f" + i, TOWN, Icons.DEPTH ).floor( 55 + i ) );
		}
		mines.child( page( "mines_boss", TOWN, Icons.DEPTH_LARGE ).floor( 65 ) );
		mines.child( boss( "boss_otiluke", TOWN, Otiluke.class ) );
		town.child( mines );
		town.child( page( "town_dragoncave", TOWN, Icons.DEPTH_DARK ).floor( 67 ) );
		root.child( town );

		Node sewers = new Node( "ch_sewers", SEWERS ).icon( Icons.STAIRS );
		sewers.child( page( "sewers_overview", SEWERS, Icons.INFO ).floor( 1 ) );
		sewers.child( page( "sewers_f1", SEWERS, Icons.DEPTH ).floor( 1 ) );
		sewers.child( page( "sewers_f2", SEWERS, Icons.DEPTH ).floor( 2 ) );
		sewers.child( page( "sewers_f3", SEWERS, Icons.DEPTH ).floor( 3 ) );
		sewers.child( page( "sewers_f4", SEWERS, Icons.DEPTH ).floor( 4 ) );
		sewers.child( page( "sewers_f5", SEWERS, Icons.DEPTH_LARGE ).floor( 5 ) );
		sewers.child( boss( "boss_goo", SEWERS, Goo.class ) );
		root.child( sewers );

		Node prison = new Node( "ch_prison", PRISON ).icon( Icons.STAIRS );
		prison.child( page( "prison_overview", PRISON, Icons.INFO ).floor( 6 ) );
		prison.child( page( "prison_f1", PRISON, Icons.DEPTH ).floor( 6 ) );
		prison.child( page( "prison_f2", PRISON, Icons.DEPTH ).floor( 7 ) );
		prison.child( page( "prison_f3", PRISON, Icons.DEPTH ).floor( 8 ) );
		prison.child( page( "prison_f4", PRISON, Icons.DEPTH ).floor( 9 ) );
		prison.child( page( "prison_f5", PRISON, Icons.DEPTH_LARGE ).floor( 10 ) );
		prison.child( boss( "boss_tengu", PRISON, Tengu.class ) );
		Node nest = new Node( "ch_nest", NEST ).icon( Icons.STAIRS_TRAPS );
		nest.child( page( "nest_overview", NEST, Icons.DEPTH_TRAPS ).branch( 5, "nest", 6 ) );
		nest.child( boss( "boss_spiderqueen", NEST, SpiderQueen.class ) );
		prison.child( nest );
		root.child( prison );

		Node caves = new Node( "ch_caves", CAVES ).icon( Icons.STAIRS );
		caves.child( page( "caves_overview", CAVES, Icons.INFO ).floor( 11 ) );
		caves.child( page( "caves_f1", CAVES, Icons.DEPTH ).floor( 11 ) );
		caves.child( page( "caves_f2", CAVES, Icons.DEPTH ).floor( 12 ) );
		caves.child( page( "caves_mine", CAVES, Icons.DEPTH_SECRETS ).branch( 1, "mine", 12, 13, 14 ) );
		caves.child( page( "caves_f3", CAVES, Icons.DEPTH ).floor( 13 ) );
		caves.child( page( "caves_f4", CAVES, Icons.DEPTH ).floor( 14 ) );
		caves.child( page( "temple_overview", TEMPLE, Icons.ALTAR_SHRINE ).branch( 3, "temple", 14 ) );
		caves.child( page( "caves_f5", CAVES, Icons.DEPTH_LARGE ).floor( 15 ) );
		caves.child( boss( "boss_dm300", CAVES, DM300.class ) );
		root.child( caves );

		Node city = new Node( "ch_city", CITY ).icon( Icons.STAIRS );
		city.child( page( "city_overview", CITY, Icons.INFO ).floor( 16 ) );
		city.child( page( "city_f1", CITY, Icons.DEPTH ).floor( 16 ) );
		city.child( page( "city_f2", CITY, Icons.DEPTH ).floor( 17 ) );
		city.child( page( "city_vault", CITY, Icons.DEPTH_SECRETS ).branch( 1, "vault", 16, 17, 18, 19 ) );
		city.child( page( "city_f3", CITY, Icons.DEPTH ).floor( 18 ) );
		city.child( page( "city_f4", CITY, Icons.DEPTH ).floor( 19 ) );
		city.child( page( "city_f5", CITY, Icons.DEPTH_LARGE ).floor( 20 ) );
		city.child( boss( "boss_dwarfking", CITY, DwarfKing.class ) );
		city.child( boss( "boss_dwarftomb", CITY, DwarfKingTomb.class ) );
		root.child( city );

		Node halls = new Node( "ch_halls", HALLS ).icon( Icons.STAIRS_DARK );
		halls.child( page( "halls_overview", HALLS, Icons.INFO ).floor( 21 ) );
		halls.child( page( "halls_f1", HALLS, Icons.DEPTH_DARK ).floor( 21 ) );
		halls.child( page( "halls_f2", HALLS, Icons.DEPTH_DARK ).floor( 22 ) );
		halls.child( page( "halls_f3", HALLS, Icons.DEPTH_DARK ).floor( 23 ) );
		halls.child( page( "halls_f4", HALLS, Icons.DEPTH_DARK ).floor( 24 ) );
		halls.child( page( "halls_f5", HALLS, Icons.DEPTH_LARGE ).floor( 25 ) );
		halls.child( page( "halls_end", HALLS, Icons.STAIRS_LARGE ).floor( 26 ) );
		halls.child( boss( "boss_yog", HALLS, YogDzewa.class ) );
		Node frozen = new Node( "ch_frozen", FROZEN ).icon( Icons.STAIRS_WATER );
		frozen.child( page( "frozen_overview", FROZEN, Icons.DEPTH_WATER ).branch( 2, "frozen", 21 ) );
		frozen.child( boss( "boss_demonlord", FROZEN, DemonLord.class ) );
		halls.child( frozen );
		root.child( halls );

		Node postgame = new Node( "ch_postgame", POSTGAME ).icon( Icons.STAIRS_GRASS );
		postgame.child( page( "postgame_overview", POSTGAME, Icons.INFO ).floor( 27, 28, 29, 30, 31, 32, 33 ) );
		postgame.child( page( "pg_field", POSTGAME, Icons.DEPTH_GRASS ).floor( 27 ) );
		postgame.child( page( "pg_battle", POSTGAME, Icons.DEPTH_GRASS ).floor( 28 ) );
		postgame.child( page( "pg_fishing", POSTGAME, Icons.DEPTH_WATER ).floor( 29 ) );
		postgame.child( page( "pg_vault", POSTGAME, Icons.DEPTH ).floor( 30 ) );
		postgame.child( page( "pg_catacomb", POSTGAME, Icons.DEPTH_DARK ).floor( 31 ) );
		postgame.child( page( "pg_fortress", POSTGAME, Icons.DEPTH ).floor( 32 ) );
		postgame.child( page( "pg_chasm", POSTGAME, Icons.DEPTH_CHASM ).floor( 33 ) );
		root.child( postgame );

		Node dens = new Node( "ch_dens", DENS ).icon( Icons.STAIRS_TRAPS );
		dens.child( page( "dens_overview", DENS, Icons.INFO ).floor( 35, 36, 37, 38, 40, 41 ) );
		dens.child( page( "den_infest", DENS, Icons.DEPTH_LARGE ).floor( 35 ) );
		dens.child( page( "den_tengu", DENS, Icons.DEPTH_LARGE ).floor( 36 ) );
		dens.child( page( "den_skeleton", DENS, Icons.DEPTH_LARGE ).floor( 37 ) );
		dens.child( page( "den_crab", DENS, Icons.DEPTH_WATER ).floor( 38 ) );
		dens.child( page( "den_thief", DENS, Icons.DEPTH_LARGE ).floor( 40 ) );
		dens.child( page( "den_thiefcatch", DENS, Icons.DEPTH_TRAPS ).floor( 41 ) );
		dens.child( boss( "boss_shadowyog", DENS, ShadowYog.class ) );
		dens.child( boss( "boss_skeletonking", DENS, SkeletonKing.class ) );
		dens.child( boss( "boss_crabking", DENS, CrabKing.class ) );
		dens.child( boss( "boss_thiefking", DENS, ThiefKing.class ) );
		root.child( dens );

		Node sokoban = new Node( "ch_sokoban", SOKOBAN ).icon( Icons.STAIRS_SECRETS );
		sokoban.child( page( "sokoban_overview", SOKOBAN, Icons.INFO ).floor( 50, 51, 52, 53, 54, 66 ) );
		sokoban.child( page( "sok_saferoom", SOKOBAN, Icons.DEPTH ).floor( 50 ) );
		sokoban.child( page( "sok_practice", SOKOBAN, Icons.DEPTH ).floor( 51 ) );
		sokoban.child( page( "sok_castle", SOKOBAN, Icons.DEPTH ).floor( 52 ) );
		sokoban.child( page( "sok_portals", SOKOBAN, Icons.DEPTH ).floor( 53 ) );
		sokoban.child( page( "sok_puzzles", SOKOBAN, Icons.DEPTH ).floor( 54 ) );
		sokoban.child( page( "sok_vault", SOKOBAN, Icons.DEPTH_SECRETS ).floor( 66 ) );
		root.child( sokoban );

		Node zot = new Node( "ch_zot", ZOT ).icon( Icons.STAIRS_DARK );
		zot.child( page( "zot_overview", ZOT, Icons.INFO ).floor( 99 ) );
		zot.child( page( "zot_level", ZOT, Icons.DEPTH_DARK ).floor( 99 ) );
		zot.child( boss( "boss_zot", ZOT, Zot.class ) );
		root.child( zot );

		Node topics = new Node( "ch_topics", TOPIC ).icon( Icons.JOURNAL ).open();
		topics.child( page( "topic_meta", TOPIC, Icons.INFO ) );
		topics.child( page( "topic_heroes", TOPIC, Icons.TALENT ) );
		topics.child( page( "topic_weather", TOPIC, Icons.CALENDAR ) );
		topics.child( page( "topic_quests", TOPIC, Icons.SCROLL_COLOR ).floor( 2 ) );
		topics.child( page( "topic_plants", TOPIC, Icons.GRASS ).floor( 2 ) );
		topics.child( page( "topic_skills", TOPIC, Icons.BUFFS ).floor( 3 ) );
		topics.child( page( "topic_rarity", TOPIC, Icons.CATALOG ).floor( 4 ) );
		topics.child( page( "topic_loot", TOPIC, Icons.GOLD ).floor( 6 ) );
		topics.child( page( "topic_rooms", TOPIC, Icons.STAIRS_SECRETS ).floor( 7 ) );
		topics.child( page( "topic_bestiary", TOPIC, Icons.SKULL ).floor( 10 ) );
		topics.child( page( "topic_endings", TOPIC, Icons.STAIRS_LARGE ).floor( 26 ) );
		root.child( topics );

		return root;
	}
}
