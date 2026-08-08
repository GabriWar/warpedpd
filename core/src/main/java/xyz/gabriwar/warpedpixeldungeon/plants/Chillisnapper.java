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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.ChillisnapperPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.HashSet;

public class Chillisnapper extends Plant {

	{
		image = 49;
		livingPlantImage = 31;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return 20f; }

	//the actual affected cells
	private HashSet<Integer> affectedCells;
	//the cells to trace fire shots to, for visual effects.
	private HashSet<Integer> visualCells;
	private int direction = 0;

	public void shoot( Char ch, int pos ){
		final Ballistica shot = new Ballistica( ch.pos, pos, Ballistica.PROJECTILE);
		fx(shot, new Callback() {
			@Override
			public void call() {
				onZap(shot);
			}
		}, ch);
	}

	protected void fx( Ballistica bolt, Callback callback, Char ch ) {
		//need to perform flame spread logic here so we can determine what cells to put flames in.
		affectedCells = new HashSet<>();
		visualCells = new HashSet<>();

		int maxDist = 2 + 2 * 2;
		int dist = Math.min(bolt.dist, maxDist);

		for (int i = 0; i < PathFinder.CIRCLE8.length; i++){
			if (bolt.sourcePos+PathFinder.CIRCLE8[i] == bolt.path.get(1)){
				direction = i;
				break;
			}
		}

		float strength = maxDist;
		for (int c : bolt.subPath(1, dist)) {
			strength--; //as we start at dist 1, not 0.
			affectedCells.add(c);
			if (strength > 1) {
				spreadFlames(c + PathFinder.CIRCLE8[left(direction)], strength - 1);
				spreadFlames(c + PathFinder.CIRCLE8[direction], strength - 1);
				spreadFlames(c + PathFinder.CIRCLE8[right(direction)], strength - 1);
			} else {
				visualCells.add(c);
			}
		}

		//going to call this one manually
		visualCells.remove(bolt.path.get(dist));

		for (int cell : visualCells){
			//this way we only get the cells at the tip, much better performance.
			((MagicMissile)ch.sprite.parent.recycle( MagicMissile.class )).reset(
					MagicMissile.FIRE_CONE,
					ch.sprite,
					cell,
					null
			);
		}
		MagicMissile.boltFromChar( ch.sprite.parent,
				MagicMissile.FIRE_CONE,
				ch.sprite,
				bolt.path.get(dist/2),
				callback );
		if (Dungeon.level.heroFOV[bolt.sourcePos] || Dungeon.level.heroFOV[bolt.collisionPos]){
			Sample.INSTANCE.play( Assets.Sounds.ZAP );
		}
	}

	//burn... BURNNNNN!.....
	private void spreadFlames( int cell, float strength ){
		if (strength >= 0 && (Dungeon.level.passable[cell] || Dungeon.level.flamable[cell])){
			affectedCells.add(cell);
			if (strength >= 1.5f) {
				visualCells.remove(cell);
				spreadFlames(cell + PathFinder.CIRCLE8[left(direction)], strength - 1.5f);
				spreadFlames(cell + PathFinder.CIRCLE8[direction], strength - 1.5f);
				spreadFlames(cell + PathFinder.CIRCLE8[right(direction)], strength - 1.5f);
			} else {
				visualCells.add(cell);
			}
		} else if (!Dungeon.level.passable[cell])
			visualCells.add(cell);
	}

	private int left( int direction ){
		return direction == 0 ? 7 : direction-1;
	}

	private int right( int direction ){
		return direction == 7 ? 0 : direction+1;
	}

	protected void onZap( Ballistica bolt ) {

		for (int cell : affectedCells){

			//ignore caster cell
			if (cell == bolt.sourcePos){
				continue;
			}

			//only ignite cells directly near caster if they are flammable
			if (!Dungeon.level.adjacent(bolt.sourcePos, cell)
					|| Dungeon.level.flamable[cell]){
				GameScene.add( Blob.seed( cell, 1+2, Fire.class ) );
			}
		}
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		GameScene.add(Blob.seed(enemy.pos, 1+2, Fire.class));
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): seed fire at the plant's position
			GameScene.add(Blob.seed(pos, 1+2, Fire.class));
			return;
		}

		if (ch instanceof Hero) {
			for (Char cha : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (ch.fieldOfView[cha.pos]) {
					if (cha != null){
						shoot(ch, cha.pos);
						if (((Hero) ch).subClass != HeroSubClass.WARDEN){
							return;
						}
					}
				}
			}
		} else {
			if (ch.fieldOfView[Dungeon.hero.pos]){
				shoot(ch, Dungeon.hero.pos);
				return;
			} else {
				for (Char cha : Dungeon.level.mobs.toArray(new Mob[0])) {
					if (cha != null){
						if (ch != cha){
							shoot(ch, cha.pos);
							return;
						}
					}
				}
			}
		}
		GameScene.add(Blob.seed(ch.pos, 1+2, Fire.class));
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new ChillisnapperPoisonParticle().getColor(), 10);
		int newpos;
		int trys = 8;
		do{
			newpos = ch.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			trys--;
			if (trys <= 0){
				return;
			}
		} while (!Dungeon.level.passable[newpos]);
		shoot(ch, newpos);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_CHILLISNAPPER;
			plantClass = Chillisnapper.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			GameScene.add(Blob.seed(defender.pos, 3, Fire.class));
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return ChillisnapperPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new ChillisnapperPoisonParticle();
		}
	}
}
