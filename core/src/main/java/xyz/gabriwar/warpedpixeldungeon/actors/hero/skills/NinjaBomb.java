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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;


import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;

public class NinjaBomb extends ActiveSkill2 {
    @Override public boolean toggleable(){return false;}

	{
		name = "Ninja Bomb";
		castText = "Into the smoke!";
		tier = 2;
		image = 65;
		mana = 8;
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
		if (action.equals(Skill.AC_CAST) && hero.MP >= getManaCost()){
			GameScene.selectCell( thrower );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private static CellSelector.Listener thrower = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ){
			if (target == null) return;
			final Hero curUser = Dungeon.hero;
			final Skill skill = curUser.heroSkills.get( NinjaBomb.class );
			if (skill.level <= 0 || curUser.MP < skill.getManaCost()) return;
			Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.PROJECTILE );
			final int cell = shot.collisionPos;
			curUser.MP -= skill.getManaCost();
			skill.castTextYell();
			Dungeon.hero.heroSkills.lastUsed = skill;
			Invisibility.dispel();
			//the bomb is seen flying, and the gas goes off where it lands
			curUser.busy();
			curUser.sprite.zap( cell );
			Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 1.3f );
			SkillFX.streak( curUser.sprite, cell, new Bomb(), () -> {
				Sample.INSTANCE.play( Assets.Sounds.PUFF, 1f, 1.0f );
                for(int c:SkillInteractions.area(cell,skill.level)) GameScene.add(
                        xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob.seed(c,4+skill.level,
                        xyz.gabriwar.warpedpixeldungeon.actors.blobs.SmokeScreen.class));
                //at mastery the smoke leaves a clone behind to draw enemies off
                if(skill.level>=MAX_LEVEL) for(int c:SkillInteractions.area(cell,1)) if(Dungeon.level.passable[c]&&xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar(c)==null){
                    xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkillDecoy decoy=new xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkillDecoy();
                    decoy.pos=c;decoy.rank=skill.level;decoy.left=2+skill.level;decoy.blinding=false;GameScene.add(decoy);decoy.sprite.alpha(.55f);
                    SkillSpectacleFX.show(SkillSpectacleFX.SHADOW,c);break;
                }
				//the plume: a thick column that keeps pouring for a second and drifts, and the smoke
				//rolling outward one ring of tiles after another, the sleepers nodding off as it reaches them
				Camera.main.shake( 1, 0.2f );
				CellEmitter.get( cell ).burst( SmokeParticle.FACTORY, 8 );
				CellEmitter.get( cell ).start( SmokeParticle.FACTORY, 0.06f, 20 );
				FxTimeline t = FxTimeline.start();
				t.at( 0.25f, () -> Sample.INSTANCE.play( Assets.Sounds.PUFF, 0.7f, 0.8f ) );
				for (int r = 1; r <= skill.level; r++){
					final int ring = r;
					t.at( 0.09f * r, () -> {
						for (int c : SkillInteractions.area( cell, ring )){
							if (Dungeon.level.distance( cell, c ) == ring && Dungeon.level.heroFOV[c]){
								CellEmitter.get( c ).burst( Speck.factory( Speck.SMOKE ), 3 );
							}
						}
					} );
				}
				for (int c : SkillInteractions.area(cell, skill.level)) {
					Char enemy = Actor.findChar(c);
					if (enemy instanceof Mob && enemy.alignment == Char.Alignment.ENEMY
							&& enemy.isAlive() && !enemy.properties().contains(Char.Property.BOSS)) {
						Buff.affect(enemy, MagicalSleep.class);
						final Char sleeper = enemy;
						t.at( 0.12f + 0.09f * Dungeon.level.distance( cell, c ), () -> {
							if (sleeper.sprite != null && sleeper.isAlive() && Dungeon.level.heroFOV[sleeper.pos]){
								sleeper.sprite.showStatus( CharSprite.NEUTRAL, "Zzz" );
							}
						} );
					}
				}
				curUser.spendAndNext( TIME_TO_USE );
			} );
		}
		@Override
		public String prompt(){
			return Messages.get(NinjaBomb.class, "prompt");
		}
	};

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
