/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CorruptGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.items.StoneOre;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.*;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.RelicMeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.GullinSprite;
import com.watabou.utils.Random;

public class Gullin extends Mob {

	{
		spriteClass = GullinSprite.class;

		HP = HT = 750;
		defenseSkill = 20;

		EXP = 20;

		immunities.add( ToxicGas.class );
		immunities.add( CorruptGas.class );

		resistances.add( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim.class );

		loot = new StoneOre();
		lootChance = 0.0f;
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 100, 200 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 72;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 145);
	}

	@Override
	public void damage(int dmg, Object src) {
		if (!(src instanceof RelicMeleeWeapon) && !(src instanceof JupitersWraith)) {
			int max = Math.max(1, Math.round(dmg * 0.25f));
			dmg = Random.Int(1, max);
		}
		if (dmg > HT/8) {
			GameScene.add(Blob.seed(pos, 30, CorruptGas.class));
		}
		super.damage(dmg, src);
	}

	@Override
	public void die(Object cause) {
		if (Dungeon.LimitedDrops.NORNSTONES.count < 5
				&& Random.Int(5) < 3) {
			NornStone stone;
			if (Dungeon.hero.heroClass == HeroClass.HUNTRESS) {
				// Huntress gets all 5 types equally
				stone = Random.element( new NornStone[]{
						new BlueNornStone(), new GreenNornStone(), new OrangeNornStone(),
						new PurpleNornStone(), new YellowNornStone() } );
			} else {
				// Other classes don't get GreenNornStone
				stone = Random.element( new NornStone[]{
						new BlueNornStone(), new OrangeNornStone(),
						new PurpleNornStone(), new YellowNornStone() } );
			}
			Dungeon.level.drop( stone, pos ).sprite.drop();
			Dungeon.LimitedDrops.NORNSTONES.count++;
		}
		super.die(cause);
	}

	@Override
	public void notice() {
		super.notice();
		yell(Messages.get(this, "notice"));
	}

}
