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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class BladeBind extends Skill {

	{
		tag = "CD";
		name = "Blade Bind";
		castText = "Bound!";
		image = 137;
		tier = 4;
		mana = 7;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			GameScene.selectCell( new CellSelector.Listener(){
				@Override
				public void onSelect( Integer target ){
					if (target != null){
						bind( hero, target );
					}
				}

				@Override
				public String prompt(){
					return "Choose a blade to bind";
				}
			} );
		}
	}

	private void bind( Hero hero, int target ){
		//the selector stays live across other casts, so re-check before spending
		if (level <= 0 || hero.MP < getManaCost()) return;

		final Char ch = Actor.findChar( target );
		if (ch == null || ch.alignment != Char.Alignment.ENEMY
				|| !Dungeon.level.heroFOV[target]
				|| Dungeon.level.distance( hero.pos, target ) > 3){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		final int cripple = 3 + 2 * level;
		final int vulnerable = 3 + level;
		final boolean weaken = level >= MAX_LEVEL;

		hero.MP -= getManaCost();
		castTextYell();
		Dungeon.hero.heroSkills.lastUsed = this;
		hero.sprite.zap( ch.pos );
		hero.busy();

		MagicMissile.boltFromChar( hero.sprite.parent,
				MagicMissile.RAINBOW,
				hero.sprite,
				ch.pos,
				new Callback(){
					@Override
					public void call(){
						if (ch.isAlive()){
							Buff.prolong( ch, Cripple.class, cripple );
							Buff.prolong( ch, Vulnerable.class, vulnerable );
							if (weaken){
								Buff.prolong( ch, Weakness.class, 5 );
							}
							if (ch.sprite != null){
								ch.sprite.showStatus( CharSprite.WARNING, "bound!" );
							}
						}
						Dungeon.hero.spendAndNext( TIME_TO_USE );
					}
				} );
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
