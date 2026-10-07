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

package xyz.gabriwar.warpedpixeldungeon.items.quest;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.MiningLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class Pickaxe extends MeleeWeapon {
	
	{
		image = ItemSpriteSheet.PICKAXE;

		levelKnown = true;
		
		unique = true;
		bones = false;

		tier = 2;
	}

	@Override
	public int STRReq(int lvl) {
		return super.STRReq(lvl) + 2; //tier 3 strength requirement with tier 2 damage stats
	}

	public static final String AC_DIG = "DIG";

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (Dungeon.level instanceof MiningLevel){
			actions.remove(AC_DROP);
			actions.remove(AC_THROW);
		}
		if (canDigDown( hero )) actions.add( AC_DIG );
		return actions;
	}

	//on a slice of the world with a slice under it, standing on plain ground
	//(not a bridge, not water, not a way between slices, not in the town)
	private static boolean canDigDown( Hero hero ){
		if (!(Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel)) return false;
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow
				= (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
		int cell = hero.pos;
		//never down onto a sealed tomb of the slice below (levels/overworld/CaveSites)
		if (!ow.fallsThrough( cell ) || ow.inTown( cell ) || !ow.insideMap( cell ) || ow.sealedBelow( cell )) return false;
		int t = Dungeon.level.map[cell];
		return (xyz.gabriwar.warpedpixeldungeon.levels.Terrain.flags[t] & xyz.gabriwar.warpedpixeldungeon.levels.Terrain.LIQUID) == 0
				&& t != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.BRIDGE
				&& t != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.FROZEN_WATER
				&& t != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.ENTRANCE
				&& t != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.EXIT
				&& t != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.ENTRANCE_SP
				&& t != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.PEDESTAL
				&& t != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.DOOR
				&& t != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.OPEN_DOOR;
	}

	@Override
	public void execute( Hero hero, String action ) {
		if (action.equals( AC_DIG )){
			if (!canDigDown( hero )) return;
			final xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow
					= (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
			//three swings' worth of work, then down the shaft
			hero.busy();
			hero.spend( 3 * Actor.TICK );
			hero.sprite.operate( hero.pos, new Callback() {
				@Override
				public void call() {
					xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene.shake( 0.5f, 0.5f );
					xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( hero.pos ).burst(
							xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.ROCK ), 6 );
					Sample.INSTANCE.play( Assets.Sounds.MINE );
					GLog.i( Messages.get( Pickaxe.class, "dug_down" ) );
					hero.sprite.idle();
					ow.digDown( hero );
				}
			} );
			return;
		}
		super.execute( hero, action );
	}
	@Override
	public boolean keptThroughLostInventory() {
		//pickaxe is always kept when it's needed for the mining level
		return super.keptThroughLostInventory() || Dungeon.level instanceof MiningLevel;
	}
	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		if (target == null) {
			return;
		}

		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(this, "ability_no_target"));
			return;
		}

		hero.belongings.abilityWeapon = this;
		if (!hero.canAttack(enemy)){
			GLog.w(Messages.get(this, "ability_target_range"));
			hero.belongings.abilityWeapon = null;
			return;
		}
		hero.belongings.abilityWeapon = null;

		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				int damageBoost = 0;
				if (Char.hasProp(enemy, Char.Property.INORGANIC)
						|| enemy instanceof Swarm
						|| enemy instanceof Bee
						|| enemy instanceof Crab
						|| enemy instanceof Spinner
						|| enemy instanceof Scorpio) {
					//+(8+2*lvl) damage, equivalent to +100% damage
					damageBoost = augment.damageFactor(8 + 2*buffedLvl());
				}
				beforeAbilityUsed(hero, enemy);
				AttackIndicator.target(enemy);
				if (hero.attack(enemy, 1, damageBoost, Char.INFINITE_ACCURACY)) {
					if (enemy.isAlive()) {
						Buff.affect(enemy, Vulnerable.class, 3f);
					} else {
						onAbilityKill(hero, enemy);
					}
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				}
				Invisibility.dispel();
				hero.spendAndNext(hero.attackDelay());
				afterAbilityUsed(hero);
			}
		});
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = 8 + 2*buffedLvl();
		return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
	}

	public String upgradeAbilityStat(int level){
		int dmgBoost = 8 + 2*level;
		return augment.damageFactor(min(level)+dmgBoost) + "-" + augment.damageFactor(max(level)+dmgBoost);
	}

}
