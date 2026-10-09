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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles;

import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HolyWaterPool;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPurity;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

/**
 * A throwable vial of blessed water. It shatters on whatever it hits - or on
 * bare ground - splashing everything nearby and soaking the area in a blessed
 * pool that scalds whatever stands in it for a few turns. Striking a creature
 * directly also mends the thrower a little. Upgrades widen the splash and
 * make the pool linger longer.
 */
public class HolyWater extends MissileWeapon {

	{
		image = ItemSpriteSheet.HOLY_WATER;
		hitSound = Assets.Sounds.HIT_MAGIC;
		hitSoundPitch = 1.2f;

		tier = 2;
		baseUses = 1;
		sticky = false;
	}

	@Override
	public int defaultQuantity() {
		return 30;
	}

	//splash radius: 1, +1 at +3 levels, capped at 3
	private int splashRadius(){
		return Math.min(3, 1 + buffedLvl()/3);
	}

	//pool duration in turns: 3 base, +1 per level, capped at 10
	private int poolTurns(){
		return Math.min(10, 3 + buffedLvl());
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {

		//the blessing mends the thrower
		int healAmt = Math.min(damage / 5, attacker.HT - attacker.HP);
		if (healAmt > 0 && attacker.isAlive()) {
			attacker.HP += healAmt;
			attacker.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healAmt), FloatingText.HEALING);
		}

		shatter(defender.pos, attacker, defender, damage);

		return super.proc(attacker, defender, damage);
	}

	/**
	 * The blessing on its own, for anything that carries holy water without being
	 * a vial: the thrower is mended and the splash goes out around the target.
	 */
	public static void blessProc(Char attacker, Char defender, int damage){
		HolyWater water = new HolyWater();
		int healAmt = Math.min(damage / 5, attacker.HT - attacker.HP);
		if (healAmt > 0 && attacker.isAlive()) {
			attacker.HP += healAmt;
			attacker.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healAmt), FloatingText.HEALING);
		}
		water.shatter(defender.pos, attacker, defender, damage);
	}

	//from +6 the blessing is strong enough that the vial comes back together after
	//every throw and returns to the pack; below that it is a fragile vial that is
	//spent whether it hits, misses or lands on bare ground
	private boolean reconstitutes(){
		return buffedLvl() >= 6;
	}

	private void returnToPack(){
		if (spawnedForEffect) return;
		parent = null;
		Hero hero = Dungeon.hero;
		if (collect( hero.belongings.backpack )){
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
		} else {
			Dungeon.level.drop( this, hero.pos ).sprite.drop();
		}
	}

	@Override
	protected void onThrow(int cell) {
		if (Actor.findChar(cell) != null){
			super.onThrow(cell);
		} else {
			//no target: the vial still shatters and soaks the ground
			shatter(cell, null, null, 0);
			if (reconstitutes()) returnToPack();
		}
	}

	//a hit: the splash already happened in proc(); the vial is gone, or back in the pack
	@Override
	protected void rangedHit(Char enemy, int cell) {
		if (reconstitutes()){
			returnToPack();
		} else {
			super.rangedHit(enemy, cell);
		}
	}

	//a miss still shatters the vial where it lands; a plain missile would just drop
	@Override
	protected void rangedMiss(int cell) {
		shatter(cell, Dungeon.hero, null, 0);
		if (reconstitutes()) returnToPack();
	}

	private void shatter(int center, Char attacker, Char target, int damage){

		SpatialSound.play(Assets.Sounds.SHATTER, center);
		if (Dungeon.level.heroFOV[center]){
			//the classic potion-break splash, in blessed pale blue
			Splash.at(center, 0xAADDFF, 5);
		}

		PathFinder.buildDistanceMap(center, BArray.not(Dungeon.level.solid, null), splashRadius());
		for (int i = 0; i < PathFinder.distance.length; i++){
			if (PathFinder.distance[i] == Integer.MAX_VALUE) continue;

			GameScene.add(Blob.seed(i, poolTurns(), HolyWaterPool.class));
			if (Dungeon.level.heroFOV[i]){
				CellEmitter.get(i).burst(FlowParticle.FACTORY, 4);
			}

			//the splash carries half of the impact to everything else caught in it
			Char ch = Actor.findChar(i);
			if (ch != null && ch != attacker && ch != target && damage > 0){
				ch.damage(damage / 2, this);
			}
		}
	}

	//no wear at +6, to match reconstitutes(): the stack never thins
	@Override
	public float durabilityPerUse(int level){
		if (level >= 6) return 0;
		return super.durabilityPerUse(level);
	}

	@Override
	public int value() {
		return 6 * quantity;
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{PotionOfPurity.class};
			inQuantity = new int[]{1};

			cost = 6;

			output = HolyWater.class;
			outQuantity = 30;
		}
	}
}
