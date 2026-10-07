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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.MountainSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CairnSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.Locale;

/**
 * The cairn on a summit of the mountains (levels/overworld/MountainSites.summitOf): a pile of
 * stones every climber adds one to. The first time a hero does - once per cairn, ever - the land
 * shows round the summit for SUMMIT_VIEW cells, the view lifts his spirits (Bless) and the log
 * tells how high he stands. Furniture: it cannot be hurt, moved or touched by any buff.
 */
public class SummitCairn extends NPC {

	{
		spriteClass = CairnSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	public long siteKey = 0;
	public int summitX = Integer.MIN_VALUE, summitY = Integer.MIN_VALUE;
	public int feet = 0;
	public boolean topped = false;

	public static SummitCairn of( MountainSites.Site s ){
		SummitCairn c = new SummitCairn();
		c.siteKey = s.key;
		c.summitX = s.wx;
		c.summitY = s.wy;
		c.feet = s.feet();
		return c;
	}

	@Override
	public CharSprite sprite(){
		CairnSprite s = new CairnSprite();
		s.topped( topped );
		return s;
	}

	@Override
	public boolean interact( Char c ){
		if (!(c instanceof Hero) || !(Dungeon.level instanceof OverworldLevel)) return true;
		Hero hero = (Hero) c;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (!ow.claimHoard( siteKey )){
			NetManager.heroLog( hero, Messages.get( MountainSites.class, "summit_again" ) );
			hero.spendAndNext( 1f );
			return true;
		}
		topped = true;
		if (sprite instanceof CairnSprite) ((CairnSprite) sprite).topped( true );
		if (hero.sprite != null) hero.sprite.operate( pos );
		Sample.INSTANCE.play( Assets.Sounds.STURDY );
		ow.revealAround( pos, MountainSites.SUMMIT_VIEW );
		Buff.prolong( hero, Bless.class, Bless.DURATION * 3f );
		NetManager.heroLog( hero, GLog.POSITIVE + Messages.get( MountainSites.class, "summit",
				String.format( Locale.ENGLISH, "%,d", feet ) ) );
		NetManager.heroLog( hero, Messages.get( MountainSites.class, "summit_mood" ) );
		hero.spendAndNext( 1f );
		return true;
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

	private static final String SITE = "cairn_site";
	private static final String SUMMIT_X = "summit_x";
	private static final String SUMMIT_Y = "summit_y";
	private static final String FEET = "feet";
	private static final String TOPPED = "topped";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SITE, siteKey );
		bundle.put( SUMMIT_X, summitX );
		bundle.put( SUMMIT_Y, summitY );
		bundle.put( FEET, feet );
		bundle.put( TOPPED, topped );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		siteKey = bundle.contains( SITE ) ? bundle.getLong( SITE ) : 0;
		summitX = bundle.contains( SUMMIT_X ) ? bundle.getInt( SUMMIT_X ) : Integer.MIN_VALUE;
		summitY = bundle.contains( SUMMIT_Y ) ? bundle.getInt( SUMMIT_Y ) : Integer.MIN_VALUE;
		feet = bundle.contains( FEET ) ? bundle.getInt( FEET ) : 0;
		topped = bundle.contains( TOPPED ) && bundle.getBoolean( TOPPED );
	}
}
