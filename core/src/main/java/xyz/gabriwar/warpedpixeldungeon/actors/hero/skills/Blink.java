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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Rogue: while active the rogue moves as lightning. Walking into an enemy carries him straight
 * through it to the free tile beyond, striking it as he passes. If he lands beside another enemy
 * he has not struck yet, he blinks through that one too, chaining up to 3 / 4 / 5 different
 * enemies. The strike is a share of his weapon's damage, so it keeps pace with his gear.
 */
public class Blink extends ActiveSkill {

	{
		tag = "A5A";   //Ash Veil's slot: points already spent there carry over
		name = "Blink";
		castText = "Too slow";
		image = 54;
		tier = 4;
		level = 0;
	}

	//the mana one chain costs, however many enemies it strikes
	private static final int CHAIN_MANA = 4;

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** how many different enemies one chain can strike */
	public int maxTargets(){
		return 2 + level;
	}

	/** the share of a weapon roll each strike deals */
	public float damageShare(){
		return 0.4f + 0.2f * level;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals(Skill.AC_ACTIVATE) && hero.sprite != null){
			SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 1f, 1.6f );
			hero.sprite.emitter().burst( SparkParticle.FACTORY, 8 );
		}
	}

	@Override
	public boolean onHeroBump( Hero hero, Char enemy ){
		if (!active || level <= 0 || hero == null || enemy == null || hero.rooted || hero.MP < CHAIN_MANA
				|| !Dungeon.level.adjacent( hero.pos, enemy.pos )) return false;
		int first = landingBeyond( hero, hero.pos, enemy );
		if (first < 0) return false;

		hero.MP -= CHAIN_MANA;
		//the chain is planned at once and costs one turn; each strike lands with its own arc,
		//so the damage numbers pop in step with the lightning
		final ArrayList<Jump> jumps = new ArrayList<>();
		HashSet<Char> struck = new HashSet<>();

		Char target = enemy;
		int land = first;
		while (target != null){
			jumps.add( new Jump( hero.pos, land, target, rollDamage( hero ) ) );
			hero.move( land, false );
			Dungeon.level.occupyCell( hero );
			struck.add( target );

			target = null;
			if (struck.size() >= maxTargets()) break;
			//the next enemy beside where he landed, one he can pass straight through
			ArrayList<Char> options = new ArrayList<>();
			ArrayList<Integer> lands = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( hero.pos + n );
				if (ch == null || struck.contains( ch ) || !ch.isAlive()) continue;
				int beyond = landingBeyond( hero, hero.pos, ch );
				if (beyond < 0) continue;
				options.add( ch );
				lands.add( beyond );
			}
			if (!options.isEmpty()){
				int pick = Random.Int( options.size() );
				target = options.get( pick );
				land = lands.get( pick );
			}
		}

		Dungeon.observe();
		GameScene.updateFog();
		hero.spend( Actor.TICK );
		if (hero.sprite == null || hero.sprite.parent == null){
			//nothing to watch: every strike lands now
			for (Jump j : jumps) j.strike( this );
			if (hero.sprite != null) hero.sprite.place( hero.pos );
			hero.next();
			return true;
		}
		hero.busy();
		playJump( this, hero, jumps, 0 );
		return true;
	}

	//one leg of the chain: where the rogue left from and lands, whom he cuts through, how hard
	private static final class Jump {
		final int from, land;
		final Char target;
		final int damage;
		Jump( int from, int land, Char target, int damage ){
			this.from = from;
			this.land = land;
			this.target = target;
			this.damage = damage;
		}
		void strike( Blink source ){
			if (!target.isAlive()) return;
			target.damage( damage, source );
			if (target.sprite != null) target.sprite.flash();
		}
	}

	/** one arc at a time: the rogue's sprite follows each jump, the strike lands as its arc
	 *  passes through the enemy's middle, the next jump starts when the arc fades */
	private static void playJump( final Blink source, final Hero hero, final ArrayList<Jump> jumps, final int i ){
		if (i >= jumps.size() || hero.sprite == null || hero.sprite.parent == null){
			//the rest of the chain (if the scene went away mid-way) still lands
			for (int k = i; k < jumps.size(); k++) jumps.get( k ).strike( source );
			if (hero.sprite != null){
				hero.sprite.place( hero.pos );
				hero.sprite.emitter().burst( SparkParticle.FACTORY, 10 );
			}
			hero.next();
			return;
		}
		final Jump j = jumps.get( i );
		SpatialSound.play( Assets.Sounds.LIGHTNING, hero, 1f, 1.1f + 0.08f * i );
		//the arc runs body to body: out of the rogue's middle, through the enemy's, into where he lands
		hero.sprite.place( j.from );
		PointF start = hero.sprite.center();
		hero.sprite.place( j.land );
		PointF end = hero.sprite.center();
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		if (j.target.sprite != null){
			PointF middle = j.target.sprite.center();
			arcs.add( new Lightning.Arc( start, middle ) );
			arcs.add( new Lightning.Arc( middle, end ) );
			j.target.sprite.emitter().burst( SparkParticle.FACTORY, 6 );
		} else {
			arcs.add( new Lightning.Arc( start, end ) );
		}
		j.strike( source );
		hero.sprite.parent.add( new Lightning( arcs, () -> playJump( source, hero, jumps, i + 1 ) ) );
	}

	/** the free tile straight past an enemy seen from a cell, or -1 when he cannot pass it */
	private static int landingBeyond( Hero hero, int from, Char ch ){
		//bosses and rooted foes too: only the rogue moves, the enemy stays where it is
		if (ch.alignment != Char.Alignment.ENEMY
				|| !Dungeon.level.adjacent( from, ch.pos )) return -1;
		int cell = ch.pos + (ch.pos - from);
		if (!SkillInteractions.valid( cell ) || !Dungeon.level.adjacent( ch.pos, cell )) return -1;
		if (Dungeon.level.solid[cell] || !Dungeon.level.passable[cell] || Dungeon.level.pit[cell]
				|| Actor.findChar( cell ) != null
				|| (Char.hasProp( hero, Char.Property.LARGE ) && !Dungeon.level.openSpace[cell])) return -1;
		return cell;
	}

	private int rollDamage( Hero hero ){
		KindOfWeapon w = hero.belongings.weapon();
		int lo = w != null ? w.min() : 1;
		int hi = w != null ? w.max() : 4;
		return Math.max( 1, Math.round( Random.NormalIntRange( lo, hi ) * damageShare() ) );
	}
}
