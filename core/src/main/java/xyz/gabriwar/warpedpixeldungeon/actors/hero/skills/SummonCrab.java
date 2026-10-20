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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SummonCrab extends ActiveSkill2 {

	{
		name = "Summon Crab";
		castText = "Pinch them!";
		image = 42;
		mana = 3;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_SUMMON);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_SUMMON)){

			if (SummonedPet.activeCount() >= 3 + hero.heroSkills.allSummonLimit()){
				GLog.w( "You cannot control more summons." );
				return;
			}

			ArrayList<Integer> candidates = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS4){
				int c = hero.pos + n;
				if (c < 0 || c >= Dungeon.level.length())
					continue;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
					candidates.add(c);
				}
			}
			int newPos = candidates.size() > 0 ? Random.element(candidates) : -1;
			if (newPos != -1){
				SummonedPet pet = new SummonedPet(SummonedPet.PET_TYPES.CRAB);
				pet.spawn(level);
				pet.pos = newPos;
				GameScene.add(pet);
				Actor.addDelayed(new Pushing(pet, hero.pos, newPos), -1);
				pet.sprite.alpha(0);
				pet.sprite.parent.add(new AlphaTweener(pet.sprite, 1, 0.15f));
				//it digs itself out where it lands: a ring of dust and sand thrown aside (a splash on water)
				arrival( newPos );

				hero.MP -= getManaCost();
				castTextYell();
				SpatialSound.play( Assets.Sounds.PUFF, pet, 1f, 0.9f );
				hero.spend( TIME_TO_USE );
				hero.busy();
				hero.sprite.operate( hero.pos );
			}
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	/** Sand grit a crab throws aside as it digs out on dry ground. */
	public static final int SAND = 4;

	//the ground's answer to the crab's arrival (SkillFX.landing: its dust ring, or its splash on
	//water) and, on dry ground, SAND grains of sand kicked out to either side. Any thread
	public static void arrival( final int cell ){
		SkillFX.landing( cell );
		if (!SkillVfx.seen( cell )) return;
		Fx.post( () -> {
			if (!SkillVfx.valid( cell )) return;
			float x = SkillVfx.cx( cell ), y = SkillVfx.cy( cell ) + 5;
			if (WaterFX.onWater( x, y )) return;
			for (int i = 0; i < SAND; i++){
				float side = i % 2 == 0 ? -1f : 1f;
				FxParticle p = SkillVfx.matter( x + side * FxRandom.Float( 1f, 4f ), y - 1, FxBudget.P3 );
				if (p != null) p.look( FxRandom.chance( 0.5f ) ? FxFrames.PEBBLE_2 : FxFrames.EMBER_1 )
						.paint( Element.EARTH.ramp.stop( FxRandom.Int( 2 ) ) )
						.vel( side * FxRandom.Float( 20f, 40f ), FxRandom.Float( -45f, -25f ) ).gravity( 220f )
						.ground( y + 2, FxParticle.SETTLE, 0 ).life( 0.6f ).alpha( 1f, 0, 0.25f );
			}
		} );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
