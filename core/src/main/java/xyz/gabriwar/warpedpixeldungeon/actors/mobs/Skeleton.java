/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicImmune;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.ShieldOfLight;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLivingEarth;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Earthroot;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkeletonSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.TargetHealthIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Skeleton extends Mob {
	
	{
		spriteClass = SkeletonSprite.class;
		
		HP = HT = 25;
		defenseSkill = 9;
		
		EXP = 5;
		maxLvl = 10;

		loot = Generator.Category.WEAPON;
		lootChance = 0.1667f; //by default, see lootChance()

		properties.add(Property.UNDEAD);
		properties.add(Property.INORGANIC);

		resistances.add(xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim.class);
	}
	
	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 10, 20 );
	}
	
	@Override
	public void die( Object cause ) {
		
		super.die( cause );
		
		if (cause == Chasm.class) return;
		
		boolean heroKilled = false;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			Char ch = findChar( pos + PathFinder.NEIGHBOURS8[i] );
			if (ch != null && ch.isAlive()) {
				int damage = Math.round(Random.NormalIntRange(6, 12));
				damage = Math.round( damage * AscensionChallenge.statModifier(this));

				//all sources of DR are 2x effective vs. bone explosion
				//this does not consume extra uses of rock armor and earthroot armor

				WandOfLivingEarth.RockArmor rockArmor = ch.buff(WandOfLivingEarth.RockArmor.class);
				if (rockArmor != null) {
					int preDmg = damage;
					damage = rockArmor.absorb(damage);
					damage *= Math.round(damage/(float)preDmg); //apply the % reduction twice
				}

				Earthroot.Armor armor = ch.buff( Earthroot.Armor.class );
				if (damage > 0 && armor != null) {
					int preDmg = damage;
					damage = armor.absorb( damage );
					damage -= (preDmg - damage); //apply the flat reduction twice
				}

				if (ch.buff(MagicImmune.class) == null) {
					ShieldOfLight.ShieldOfLightTracker shield = ch.buff(ShieldOfLight.ShieldOfLightTracker.class);
					if (shield != null && shield.object == id()) {
						int min = 1 + Dungeon.hero.pointsInTalent(Talent.SHIELD_OF_LIGHT);
						damage -= Random.NormalIntRange(min, 2 * min);
						damage -= Random.NormalIntRange(min, 2 * min); //apply twice
						damage = Math.max(damage, 0);
					} else if (ch == Dungeon.hero
							&& Dungeon.hero.heroClass != HeroClass.CLERIC
							&& Dungeon.hero.hasTalent(Talent.SHIELD_OF_LIGHT)
							&& TargetHealthIndicator.instance.target() == this) {
						//33/50%
						if (Random.Int(6) < 1 + Dungeon.hero.pointsInTalent(Talent.SHIELD_OF_LIGHT)) {
							damage -= 2; //doubled
						}
					}

					if (ch.buff(HolyWard.HolyArmBuff.class) != null){
						//doubled
						damage -= Dungeon.hero.subClass == HeroSubClass.PALADIN ? 6 : 2;
					}
				}

				//apply DR twice (with 2 rolls for more consistency)
				damage = Math.max( 0,  damage - (ch.drRoll() + ch.drRoll()) );
				ch.damage( damage, this );
				if (ch == Dungeon.hero && !ch.isAlive()) {
					heroKilled = true;
				}
			}
		}
		
		if (Dungeon.level.heroFOV[pos]) {
			Sample.INSTANCE.play( Assets.Sounds.BONES );
		}
		
		if (heroKilled) {
			Dungeon.fail( this );
			GLog.n( Messages.get(this, "explo_kill") );
		}
	}

	@Override
	public float lootChance() {
		//each drop makes future drops 1/3 as likely
		// so loot chance looks like: 1/6, 1/18, 1/54, 1/162, etc.
		return super.lootChance() * (float)Math.pow(1/3f, Dungeon.LimitedDrops.SKELE_WEP.count);
	}

	@Override
	public Item createLoot() {
		Dungeon.LimitedDrops.SKELE_WEP.count++;
		return super.createLoot();
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 5);
	}

}
