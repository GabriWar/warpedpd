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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

/**
 * A shard of a fallen star (levels/overworld/WorldEvents): the loot in a star's crater, valuable
 * to any merchant, and star-metal the troll blacksmith in town folds two at a time into a
 * masterwork core (Blacksmith2). The one lying in a crater carries its star's id until it is
 * picked up: picking it up settles the star for good (OverworldLevel.starLooted), and a star
 * nobody reached takes back only its own piece.
 */
public class StarFragment extends Item {

	{
		image = ItemSpriteSheet.STAR_FRAGMENT;
		stackable = true;
	}

	//the fallen star this piece came down with, until someone picks it up (0: a loose fragment)
	public long eventId = 0;
	//...and the turn that star takes it back if nobody does
	public int eventEnd = 0;

	@Override
	public boolean doPickUp( Hero hero, int pos ){
		long id = eventId;
		int end = eventEnd;
		//into the pack as a plain fragment: it stacks with the others
		eventId = 0;
		eventEnd = 0;
		if (super.doPickUp( hero, pos )){
			if (id != 0 && Dungeon.level instanceof OverworldLevel){
				((OverworldLevel) Dungeon.level).starLooted( id, end, pos );
			}
			return true;
		}
		eventId = id;
		eventEnd = end;
		return false;
	}

	//a crater's own piece never merges with a plain one, or the star's mark would be lost in the stack
	@Override
	public boolean isSimilar( Item item ){
		return super.isSimilar( item ) && ((StarFragment) item).eventId == eventId;
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	@Override
	public int value(){
		return 300 * quantity;
	}

	@Override
	public ItemSprite.Glowing glowing(){
		return new ItemSprite.Glowing( 0xFFE070, 1.6f );
	}

	private static final String EVENT_ID  = "event_id";
	private static final String EVENT_END = "event_end";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( EVENT_ID, eventId );
		bundle.put( EVENT_END, eventEnd );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		eventId = bundle.contains( EVENT_ID ) ? bundle.getLong( EVENT_ID ) : 0;
		eventEnd = bundle.contains( EVENT_END ) ? bundle.getInt( EVENT_END ) : 0;
	}
}
