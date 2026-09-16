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
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * Berserker: a straight-line charge that trails blood. Every enemy in the way is slammed
 * aside onto a neighbouring tile and takes half a weapon hit; one that cannot be moved
 * ends the charge in front of it. Fully trained the rush tears the hero out of anything
 * holding him, so he can charge while rooted.
 */
public class BloodRush extends SubSkill2 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Blood Rush";
		castText = "BLOOD!";
		image = 173;
		mana = 10;
		tier = 2;
	}

	private static final float SLAM = 0.5f;

	@Override
	public boolean toggleable(){ return false; }

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
			if (hero.rooted && level < MAX_LEVEL){
				GLog.w( Messages.get( this, "rooted" ) );
				return;
			}
			GameScene.selectCell( new Charge() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private int range(){
		return 2 + level;
	}

	private class Charge extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;
			final Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost() || !SkillInteractions.valid( target ) || target == hero.pos) return;
			if (hero.rooted && level < MAX_LEVEL){
				GLog.w( Messages.get( BloodRush.class, "rooted" ) );
				return;
			}

			//+3: the rush tears the hero free before he sets off
			if (level >= MAX_LEVEL){
				Buff.detach( hero, Cripple.class );
				Buff.detach( hero, Roots.class );
				Buff.detach( hero, Slow.class );
			}

			final int start = hero.pos;
			Ballistica traj = new Ballistica( start, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID );
			int steps = Math.min( traj.dist, range() );
			int dest = start;
			final ArrayList<Integer> trail = new ArrayList<>();
			for (int i = 1; i <= steps; i++){
				int c = traj.path.get( i );
				if (!Dungeon.level.passable[c] || Dungeon.level.pit[c]) break;
				Char ch = Actor.findChar( c );
				if (ch != null && !slamAside( hero, ch, traj.path.get( i - 1 ), c )) break;
				dest = c;
				trail.add( c );
			}
			if (dest == start){
				GLog.w( Messages.get( BloodRush.class, "no_path" ) );
				return;
			}

			hero.MP -= getManaCost();
			castTextYell();
			Invisibility.dispel();
			hero.busy();
			Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 1f, 1.1f );
			new Flare( 5, 16 ).color( 0xFF3333, true ).show( hero.sprite, 0.5f );
			for (int c : trail) CellEmitter.get( c ).burst( BloodParticle.FACTORY, 6 );

			final int land = dest;
			hero.sprite.jump( start, land, 2f, 0.06f * trail.size(), () -> {
				hero.move( land );
				Dungeon.level.occupyCell( hero );
				Dungeon.observe();
				GameScene.updateFog();
				WarriorImpactFX.show( land );
				hero.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 6 );
				Sample.INSTANCE.play( Assets.Sounds.TRAMPLE, 1f, 0.9f );
				Camera.main.shake( 2, 0.2f );
				hero.spendAndNext( TIME_TO_USE );
			} );
		}

		@Override
		public String prompt(){
			return Messages.get( BloodRush.class, "prompt" );
		}
	}

	//throws an enemy standing on the charge line onto a tile beside it; false if it holds its ground
	private boolean slamAside( Hero hero, Char ch, int from, int cell ){
		if (ch.alignment != Char.Alignment.ENEMY || ch.rooted
				|| ch.properties().contains( Char.Property.BOSS )
				|| ch.properties().contains( Char.Property.IMMOVABLE )) return false;
		int w = Dungeon.level.width();
		int dx = cell % w - from % w, dy = cell / w - from / w;
		int[][] sides = { { -dy, dx }, { dy, -dx } };
		for (int[] side : sides){
			int x = cell % w + side[0], y = cell / w + side[1];
			if (x < 0 || x >= w || y < 0 || y >= Dungeon.level.height()) continue;
			int to = x + y * w;
			if (!Dungeon.level.passable[to] || Dungeon.level.pit[to] || Actor.findChar( to ) != null
					|| (Char.hasProp( ch, Char.Property.LARGE ) && !Dungeon.level.openSpace[to])) continue;
			ch.move( to, false );
			if (ch.sprite != null) ch.sprite.place( to );
			WarriorImpactFX.show( to );
			ch.damage( Math.round( hero.damageRoll() * SLAM ), this );
			Wound.hit( ch );
			Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 0.9f );
			return true;
		}
		return false;
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
