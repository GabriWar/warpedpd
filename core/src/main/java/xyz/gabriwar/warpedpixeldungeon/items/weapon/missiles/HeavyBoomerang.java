/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class HeavyBoomerang extends MissileWeapon {
	
	{
		image = ItemSpriteSheet.BOOMERANG;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;
		
		tier = 4;
		sticky = false;
		baseUses = 5;
	}

	@Override
	public int max(int lvl) {
		return  4 * tier +                  //16 base, down from 20
				(tier-1) * lvl;             //3 scaling, down from 4
	}

	boolean circlingBack = false;

	@Override
	protected float adjacentAccFactor(Char owner, Char target) {
		if (circlingBack){
			return 1.5f;
		}
		return super.adjacentAccFactor(owner, target);
	}

	@Override
	public float pickupDelay() {
		//pickup is instant when circling back
		return circlingBack ? 0f : super.pickupDelay();
	}

	//it comes back to whoever threw it, and strikes on the way back for them
	@Override
	protected void rangedHit(Char enemy, int cell) {
		decrementDurability();
		if (durability > 0){
			Buff.append(curUser, CircleBack.class).setup(this, cell, curUser.pos, Dungeon.depth, Dungeon.branch);
		}
	}
	
	@Override
	protected void rangedMiss(int cell) {
		parent = null;
		Buff.append(curUser, CircleBack.class).setup(this, cell, curUser.pos, Dungeon.depth, Dungeon.branch);
	}

	/** Arc: the strikes on the way back, at every enemy on the line home but the one where it
	 *  lands (that one takes a full throw), each a share of a hit that wears nothing. The same
	 *  for Jupiter's Wraith */
	static void returnStrikes( MissileWeapon boomerang, Char thrower, int thrownPos, int returnPos ){
		if (WeaponPerk.arcRank( boomerang ) == 0 || !(thrower instanceof Hero) || !thrower.isAlive()) return;
		Quality q = Quality.of( boomerang );
		Hero hero = (Hero) thrower;
		Ballistica line = new Ballistica( thrownPos, returnPos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID );
		ArrayList<Char> struck = new ArrayList<>();
		for (int c : line.subPath( 1, line.dist )){
			Char ch = Actor.findChar( c );
			if (c != returnPos && ch != null && ch != hero && ch.alignment == Char.Alignment.ENEMY) struck.add( ch );
		}
		q.returnStrike = true;
		hero.extraShotAttack = true;
		try {
			for (Char ch : struck){
				if (ch.isAlive() && hero.shoot( ch, boomerang )) xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.bite( hero, ch, struck.indexOf( ch ), boomerang );
			}
		} finally {
			q.returnStrike = false;
			hero.extraShotAttack = false;
		}
	}
	
	public static class CircleBack extends Buff {

		{
			revivePersists = true;
		}
		
		private HeavyBoomerang boomerang;
		private int thrownPos;
		private int returnPos;
		private int returnDepth;
		private int returnBranch;
		
		private int left;
		
		public void setup( HeavyBoomerang boomerang, int thrownPos, int returnPos, int returnDepth, int returnBranch){
			this.boomerang = boomerang;
			this.thrownPos = thrownPos;
			this.returnPos = returnPos;
			this.returnDepth = returnDepth;
			this.returnBranch = returnBranch;
			//alpha Arc: it comes back after 2 turns instead of 5
			left = WeaponPerk.returnTurns( boomerang );
		}
		
		public int returnPos(){
			return returnPos;
		}
		
		public MissileWeapon cancel(){
			detach();
			return boomerang;
		}

		public int activeDepth(){
			return returnDepth;
		}
		
		@Override
		public boolean act() {
			if (returnDepth == Dungeon.depth && returnBranch == Dungeon.branch){
				left--;
				if (left <= 0){
					final Char returnTarget = Actor.findChar(returnPos);
					final Char target = this.target;
					MissileSprite visual = ((MissileSprite) Dungeon.hero.sprite.parent.recycle(MissileSprite.class));
					visual.reset( thrownPos,
									returnPos,
									boomerang,
									new Callback() {
										@Override
										public void call() {
											detach();
											boomerang.circlingBack = true;
											returnStrikes( boomerang, target, thrownPos, returnPos );
											if (returnTarget == target){
												if (!boomerang.spawnedForEffect) {
													boolean caught = target instanceof Hero && boomerang.doPickUp((Hero) target);
													if (caught) xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.caught( target );
													if (!caught) {
														Dungeon.level.drop(boomerang, returnPos).sprite.drop();
													}
												}
												
											} else if (returnTarget != null){
												if (((Hero)target).shoot( returnTarget, boomerang )) {
													boomerang.decrementDurability();
												}
												if (!boomerang.spawnedForEffect && boomerang.durability > 0) {
													Dungeon.level.drop(boomerang, returnPos).sprite.drop();
												}
												
											} else if (!boomerang.spawnedForEffect) {
												Dungeon.level.drop(boomerang, returnPos).sprite.drop();
											}
											boomerang.circlingBack = false;
											CircleBack.this.next();
										}
									});
					visual.alpha(0f);
					xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.telegraph( thrownPos, returnPos, target, boomerang );
					float duration = Dungeon.level.trueDistance(thrownPos, returnPos) / 20f;
					target.sprite.parent.add(new AlphaTweener(visual, 1f, duration));
					return false;
				}
			}
			spend( TICK );
			return true;
		}
		
		private static final String BOOMERANG = "boomerang";
		private static final String THROWN_POS = "thrown_pos";
		private static final String RETURN_POS = "return_pos";
		private static final String RETURN_DEPTH = "return_depth";
		private static final String RETURN_BRANCH = "return_branch";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(BOOMERANG, boomerang);
			bundle.put(THROWN_POS, thrownPos);
			bundle.put(RETURN_POS, returnPos);
			bundle.put(RETURN_DEPTH, returnDepth);
			bundle.put(RETURN_BRANCH, returnBranch);
		}
		
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			boomerang = (HeavyBoomerang) bundle.get(BOOMERANG);
			thrownPos = bundle.getInt(THROWN_POS);
			returnPos = bundle.getInt(RETURN_POS);
			returnDepth = bundle.getInt(RETURN_DEPTH);
			returnBranch = bundle.contains(RETURN_BRANCH) ? bundle.getInt(RETURN_BRANCH) : 0;
		}
	}
	
}
