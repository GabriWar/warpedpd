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


public class Garrote extends SubSkill2 {

	{
		name = "Garrote";
		image = 184;
		tier = 2;
	}

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	//the wire goes round the throat of an enemy busy with someone else (a shadow, a double, an ally,
	//a maddened foe): the melee hit always opens a bleeding wound
	@Override
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){
		if (ranged || level <= 0 || !(enemy instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob) || !enemy.isAlive()) return damage;
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob = (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob) enemy;
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero = xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;
		boolean busy = (mob.state == mob.HUNTING && !mob.isTargeting( hero ))
				|| mob.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok.class ) != null;
		if (!busy) return damage;
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( enemy, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding.class ).set( 2 + level );
		//a wire drawn tight: at mastery the choke leaves it lame as well
		if (level >= Skill.MAX_LEVEL){
			xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.prolong( enemy, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple.class, 2f );
		}
		xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( enemy.pos, 0xCC1111, 6 );
		if (enemy.sprite != null){
			enemy.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.NEGATIVE, xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "choke" ) );
		}
		com.watabou.noosa.audio.Sample.INSTANCE.play( xyz.gabriwar.warpedpixeldungeon.Assets.Sounds.HIT_STAB, 1f, 0.8f );
		return damage;
	}
}
