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
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * A straight ground dash in a streak of light. The paladin runs along the line toward
 * the chosen tile and stops at the first creature in the way; an enemy there is
 * slammed and stunned. At mastery the light blinds every enemy beside the path.
 */
public class HolyCharge extends SubSkill2 {

	public static final float STUN = 2f;

	{
		name = "Holy Charge";
		castText = "For the light!";
		image = 180;
		mana = 7;
		tier = 2;
	}

	/** 3/5/7 tiles of charge */
	public int range(){ return 1 + 2 * level; }

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
		hero.heroSkills.lastUsed = this;
		GameScene.selectCell( new CellSelector.Listener(){
			@Override
			public void onSelect( Integer cell ){
				if (cell != null) charge( hero, cell );
			}
			@Override
			public String prompt(){
				return Messages.get( HolyCharge.class, "prompt" );
			}
		} );
	}

	private void charge( Hero hero, int target ){
		if (level <= 0 || !canPayMana( hero, getManaCost() ) || !hero.isAlive()
				|| !SkillInteractions.valid( target ) || target == hero.pos) return;
		if (hero.rooted){
			GLog.w( Messages.get( HolyCharge.class, "no_path" ) );
			return;
		}

		//walk the line: open ground carries the charge, the first creature ends it
		Ballistica line = new Ballistica( hero.pos, target, Ballistica.PROJECTILE );
		ArrayList<Integer> path = new ArrayList<>();
		int land = hero.pos;
		Char struck = null;
		for (int i = 1; i <= line.dist && i <= range() + 1; i++){
			int c = line.path.get( i );
			Char ch = Actor.findChar( c );
			if (ch != null){
				if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive()) struck = ch;
				break;
			}
			if (i > range() || !Dungeon.level.passable[c] || Dungeon.level.pit[c] || Dungeon.level.avoid[c]
					|| (Char.hasProp( hero, Char.Property.LARGE ) && !Dungeon.level.openSpace[c])) break;
			land = c;
			path.add( c );
		}
		if (land == hero.pos && struck == null){
			GLog.w( Messages.get( HolyCharge.class, "no_path" ) );
			return;
		}

		payMana( hero, getManaCost() );
		hero.heroSkills.lastUsed = this;
		castTextYell();
		Invisibility.dispel();
		hero.busy();
		SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 1f, 1.3f );

		final int from = hero.pos;
		final int dest = land;
		final Char victim = struck;
		if (dest == from){
			impact( hero, from, path, victim );
			return;
		}

		//a flat, fast slide along the ground under a ray of light, not a leap
		if (hero.sprite.parent != null){
			hero.sprite.parent.add( new Beam.SunRay( hero.sprite.center(), DungeonTilemap.raisedTileCenterToWorld( dest ) ) );
		}
		for (int c : path){
			CellEmitter.center( c ).burst( Speck.factory( Speck.LIGHT ), 3 );
		}
		hero.sprite.jump( from, dest, 0f, 0.04f * Dungeon.level.distance( from, dest ), () -> {
			if (hero.isAlive() && Actor.findChar( dest ) == null){
				hero.move( dest, false );
				Dungeon.observe();
				GameScene.updateFog();
			}
			hero.sprite.place( hero.pos );
			impact( hero, from, path, victim );
		} );
	}

	private void impact( Hero hero, int from, ArrayList<Integer> path, Char victim ){
		if (victim != null && victim.isAlive() && Dungeon.level.adjacent( hero.pos, victim.pos )){
			int drive = Math.max( 0, range() - Dungeon.level.distance( from, hero.pos ) );
			boolean slammed = true;
			if (drive > 0 && !victim.properties().contains( Char.Property.BOSS )){
				int before = victim.pos;
				SkillInteractions.push( victim, hero.pos, drive, 0 );
				slammed = Dungeon.level.distance( before, victim.pos ) < drive;
			}
			if (slammed && victim.isAlive() && !victim.properties().contains( Char.Property.BOSS )){
				SkillInteractions.affectAfterHit( victim, Paralysis.class, STUN );
			}
			SkillFX.flash( victim );
			if (victim.sprite != null){
				new Flare( 8, 28 ).color( 0xFFEE88, true ).show( victim.sprite, 0.6f );
				victim.sprite.emitter().burst( Speck.factory( Speck.STAR ), 8 );
			}
			SpatialSound.play( Assets.Sounds.HIT_STRONG, victim, 1f, 0.9f );
			Camera.main.shake( 3, 0.3f );
		} else {
			CellEmitter.bottom( hero.pos ).burst( Speck.factory( Speck.DUST ), 6 );
			SpatialSound.play( Assets.Sounds.TRAMPLE, hero, 1f, 1f );
		}

		//at mastery the streak of light blinds whatever stands beside it
		if (level >= MAX_LEVEL){
			ArrayList<Integer> lit = new ArrayList<>( path );
			lit.add( from );
			HashSet<Char> blinded = new HashSet<>();
			for (int c : lit){
				for (int n : PathFinder.NEIGHBOURS9){
					if (!SkillInteractions.valid( c + n )) continue;
					Char ch = Actor.findChar( c + n );
					if (ch != null && ch != hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()) blinded.add( ch );
				}
			}
			for (Char ch : blinded){
				Buff.prolong( ch, Blindness.class, 2f );
				if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 5 );
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
