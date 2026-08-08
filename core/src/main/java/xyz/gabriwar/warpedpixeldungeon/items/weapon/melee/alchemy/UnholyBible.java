/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2021 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Evolution;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UpgradeDust;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Bible;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

public class UnholyBible extends MeleeWeapon implements AlchemyWeapon {

	{
		image = ItemSpriteSheet.UNHOLY_BIBLE;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		tier = 4;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		switch (Random.Int(15)) {
			case 0: case 1: default:
				Buff.affect( defender, Weakness.class, 3f );
				break;
			case 2: case 3:
				Buff.affect( defender, Vulnerable.class, 3f );
				break;
			case 4:
				Buff.affect( defender, Cripple.class, 3f );
				break;
			case 5:
				Buff.affect( defender, Blindness.class, 3f );
				break;
			case 6:
				Buff.affect( defender, Terror.class, 3f );
				break;
			case 7: case 8: case 9:
				Buff.affect( defender, Amok.class, 3f );
				break;
			case 10: case 11:
				Buff.affect( defender, Slow.class, 3f );
				break;
			case 12: case 13:
				Buff.affect( defender, Hex.class, 3f );
				break;
			case 14:
				Buff.affect( defender, Paralysis.class, 3f );
				break;
		}
		if (Random.Float() < Math.min(0.01f*(1+buffedLvl()), 0.1f)) { //1% base, +1% per lvl, max 10%
			Buff.affect( defender, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom.class);
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public int max(int lvl) {
		return  3*(tier) +    		//12 base
				lvl*(tier-1);     	//+3 per level
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		demonAbility(hero, 5+buffedLvl(), this);
	}

	public static void demonAbility(Hero hero, int duration, MeleeWeapon wep){
		wep.beforeAbilityUsed(hero, null);
		Buff.prolong(hero, Demon.class, duration);
		hero.next();
		((HeroSprite)hero.sprite).read();
		SpellSprite.show(hero, SpellSprite.BERSERK);
		hero.sprite.centerEmitter().start( Speck.factory( Speck.SCREAM ), 0.3f, 3 );
		Sample.INSTANCE.play( Assets.Sounds.CHALLENGE );
		Sample.INSTANCE.play( Assets.Sounds.READ );
		wep.afterAbilityUsed(hero);
	}

	@Override
	public String abilityInfo() {
		int duration = levelKnown ? 6+buffedLvl() : 6;
		if (levelKnown){
			return Messages.get(this, "ability_desc", duration);
		} else {
			return Messages.get(this, "typical_ability_desc", duration);
		}
	}

	public static class Demon extends FlavourBuff {

		{
			announced = true;
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.DUEL_EVIL;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (6 - visualcooldown()) / 6);
		}
	}

	@Override
	public ArrayList<Class<?extends Item>> weaponRecipe() {
		return new ArrayList<>(Arrays.asList(Bible.class, UpgradeDust.class, Evolution.class));
	}

	@Override
	public String discoverHint() {
		return AlchemyWeapon.hintString(weaponRecipe());
	}

	@Override
	public String desc() {
		String info = super.desc();

		info += "\n\n" + AlchemyWeapon.hintString(weaponRecipe());

		return info;
	}

}
