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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

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
                    shadow.HP=Math.min(shadow.HT,shadow.HP+SkillInteractions.ofHealth(shadow.HT,0.02f+0.02f*level));
                    SkillSpectacleFX.fly(SkillSpectacleFX.SHADOW,cell,shadow.pos,0,.45f);
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

			CellEmitter.get( cell ).burst( Speck.factory( Speck.SMOKE ), 6 );
			SpatialSound.play( Assets.Sounds.MELD, cell, 0.7f, 0.6f );
			//lights out: a dark flare blooms where it lands and the darkness rolls out ring by ring
			if (curUser.sprite.parent != null && Dungeon.level.heroFOV[cell]){
				new Flare( 8, 36 ).color( 0x1A0A2A, false ).show( curUser.sprite.parent, DungeonTilemap.tileCenterToWorld( cell ), 0.6f );
			}
			FxTimeline t = FxTimeline.start();
			for (int r = 0; r <= 2; r++){
				final int ring = r;
				t.at( 0.09f * r, () -> {
					for (int c : SkillInteractions.area( cell, ring )){
						if (Dungeon.level.distance( cell, c ) == ring && Dungeon.level.heroFOV[c]){
							CellEmitter.get( c ).burst( ShadowParticle.MISSILE, 3 );
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
					CellEmitter.get( ch.pos ).burst( Speck.factory( Speck.SMOKE ), 4 );
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
