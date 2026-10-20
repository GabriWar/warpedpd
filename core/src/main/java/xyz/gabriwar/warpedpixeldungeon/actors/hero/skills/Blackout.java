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
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;


import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;

public class Blackout extends Skill {

	{
		tag = "D3";
		name = "Blackout";
		castText = "Lights out";
		image = 55;
		tier = 3;
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
			Dungeon.hero.heroSkills.lastUsed = this;
			GameScene.selectCell( thrower );
		}
	}

	/** When the shade of ink flying to a fed shadow lands on it, s. */
	public static final float FED_AT = 0.45f;

	/** The dark feeding a shadow (mastery): a shade of ink flies to it from where the dark fell, and
	 *  where its HP rose the heal's red crosses and green light show on it as the shade lands (FED_AT
	 *  s on). Kit only: it draws nothing of the game's Random (FxRandom alone). */
	public static void fed( final Char shadow, int cell, boolean mended ){
		SkillSpectacleFX.fly( SkillSpectacleFX.SHADOW, cell, shadow.pos, 0, FED_AT );
		if (mended) Fx.post( () -> StaggerFX.after( FED_AT, () -> {
			if (shadow.isAlive()) SkillInteractions.mended( shadow );
		} ) );
	}

	private final CellSelector.Listener thrower = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ){
			if (target == null) return;

			Hero curUser = Dungeon.hero;
			if (level <= 0 || curUser.MP < getManaCost()) return;

			Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.PROJECTILE );
			int cell = shot.collisionPos;

			curUser.sprite.zap( cell );
            //at mastery the dark feeds your shadows
            if(level>=MAX_LEVEL)for(xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RogueShadow shadow:PhantomStrike.shadows()){
                if(Dungeon.level.distance(shadow.pos,cell)<=4+level){
                    int before=shadow.HP;
                    shadow.HP=Math.min(shadow.HT,shadow.HP+SkillInteractions.ofHealth(shadow.HT,0.02f+0.02f*level));
                    fed(shadow,cell,shadow.HP>before);
                }
            }
            xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.place(curUser,
                    xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.SMOKE,level,2+level,SkillInteractions.area(cell,2)).origin=cell;
            for(int c:SkillInteractions.area(cell,2)) GameScene.add(
                    xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob.seed(c,3+level,
                    xyz.gabriwar.warpedpixeldungeon.actors.blobs.SmokeScreen.class));
			curUser.MP -= getManaCost();
			castTextYell();
			SpatialSound.play( Assets.Sounds.PUFF, cell, 1f, 0.8f );

			CellEmitter.get( cell ).burst( SkillBursts.INK_DRIFT, 6 );
			SpatialSound.play( Assets.Sounds.MELD, cell, 0.7f, 0.6f );
			//lights out: a dark violet star where it lands and the darkness rolls out ring by ring, ink
			//sinking in over every cell it reaches (the smoke itself is the real SmokeScreen's)
			if (curUser.sprite.parent != null && Dungeon.level.heroFOV[cell]){
				PixelFlare.show( SkillVfx.cx( cell ), SkillVfx.cy( cell ), ShoutFX.DREAD[3], PixelFlare.M, 0.6f );
			}
			FxTimeline t = FxTimeline.start();
			for (int r = 0; r <= 2; r++){
				final int ring = r;
				t.at( 0.09f * r, () -> {
					for (int c : SkillInteractions.area( cell, ring )){
						if (Dungeon.level.distance( cell, c ) == ring && Dungeon.level.heroFOV[c]){
							CellEmitter.get( c ).burst( SkillBursts.DREAD, 3 );
						}
					}
				} );
			}
			for (Char ch : Actor.chars()){
				if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY) continue;
				if (Dungeon.level.distance( cell, ch.pos ) > 2 || !SkillInteractions.clear(cell,ch.pos)) continue;
				Buff.prolong( ch, Blindness.class, 4 + 2 * level );
				Buff.affect( ch, Terror.class, 2 + level ).object = curUser.id();
				if (Dungeon.level.heroFOV[ch.pos]){
					CellEmitter.get( ch.pos ).burst( SkillBursts.INK_DRIFT, 4 );
				}
			}

			curUser.spendAndNext( TIME_TO_USE );
			curUser.busy();
		}

		@Override
		public String prompt(){ return Messages.get(Blackout.class, "prompt"); }
	};

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
