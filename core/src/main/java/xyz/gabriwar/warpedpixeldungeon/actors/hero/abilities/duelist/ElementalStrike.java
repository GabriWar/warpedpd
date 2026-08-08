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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AllyBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.CounterBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Dazzling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Explosive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Friendly;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wayward;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blooming;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Corrupting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Elastic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Unstable;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ConeAOE;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ElementalStrike extends ArmorAbility {

	//TODO a few duplicates here (curse duplicates are fine)
	private static final HashMap<Class<?extends Weapon.Enchantment>, Integer> effectTypes = new HashMap<>();
	static {
		effectTypes.put(Blazing.class,      MagicMissile.FIRE_CONE);
		effectTypes.put(Chilling.class,     MagicMissile.FROST_CONE);
		effectTypes.put(Kinetic.class,      MagicMissile.FORCE_CONE);
		effectTypes.put(Shocking.class,     MagicMissile.SPARK_CONE);
		effectTypes.put(Blocking.class,     MagicMissile.WARD_CONE);
		effectTypes.put(Blooming.class,     MagicMissile.FOLIAGE_CONE);
		effectTypes.put(Elastic.class,      MagicMissile.FORCE_CONE);
		effectTypes.put(Lucky.class,        MagicMissile.RAINBOW_CONE);
		effectTypes.put(Projecting.class,   MagicMissile.PURPLE_CONE);
		effectTypes.put(Unstable.class,     MagicMissile.RAINBOW_CONE);
		effectTypes.put(Corrupting.class,   MagicMissile.SHADOW_CONE);
		effectTypes.put(Grim.class,         MagicMissile.SHADOW_CONE);
		effectTypes.put(Vampiric.class,     MagicMissile.BLOOD_CONE);

		effectTypes.put(Annoying.class,     MagicMissile.SHADOW_CONE);
		effectTypes.put(Displacing.class,   MagicMissile.SHADOW_CONE);
		effectTypes.put(Dazzling.class,     MagicMissile.SHADOW_CONE);
		effectTypes.put(Explosive.class,    MagicMissile.SHADOW_CONE);
		effectTypes.put(Sacrificial.class,  MagicMissile.SHADOW_CONE);
		effectTypes.put(Wayward.class,      MagicMissile.SHADOW_CONE);
		effectTypes.put(Polarized.class,    MagicMissile.SHADOW_CONE);
		effectTypes.put(Friendly.class,     MagicMissile.SHADOW_CONE);

		effectTypes.put(null,               MagicMissile.MAGIC_MISS_CONE);
	}

	{
		baseChargeUse = 25;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return dst;
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null){
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Ballistica aim = new Ballistica(hero.pos, target, Ballistica.WONT_STOP);

		int maxDist = 4 + hero.pointsInTalent(Talent.ELEMENTAL_REACH);
		int dist = Math.min(aim.dist, maxDist);

		ConeAOE cone = new ConeAOE(aim,
				dist,
				65 + 10*hero.pointsInTalent(Talent.ELEMENTAL_REACH),
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);

		KindOfWeapon w = hero.belongings.weapon();
		Weapon.Enchantment enchantment = null;
		if (w instanceof MeleeWeapon) {
			enchantment = ((MeleeWeapon) w).enchantment;
		}
		Class<?extends Weapon.Enchantment> enchCls = null;
		if (enchantment != null){
			enchCls = enchantment.getClass();
		}

		//cast to cells at the tip, rather than all cells, better performance.
		for (Ballistica ray : cone.outerRays){
			((MagicMissile)hero.sprite.parent.recycle( MagicMissile.class )).reset(
					effectTypes.get(enchCls),
					hero.sprite,
					ray.path.get(ray.dist),
					null
			);
		}

		Weapon.Enchantment finalEnchantment = enchantment;
		hero.sprite.attack(target, new Callback() {
			@Override
			public void call() {

				Char enemy = Actor.findChar(target);

				if (enemy != null) {
					if (hero.isCharmedBy(enemy)) {
						enemy = null;
					} else if (enemy.alignment == hero.alignment) {
						enemy = null;
					} else if (!hero.canAttack(enemy)) {
						enemy = null;
					}
				}

				preAttackEffect(cone, hero, finalEnchantment);

				if (enemy != null){
					AttackIndicator.target(enemy);
					oldEnemyPos = enemy.pos;
					if (hero.attack(enemy, 1, 0, Char.INFINITE_ACCURACY)) {
						Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
					}
				}

				perCellEffect(cone, finalEnchantment);

				perCharEffect(cone, hero, enemy, finalEnchantment);

				Invisibility.dispel();
				hero.spendAndNext(hero.attackDelay());
			}
		});

		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		hero.busy();

	}

	//effects that trigger before the attack
	private void preAttackEffect(ConeAOE cone, Hero hero, Weapon.Enchantment ench){

		int targetsHit = 0;
		for (Char ch : Actor.chars()){
			if (ch.alignment == Char.Alignment.ENEMY && cone.cells.contains(ch.pos)){
				targetsHit++;
			}
		}

		if (hero.hasTalent(Talent.DIRECTED_POWER)){
			float enchBoost = 0.30f * targetsHit * hero.pointsInTalent(Talent.DIRECTED_POWER);
			Buff.affect(hero, DirectedPowerTracker.class, 0f).enchBoost = enchBoost;
		}

		float powerMulti = 1f + 0.30f*Dungeon.hero.pointsInTalent(Talent.STRIKING_FORCE);

		//*** Kinetic ***
		if (ench instanceof Kinetic){
			if (hero.buff(Kinetic.ConservedDamage.class) != null) {
				storedKineticDamage = hero.buff(Kinetic.ConservedDamage.class).damageBonus();
			}

		//*** Blocking ***
		} else if (ench instanceof Blocking){
			if (targetsHit > 0){
				int shield = Math.round(Math.round(6f*targetsHit*powerMulti));
				Buff.affect(hero, Barrier.class).setShield(Math.round(6f*targetsHit*powerMulti));
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
			}

		//*** Vampiric ***
		} else if (ench instanceof Vampiric){
			if (targetsHit > 0){
				int heal = Math.round(2.5f*targetsHit*powerMulti);
				heal = Math.min( heal, hero.HT - hero.HP );
				if (heal > 0){
					hero.HP += heal;
					hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );
				}
			}

		//*** Sacrificial ***
		} else if (ench instanceof Sacrificial){
			Buff.affect(hero, Bleeding.class).set(10 * powerMulti);
		}

	}

	public static class DirectedPowerTracker extends FlavourBuff{
		public float enchBoost = 0f;
	}

	public static class ElementalStrikeLuckyTracker extends Buff{};

	private int storedKineticDamage = 0;

	public static class ElementalStrikeFurrowCounter extends CounterBuff{{revivePersists = true;}};

	//effects that affect the cells of the environment themselves
	private void perCellEffect(ConeAOE cone, Weapon.Enchantment ench){

		int targetsHit = 0;
		for (Char ch : Actor.chars()){
			if (ch.alignment == Char.Alignment.ENEMY && cone.cells.contains(ch.pos)){
				targetsHit++;
			}
		}

		float powerMulti = 1f + 0.30f*Dungeon.hero.pointsInTalent(Talent.STRIKING_FORCE);

		//*** Blazing ***
		if (ench instanceof Blazing){
			for (int cell : cone.cells) {
				GameScene.add(Blob.seed(cell, Math.round(8 * powerMulti), Fire.class));
			}

		//*** Chilling ***
		} else if (ench instanceof Chilling){
			for (int cell : cone.cells) {
				GameScene.add(Blob.seed(cell, Math.round(8 * powerMulti), Freezing.class));
			}

		//*** Shocking ***
		} else if (ench instanceof Shocking){
			for (int cell : cone.cells) {
				GameScene.add(Blob.seed(cell, Math.round(8 * powerMulti), Electricity.class));
			}

		//*** Blooming ***
		} else if (ench instanceof Blooming){
			ArrayList<Integer> cells = new ArrayList<>(cone.cells);
			Random.shuffle(cells);
			int grassToPlace = Math.round(8*powerMulti);

			//start spawning furrowed grass if exp is not being gained
			// each hero level is worth 20 normal uses, but just 5 if no enemies are present
			// cap of 40/10 uses
			int highGrassType = Terrain.HIGH_GRASS;
			if (Buff.affect(Dungeon.hero, ElementalStrikeFurrowCounter.class).count() >= 40){
				highGrassType = Terrain.FURROWED_GRASS;
			} else {
				if (Dungeon.hero.visibleEnemies() == 0 && targetsHit == 0) {
					Buff.count(Dungeon.hero, ElementalStrikeFurrowCounter.class, 4f);
				} else {
					Buff.count(Dungeon.hero, ElementalStrikeFurrowCounter.class, 1f);
				}
			}

			for (int cell : cells) {
				int terr = Dungeon.level.map[cell];
				if (terr == Terrain.EMPTY || terr == Terrain.EMBERS || terr == Terrain.EMPTY_DECO ||
						terr == Terrain.GRASS) {
					if (grassToPlace > 0
							&& !Char.hasProp(Actor.findChar(cell), Char.Property.IMMOVABLE)
							&& Dungeon.level.plants.get(cell) == null){
						Level.set(cell, highGrassType);
						grassToPlace--;
					} else {
						Level.set(cell, Terrain.GRASS);
					}
					GameScene.updateMap( cell );
				}
			}
			Dungeon.observe();
		}
	}

	private int oldEnemyPos;

	//effects that affect the characters within the cone AOE
	private void perCharEffect(ConeAOE cone, Hero hero, Char primaryTarget, Weapon.Enchantment ench) {

		float powerMulti = 1f + 0.30f * Dungeon.hero.pointsInTalent(Talent.STRIKING_FORCE);

		ArrayList<Char> affected = new ArrayList<>();

		for (Char ch : Actor.chars()) {
			if (ch.alignment != Char.Alignment.ALLY && cone.cells.contains(ch.pos)) {
				affected.add(ch);
			}
		}

		//*** no enchantment ***
		if (ench == null) {
			for (Char ch : affected){
				ch.damage(Math.round(powerMulti* Hero.heroDamageIntRange(6, 12)), ElementalStrike.this);
			}

		//*** Kinetic ***
		} else if (ench instanceof Kinetic){
			if (storedKineticDamage > 0) {
				for (Char ch : affected) {
					if (ch != primaryTarget) {
						ch.damage(Math.round(storedKineticDamage * 0.4f * powerMulti), ench);
					}
				}
				storedKineticDamage = 0;
			}
			//clear stored damage if there was no primary target
			if (primaryTarget == null && hero.buff(Kinetic.ConservedDamage.class) != null){
				hero.buff(Kinetic.ConservedDamage.class).detach();
			}

		//*** Blooming ***
		} else if (ench instanceof Blooming){
			for (Char ch : affected){
				Buff.affect(ch, Roots.class, Math.round(6f*powerMulti));
			}

		//*** Elastic ***
		} else if (ench instanceof Elastic){

			//sorts affected from furthest to closest
			Collections.sort(affected, new Comparator<Char>() {
				@Override
				public int compare(Char a, Char b) {
					return Dungeon.level.distance(hero.pos, a.pos) - Dungeon.level.distance(hero.pos, b.pos);
				}
			});

			for (Char ch : affected){
				if (ch == primaryTarget && oldEnemyPos != primaryTarget.pos) continue;

				Ballistica aim = new Ballistica(hero.pos, ch.pos, Ballistica.WONT_STOP);
				int knockback = Math.round(5*powerMulti);
				WandOfBlastWave.throwChar(ch,
						new Ballistica(ch.pos, aim.collisionPos, Ballistica.MAGIC_BOLT),
						knockback,
						true,
						true,
						ElementalStrike.this);
			}

		//*** Lucky ***
		} else if (ench instanceof Lucky){
			for (Char ch : affected){
				if (ch.alignment == Char.Alignment.ENEMY
						&& Random.Float() < 0.125f*powerMulti
						&& ch.buff(ElementalStrikeLuckyTracker.class) == null) {
					Dungeon.level.drop(Lucky.genLoot(), ch.pos).sprite.drop();
					Lucky.showFlare(ch.sprite);
					Buff.affect(ch, ElementalStrikeLuckyTracker.class);
				}
			}

		//*** Projecting ***
		} else if (ench instanceof Projecting){
			for (Char ch : affected){
				if (ch != primaryTarget) {
					ch.damage(Math.round(hero.damageRoll() * 0.3f * powerMulti), ench);
				}
			}

		//*** Unstable ***
		} else if (ench instanceof Unstable){
			KindOfWeapon w = hero.belongings.weapon();
			if (w instanceof Weapon) {
				for (Char ch : affected){
					if (ch != primaryTarget) {
						ench.proc((Weapon) w, hero, ch, w.damageRoll(hero));
					}
				}
			}

		//*** Corrupting ***
		} else if (ench instanceof Corrupting){
			for (Char ch : affected){
				if (ch != primaryTarget
						&& !ch.isImmune(Corruption.class)
						&& ch.buff(Corruption.class) == null
						&& ch instanceof Mob
						&& ch.isAlive()) {
					float hpMissing = 1f - (ch.HP / (float)ch.HT);
					float chance = 0.05f + 0.2f*hpMissing; //5-25%
					if (Random.Float() < chance*powerMulti){
						Corruption.corruptionHeal(ch);
						AllyBuff.affectAndLoot((Mob) ch, hero, Corruption.class);
					}
				}
			}

		//*** Grim ***
		} else if (ench instanceof Grim){
			for (Char ch : affected){
				if (ch != primaryTarget) {
					float hpMissing = 1f - (ch.HP / (float)ch.HT);
					float chance = 0.06f + 0.24f*hpMissing; //6-30%
					if (Random.Float() < chance*powerMulti){
						ch.damage( ch.HP, Grim.class );
						ch.sprite.emitter().burst( ShadowParticle.UP, 5 );
					}
				}
			}

		//*** Annoying ***
		} else if (ench instanceof Annoying){
			for (Char ch : affected){
				if (Random.Float() < 0.2f*powerMulti){
					//TODO totally should add a bit of dialogue here
					Buff.affect(ch, Amok.class, 6f);
				}
			}

		//*** Displacing ***
		} else if (ench instanceof Displacing){
			for (Char ch : affected){
				if (Random.Float() < 0.5f*powerMulti){
					int oldpos = ch.pos;
					if (ScrollOfTeleportation.teleportChar(ch)){
						if (Dungeon.level.heroFOV[oldpos]) {
							CellEmitter.get( oldpos ).start( Speck.factory( Speck.LIGHT ), 0.2f, 3 );
						}

						if (ch instanceof Mob && ((Mob) ch).state == ((Mob) ch).HUNTING){
							((Mob) ch).state = ((Mob) ch).WANDERING;
						}
					}
				}
			}

		//*** Dazzling ***
		} else if (ench instanceof Dazzling){
			for (Char ch : affected){
				if (Random.Float() < 0.5f*powerMulti){
					Buff.affect(ch, Blindness.class, 6f);
				}
			}

		//*** Explosive ***
		} else if (ench instanceof Explosive){
			if (Random.Float() < 0.5f*powerMulti){
				Char exploding = Random.element(affected);
				if (exploding != null) new Bomb.ConjuredBomb().explode(exploding.pos);
			}

		//*** Sacrificial ***
		} else if (ench instanceof Sacrificial){
			for (Char ch : affected){
				Buff.affect(ch, Bleeding.class).set(12f*powerMulti);
			}

		//*** Wayward ***
		} else if (ench instanceof Wayward){
			for (Char ch : affected){
				if (Random.Float() < 0.5f*powerMulti){
					Buff.affect(ch, Hex.class, 6f);
				}
			}

		//*** Polarized ***
		} else if (ench instanceof Polarized){
			for (Char ch : affected){
				if (Random.Float() < 0.5f*powerMulti){
					ch.damage(Hero.heroDamageIntRange(24, 36), ElementalStrike.this);
				}
			}

		//*** Friendly ***
		} else if (ench instanceof Friendly){
			for (Char ch : affected){
				if (Random.Float() < 0.5f*powerMulti){
					Buff.affect(ch, Charm.class, 6f).object = hero.id();
				}
			}
		}

	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		if (Game.scene() instanceof GameScene){
			KindOfWeapon w = Dungeon.hero.belongings.weapon();
			if (w instanceof MeleeWeapon && ((MeleeWeapon) w).enchantment != null){
				desc += "\n\n" + Messages.get(((MeleeWeapon) w).enchantment, "elestrike_desc");
			} else {
				desc += "\n\n" + Messages.get(this, "generic_desc");
			}
		} else {
			desc += "\n\n" + Messages.get(this, "generic_desc");
		}
		desc += "\n\n" + Messages.get(this, "cost", (int)baseChargeUse);
		return desc;
	}

	@Override
	public int icon() {
		return HeroIcon.ELEMENTAL_STRIKE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.ELEMENTAL_REACH, Talent.STRIKING_FORCE, Talent.DIRECTED_POWER, Talent.HEROIC_ENERGY};
	}

}
