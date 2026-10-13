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

package xyz.gabriwar.warpedpixeldungeon.journal.guidecites;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

import java.util.ArrayList;
import java.util.LinkedHashMap;

//What the guide's pages name, page by page: the groups (GuideCites.GROUPS) write into it.
//
//    book.page( "sewers_f1" )
//            .mob( Snake.class, "sewer snake" ).primary()
//            .mob( BrownBat.class, "brown bat" )
//            .item( GoldenKey.class, "golden key" )
//            .trap( WornDartTrap.class, "worn dart trap" );
//
//The first word of a cite is its caption; every word is matched against the page's text
//(CiteMatch), so a cite lands under the paragraph that names it.
public final class CiteBook {

	CiteBook(){}

	String group;                                   //set by GuideCites before each group registers
	final LinkedHashMap<String, Page> pages = new LinkedHashMap<>();

	/** The cites of a guide page, by its key (guide.<key>): opened on the first call, continued after. */
	public Page page( String key ){
		if (key == null || key.trim().isEmpty()) throw new IllegalArgumentException( "a page needs its key" );
		Page p = pages.get( key );
		if (p == null){
			p = new Page( key, group );
			pages.put( key, p );
		}
		return p;
	}

	public static final class Page {

		public final String key;
		final String group;                          //who registered it (the test checks it is the owner)
		final ArrayList<Cite> cites = new ArrayList<>();
		final ArrayList<String> plain = new ArrayList<>();

		Page( String key, String group ){
			this.key = key;
			this.group = group;
		}

		/** A monster, boss or NPC: its real sprite, alive. */
		public Page mob( Class<? extends Mob> mob, String... words ){
			return add( Cite.Kind.MOB, words, need( mob, "mob class" ), null, -1, null, null, null, null );
		}

		/** A hero class: its avatar. */
		public Page hero( HeroClass cls, String... words ){
			return add( Cite.Kind.HERO, words, null, need( cls, "hero class" ), -1, null, null, null, null );
		}

		/** An item with its glow (potions, scrolls and rings show their kind, never this run's look). */
		public Page item( Class<? extends Item> item, String... words ){
			return add( Cite.Kind.ITEM, words, need( item, "item class" ), null, -1, null, null, null, null );
		}

		/** Item-sheet art with no class (a chest, a tomb, bones): an ItemSpriteSheet image. */
		public Page sprite( int image, String... words ){
			return add( Cite.Kind.SPRITE, words, null, null, image, null, null, null, null );
		}

		/** An interface icon: mechanics, places, rooms. */
		public Page icon( Icons icon, String... words ){
			return add( Cite.Kind.ICON, words, null, null, -1, need( icon, "icon" ), null, null, null );
		}

		/** A status, by its own buff icon, tinted. */
		public Page buff( Class<? extends Buff> buff, String... words ){
			return add( Cite.Kind.BUFF, words, need( buff, "buff class" ), null, -1, null, null, null, null );
		}

		/** A status whose icon depends on its state (Hunger shows none while you are fed): drawn with this
		 *  BuffIndicator icon, its description still the buff's. */
		public Page buff( Class<? extends Buff> buff, int icon, String... words ){
			if (icon < 0) throw new IllegalArgumentException( key + ": a buff icon is a BuffIndicator constant" );
			return add( Cite.Kind.BUFF, words, need( buff, "buff class" ), null, icon, null, null, null, null );
		}

		/** One 16x16 tile of a tileset (Assets.Environment.TILES_*, a DungeonTileSheet ground or FLAT_ tile). */
		public Page terrain( String tiles, int tile, String... words ){
			return add( Cite.Kind.TERRAIN, words, null, null, tile, null, need( tiles, "tileset" ), null, null );
		}

		/** A scrolling patch of a water texture (Assets.Environment.WATER_*). */
		public Page water( String water, String... words ){
			return add( Cite.Kind.WATER, words, null, null, -1, null, need( water, "water texture" ), null, null );
		}

		/** A trap, as the floor shows it. */
		public Page trap( Class<? extends Trap> trap, String... words ){
			return add( Cite.Kind.TRAP, words, need( trap, "trap class" ), null, -1, null, null, null, null );
		}

		/** A plant, as the floor shows it. */
		public Page plant( Class<? extends Plant> plant, String... words ){
			return add( Cite.Kind.PLANT, words, need( plant, "plant class" ), null, -1, null, null, null, null );
		}

		/** The game's particles, pouring (fire, gases, clouds). */
		public Page effect( GuideEffect effect, String... words ){
			return add( Cite.Kind.EFFECT, words, null, null, -1, null, null, need( effect, "effect" ), null );
		}

		/** Anything else: a picture made when the page opens. */
		public Page custom( Cite.Picture picture, String... words ){
			return add( Cite.Kind.CUSTOM, words, null, null, -1, null, null, null, need( picture, "picture" ) );
		}

		/** The cite added last is this page's card icon (and its window's title icon). */
		public Page primary(){
			if (cites.isEmpty()) throw new IllegalStateException( key + ": primary() before any cite" );
			for (Cite c : cites){
				if (c.primary) throw new IllegalStateException( key + ": two primaries" );
			}
			cites.get( cites.size() - 1 ).primary = true;
			return this;
		}

		/** Highlighted words on this page that name no thing at all (a phase, a heading). At most 4. */
		public Page plain( String... highlights ){
			for (String h : checked( highlights )) plain.add( h );
			return this;
		}

		private Page add( Cite.Kind kind, String[] words, Class<?> type, HeroClass hero, int image, Icons icon,
		                  String asset, GuideEffect effect, Cite.Picture picture ){
			cites.add( new Cite( kind, checked( words ), type, hero, image, icon, asset, effect, picture ) );
			return this;
		}

		private String[] checked( String[] words ){
			if (words == null || words.length == 0){
				throw new IllegalArgumentException( key + ": a cite needs at least one word" );
			}
			for (String w : words){
				if (w == null || w.trim().isEmpty()) throw new IllegalArgumentException( key + ": a blank word" );
			}
			return words.clone();
		}

		private <T> T need( T thing, String what ){
			if (thing == null) throw new IllegalArgumentException( key + ": no " + what );
			return thing;
		}
	}
}
