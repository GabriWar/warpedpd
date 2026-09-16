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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * Warrior: a running leap onto an enemy a few tiles off. The hero arcs over the
 * ground between, lands beside the target with a thud that shakes the screen, and
 * comes down on it with the weapon, hard enough to leave it stunned for a turn.
 */
public class Leap extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	{
		tag = "A5";
		name = "Leap";
		castText = "Hyah!";
		image = 196;
		tier = 3;
		mana = 6;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			if (hero.rooted){
				GLog.w( Messages.get(Leap.class, "rooted") );
				return;
			}
			GameScene.selectCell( new Pouncer() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	public int range(){
		return 2 + level;
	}

	private class Pouncer extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;
			final Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost()) return;
			if (hero.rooted){
				GLog.w( Messages.get(Leap.class, "rooted") );
				return;
			}

			final Char victim = Actor.findChar( target );
			if (victim == null || victim.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[target]){
				GLog.w( Messages.get(Leap.class, "no_target") );
				return;
			}
			if (Dungeon.level.distance( hero.pos, target ) > range()){
				GLog.w( Messages.get(Leap.class, "too_far") );
				return;
			}
			//the leap arcs over whatever is on the ground between, but not through walls
			Ballistica traj = new Ballistica( hero.pos, target, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET );
			if (traj.collisionPos.intValue() != target.intValue()){
				GLog.w( Messages.get(Leap.class, "no_path") );
				return;
			}

			int landing = hero.pos;
			if (!Dungeon.level.adjacent( hero.pos, target )){
				landing = -1;
				if (traj.dist >= 1){
					int c = traj.path.get( traj.dist - 1 );
					if (Dungeon.level.passable[c] && Actor.findChar( c ) == null) landing = c;
				}
				if (landing == -1){
					for (int n : PathFinder.NEIGHBOURS8){
						int c = target + n;
						if (c < 0 || c >= Dungeon.level.length()) continue;
						if (!Dungeon.level.passable[c] || Actor.findChar( c ) != null) continue;
						if (landing == -1 || Dungeon.level.distance( hero.pos, c ) < Dungeon.level.distance( hero.pos, landing )){
							landing = c;
						}
					}
				}
				if (landing == -1){
					GLog.w( Messages.get(Leap.class, "no_room") );
					return;
				}
			}

			hero.MP -= getManaCost();
			castTextYell();
			Dungeon.hero.heroSkills.lastUsed = Leap.this;
			Invisibility.dispel();

			final int land = landing;
			hero.busy();
			Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 0.7f );
			hero.sprite.jump( hero.pos, land, () -> {
				if (land != hero.pos){
					hero.move( land );
					Dungeon.level.occupyCell( hero );
					Dungeon.observe();
					GameScene.updateFog();
				}
				SkillFX.land( land );
                xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX.show(land,true);
				if (victim.isAlive()){
					KindOfWeapon wep = hero.belongings.weapon();
					int roll = wep != null ? wep.damageRoll( hero ) : RingOfForce.damageRoll( hero );
					int dmg = Math.round( roll * (1f + 0.1f * level) );
					victim.damage( dmg, Leap.this );
					Wound.hit( victim );
					Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 0.9f );
					if (victim.isAlive() && !victim.properties().contains( Char.Property.BOSS )){
						Buff.affect( victim, Paralysis.class, 1f );
					}
				}
				//+3: the landing's shockwave throws every other enemy next to the hero a tile away
				if (level >= MAX_LEVEL){
					for (int c : SkillInteractions.area( land, 1 )){
						if (c == land) continue;
						xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX.show( c );
						Char near = Actor.findChar( c );
						if (near != null && near != victim && near.alignment == Char.Alignment.ENEMY)
							SkillInteractions.push( near, land, 1, 0 );
					}
				}
				hero.spendAndNext( TIME_TO_USE );
			} );
		}

		@Override
		public String prompt(){
			return Messages.get(Leap.class, "prompt");
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
