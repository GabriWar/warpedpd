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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.noosa.Image;

//One thing a guide page names, and how to draw it: a creature, an item, a tile, a gas...
//Made only through CiteBook.Page; pure data, so the guide's coverage can be checked headlessly.
public final class Cite {

	public enum Kind { MOB, HERO, ITEM, SPRITE, ICON, BUFF, TERRAIN, WATER, TRAP, PLANT, EFFECT, CUSTOM }

	/** Anything the other kinds cannot draw (own interface: no java.util.function on API 21). */
	public interface Picture { Image make(); }

	public final Kind kind;
	public final String[] words;   //words[0] is the caption; every word is matched against the page text
	public final Class<?> type;    //MOB (Mob), ITEM (Item), BUFF (Buff), TRAP (Trap), PLANT (Plant)
	public final HeroClass hero;   //HERO
	public final int image;        //SPRITE: ItemSpriteSheet image; TERRAIN: DungeonTileSheet tile (single 16x16 tile);
	                               //BUFF: a BuffIndicator icon drawn in place of the buff's own (-1: its own)
	public final Icons icon;       //ICON
	public final String asset;     //TERRAIN: Assets.Environment.TILES_*; WATER: Assets.Environment.WATER_*
	public final GuideEffect effect; //EFFECT
	public final Picture picture;  //CUSTOM
	boolean primary;

	Cite( Kind kind, String[] words, Class<?> type, HeroClass hero, int image, Icons icon, String asset,
	      GuideEffect effect, Picture picture ){
		this.kind = kind;
		this.words = words;
		this.type = type;
		this.hero = hero;
		this.image = image;
		this.icon = icon;
		this.asset = asset;
		this.effect = effect;
		this.picture = picture;
	}

	public String caption(){
		return words[0];
	}

	public boolean isPrimary(){
		return primary;
	}

	/** Whether it can stand as a card's icon: every kind but EFFECT. */
	public boolean still(){
		return kind != Kind.EFFECT;
	}
}
