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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;


import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class Fleche extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	{
		tag = "D2";
		name = "Fleche";
		castText = "En garde!";
		image = 142;
		tier = 2;
		mana = 6;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			GameScene.selectCell( new CellSelector.Listener(){
				@Override
				public void onSelect( Integer target ){
					if (target != null){
						charge( hero, target );
					}
				}

				@Override
				public String prompt(){
					return Messages.get(Fleche.class, "prompt");
				}
			} );
		}
	}

	private void charge( Hero hero, int target ){
		//the selector stays live across other casts, so re-check before spending
		if (level <= 0 || hero.MP < getManaCost()) return;
		if (hero.rooted){
			GLog.w( Messages.get(this, "rooted") );
			return;
		}
		final int start = hero.pos;

		Char ch = Actor.findChar( target );
		if (ch == null || ch.alignment != Char.Alignment.ENEMY
				|| !Dungeon.level.heroFOV[target]
				|| Dungeon.level.distance( hero.pos, target ) > 3 + level){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		int landing = hero.pos;
		if (!Dungeon.level.adjacent( hero.pos, ch.pos )){
			landing = -1;

			//first choice: the last cell of the straight line before the target
			Ballistica traj = new Ballistica( hero.pos, ch.pos, Ballistica.STOP_TARGET );
			if (traj.dist >= 1){
				int c = traj.path.get( traj.dist - 1 );
				if (Dungeon.level.passable[c] && Actor.findChar( c ) == null){
					landing = c;
				}
			}

			//otherwise any free cell beside the target, nearest to where you stand
			if (landing == -1){
				for (int n : PathFinder.NEIGHBOURS8){
					int c = ch.pos + n;
					if (c < 0 || c >= Dungeon.level.length()) continue;
					if (!Dungeon.level.passable[c] || Actor.findChar( c ) != null) continue;
					if (landing == -1
							|| Dungeon.level.distance( hero.pos, c ) < Dungeon.level.distance( hero.pos, landing )){
						landing = c;
					}
				}
			}

			if (landing == -1){
				GLog.w( Messages.get(this, "no_room") );
				return;
			}
		}

		int strikeCell = ch.pos;
		if (landing != hero.pos){
			//the run is drawn as dust along the line before the blink lands the hero
			Ballistica run = new Ballistica( hero.pos, landing, Ballistica.STOP_TARGET );
			for (int c : run.subPath( 0, run.dist )){
				if (Dungeon.level.heroFOV[c]) CellEmitter.bottom( c ).burst( Speck.factory( Speck.DUST ), 3 );
			}
			Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 0.8f );
			//the run itself: a thin white streak ahead of her, a puff where her feet land
			StreakFX.show( hero.pos, landing, 0xFFFFFF, 0.5f, 0.3f );
			ScrollOfTeleportation.appear( hero, landing );
			CellEmitter.bottom( landing ).burst( Speck.factory( Speck.DUST ), 5 );
			Dungeon.observe();
			GameScene.updateFog();
		}
		//a raw roll of what is in hand. Deliberately not Hero.damageRoll(): that path runs the
		//skill tree's damage modifiers, so an active Lunge would spend its mana and yell its cast
		//text from inside this charge, and an active Whirling Flurry would quietly scale it down
		KindOfWeapon wep = hero.belongings.weapon();
		int roll = wep != null ? wep.damageRoll( hero ) : RingOfForce.damageRoll( hero );
		ch.damage( Math.round( roll * 1.3f ), this );
		Wound.hit( ch );
		if (ch.sprite != null) ch.sprite.flash();
		ArcSpinFX.at( ch.pos, 0xFFFFFF, 10, 0.25f, 200, 900, 0.22f );

		//hit and run: back to the tile the run began from, if it is still free
		if (hero.pos != start && Actor.findChar( start ) == null){
			StreakFX.show( hero.pos, start, 0xFFFFFF, 0.4f, 0.3f );
			ScrollOfTeleportation.appear( hero, start );
			CellEmitter.bottom( start ).burst( Speck.factory( Speck.DUST ), 4 );
			Dungeon.observe();
			GameScene.updateFog();
		}

		//level 3: a finishing Fleche is free
		if (level >= MAX_LEVEL && !ch.isAlive()){
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get(this, "free") );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
		} else {
			hero.MP -= getManaCost();
		}
		castTextYell();
		Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH, 1f, 1.1f );
		Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		Dungeon.hero.heroSkills.lastUsed = this;
		hero.spend( TIME_TO_USE );
		hero.busy();
		hero.sprite.operate( strikeCell );
		Invisibility.dispel();
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
