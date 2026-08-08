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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class ScouringFlame extends Skill {

	{
		name = "Scouring Flame";
		castText = "Burn clean!";
		tag = "D2";
		image = 156;
		tier = 2;
		mana = 7;
		level = 0;
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
			GameScene.selectCell( zapper );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private final CellSelector.Listener zapper = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer cell ){
			if (cell == null)
				return;

			final Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost())
				return;

			final Char target = Actor.findChar( cell );
			if (target == null || target == hero || !Dungeon.level.heroFOV[target.pos]){
				GLog.w( Messages.get(ScouringFlame.this, "no_target") );
				return;
			}

			hero.MP -= getManaCost();
			castTextYell();
			hero.sprite.zap( target.pos );
			hero.busy();
			Sample.INSTANCE.play( Assets.Sounds.ZAP );

			MagicMissile.boltFromChar( hero.sprite.parent,
					MagicMissile.LIGHT_MISSILE,
					hero.sprite,
					target.pos,
					new Callback() {
						@Override
						public void call(){
							int dmg = 4 + 3 * level;
							if (Char.hasProp( target, Char.Property.UNDEAD ) || Char.hasProp( target, Char.Property.DEMONIC ))
								dmg *= 2;
							CellEmitter.center( target.pos ).burst( Speck.factory( Speck.LIGHT ), 5 );
							target.damage( dmg, ScouringFlame.this );
							if (target.isAlive())
								Buff.prolong( target, Blindness.class, 2 + level );
							hero.spendAndNext( TIME_TO_USE );
						}
					} );

			Invisibility.dispel();
		}

		@Override
		public String prompt(){
			return "Choose a target to scour";
		}
	};

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
