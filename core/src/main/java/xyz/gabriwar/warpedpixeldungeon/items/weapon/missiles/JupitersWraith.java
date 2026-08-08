/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class JupitersWraith extends MissileWeapon {

	{
		image = ItemSpriteSheet.JUPITERS_WRAITH;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.8f;

		tier = 4;
		sticky = false;
		baseUses = Integer.MAX_VALUE;

		bones = false;
		unique = true;
		stackable = false;
	}

	public int charge = 0;
	public int chargeCap = 1000;

	public static final String AC_EXPLODE = "EXPLODE";

	@Override
	public int min(int lvl) { return 4 + lvl * 2; }

	@Override
	public int max(int lvl) { return 8 + lvl * 4; }

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (charge >= chargeCap) {
			actions.add(AC_EXPLODE);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_EXPLODE)) {
			GLog.w(Messages.get(this, "activate"));
			explode(hero);
		}
	}

	private void explode(Hero hero) {
		charge = 0;
		int distance = Math.round(buffedLvl() / 3f) + 1;
		int length = Dungeon.level.length();
		int width = Dungeon.level.width();

		for (int i = width; i < length - width; i++) {
			int dist = Dungeon.level.distance(hero.pos, i);
			if (dist < distance) {
				if (Dungeon.level.heroFOV[i] && Dungeon.level.passable[i]) {
					CellEmitter.center(i).start(Speck.factory(Speck.ROCK), 0.07f, 10);
				}

				Char ch = Actor.findChar(i);
				if (ch != null && ch != hero) {
					int minDmg = min() * 2;
					int maxDmg = max() * 4;
					int dmg = Random.NormalIntRange(minDmg, maxDmg);
					if (dmg > 0) {
						ch.damage(dmg, this);
						if (Random.Int(3) == 0 && ch.isAlive()) {
							Buff.prolong(ch, Paralysis.class, 1);
						}
					}
				}
			}
		}
		Camera.main.shake(3, 0.7f);
		updateQuickslot();
	}

	@Override
	public String status() {
		if (charge >= chargeCap) {
			return Messages.get(this, "status_ready");
		} else {
			return charge + "/" + chargeCap;
		}
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		if (charge >= chargeCap) {
			desc += "\n\n" + Messages.get(this, "desc_charged");
		} else {
			desc += "\n\n" + Messages.get(this, "desc_charging", charge, chargeCap);
		}
		return desc;
	}

	// ── Boomerang return ─────────────────────────────────────────────

	boolean circlingBack = false;

	@Override
	protected float adjacentAccFactor(Char owner, Char target) {
		if (circlingBack) return 1.5f;
		return super.adjacentAccFactor(owner, target);
	}

	@Override
	public float pickupDelay() {
		return circlingBack ? 0f : super.pickupDelay();
	}

	@Override
	protected void rangedHit(Char enemy, int cell) {
		// Don't decrement durability — infinite uses
		Buff.append(Dungeon.hero, CircleBack.class).setup(this, cell, Dungeon.hero.pos, Dungeon.depth, Dungeon.branch);
	}

	@Override
	protected void rangedMiss(int cell) {
		parent = null;
		Buff.append(Dungeon.hero, CircleBack.class).setup(this, cell, Dungeon.hero.pos, Dungeon.depth, Dungeon.branch);
	}

	// ── Charge buff ──────────────────────────────────────────────────

	@Override
	public boolean doPickUp(Hero hero, int pos) {
		if (super.doPickUp(hero, pos)) {
			Buff.affect(hero, ExplodeCharge.class);
			return true;
		}
		return false;
	}

	//The charge ticker lives on the hero as a plain (static, hence deserialisable)
	//Buff and looks the wraith up in the backpack each turn. The old version was a
	//non-static inner class held in a transient field: after a save/load the field
	//was null and the buff couldn't be restored, so the charge froze forever.
	public static class ExplodeCharge extends Buff {
		@Override
		public boolean act() {
			JupitersWraith wraith = (target instanceof Hero)
					? ((Hero) target).belongings.getItem(JupitersWraith.class)
					: null;

			if (wraith == null) {
				//no longer carrying it — stop ticking
				detach();
				return true;
			}

			if (wraith.charge < wraith.chargeCap) {
				wraith.charge += Math.max(1, wraith.buffedLvl());
				if (wraith.charge >= wraith.chargeCap) {
					wraith.charge = wraith.chargeCap;
					GLog.w(Messages.get(JupitersWraith.class, "charged"));
				}
				updateQuickslot();
			}
			spend(TICK);
			return true;
		}
	}

	// ── Serialization ────────────────────────────────────────────────

	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
	}

	// ── CircleBack (adapted from HeavyBoomerang) ─────────────────────

	public static class CircleBack extends Buff {

		{ revivePersists = true; }

		private JupitersWraith wraith;
		private int thrownPos;
		private int returnPos;
		private int returnDepth;
		private int returnBranch;
		private int left;

		public void setup(JupitersWraith wraith, int thrownPos, int returnPos, int returnDepth, int returnBranch) {
			this.wraith = wraith;
			this.thrownPos = thrownPos;
			this.returnPos = returnPos;
			this.returnDepth = returnDepth;
			this.returnBranch = returnBranch;
			left = 5;
		}

		@Override
		public boolean act() {
			if (returnDepth == Dungeon.depth && returnBranch == Dungeon.branch) {
				left--;
				if (left <= 0) {
					final Char returnTarget = Actor.findChar(returnPos);
					final Char target = this.target;
					MissileSprite visual = ((MissileSprite) Dungeon.hero.sprite.parent.recycle(MissileSprite.class));
					visual.reset(thrownPos, returnPos, wraith, new Callback() {
						@Override
						public void call() {
							detach();
							wraith.circlingBack = true;
							if (returnTarget == target) {
								if (!(target instanceof Hero) || !wraith.doPickUp((Hero) target)) {
									Dungeon.level.drop(wraith, returnPos).sprite.drop();
								}
							} else if (returnTarget != null) {
								if (((Hero) target).shoot(returnTarget, wraith)) {
									// hit on return
								}
								Dungeon.level.drop(wraith, returnPos).sprite.drop();
							} else {
								Dungeon.level.drop(wraith, returnPos).sprite.drop();
							}
							wraith.circlingBack = false;
							CircleBack.this.next();
						}
					});
					visual.alpha(0f);
					float duration = Dungeon.level.trueDistance(thrownPos, returnPos) / 20f;
					target.sprite.parent.add(new AlphaTweener(visual, 1f, duration));
					return false;
				}
			}
			spend(TICK);
			return true;
		}

		private static final String WRAITH = "wraith";
		private static final String THROWN_POS = "thrown_pos";
		private static final String RETURN_POS = "return_pos";
		private static final String RETURN_DEPTH = "return_depth";
		private static final String RETURN_BRANCH = "return_branch";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(WRAITH, wraith);
			bundle.put(THROWN_POS, thrownPos);
			bundle.put(RETURN_POS, returnPos);
			bundle.put(RETURN_DEPTH, returnDepth);
			bundle.put(RETURN_BRANCH, returnBranch);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			wraith = (JupitersWraith) bundle.get(WRAITH);
			thrownPos = bundle.getInt(THROWN_POS);
			returnPos = bundle.getInt(RETURN_POS);
			returnDepth = bundle.getInt(RETURN_DEPTH);
			returnBranch = bundle.contains(RETURN_BRANCH) ? bundle.getInt(RETURN_BRANCH) : 0;
		}
	}
}
