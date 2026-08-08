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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSharpshooting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Ported from SPS-PD. An arm cannon that charges each turn while equipped and
//dumps its whole charge into a single, unmissable blast.
public class MegaCannon extends Weapon {

	public static final String AC_SHOOT = "SHOOT";

	public static final int MAX_CHARGE = 3;

	{
		image = ItemSpriteSheet.MEGA_CANNON;

		defaultAction = AC_SHOOT;
		usesTargeting = true;

		unique = true;
		bones = false;

		DLY = 0.75f;

		//like the SPS original, once bolted on the cannon cannot be removed
		cursed = true;
	}

	public int charge = 0;

	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( CHARGE, charge );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt( CHARGE );
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_SHOOT);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_SHOOT)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell( shooter );
		}
	}

	@Override
	public boolean doEquip(Hero hero) {
		if (super.doEquip(hero)){
			Buff.affect(hero, Charger.class);
			return true;
		}
		return false;
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)) {
			Charger charger = hero.buff(Charger.class);
			if (charger != null) charger.detach();
			charge = 0;
			return true;
		}
		return false;
	}

	//gains 1 charge per turn while equipped, up to 3
	public static class Charger extends Buff {
		@Override
		public boolean act() {
			if (target instanceof Hero
					&& ((Hero)target).belongings.weapon instanceof MegaCannon){
				MegaCannon cannon = (MegaCannon)((Hero)target).belongings.weapon;
				if (cannon.charge < MAX_CHARGE){
					cannon.charge++;
					Item.updateQuickslot();
				}
			} else {
				detach();
			}
			spend(TICK);
			return true;
		}
	}

	@Override
	public int STRReq(int lvl) {
		return STRReq(1, lvl); //tier 1, STR 10 like the SPS original
	}

	//SPS stats: 1-5 base, +1 min / +3 max per upgrade
	@Override
	public int min(int lvl) {
		return 1 + lvl
				+ (Dungeon.hero != null ? RingOfSharpshooting.levelDamageBonus(Dungeon.hero) : 0);
	}

	@Override
	public int max(int lvl) {
		return 5 + 3*lvl
				+ (Dungeon.hero != null ? 2*RingOfSharpshooting.levelDamageBonus(Dungeon.hero) : 0);
	}

	//the cannon itself deals no damage when swung, like the SPS original...
	@Override
	public int damageRoll(Char owner) {
		return 0;
	}

	//...but melee hits build charge
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		charge++;
		updateQuickslot();
		return super.proc(attacker, defender, damage);
	}

	public int shotDamageRoll(Char owner) {
		int damage = augment.damageFactor(Random.NormalIntRange(min(), max()));
		if (owner instanceof Hero) {
			int exStr = ((Hero)owner).STR() - STRReq();
			if (exStr > 0) {
				damage += Hero.heroDamageIntRange( 0, exStr );
			}
		}
		return damage;
	}

	@Override
	public String status() {
		return Integer.toString( charge );
	}

	@Override
	public String info() {
		String info = super.info();

		info += "\n\n" + Messages.get( MegaCannon.class, "stats",
				Math.round(augment.damageFactor(min())),
				Math.round(augment.damageFactor(max())),
				STRReq());

		if (STRReq() > Dungeon.hero.STR()) {
			info += " " + Messages.get(Weapon.class, "too_heavy");
		} else if (Dungeon.hero.STR() > STRReq()){
			info += " " + Messages.get(Weapon.class, "excess_str", Dungeon.hero.STR() - STRReq());
		}

		if (enchantment != null && (cursedKnown || !enchantment.curse())){
			info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", enchantment.name()));
			info += " " + enchantment.desc();
		}

		if (cursed && isEquipped( Dungeon.hero )) {
			info += "\n\n" + Messages.get(Weapon.class, "cursed_worn");
		} else if (cursedKnown && cursed) {
			info += "\n\n" + Messages.get(Weapon.class, "cursed");
		}

		info += "\n\n" + Messages.get(this, "charge_info", charge, MAX_CHARGE);

		info += "\n\n" + Messages.get(MissileWeapon.class, "distance");

		return info;
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		return knockShot().targetingPos(user, dst);
	}

	public CannonShot knockShot(){
		return new CannonShot();
	}

	public class CannonShot extends MissileWeapon {

		{
			image = ItemSpriteSheet.MEGA_CANNON_AMMO;

			setID = 0;
		}

		@Override
		public ArrayList<String> actions(Hero hero) {
			return new ArrayList<>();
		}

		@Override
		public String defaultAction() {
			return null;
		}

		@Override
		public int defaultQuantity() {
			return 1;
		}

		//all damage is dealt in proc, scaled by stored charge
		@Override
		public int damageRoll(Char owner) {
			return 0;
		}

		//SPS gave this shot ACU 100, it effectively cannot miss
		@Override
		public float accuracyFactor(Char owner, Char target) {
			return Float.POSITIVE_INFINITY;
		}

		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return MegaCannon.this.hasEnchant(type, owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			int dmg = MegaCannon.this.shotDamageRoll(attacker) * charge - defender.drRoll();
			if (dmg > 0) defender.damage(dmg, this);
			charge = 0;
			updateQuickslot();
			return super.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return MegaCannon.this.delayFactor(user);
		}

		@Override
		public int STRReq(int lvl) {
			return MegaCannon.this.STRReq();
		}

		@Override
		protected void onThrow( int cell ) {
			Char enemy = Actor.findChar( cell );
			if (enemy == null || enemy == curUser) {
				parent = null;
				Splash.at( cell, 0xCC99FFFF, 1 );
				charge = 0;
				updateQuickslot();
			} else {
				if (!curUser.shoot( enemy, this )) {
					Splash.at(cell, 0xCC99FFFF, 1);
					charge = 0;
					updateQuickslot();
				}
			}
		}

		@Override
		public Item split(int amount) {
			return null;
		}
	}

	private CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ) {
			if (target != null) {
				knockShot().cast(curUser, target);
			}
		}
		@Override
		public String prompt() {
			return Messages.get(MegaCannon.class, "prompt");
		}
	};
}
