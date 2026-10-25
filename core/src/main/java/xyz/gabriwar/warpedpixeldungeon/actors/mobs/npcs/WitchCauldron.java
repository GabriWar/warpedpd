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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.WitchCauldronSprite;
import com.watabou.utils.Bundle;

/**
 * The swamp witch's cauldron (levels/overworld/WetlandSites), in the corner of her hut by the
 * door: always on the boil, green steam curling off it (WitchCauldronSprite). Furniture: it never
 * moves, burns or takes harm. A look tells the hero who brews in it (SwampWitch).
 */
public class WitchCauldron extends NPC {

	{
		spriteClass = WitchCauldronSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	public long siteKey = 0;

	public static WitchCauldron of( long siteKey ){
		WitchCauldron c = new WitchCauldron();
		c.siteKey = siteKey;
		return c;
	}

	@Override
	public boolean interact( Char c ){
		if (c instanceof Hero) NetManager.heroLog( (Hero) c, Messages.get( this, "look" ) );
		return true;
	}

	@Override
	public boolean isImmune( Class effect ){
		return Blob.class.isAssignableFrom( effect ) || super.isImmune( effect );
	}

	@Override
	public void damage( int dmg, Object src ){
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public int defenseSkill( Char enemy ){
		return INFINITE_EVASION;
	}

	@Override
	public boolean reset(){
		return true;
	}

	private static final String SITE = "cauldron_site";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SITE, siteKey );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		siteKey = bundle.contains( SITE ) ? bundle.getLong( SITE ) : 0;
	}
}
