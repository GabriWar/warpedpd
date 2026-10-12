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

package xyz.gabriwar.warpedpixeldungeon.homestead.animals;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.TiePostSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * A tie post (FarmPieces' "tie_post"): a short post with an iron ring, built anywhere, to tie a
 * roped animal to (Roping.tie) - as a fence or a palisade will do. A tied animal keeps within a
 * step of it; the post taken down or wrecked lets it go.
 */
public class TiePost extends Mob implements HomesteadActor {

	{
		spriteClass = TiePostSprite.class;

		HP = HT = 1;
		defenseSkill = 0;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	@Override
	protected boolean act(){
		spend( TICK );
		return true;
	}

	//a blow at it is a blow at the built piece (Homestead.damage)
	@Override
	public void damage( int dmg, Object src ){
		Homestead.damage( Dungeon.level, pos, dmg, src );
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public boolean interact( Char c ){
		if (c == Dungeon.hero) GLog.i( Messages.get( this, "tap", Roping.tiedTo( Dungeon.level, pos ) ) );
		else if (c instanceof Hero) xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( (Hero) c,
				Messages.get( this, "tap", Roping.tiedTo( Dungeon.level, pos ) ) );
		return true;
	}
}
