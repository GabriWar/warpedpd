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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSharpshooting;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Ported from SPS-PD. An alien-tech shotgun with a full-magazine AoE blast.
public class ShootGun extends Weapon {

	public static final String AC_SHOOT    = "SHOOT";
	public static final String AC_ENDSHOOT = "ENDSHOOT";
	public static final String AC_RELOAD   = "RELOAD";

	{
		image = ItemSpriteSheet.SHOOT_GUN;

		defaultAction = AC_SHOOT;
		usesTargeting = true;

		unique = true;
		bones = false;
	}

	public int charge = 0;
	public int fullcharge = 3;

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
		actions.add(AC_ENDSHOOT);
		actions.add(AC_RELOAD);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_SHOOT)) {
			if (charge < 1){
				charge = fullcharge;
				hero.sprite.showStatus(CharSprite.DEFAULT, Messages.get(this, "reloading"));
				hero.spendAndNext(1.5f);
			} else {
				curUser = hero;
				curItem = this;
				GameScene.selectCell( shooter );
			}
		} else if (action.equals(AC_ENDSHOOT)) {
			if (charge < 1){
				charge = fullcharge;
				hero.sprite.showStatus(CharSprite.DEFAULT, Messages.get(this, "reloading"));
				hero.spendAndNext(1.5f);
			} else {
				curUser = hero;
				curItem = this;
				GameScene.selectCell( endShooter );
			}
		} else if (action.equals(AC_RELOAD)){
			if (charge == fullcharge){
				GLog.n(Messages.get(this, "full"));
			} else {
				float reloadtime = (fullcharge - charge)/2;
				hero.spendAndNext(reloadtime*1f);
				hero.sprite.showStatus(CharSprite.DEFAULT, Messages.get(this, "reloading"));
				charge = fullcharge;
			}
		}
	}

	@Override
	public int STRReq(int lvl) {
		return STRReq(1, lvl); //tier 1, STR 10 like the SPS original
	}

	//SPS stats: 5-10 base, +3 min / +5 max per upgrade
	@Override
	public int min(int lvl) {
		return 5 + 3*lvl
				+ (Dungeon.hero != null ? RingOfSharpshooting.levelDamageBonus(Dungeon.hero) : 0);
	}

	@Override
	public int max(int lvl) {
		return 10 + 5*lvl
				+ (Dungeon.hero != null ? 2*RingOfSharpshooting.levelDamageBonus(Dungeon.hero) : 0);
	}

	//the gun itself deals no damage when swung, like the SPS original
	@Override
	public int damageRoll(Char owner) {
		return 0;
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
	public int proc(Char attacker, Char defender, int damage) {
		//direct hits with the gun knock the target back 1 tile
		int oppositeDefender = defender.pos + (defender.pos - attacker.pos);
		Ballistica trajectory = new Ballistica(defender.pos, oppositeDefender, Ballistica.MAGIC_BOLT);
		WandOfBlastWave.throwChar(defender, trajectory, 1, true, true, this);
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String status() {
		return charge + "/" + fullcharge;
	}

	@Override
	public String info() {
		String info = super.info();

		info += "\n\n" + Messages.get( ShootGun.class, "stats",
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

		info += "\n\n" + Messages.get(this, "charge_info", charge, fullcharge);

		info += "\n\n" + Messages.get(MissileWeapon.class, "distance");

		return info;
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		return knockShot().targetingPos(user, dst);
	}

	public ShotAmmo knockShot(){
		return new ShotAmmo();
	}

	public EndShotAmmo knockEndShot(){
		return new EndShotAmmo();
	}

	public class ShotAmmo extends MissileWeapon {

		{
			image = ItemSpriteSheet.SHOOT_GUN_AMMO;

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

		@Override
		public int damageRoll(Char owner) {
			return ShootGun.this.shotDamageRoll(owner);
		}

		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return ShootGun.this.hasEnchant(type, owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			//SPS applied ArmorBreak lvl 30; substituted with Vulnerable
			Buff.prolong(defender, Vulnerable.class, 5f);
			return super.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return ShootGun.this.delayFactor(user);
		}

		@Override
		public int STRReq(int lvl) {
			return ShootGun.this.STRReq();
		}

		@Override
		protected void onThrow( int cell ) {
			Char enemy = Actor.findChar( cell );
			if (enemy == null || enemy == curUser) {
				parent = null;
				Splash.at( cell, 0xCC99FFFF, 1 );
			} else {
				if (!curUser.shoot( enemy, this )) {
					Splash.at(cell, 0xCC99FFFF, 1);
				}
			}
		}

		@Override
		public Item split(int amount) {
			return null;
		}

		@Override
		public void cast(final Hero user, final int dst) {
			useAmmo();
			updateQuickslot();
			super.cast(user, dst);
		}

		protected void useAmmo(){
			charge--;
		}
	}

	public class EndShotAmmo extends ShotAmmo {

		{
			image = ItemSpriteSheet.SHOOT_GUN_AMMO;
		}

		//SPS gave this shot ACU 1000, it effectively cannot miss
		@Override
		public float accuracyFactor(Char owner, Char target) {
			return Float.POSITIVE_INFINITY;
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			//blast every char around the target
			int p = defender.pos;
			for (int n : PathFinder.NEIGHBOURS8) {
				Char ch = Actor.findChar(n+p);
				if (ch != null && ch != defender && ch != attacker && ch.isAlive()) {
					int dr = Random.IntRange( 0, 1 );
					int dmg = Random.NormalIntRange( min(), max() );
					int effectiveDamage = Math.max( dmg - dr, 0 );
					Buff.prolong(ch, Vulnerable.class, 5f);
					ch.damage( effectiveDamage, this );
				}
			}
			//and burst the target for a chunk of its missing HP
			if (defender.properties().contains(Char.Property.BOSS)
					|| defender.properties().contains(Char.Property.MINIBOSS)){
				defender.damage(Math.min(defender.HT - defender.HP, defender.HT/6), this);
			} else {
				defender.damage(Math.min(defender.HT - defender.HP, defender.HT/3), this);
			}
			return super.proc(attacker, defender, damage);
		}

		//the whole magazine is dumped at once
		@Override
		protected void useAmmo(){
			charge = 0;
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
			return Messages.get(ShootGun.class, "prompt");
		}
	};

	private CellSelector.Listener endShooter = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ) {
			if (target != null) {
				knockEndShot().cast(curUser, target);
			}
		}
		@Override
		public String prompt() {
			return Messages.get(ShootGun.class, "prompt");
		}
	};
}
