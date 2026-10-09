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
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class StormCall extends Skill {

	private static final int ARC_RANGE = 4;

	{
		tag = "D3";
		name = "Storm Call";
		castText = "The sky answers!";
		tier = 3;
		image = 47;
		mana = 9;
	}

	@Override
	public boolean rangedSource(){ return true; }

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
			GameScene.selectCell( caller );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private final CellSelector.Listener caller = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ){
			if (target == null)
				return;

			Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost())
				return;

			//the sky strikes the chosen tile itself: no line of fire, but it must be seen and hold an enemy
			final int cell = target;
			Char primary = SkillInteractions.valid( cell ) && Dungeon.level.heroFOV[cell] ? Actor.findChar( cell ) : null;
			if (primary == null || primary == hero || primary.alignment != Char.Alignment.ENEMY){
				GLog.w( Messages.get(StormCall.this, "no_target") );
				return;
			}

			//the whole storm is decided now; it is only shown one arc at a time afterwards
			final ArrayList<int[]> arcs = new ArrayList<>();
			arcs.add( new int[]{ -1, cell } );
			strike( primary, roll() );

			//each arc hops from the last victim to the nearest enemy in sight and in reach
			ArrayList<Char> struck = new ArrayList<>();
			struck.add( primary );
			int from = cell;
			for (int i = 0; i < level; i++){
				Mob next = null;
				for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
					if (struck.contains( mob ) || mob.alignment != Char.Alignment.ENEMY || !mob.isAlive()
							|| !Dungeon.level.heroFOV[mob.pos] || Dungeon.level.distance( from, mob.pos ) > ARC_RANGE
							|| !SkillInteractions.clear( from, mob.pos )) continue;
					if (next == null || Dungeon.level.distance( from, mob.pos ) < Dungeon.level.distance( from, next.pos ))
						next = mob;
				}
				if (next == null) break;
				arcs.add( new int[]{ from, next.pos } );
				strike( next, Math.round( roll() * 0.6f ) );
				struck.add( next );
				from = next.pos;
			}

			hero.MP -= getManaCost();
			castTextYell();
			Invisibility.dispel();
			hero.spend( TIME_TO_USE );
			hero.busy();
			if (hero.sprite == null || hero.sprite.parent == null){
				hero.next();
				return;
			}
			//arms up towards the mark; the zap pose asks nothing of the hero when it ends
			hero.sprite.zap( cell );
			playArc( hero, arcs, 0 );
		}

		@Override
		public String prompt(){
			return Messages.get( StormCall.class, "prompt" );
		}
	};

	private int roll(){
		return Random.NormalIntRange( 4 + 2 * level, 8 + 4 * level );
	}

	//one arc at a time: the first comes down out of the sky with a jolt of the screen, the rest leap
	//body to body, each crack pitched above the last; the mage is free again when the last one fades
	private static void playArc( final Hero hero, final ArrayList<int[]> arcs, final int i ){
		if (i >= arcs.size() || hero.sprite == null || hero.sprite.parent == null){
			hero.next();
			return;
		}
		final int[] a = arcs.get( i );
		SpatialSound.play( Assets.Sounds.LIGHTNING, a[1], 1f, 0.9f + 0.1f * i );
		CellEmitter.center( a[1] ).burst( SparkParticle.FACTORY, 8 );
		Char ch = Actor.findChar( a[1] );
		if (ch != null && ch.sprite != null) ch.sprite.flash();
		Callback then = () -> playArc( hero, arcs, i + 1 );
		if (a[0] < 0){
			Camera.main.shake( 2, 0.3f );
			PointF foot = DungeonTilemap.raisedTileCenterToWorld( a[1] );
			hero.sprite.parent.add( new Lightning( new PointF( foot.x, foot.y - DungeonTilemap.SIZE * 7 ), a[1], then ) );
		} else {
			hero.sprite.parent.add( new Lightning( a[0], a[1], then ) );
		}
	}

	private void strike( Char ch, int damage ){
		int before = SkillInteractions.beforeMagicHit( ch, this );
		ch.damage( damage, this );
		SkillInteractions.afterMagicHit( ch, before, this );

		//at mastery the storm stuns everything it strikes
		if (ch.isAlive() && level >= MAX_LEVEL)
			SkillInteractions.affectAfterHit( ch, Paralysis.class, 1f );
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
