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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BannerStandard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class Banner extends SubSkill1 {

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	{
		name = "Banner";
		image = 158;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){ return true; }

	//every melee kill plants the standard where the enemy fell; only one stands at a time,
	//so the next kill pulls it up and plants it again. What it does lives in BannerStandard
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || ranged || hero == null || mob == null) return;
		BannerStandard.plant( hero, mob.pos, level );
		if (hero.sprite != null){
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "planted" ) );
		}
	}
}
