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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Recharging;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSharpshooting;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Ported from SPS-PD. An upgradable elven shortbow whose shots scale with magic power.
public class ElfBow extends Weapon {

	public static final String AC_SHOOT = "SHOOT";
	public static final String AC_DRINK = "DRINK";

	{
		image = ItemSpriteSheet.ELF_BOW;

		defaultAction = AC_SHOOT;
		usesTargeting = true;

		unique = true;
		bones = false;
	}

	//number of times the mana water has been drunk
	public int charge = 0;

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

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_SHOOT);
		actions.add(AC_DRINK);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_SHOOT)) {

			if (!isEquipped( hero )){
				GLog.i( Messages.get(this, "need_equip") );
			} else {
				curUser = hero;
				curItem = this;
				GameScene.selectCell( shooter );
			}

		} else if (action.equals(AC_DRINK)) {

			if (!isEquipped( hero )){
				GLog.i( Messages.get(this, "need_equip") );
			} else {
				//SPS granted 10 skill points; WPD has no skill points, so grant
				//10 turns of recharging instead
				Buff.prolong( hero, Recharging.class, 10f );
				charge++;
				if (charge > 10) {
					//SPS turned the bow into a +3 Demon Blade; WPD has no demon
					//blade, so a +3 war scythe is dropped instead
					hero.belongings.weapon = null;
					Dungeon.level.drop( new WarScythe().upgrade(3), hero.pos ).sprite.drop();
					GLog.w( Messages.get(this, "transform") );
					updateQuickslot();
				}
			}
		}
	}

	@Override
	public int STRReq(int lvl) {
		return STRReq(1, lvl); //tier 1, STR 10 like the SPS original
	}

	//SPS stats: 2-6 base, +2 min / +4 max per upgrade
	@Override
	public int min(int lvl) {
		return 2 + 2*lvl
				+ (Dungeon.hero != null ? RingOfSharpshooting.levelDamageBonus(Dungeon.hero) : 0);
	}

	@Override
	public int max(int lvl) {
		return 6 + 4*lvl
				+ (Dungeon.hero != null ? 2*RingOfSharpshooting.levelDamageBonus(Dungeon.hero) : 0);
	}

	@Override
	public int damageRoll(Char owner) {
		int damage = super.damageRoll(owner);

		//SPS: +10% damage per point of magic skill. WPD has no magic skill stat,
		//so +10% per level of the strongest wand being carried is used instead.
		if (owner instanceof Hero){
			int wandLvl = 0;
			for (Item i : ((Hero) owner).belongings){
				if (i instanceof Wand){
					wandLvl = Math.max(wandLvl, ((Wand) i).buffedLvl());
				}
			}
			damage = Math.round(damage * (1f + 0.1f*wandLvl));
		}

		return damage;
	}

	@Override
	public String info() {
		String info = super.info();

		info += "\n\n" + Messages.get( ElfBow.class, "stats",
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

		info += "\n\n" + Messages.get(this, "charge_info", charge, 10);

		info += "\n\n" + Messages.get(MissileWeapon.class, "distance");

		return info;
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		return knockArrow().targetingPos(user, dst);
	}

	public ElfArrow knockArrow(){
		return new ElfArrow();
	}

	public class ElfArrow extends MissileWeapon {

		{
			image = ItemSpriteSheet.ELF_BOW_AMMO;

			hitSound = Assets.Sounds.HIT_ARROW;

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
			return ElfBow.this.damageRoll(owner);
		}

		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return ElfBow.this.hasEnchant(type, owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			//SPS: killing blows restore a skill point; grant a short burst of
			//recharging instead
			if (damage > defender.HP && attacker instanceof Hero){
				Buff.prolong( attacker, Recharging.class, 2f );
			}
			return ElfBow.this.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return ElfBow.this.delayFactor(user);
		}

		@Override
		public int STRReq(int lvl) {
			return ElfBow.this.STRReq();
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
		public void throwSound() {
			Sample.INSTANCE.play( Assets.Sounds.ATK_SPIRITBOW, 1, Random.Float(0.87f, 1.15f) );
		}
	}

	private CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ) {
			if (target != null) {
				knockArrow().cast(curUser, target);
			}
		}
		@Override
		public String prompt() {
			return Messages.get(ElfBow.class, "prompt");
		}
	};
}
