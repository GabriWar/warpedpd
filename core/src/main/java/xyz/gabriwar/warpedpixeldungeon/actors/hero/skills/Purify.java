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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;


import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;

import java.util.ArrayList;

public class Purify extends SubSkill3 {

	/** How long the cleansing ray to an ally holds, s. */
	public static final float CLEANSE = 0.3f;

	{
		name = "Purify";
		castText = "Begone!";
		image = 174;
		//flat cost: the cleanse is the same at every rank, so a rank must not price it up
		mana = 8;
		tier = 3;
	}

	@Override
	public boolean toggleable(){ return false; }

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
			Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison.class );
			Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple.class );
			Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness.class );
			Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness.class );
			Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding.class );
			Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable.class );
			//level 3 also lifts fire, terror and slowness, and leaves a short blessing
			if (level >= MAX_LEVEL){
				Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning.class );
				Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror.class );
				Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow.class );
				Buff.prolong( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless.class, 2f );
			}
			//from level 2 the light also burns away hex, degradation and ooze, and reaches every ally in sight
			if (level >= 2){
				Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hex.class );
				Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Degrade.class );
				Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze.class );
				for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob : Dungeon.level.mobs.toArray( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0] )){
					Char ally = mob;
					if (ally.alignment != Char.Alignment.ALLY || !Dungeon.level.heroFOV[ally.pos]) continue;
					Buff.detach( ally, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison.class );
					Buff.detach( ally, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple.class );
					Buff.detach( ally, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness.class );
					Buff.detach( ally, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness.class );
					Buff.detach( ally, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding.class );
					Buff.detach( ally, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable.class );
					if (ally.sprite != null){
						ally.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
						//a short cleansing ray of holy light from the cleric to the ally
						SkillRay.show( hero.sprite, ally.sprite, SkillRay.HOLY, CLEANSE );
					}
				}
			}
			payMana( hero, getManaCost() );
			castTextYell();
			SpatialSound.play( Assets.Sounds.CHARMS, hero, 1f, 1.4f );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			Dungeon.hero.sprite.emitter().burst( SkillBursts.HOLY_MOTE, 5 );
			splashDraw();
			//the purge: a pulse of holy light's palest gold (never a white sonar ping), the gold one after it
			PulseRingFX.around( hero.sprite, SkillPalette.HOLY.stop( 0 ), 14, 0.45f );
			FxTimeline.start().at( 0.1f, () -> PulseRingFX.around( hero.sprite, 0xF1D99C, 16, 0.4f ) );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** The one draw of the game's Random the white splash round the cleric always made
	 *  (Splash.around's, when the scene has its effects): the purge is a pulse of light, never a splash. */
	public static void splashDraw(){
		if (GameScene.layer( GameScene.Layer.EFFECTS ) != null) Random.Float();
	}
}
