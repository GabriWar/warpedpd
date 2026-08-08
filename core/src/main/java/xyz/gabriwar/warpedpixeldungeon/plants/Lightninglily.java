/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.plants;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.LightninglillyPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.BArray;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Lightninglily extends Plant {

	{
		image = 58;
		livingPlantImage = 48;
		seedClass = Seed.class;
	}

	private ArrayList<Char> affected = new ArrayList<>();

	ArrayList<Lightning.Arc> arcs = new ArrayList<>();

	@Override
	public void attackProc( Char enemy, int damage ) {
		shoot(pos, enemy.pos);
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				shoot(pos, mob.pos);
			}
			shoot(pos, Dungeon.hero.pos);
			return;
		}
		shoot(pos, ch.pos);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new LightninglillyPoisonParticle().getColor(), 10);
		int newpos;
		int trys = 8;
		do {
			newpos = ch.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			trys--;
			if (trys <= 0) {
				return;
			}
		} while (!Dungeon.level.passable[newpos]);
		shoot(ch.pos, newpos);
	}

	public void shoot( int pos1, int pos2 ){
		final Ballistica shot = new Ballistica( pos1, pos2, Ballistica.PROJECTILE);
		fx(shot, new Callback() {
			@Override
			public void call() {
				onZap(shot);
			}
		});
	}

	protected void onZap( Ballistica bolt ) {

		//lightning deals less damage per-target, the more targets that are hit.
		float multipler = 0.4f + (0.6f/affected.size());
		//if the main target is in water, all affected take full damage
		if (Dungeon.level.water[bolt.collisionPos]) multipler = 1f;

		for (Char ch : affected){
			ch.damage(Math.round(2 * multipler), this);

			if (ch == Dungeon.hero) Camera.main.shake( 2, 0.3f );
			ch.sprite.centerEmitter().burst( SparkParticle.FACTORY, 3 );
			ch.sprite.flash();
		}
	}

	private void arc( Char ch ) {

		affected.add( ch );

		int dist;
		if (Dungeon.level.water[ch.pos] && !ch.flying)
			dist = 2;
		else
			dist = 1;

		PathFinder.buildDistanceMap( ch.pos, BArray.not( Dungeon.level.solid, null ), dist );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE){
				Char n = Actor.findChar( i );
				if (n == Dungeon.hero && PathFinder.distance[i] > 1)
					//the hero is only zapped if they are adjacent
					continue;
				else if (n != null && !affected.contains( n )) {
					arcs.add(new Lightning.Arc(ch.sprite.center(), n.sprite.center()));
					arc(n);
				}
			}
		}
	}

	protected void fx( Ballistica bolt, Callback callback ) {

		affected.clear();
		arcs.clear();

		int cell = bolt.collisionPos;

		Char ch = Actor.findChar( cell );
		if (ch != null) {
			arcs.add( new Lightning.Arc(DungeonTilemap.raisedTileCenterToWorld(pos), ch.sprite.center()));
			arc(ch);
		} else {
			arcs.add( new Lightning.Arc(DungeonTilemap.raisedTileCenterToWorld(pos), DungeonTilemap.raisedTileCenterToWorld(bolt.collisionPos)));
			CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 3 );
		}

		callback.call();
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_LIGHTNINGLILY;
			plantClass = Lightninglily.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			new Lightninglily().shoot(attacker.pos, defender.pos);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return LightninglillyPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new LightninglillyPoisonParticle();
		}
	}
}
