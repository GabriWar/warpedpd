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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
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
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Ported from SPS-PD. A centaur bow that charges up special arrows as it is fired.
public class TaurcenBow extends Weapon {

	public static final String AC_SHOOT  = "SHOOT";
	public static final String AC_BREAK  = "BREAK";
	public static final String AC_FIRE   = "FIRE";
	public static final String AC_ICE    = "ICE";
	public static final String AC_POISON = "POISON";
	public static final String AC_ELE    = "ELE";

	public static final int MAX_CHARGE = 8;

	{
		image = ItemSpriteSheet.TAURCEN_BOW;

		defaultAction = AC_SHOOT;
		usesTargeting = true;

		unique = true;
		bones = false;
	}

	public enum Arrow {
		NONE, FIRE, ICE, POISON, ELE
	}

	public Arrow arrow = Arrow.NONE;

	public int charge = 0;

	private static final String CHARGE = "charge";
	private static final String ARROW  = "arrow";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( ARROW, arrow );
		bundle.put( CHARGE, charge );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		arrow = bundle.getEnum( ARROW, Arrow.class );
		charge = bundle.getInt( CHARGE );
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		//like the SPS original, this bow is never equipped, dropped or thrown
		actions.remove(AC_EQUIP);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		actions.add(AC_SHOOT);
		actions.add(AC_BREAK);
		actions.add(AC_FIRE);
		actions.add(AC_ICE);
		actions.add(AC_POISON);
		actions.add(AC_ELE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_SHOOT)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell( shooter );
		} else if (action.equals(AC_BREAK)) {
			arrow = Arrow.NONE;
		} else if (action.equals(AC_FIRE)) {
			arrow = Arrow.FIRE;
		} else if (action.equals(AC_ICE)) {
			arrow = Arrow.ICE;
		} else if (action.equals(AC_POISON)) {
			arrow = Arrow.POISON;
		} else if (action.equals(AC_ELE)) {
			arrow = Arrow.ELE;
		}
	}

	@Override
	public int STRReq(int lvl) {
		return STRReq(1, lvl); //tier 1, STR 10 like the SPS original
	}

	//SPS stats: 4-8 base, +3 min / +5 max per upgrade
	@Override
	public int min(int lvl) {
		return 4 + 3*lvl
				+ (Dungeon.hero != null ? RingOfSharpshooting.levelDamageBonus(Dungeon.hero) : 0);
	}

	@Override
	public int max(int lvl) {
		return 8 + 5*lvl
				+ (Dungeon.hero != null ? 2*RingOfSharpshooting.levelDamageBonus(Dungeon.hero) : 0);
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {

		if (charge >= MAX_CHARGE){
			specialArrow(attacker, defender, damage);
			//SPS: 1 in 8 special shots trigger the arrow effect twice
			if (Random.Int(8) == 0){
				specialArrow(attacker, defender, damage);
			}
			charge = 0;
		}

		charge++;
		updateQuickslot();

		return super.proc(attacker, defender, damage);
	}

	private void specialArrow(Char attacker, Char defender, int damage){
		switch (arrow){
			case NONE: default:
				//SPS applied ArmorBreak lvl 40; substituted with Vulnerable
				if (defender.isAlive()) Buff.prolong(defender, Vulnerable.class, 5f);
				defender.damage(damage, this);
				break;
			case FIRE:
				if (defender.isAlive()) Buff.affect(defender, Burning.class).reignite(defender, 3f);
				defender.damage(damage/2, this);
				break;
			case ICE:
				defender.damage(damage/2, this);
				//SPS applied Wet + Slow; substituted with Chill + Slow
				if (defender.isAlive()) {
					Buff.prolong(defender, Chill.class, 5f);
					Buff.prolong(defender, Slow.class, 5f);
				}
				break;
			case POISON:
				defender.damage(damage/4, this);
				if (defender.isAlive()) Buff.affect(defender, Ooze.class).set(5f);
				break;
			case ELE:
				//SPS applied Shocked lvl 3 (shock DoT) and AttackUp 30% for 10 turns;
				//substituted with Vulnerable and Adrenaline
				if (defender.isAlive()) Buff.prolong(defender, Vulnerable.class, 3f);
				Buff.prolong(attacker, Adrenaline.class, 10f);
				defender.damage(damage/3, this);
				break;
		}
	}

	@Override
	public String status() {
		return charge + "/" + MAX_CHARGE;
	}

	@Override
	public String info() {
		String info = super.info();

		info += "\n\n" + Messages.get( TaurcenBow.class, "stats",
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

		if (cursedKnown && cursed) {
			info += "\n\n" + Messages.get(Weapon.class, "cursed");
		}

		info += "\n\n" + Messages.get(this, "charge_info", charge, MAX_CHARGE);

		info += "\n\n" + Messages.get(MissileWeapon.class, "distance");

		return info;
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		return knockArrow().targetingPos(user, dst);
	}

	public TaurcenArrow knockArrow(){
		return new TaurcenArrow();
	}

	public class TaurcenArrow extends MissileWeapon {

		{
			image = ItemSpriteSheet.TAURCEN_BOW_AMMO;

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
			return TaurcenBow.this.damageRoll(owner);
		}

		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return TaurcenBow.this.hasEnchant(type, owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			return TaurcenBow.this.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return TaurcenBow.this.delayFactor(user);
		}

		@Override
		public int STRReq(int lvl) {
			return TaurcenBow.this.STRReq();
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
			return Messages.get(TaurcenBow.class, "prompt");
		}
	};
}
