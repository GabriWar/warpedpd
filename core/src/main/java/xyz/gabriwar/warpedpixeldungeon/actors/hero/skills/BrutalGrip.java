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

import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

public class BrutalGrip extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	private static final float CRUSH_MULTIPLIER = 1.5f;

	{
		tag = "CB";
		name = "Brutal Grip";
		image = 138;
		tier = 4;
	}

	//live only while holding a heavy (tier 4+) melee weapon
	private boolean qualifies(){
		if (level <= 0 || Dungeon.hero == null) return false;
		KindOfWeapon w = Dungeon.hero.belongings.weapon();
		return w instanceof MeleeWeapon && ((MeleeWeapon)w).tier >= 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//a heavy melee hit can crush: 15% / 25% / 35% chance to deal 50% more and Cripple for 3 / 4 / 5 turns.
	//+3: the crush also hurls the target 2 tiles back into whatever is behind it, the same way
	//Hero's own knockback does it from attackProc before the damage lands
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || enemy == null || !enemy.isAlive() || !qualifies()
				|| Random.Int( 100 ) >= 5 + 10 * level){
			return damage;
		}
		Buff.prolong( enemy, Cripple.class, 2 + level );
		Wound.hit( enemy );
		if (enemy.sprite != null){
			enemy.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "crush" ) );
		}
		Camera.main.shake( 1, 0.2f );
		Sample.INSTANCE.play( Assets.Sounds.HIT_CRUSH, 1f, 0.8f );
		int heroPos = Dungeon.hero.pos;
		if (level >= MAX_LEVEL && enemy.pos != heroPos){
			Ballistica trajectory = new Ballistica( enemy.pos, enemy.pos + (enemy.pos - heroPos), Ballistica.MAGIC_BOLT );
			WandOfBlastWave.throwChar( enemy, trajectory, 2, true, true, this );
		}
		return Math.round( damage * CRUSH_MULTIPLIER );
	}
}
