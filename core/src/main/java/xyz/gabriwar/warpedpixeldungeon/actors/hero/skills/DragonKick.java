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

import com.watabou.noosa.Camera;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * Monk: fly at an enemy and kick it across the room. The body bowls through every
 * enemy in its path, and one that meets a wall is slammed for the kick again.
 */
public class DragonKick extends SubSkill3 {

	{
		name = "Dragon Kick";
		castText = "HWAAA!";
		image = 172;
		mana = 12;
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
		if (!action.equals(Skill.AC_CAST) || level <= 0 || !canPayMana( hero, getManaCost() )) return;
		if (hero.rooted){
			GLog.w( Messages.get( this, "rooted" ) );
			return;
		}
		GameScene.selectCell( new CellSelector.Listener(){
			@Override
			public void onSelect( Integer target ){
				if (target != null) fly( hero, target );
			}

			@Override
			public String prompt(){
				return Messages.get( DragonKick.class, "prompt" );
			}
		} );
	}

	//how far the monk flies to reach the enemy, and how far the enemy is launched: 2 / 3 / 4
	private int reach(){
		return 1 + level;
	}

	private int minDamage(){ return 3 + 2 * level; }
	private int maxDamage(){ return 8 + 4 * level; }

	private void fly( Hero hero, int target ){
		if (level <= 0 || !canPayMana( hero, getManaCost() ) || hero.rooted) return;
		Char victim = Actor.findChar( target );
		if (victim == null || victim.alignment != Char.Alignment.ENEMY || !victim.isAlive() || !Dungeon.level.heroFOV[target]){
			GLog.w( Messages.get( this, "no_target" ) );
			return;
		}
		if (Dungeon.level.distance( hero.pos, target ) > reach()){
			GLog.w( Messages.get( this, "too_far" ) );
			return;
		}

		int landing = hero.pos;
		if (!Dungeon.level.adjacent( hero.pos, target )){
			//the flight is a straight line: it lands on the last tile before the enemy
			Ballistica traj = new Ballistica( hero.pos, target, Ballistica.PROJECTILE );
			if (traj.collisionPos.intValue() != target){
				GLog.w( Messages.get( this, "no_path" ) );
				return;
			}
			landing = traj.path.get( traj.dist - 1 );
			if (!Dungeon.level.passable[landing] || Dungeon.level.pit[landing] || Actor.findChar( landing ) != null){
				GLog.w( Messages.get( this, "no_room" ) );
				return;
			}
		}

		payMana( hero, getManaCost() );
		castTextYell();
		Invisibility.dispel();
		Dungeon.hero.heroSkills.lastUsed = this;

		final int land = landing;
		if (land == hero.pos){
			kick( hero, victim );
			return;
		}
		hero.busy();
		SpatialSound.play( Assets.Sounds.MISS, hero, 1f, 0.7f );
		hero.sprite.jump( hero.pos, land, () -> {
			hero.move( land );
			Dungeon.level.occupyCell( hero );
			Dungeon.observe();
			GameScene.updateFog();
			SkillFX.land( land );
			kick( hero, victim );
		} );
	}

	private void kick( Hero hero, Char victim ){
		if (!victim.isAlive() || !Dungeon.level.adjacent( hero.pos, victim.pos )){
			hero.spendAndNext( TIME_TO_USE );
			return;
		}
		hero.sprite.attack( victim.pos );
		victim.damage( Random.NormalIntRange( minDamage(), maxDamage() ), this );
		Wound.hit( victim );
		SpatialSound.play( Assets.Sounds.HIT_STRONG, victim, 1f, 0.8f );
		Camera.main.shake( 2, 0.3f );
		new Flare( 6, 26 ).color( 0xFFAA44, true ).show( hero.sprite, 0.5f );

		int w = Dungeon.level.width();
		int dx = Integer.signum( victim.pos % w - hero.pos % w ), dy = Integer.signum( victim.pos / w - hero.pos / w );
		boolean movable = victim.isAlive() && !victim.rooted
				&& !Char.hasProp( victim, Char.Property.BOSS )
				&& !Char.hasProp( victim, Char.Property.IMMOVABLE );
		boolean slammed = false;
		for (int step = 0; movable && step < reach() && victim.isAlive(); step++){
			int x = victim.pos % w + dx, y = victim.pos / w + dy;
			if (x < 0 || x >= w || y < 0 || y >= Dungeon.level.height()){
				slammed = true;
				break;
			}
			int c = x + y * w;
			if (Dungeon.level.solid[c] || (!Dungeon.level.passable[c] && !Dungeon.level.pit[c])){
				slammed = true;
				break;
			}
			if (Dungeon.level.pit[c]) break;
			Char other = Actor.findChar( c );
			if (other != null){
				if (other.alignment != Char.Alignment.ENEMY) break;
				//bowled over: hurt and knocked a tile aside, and the flight goes on
				other.damage( Random.NormalIntRange( minDamage(), maxDamage() ), this );
				Wound.hit( other );
				if (other.isAlive()) SkillInteractions.push( other, victim.pos, 1, 0 );
				if (Actor.findChar( c ) != null) break;
			}
			victim.move( c, false );
			if (victim.sprite != null) victim.sprite.place( c );
			Dungeon.level.occupyCell( victim );
			CellEmitter.bottom( c ).burst( Speck.factory( Speck.DUST ), 3 );
		}

		//a wall stops the body cold: the kick lands a second time and leaves it reeling
		if (slammed && victim.isAlive()){
			victim.damage( Random.NormalIntRange( minDamage(), maxDamage() ), this );
			Wound.hit( victim );
			if (victim.isAlive()){
				Buff.prolong( victim, Daze.class, 2f );
				if (victim.sprite != null){
					victim.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "slam" ) );
					victim.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
				}
			}
			CellEmitter.center( victim.pos ).burst( Speck.factory( Speck.ROCK ), 5 );
			SpatialSound.play( Assets.Sounds.ROCKS, victim, 0.8f, 1.2f );
			Camera.main.shake( 3, 0.3f );
		}

		//+3: where it comes down, the enemies around it are left dazed
		if (level >= MAX_LEVEL && victim.isAlive()){
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( victim.pos + n );
				if (ch == null || ch == hero || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
				Buff.prolong( ch, Daze.class, 2f );
				if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
			}
		}
		hero.spendAndNext( TIME_TO_USE );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
