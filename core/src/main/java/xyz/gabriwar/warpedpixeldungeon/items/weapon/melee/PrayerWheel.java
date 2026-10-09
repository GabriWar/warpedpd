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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

public class PrayerWheel extends MeleeWeapon {

	{
		image = ItemSpriteSheet.PRAYER_WHEEL;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		tier = 4;
		ACC = 0.8f; //0.8x accuracy
		DLY = 1.2f; //~0.83x speed
	}

	private static final int CHARGE_CAP = 17;

	private int charge = 0;

	@Override
	public int max(int lvl) {
		return  6*(tier+1) +    //30 base, up from 25
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int damageRoll(Char owner) {
		int damage = super.damageRoll(owner);
		if (charge >= CHARGE_CAP) {
			damage *= 5;
			SpatialSound.play(Assets.Sounds.HIT_STRONG, owner);
		}
		return damage;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (charge >= CHARGE_CAP) {
			charge = 0;
		}
		charge++;

		return super.proc(attacker, defender, damage);
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc") + " " + Messages.get(this, "charge", charge, CHARGE_CAP);
	}

	//SPS-PD weapon: no Duelist ability was ever designed for it

	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
	}

	private int blessTurns(){ return Math.min( 6, 3 + buffedLvl() / 2 ); }

	/** the wheel is turned: a blessing, one affliction lifted, and the wheel's own count moves on three */
	@Override
	protected void duelistAbility( Hero hero, Integer target ){
		beforeAbilityUsed( hero, null );
		Buff.prolong( hero, Bless.class, blessTurns() );
		Class<?>[] afflictions = { Poison.class, Bleeding.class, Burning.class, Cripple.class, Weakness.class,
				Blindness.class, Vertigo.class, Slow.class, Terror.class, Chill.class, Ooze.class };
		for (Class<?> c : afflictions){
			Buff b = hero.buff( (Class<? extends Buff>) c );
			if (b != null){
				b.detach();
				break;
			}
		}
		charge = Math.min( CHARGE_CAP, charge + 3 );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 8 );
			new Flare( 6, 18 ).color( 0xFFE9A0, true ).show( hero.sprite, 0.8f ).angularSpeed = 180;
		}
		SpatialSound.play( Assets.Sounds.CHARMS, hero, 1f, 1.1f );
		hero.sprite.operate( hero.pos );
		hero.next();
		afterAbilityUsed( hero );
	}

	@Override
	public String abilityInfo() {
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", blessTurns());
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString( Math.min( 6, 3 + level / 2 ) );
	}
}
