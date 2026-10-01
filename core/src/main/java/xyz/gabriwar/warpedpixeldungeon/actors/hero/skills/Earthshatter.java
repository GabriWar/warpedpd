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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;


import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import com.watabou.noosa.Camera;

import java.util.ArrayList;
import java.util.HashSet;

public class Earthshatter extends Skill {

	{
		tag = "A4";
		name = "Earthshatter";
		castText = "Break!";
		image = 7;
		tier = 4;
		mana = 10;
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
			final int center = hero.pos;
            SkillSequence.start(hero,SkillSequence.QUAKE,level,center,3+2*level,1+level,java.util.Collections.emptyList());
			//the blow itself lands now; only the cracks take their time crossing the floor
			final HashSet<Integer> struck = new HashSet<>();
			for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )){
				if (mob.alignment == Char.Alignment.ENEMY
						&& mob.isAlive()
						&& Dungeon.level.heroFOV[mob.pos]
						&& Dungeon.level.distance( center, mob.pos ) <= 2){
					struck.add( mob.pos );
					Buff.prolong( mob, Roots.class, 2 + level );
					mob.damage( 6 + 4 * level, this );
				}
			}
			hero.MP -= getManaCost();
			castTextYell();
			Dungeon.hero.heroSkills.lastUsed = this;
			Invisibility.dispel();
			hero.spend( TIME_TO_USE );
			hero.busy();
			if (hero.sprite == null || hero.sprite.parent == null){
				hero.next();
				return;
			}
			//a stamp: the hero hops on the spot and the shockwave leaves his heel as he comes down
			hero.sprite.jump( center, center, 6f, 0.18f, () -> {
				quake( center, struck );
				hero.next();
			} );
		}
	}

	//the cracked earth runs outward one ring at a time: chips of stone and dust on every cell, the
	//impact mark and a wound on every enemy caught, the rumble dropping in pitch as it gets further
	private static void quake( final int center, final HashSet<Integer> struck ){
		WarriorImpactFX.show( center, true );
		CellEmitter.bottom( center ).burst( Speck.factory( Speck.DUST ), 6 );
		Camera.main.shake( 3, 0.7f );
		Sample.INSTANCE.play( Assets.Sounds.ROCKS, 1f, 0.9f );
		StaggerFX.ring( center, 2, 0.12f, ( c, r ) -> {
			WarriorImpactFX.show( c );
			CellEmitter.bottom( c ).burst( EarthParticle.FACTORY, 2 );
			CellEmitter.bottom( c ).burst( Speck.factory( Speck.DUST ), 2 );
			if (struck.contains( c )){
				CellEmitter.get( c ).burst( Speck.factory( Speck.ROCK ), 3 );
				Wound.hit( c );
				Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( c );
				if (ch != null && ch.sprite != null) ch.sprite.flash();
			}
		} );
		StaggerFX.after( 0.12f, () -> Sample.INSTANCE.play( Assets.Sounds.ROCKS_LIGHT, 0.9f, 0.8f ) );
		StaggerFX.after( 0.24f, () -> Sample.INSTANCE.play( Assets.Sounds.ROCKS_LIGHT, 0.9f, 0.7f ) );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
