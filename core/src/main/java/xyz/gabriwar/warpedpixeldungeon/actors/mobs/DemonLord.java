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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.DemonLordSprite;
import com.watabou.utils.Random;

//ported from Unleashed PD: boss of the frozen branch
public class DemonLord extends Mob {

	{
		spriteClass = DemonLordSprite.class;

		HP = HT = 180;
		defenseSkill = 35;

		EXP = 16;
		maxLvl = 34;

		properties.add( Property.BOSS );
		properties.add( Property.DEMONIC );
		//at home in the ice: unmoved by cold, and hard to burn
		properties.add( Property.ICY );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 20, 50 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 45;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 22 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (enemy instanceof Hero && Random.Int( 3 ) == 0) {
			Buff.prolong( enemy, Chill.class, 4f );
		}

		return damage;
	}

	@Override
	public void die( Object cause ) {
		GameScene.bossSlain();
		Dungeon.level.drop( new SkeletonKey( Dungeon.depth ), pos ).sprite.drop();
		super.die( cause );
		yell( Messages.get(this, "die") );
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss( this );
			yell( Messages.get(this, "notice") );
		}
	}
}
