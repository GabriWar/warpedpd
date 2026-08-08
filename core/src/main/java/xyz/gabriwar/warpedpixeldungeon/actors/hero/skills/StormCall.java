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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class StormCall extends Skill {

	private static final int ARC_RANGE = 4;

	{
		tag = "D3";
		name = "Storm Call";
		castText = "The sky answers!";
		tier = 3;
		image = 47;
		mana = 9;
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
			GameScene.selectCell( caller );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private final CellSelector.Listener caller = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ){
			if (target == null)
				return;

			Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost())
				return;

			final int cell = new Ballistica( hero.pos, target, Ballistica.MAGIC_BOLT ).collisionPos;
			Char primary = Actor.findChar( cell );
			if (primary == null){
				GLog.w( Messages.get(StormCall.this, "no_target") );
				return;
			}

			hero.sprite.parent.add( new Lightning( hero.pos, cell, null ) );
			Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
			strike( primary, roll(), true );

			ArrayList<Mob> chain = new ArrayList<>();
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob != primary && mob.alignment == Char.Alignment.ENEMY
						&& Dungeon.level.distance( cell, mob.pos ) <= ARC_RANGE)
					chain.add( mob );
			}
			Collections.sort( chain, new Comparator<Mob>() {
				@Override
				public int compare( Mob a, Mob b ){
					return Dungeon.level.distance( cell, a.pos ) - Dungeon.level.distance( cell, b.pos );
				}
			} );

			int arcs = Math.min( level, chain.size() );
			int from = cell;
			for (int i = 0; i < arcs; i++){
				Mob mob = chain.get( i );
				hero.sprite.parent.add( new Lightning( from, mob.pos, null ) );
				Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
				strike( mob, Math.round( roll() * 0.6f ), false );
				from = mob.pos;
			}

			hero.MP -= getManaCost();
			castTextYell();
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
			Invisibility.dispel();
		}

		@Override
		public String prompt(){
			return "Choose a target to call the storm down on";
		}
	};

	private int roll(){
		return Random.NormalIntRange( 4 + 2 * level, 8 + 4 * level );
	}

	private void strike( Char ch, int damage, boolean primary ){
		boolean wet = Dungeon.level.water[ch.pos];
		if (wet)
			damage = Math.round( damage * 1.33f );

		if (ch.sprite != null)
			ch.sprite.flash();
		ch.damage( damage, this );

		if (!ch.isAlive())
			return;
		if (wet && level >= MAX_LEVEL)
			Buff.affect( ch, Paralysis.class, 1f );
		if (primary && pyromancyLevel() >= 2)
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
