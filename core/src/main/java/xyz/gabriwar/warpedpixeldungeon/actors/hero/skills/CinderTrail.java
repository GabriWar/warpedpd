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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class CinderTrail extends Skill {

	{
		tag = "D1";
		name = "Cinder Trail";
		castText = "Let it burn!";
		tier = 1;
		image = 29;
		mana = 4;
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
			GameScene.selectCell( igniter );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private final CellSelector.Listener igniter = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ){
			if (target == null)
				return;

			Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost())
				return;

			int cell = new Ballistica( hero.pos, target, Ballistica.PROJECTILE ).collisionPos;
			int kicker = 2 * pyromancyLevel();

			ignite( cell, 3 + 3 * level + kicker );
			for (int n : PathFinder.NEIGHBOURS4){
				int c = cell + n;
				if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c])
					ignite( c, 2 + 2 * level + kicker );
			}

			hero.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.BURNING );
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}

		@Override
		public String prompt(){
			return "Choose a cell to set alight";
		}
	};

	//the fire does not care whose feet it is under, the hero's included
	private static void ignite( int cell, int amount ){
		GameScene.add( Blob.seed( cell, amount, Fire.class ) );
		Char ch = Actor.findChar( cell );
		if (ch != null)
			Buff.affect( ch, Burning.class ).reignite( ch );
	}

	private static int pyromancyLevel(){
		if (Dungeon.hero == null || Dungeon.hero.heroSkills == null)
			return 0;
		for (Skill s : Dungeon.hero.heroSkills.fourthSkills)
			if (s instanceof PyreAffinity)
				return s.level;
		return 0;
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
