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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class PhantomStrike extends Skill {

	{
		tag = "A4";
		name = "Phantom Strike";
		castText = "Nowhere to run";
		image = 118;
		tier = 4;
		mana = 12;
	}

	private static final int RANGE = 8;

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
			Dungeon.hero.heroSkills.lastUsed = this;
			GameScene.selectCell( targeter );
		}
	}

	private final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ){
			if (target == null) return;

			Hero curUser = Dungeon.hero;
			//the selector stays live across other casts, so re-check before spending
			if (level <= 0 || curUser.MP < getManaCost()) return;

			if (Dungeon.level.distance( curUser.pos, target ) > RANGE){
				GLog.w( Messages.get(PhantomStrike.this, "too_far") );
				return;
			}

			Ballistica path = new Ballistica( curUser.pos, target, Ballistica.PROJECTILE );
			Char ch = Actor.findChar( path.collisionPos );
			if (ch == null || ch == curUser){
				GLog.w( Messages.get(PhantomStrike.this, "no_target") );
				return;
			}

			ArrayList<Integer> spots = new ArrayList<>();
			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++){
				int p = ch.pos + PathFinder.NEIGHBOURS8[i];
				if (p < 0 || p >= Dungeon.level.length()) continue;
				if (Actor.findChar( p ) == null && Dungeon.level.passable[p]){
					spots.add( p );
				}
			}
			if (spots.isEmpty()){
				GLog.w( Messages.get(PhantomStrike.this, "no_room") );
				return;
			}

			int oldPos = curUser.pos;
			ScrollOfTeleportation.appear( curUser, Random.element( spots ) );
			CellEmitter.get( oldPos ).burst( Speck.factory( Speck.SMOKE ), 6 );

			float mult = 1f + 0.15f * level;
			if (ch instanceof Mob){
				Mob mob = (Mob) ch;
				if (mob.state == mob.SLEEPING || mob.state == mob.WANDERING){
					mult *= 1.5f;
				}
			}
			//no accuracy roll, but the rest of the tree still gets its on-hit pass
			int dmg = Math.round( curUser.damageRoll() * mult );
			dmg = curUser.heroSkills.allOnHit( ch, dmg, false );
			ch.damage( dmg, curUser );

			curUser.MP -= getManaCost();
			castTextYell();
			curUser.sprite.attack( ch.pos );
			Invisibility.dispel();
			curUser.spendAndNext( TIME_TO_USE );
			curUser.busy();
		}

		@Override
		public String prompt(){ return "Choose your mark"; }
	};

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public String info(){
		return Messages.get(this, "desc", RANGE) + "\n"
				+ costUpgradeInfo();
	}
}
