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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShadowYog;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ThiefKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

import java.util.ArrayList;

//In-game node-graph version of the Descent Guide (GUIA.html).
//POC: real structure of the guide, in English. Node body text comes later.
public class GuideGraph {

	public static class Node {
		public String title;
		public String subtitle;
		public int color;
		public Icons icon = null;
		public Class<? extends Mob> mob = null;
		//key into the guide messages bundle ("guide.<key>"), null = no page
		public String key = null;

		//unlock requirements: mob nodes gate on Bestiary.isSeen; otherwise
		// any visited depth in unlockDepths, or a visited branch, unlocks.
		// all null = always unlocked.
		public int[] unlockDepths = null;
		public String unlockBranch = null; //"mine" or "vault"

		public Node parent = null;
		public final ArrayList<Node> children = new ArrayList<>();
		public boolean expanded = false;

		//model-space layout coords, written by GuideScene
		public float mx, my;

		public Node( String title, String subtitle, int color ){
			this.title = title;
			this.subtitle = subtitle;
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

		public Node unlock( int... depths ){
			this.unlockDepths = depths;
			return this;
		}

		public Node unlockBranch( String branch ){
			this.unlockBranch = branch;
			return this;
		}

		public Node child( Node c ){
			c.parent = this;
			children.add( c );
			return this;
		}
	}

	//region accents, roughly matching the guide's tileset-derived palette
	private static final int SEWERS   = 0x59B848;
	private static final int PRISON   = 0xE0A54C;
	private static final int CAVES    = 0xB07048;
	private static final int CITY     = 0x6AA8C8;
	private static final int HALLS    = 0xC04848;
	private static final int POSTGAME = 0x9AC46A;
	private static final int DENS     = 0xB05888;
	private static final int SOKOBAN  = 0x8878C8;
	private static final int TOWN     = 0x68C8A8;
	private static final int ZOT      = 0xE8E848;
	private static final int TOPIC    = 0xCCCCCC;

	public static boolean unlocked( Node n ){
		//bosses: facing the creature is the knowledge. Tracked by the guide
		// itself (via Bestiary.setSeen) so old bestiary data doesn't pre-unlock.
		if (n.mob != null){
			return n.key != null && GuideProgress.pageFound( n.key );
		}
		//pages: the knowledge must be picked up off the dungeon floor
		if (n.key != null && (n.unlockDepths != null || n.unlockBranch != null)){
			return GuideProgress.pageFound( n.key );
		}
		//structural nodes (regions, chapters): visible once any part is known
		if (n.key == null && !n.children.isEmpty()
				&& (n.unlockDepths != null || n.unlockBranch != null)){
			for (Node c : n.children){
				if (unlocked( c )) return true;
			}
			return false;
		}
		return true;
	}

	public static String lockHint( Node n ){
		if (n.mob != null){
			return "Face it, and its chapter will write itself";
		}
		if (n.unlockBranch != null){
			return "mine".equals( n.unlockBranch )
					? "Its torn page lies somewhere in the smith's mine"
					: "Its torn page lies somewhere in the hidden vault";
		}
		if (n.unlockDepths != null){
			if (n.key == null){
				return "Find any page from this chapter";
			}
			int min = Integer.MAX_VALUE;
			for (int d : n.unlockDepths) min = Math.min( min, d );
			if (shiftedPage( n )) min = spawnDepthFor( min );
			return "Its torn page lies somewhere on depth " + min;
		}
		return "";
	}

	//floor pages describe the floor ABOVE where they drop: you find the page
	// of depth N while on depth N+1, so the guide never reads ahead of you.
	// Overviews (region-wide) and topics (general systems) drop unshifted.
	private static boolean shiftedPage( Node n ){
		return n.key != null && n.mob == null && n.unlockBranch == null
				&& !n.key.startsWith( "topic_" ) && !n.key.endsWith( "_overview" );
	}

	//linear stretches read one floor behind you; teleport specials
	// (postgame zones, boss dens, sokoban dimensions) and the last floor
	// of each chain keep their page on the floor itself
	private static int spawnDepthFor( int described ){
		if (described >= 1 && described <= 25) return described + 1;   //sewers..halls, ends at 26
		if (described >= 55 && described <= 64) return described + 1;  //dolyahaven chain, ends at 65
		return described;
	}

	//keys of pages that should be lying on the floor of this level:
	// nodes whose page hasn't been found and whose home matches depth+branch
	public static ArrayList<String> pagesToSpawn( int depth, int branch ){
		ArrayList<String> keys = new ArrayList<>();
		collectPages( build(), depth, branch, keys );
		return keys;
	}

	private static void collectPages( Node n, int depth, int branch, ArrayList<String> keys ){
		if (n.key != null && n.mob == null && !GuideProgress.pageFound( n.key )){
			if (branch == 0 && n.unlockDepths != null){
				boolean shifted = shiftedPage( n );
				for (int d : n.unlockDepths){
					if ((shifted ? spawnDepthFor( d ) : d) == depth){
						keys.add( n.key );
						break;
					}
				}
			} else if (branch == 1 && n.unlockBranch != null){
				if (("mine".equals( n.unlockBranch ) && depth >= 12 && depth <= 14)
						|| ("vault".equals( n.unlockBranch ) && depth >= 16 && depth <= 19)){
					keys.add( n.key );
				}
			}
		}
		for (Node c : n.children){
			collectPages( c, depth, branch, keys );
		}
	}

	//guide key for a mob class, if any boss node covers it
	private static java.util.HashMap<Class<?>, String> mobKeys = null;

	public static String keyForMob( Class<?> cls ){
		if (mobKeys == null){
			mobKeys = new java.util.HashMap<>();
			collectMobKeys( build(), mobKeys );
		}
		return mobKeys.get( cls );
	}

	private static void collectMobKeys( Node n, java.util.HashMap<Class<?>, String> out ){
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

	//title of the node a page belongs to, for item descriptions
	public static String titleForKey( String key ){
		return titleForKey( build(), key );
	}

	private static String titleForKey( Node n, String key ){
		if (key.equals( n.key )) return n.title;
		for (Node c : n.children){
			String t = titleForKey( c, key );
			if (t != null) return t;
		}
		return null;
	}

	public static Node build(){

		Node root = new Node( "Descent Guide", "Warped Pixel Dungeon", 0xFFFFFF ).icon( Icons.WPD );
		root.expanded = true;

		Node sewers = new Node( "Sewers", "Depths 1-5", SEWERS ).icon( Icons.STAIRS ).unlock( 1 );
		sewers.child( new Node( "Overview", "The Sewers at a glance", SEWERS ).icon( Icons.INFO ).key( "sewers_overview" ).unlock( 1 ) );
		sewers.child( new Node( "Sewers - Floor 1", "Depth 1", SEWERS ).icon( Icons.DEPTH ).key( "sewers_f1" ).unlock( 1 ) );
		sewers.child( new Node( "Sewers - Floor 2", "Depth 2", SEWERS ).icon( Icons.DEPTH ).key( "sewers_f2" ).unlock( 2 ) );
		sewers.child( new Node( "Sewers - Floor 3", "Depth 3", SEWERS ).icon( Icons.DEPTH ).key( "sewers_f3" ).unlock( 3 ) );
		sewers.child( new Node( "Sewers - Floor 4", "Depth 4", SEWERS ).icon( Icons.DEPTH ).key( "sewers_f4" ).unlock( 4 ) );
		sewers.child( new Node( "Goo's Lair", "Depth 5 - boss floor", SEWERS ).icon( Icons.DEPTH_LARGE ).key( "sewers_f5" ).unlock( 5 ) );
		sewers.child( new Node( "Goo", "Boss", SEWERS ).mob( Goo.class ).key( "boss_goo" ) );
		root.child( sewers );

		Node prison = new Node( "Prison", "Depths 6-10", PRISON ).icon( Icons.STAIRS ).unlock( 6 );
		prison.child( new Node( "Overview", "The Prison at a glance", PRISON ).icon( Icons.INFO ).key( "prison_overview" ).unlock( 6 ) );
		prison.child( new Node( "Prison - Floor 1/5", "Depth 6", PRISON ).icon( Icons.DEPTH ).key( "prison_f1" ).unlock( 6 ) );
		prison.child( new Node( "Prison - Floor 2/5", "Depth 7", PRISON ).icon( Icons.DEPTH ).key( "prison_f2" ).unlock( 7 ) );
		prison.child( new Node( "Prison - Floor 3/5", "Depth 8", PRISON ).icon( Icons.DEPTH ).key( "prison_f3" ).unlock( 8 ) );
		prison.child( new Node( "Prison - Floor 4/5", "Depth 9", PRISON ).icon( Icons.DEPTH ).key( "prison_f4" ).unlock( 9 ) );
		prison.child( new Node( "Tengu's Cell", "Depth 10 - boss floor", PRISON ).icon( Icons.DEPTH_LARGE ).key( "prison_f5" ).unlock( 10 ) );
		prison.child( new Node( "Tengu", "Boss", PRISON ).mob( Tengu.class ).key( "boss_tengu" ) );
		root.child( prison );

		Node caves = new Node( "Caves", "Depths 11-15", CAVES ).icon( Icons.STAIRS ).unlock( 11 );
		caves.child( new Node( "Overview", "The Caves at a glance", CAVES ).icon( Icons.INFO ).key( "caves_overview" ).unlock( 11 ) );
		caves.child( new Node( "Caves - Level 1", "Depth 11 - SHOP", CAVES ).icon( Icons.DEPTH ).key( "caves_f1" ).unlock( 11 ) );
		caves.child( new Node( "Caves - Level 2", "Depth 12", CAVES ).icon( Icons.DEPTH ).key( "caves_f2" ).unlock( 12 ) );
		caves.child( new Node( "The Mine", "Branch 1 - Blacksmith's mine (12-14)", CAVES ).icon( Icons.DEPTH_SECRETS ).key( "caves_mine" ).unlockBranch( "mine" ) );
		caves.child( new Node( "Caves - Level 3", "Depth 13", CAVES ).icon( Icons.DEPTH ).key( "caves_f3" ).unlock( 13 ) );
		caves.child( new Node( "Caves - Level 4", "Depth 14 - last before DM-300", CAVES ).icon( Icons.DEPTH ).key( "caves_f4" ).unlock( 14 ) );
		caves.child( new Node( "DM-300's Arena", "Depth 15 - boss floor", CAVES ).icon( Icons.DEPTH_LARGE ).key( "caves_f5" ).unlock( 15 ) );
		caves.child( new Node( "DM-300", "Boss", CAVES ).mob( DM300.class ).key( "boss_dm300" ) );
		root.child( caves );

		Node city = new Node( "Dwarven City", "Depths 16-20", CITY ).icon( Icons.STAIRS ).unlock( 16 );
		city.child( new Node( "Overview", "The Dwarven City at a glance", CITY ).icon( Icons.INFO ).key( "city_overview" ).unlock( 16 ) );
		city.child( new Node( "Dwarven Metropolis 1", "Depth 16", CITY ).icon( Icons.DEPTH ).key( "city_f1" ).unlock( 16 ) );
		city.child( new Node( "Dwarven Metropolis 2", "Depth 17", CITY ).icon( Icons.DEPTH ).key( "city_f2" ).unlock( 17 ) );
		city.child( new Node( "Ancient Dwarven Vault", "Branch 1, depths 16-19 - upcoming quest", CITY ).icon( Icons.DEPTH_SECRETS ).key( "city_vault" ).unlockBranch( "vault" ) );
		city.child( new Node( "Dwarven Metropolis 3", "Depth 18", CITY ).icon( Icons.DEPTH ).key( "city_f3" ).unlock( 18 ) );
		city.child( new Node( "Dwarven Metropolis 4", "Depth 19", CITY ).icon( Icons.DEPTH ).key( "city_f4" ).unlock( 19 ) );
		city.child( new Node( "Throne Room", "Depth 20 - boss floor", CITY ).icon( Icons.DEPTH_LARGE ).key( "city_f5" ).unlock( 20 ) );
		city.child( new Node( "King of Dwarves", "Boss", CITY ).mob( DwarfKing.class ).key( "boss_dwarfking" ) );
		root.child( city );

		Node halls = new Node( "Demon Halls", "Depths 21-26", HALLS ).icon( Icons.STAIRS_DARK ).unlock( 21 );
		halls.child( new Node( "Overview", "The Demon Halls at a glance", HALLS ).icon( Icons.INFO ).key( "halls_overview" ).unlock( 21 ) );
		halls.child( new Node( "Demon Halls 1", "Depth 21", HALLS ).icon( Icons.DEPTH_DARK ).key( "halls_f1" ).unlock( 21 ) );
		halls.child( new Node( "Demon Halls 2", "Depth 22", HALLS ).icon( Icons.DEPTH_DARK ).key( "halls_f2" ).unlock( 22 ) );
		halls.child( new Node( "Demon Halls 3", "Depth 23", HALLS ).icon( Icons.DEPTH_DARK ).key( "halls_f3" ).unlock( 23 ) );
		halls.child( new Node( "Demon Halls 4", "Depth 24", HALLS ).icon( Icons.DEPTH_DARK ).key( "halls_f4" ).unlock( 24 ) );
		halls.child( new Node( "Yog-Dzewa's Arena", "Depth 25 - boss floor", HALLS ).icon( Icons.DEPTH_LARGE ).key( "halls_f5" ).unlock( 25 ) );
		halls.child( new Node( "The End", "Depth 26 - Amulet of Yendor pedestal", HALLS ).icon( Icons.STAIRS_LARGE ).key( "halls_end" ).unlock( 26 ) );
		halls.child( new Node( "Yog-Dzewa", "Boss", HALLS ).mob( YogDzewa.class ).key( "boss_yog" ) );
		root.child( halls );

		Node postgame = new Node( "Sprouted Postgame A", "Depths 27-33", POSTGAME ).icon( Icons.STAIRS_GRASS ).unlock( 27, 28, 29, 30, 31, 32, 33 );
		postgame.child( new Node( "Overview", "The first postgame stretch at a glance", POSTGAME ).icon( Icons.INFO ).key( "postgame_overview" ).unlock( 27, 28, 29, 30, 31, 32, 33 ) );
		postgame.child( new Node( "Field", "Depth 27 - Gnoll field", POSTGAME ).icon( Icons.DEPTH_GRASS ).key( "pg_field" ).unlock( 27 ) );
		postgame.child( new Node( "Battle", "Depth 28 - Battlefield", POSTGAME ).icon( Icons.DEPTH_GRASS ).key( "pg_battle" ).unlock( 28 ) );
		postgame.child( new Node( "Fishing", "Depth 29 - Fishing lake", POSTGAME ).icon( Icons.DEPTH_WATER ).key( "pg_fishing" ).unlock( 29 ) );
		postgame.child( new Node( "Vault", "Depth 30 - not the branch VaultLevel", POSTGAME ).icon( Icons.DEPTH ).key( "pg_vault" ).unlock( 30 ) );
		postgame.child( new Node( "Catacomb", "Depth 31", POSTGAME ).icon( Icons.DEPTH_DARK ).key( "pg_catacomb" ).unlock( 31 ) );
		postgame.child( new Node( "Fortress", "Depth 32", POSTGAME ).icon( Icons.DEPTH ).key( "pg_fortress" ).unlock( 32 ) );
		postgame.child( new Node( "Chasm", "Depth 33", POSTGAME ).icon( Icons.DEPTH_CHASM ).key( "pg_chasm" ).unlock( 33 ) );
		root.child( postgame );

		Node dens = new Node( "Postgame Boss Dens", "Depths 35-41", DENS ).icon( Icons.STAIRS_TRAPS ).unlock( 35, 36, 37, 38, 39, 40, 41 );
		dens.child( new Node( "Overview", "The boss dens at a glance", DENS ).icon( Icons.INFO ).key( "dens_overview" ).unlock( 35, 36, 37, 38, 39, 40, 41 ) );
		dens.child( new Node( "Infest Boss Level", "Depth 35 - Shadow Yog's legion", DENS ).icon( Icons.DEPTH_LARGE ).key( "den_infest" ).unlock( 35 ) );
		dens.child( new Node( "Tengu Den", "Depth 36", DENS ).icon( Icons.DEPTH_LARGE ).key( "den_tengu" ).unlock( 36 ) );
		dens.child( new Node( "Skeleton King's Hall", "Depth 37", DENS ).icon( Icons.DEPTH_LARGE ).key( "den_skeleton" ).unlock( 37 ) );
		dens.child( new Node( "Crab King's Beach", "Depth 38", DENS ).icon( Icons.DEPTH_WATER ).key( "den_crab" ).unlock( 38 ) );
		dens.child( new Node( "Dead End", "Depth 39 - unused", DENS ).icon( Icons.DEPTH ).key( "den_deadend" ).unlock( 39 ) );
		dens.child( new Node( "Thief King's Vault", "Depth 40", DENS ).icon( Icons.DEPTH_LARGE ).key( "den_thief" ).unlock( 40 ) );
		dens.child( new Node( "Thief Catch", "Depth 41 - trap, near-inaccessible", DENS ).icon( Icons.DEPTH_TRAPS ).key( "den_thiefcatch" ).unlock( 41 ) );
		dens.child( new Node( "Shadow Yog", "Boss - legion of 10", DENS ).mob( ShadowYog.class ).key( "boss_shadowyog" ) );
		dens.child( new Node( "Skeleton King", "Boss", DENS ).mob( SkeletonKing.class ).key( "boss_skeletonking" ) );
		dens.child( new Node( "Crab King", "Boss", DENS ).mob( CrabKing.class ).key( "boss_crabking" ) );
		dens.child( new Node( "Thief King", "Boss", DENS ).mob( ThiefKing.class ).key( "boss_thiefking" ) );
		root.child( dens );

		Node sokoban = new Node( "Otiluke's Journal", "Depths 50-54, 66", SOKOBAN ).icon( Icons.STAIRS_SECRETS ).unlock( 50, 51, 52, 53, 54, 66 );
		sokoban.child( new Node( "Overview", "The journal dimensions at a glance", SOKOBAN ).icon( Icons.INFO ).key( "sokoban_overview" ).unlock( 50, 51, 52, 53, 54, 66 ) );
		sokoban.child( new Node( "Safe Room", "Depth 50", SOKOBAN ).icon( Icons.DEPTH ).key( "sok_saferoom" ).unlock( 50 ) );
		sokoban.child( new Node( "Sokoban Practice", "Depth 51", SOKOBAN ).icon( Icons.DEPTH ).key( "sok_practice" ).unlock( 51 ) );
		sokoban.child( new Node( "Sokoban Castle", "Depth 52", SOKOBAN ).icon( Icons.DEPTH ).key( "sok_castle" ).unlock( 52 ) );
		sokoban.child( new Node( "Sokoban Portals", "Depth 53", SOKOBAN ).icon( Icons.DEPTH ).key( "sok_portals" ).unlock( 53 ) );
		sokoban.child( new Node( "Sokoban Puzzles", "Depth 54", SOKOBAN ).icon( Icons.DEPTH ).key( "sok_puzzles" ).unlock( 54 ) );
		sokoban.child( new Node( "The Vault", "Depth 66", SOKOBAN ).icon( Icons.DEPTH_SECRETS ).key( "sok_vault" ).unlock( 66 ) );
		root.child( sokoban );

		Node town = new Node( "Town", "Town, Mines & Dragon Cave (55-67)", TOWN ).icon( Icons.STAIRS_GRASS ).unlock( 55, 56 );
		town.child( new Node( "Overview", "Town at a glance", TOWN ).icon( Icons.INFO ).key( "town_overview" ).unlock( 55, 56 ) );
		town.child( new Node( "Town", "Depth 55", TOWN ).icon( Icons.DEPTH_GRASS ).key( "town_dolyahaven" ).unlock( 55 ) );
		Node mines = new Node( "Town Mines", "Depths 56-65", TOWN ).icon( Icons.STAIRS ).unlock( 56, 57, 58, 59, 60, 61, 62, 63, 64, 65 );
		for (int i = 1; i <= 9; i++){
			mines.child( new Node( "Mines - Level " + i, "Depth " + (55 + i), TOWN ).icon( Icons.DEPTH ).key( "mines_f" + i ).unlock( 55 + i ) );
		}
		mines.child( new Node( "Mines - Boss Level", "Depth 65", TOWN ).icon( Icons.DEPTH_LARGE ).key( "mines_boss" ).unlock( 65 ) );
		mines.child( new Node( "Otiluke", "Boss - stone golem", TOWN ).mob( Otiluke.class ).key( "boss_otiluke" ) );
		town.child( mines );
		town.child( new Node( "Dragon Cave", "Depth 67", TOWN ).icon( Icons.DEPTH_DARK ).key( "town_dragoncave" ).unlock( 67 ) );
		root.child( town );

		Node zot = new Node( "Zot's Prison", "Depth 99", ZOT ).icon( Icons.STAIRS_DARK ).unlock( 99 );
		zot.child( new Node( "Overview", "Zot's Prison at a glance", ZOT ).icon( Icons.INFO ).key( "zot_overview" ).unlock( 99 ) );
		zot.child( new Node( "Zot's Prison Level", "Depth 99 - unnamed in-game", ZOT ).icon( Icons.DEPTH_DARK ).key( "zot_level" ).unlock( 99 ) );
		zot.child( new Node( "Zot", "Boss", ZOT ).mob( Zot.class ).key( "boss_zot" ) );
		root.child( zot );

		Node topics = new Node( "Guide Topics", "Everything else", TOPIC ).icon( Icons.JOURNAL );
		topics.child( new Node( "The Endings", "Amulet exit, Ascension, Zot, Otiluke's Journal", TOPIC ).icon( Icons.STAIRS_LARGE ).key( "topic_endings" ).unlock( 26 ) );
		topics.child( new Node( "Weather & Seasons", "Climate, temperature, seasons", TOPIC ).icon( Icons.CALENDAR ).key( "topic_weather" ) );
		topics.child( new Node( "Complete Bestiary", "Every mob in the dungeon", TOPIC ).icon( Icons.SKULL ).key( "topic_bestiary" ).unlock( 10 ) );
		topics.child( new Node( "Quests, NPCs & Pets", "Allies and questlines", TOPIC ).icon( Icons.SCROLL_COLOR ).key( "topic_quests" ).unlock( 2 ) );
		topics.child( new Node( "Loot & Unique Drops", "Drop tables and generation", TOPIC ).icon( Icons.GOLD ).key( "topic_loot" ).unlock( 6 ) );
		topics.child( new Node( "Heroes & Classes", "Talents and badges", TOPIC ).icon( Icons.TALENT ).key( "topic_heroes" ) );
		topics.child( new Node( "Plants", "Every seed and plant", TOPIC ).icon( Icons.GRASS ).key( "topic_plants" ).unlock( 2 ) );
		topics.child( new Node( "Metagame", "How to read this guide", TOPIC ).icon( Icons.INFO ).key( "topic_meta" ) );
		root.child( topics );

		return root;
	}
}
