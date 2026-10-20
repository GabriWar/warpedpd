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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;


import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AvatarOfLightHalo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

import java.util.ArrayList;

public class AvatarOfLight extends Skill {

	{
		name = "Avatar of Light";
		castText = "Be my light!";
		tag = "A4";
		image = 148;
		tier = 4;
		mana = 15;
		level = 0;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && canPayMana( hero, getManaCost() ))
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && canPayMana( hero, getManaCost() )){
			int duration = 4 + 3 * level;

			//blessed, and wrapped in a halo that sears and blinds each enemy the first time it comes inside
			Buff.prolong( hero, Bless.class, duration );
			Buff.affect( hero, AvatarOfLightHalo.class ).set( level, duration );
			if (hero.sprite != null){
				//holy motes rising off the cleric for as long as the old stream of light ran (its start
				//kept), never a shield's bubble or a great fan of rays: the column, the pulses and the
				//halo (AvatarOfLightHalo) carry it
				hero.sprite.emitter().start( SkillBursts.HOLY_MOTE, 0.3f, 12 );
				hero.sprite.emitter().burst( SkillBursts.HOLY_MOTE, 8 );
				Fx.shake( 2, 0.5f );
				//the light rises out of the ground round the cleric, a golden halo beats out, motes rise in two waves
				PillarRiseFX.show( hero.pos, 0xF1D99C, () -> SpatialSound.play( Assets.Sounds.RAY, hero, 0.8f, 1.2f ) );
				PulseRingFX.around( hero.sprite, 0xF1D99C, 18, 0.6f );
				FxTimeline.start()
						.at( 0.25f, () -> { hero.sprite.emitter().burst( SkillBursts.HOLY_MOTE, 5 ); PulseRingFX.around( hero.sprite, 0xFFF4D0, 12, 0.45f ); } )
						.at( 0.6f, () -> hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 ) );
			}

			payMana( hero, getManaCost() );
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.MASTERY, 1f, 1.0f );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
