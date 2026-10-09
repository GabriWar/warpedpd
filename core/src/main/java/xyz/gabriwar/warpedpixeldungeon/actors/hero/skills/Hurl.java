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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;

import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import java.util.HashSet;
import java.util.Set;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * Duelist: the weapon in hand is thrown, spinning, at an enemy up to six tiles off,
 * and flies back to her hand. Each hit may bounce it on to another nearby enemy;
 * at level 3 consecutive Hurl hits build a combo that powers the next throw.
 */
public class Hurl extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	@Override
	public boolean rangedSource(){ return true; }

	private static final int RANGE = 6;
	private static final float DAMAGE = 1.2f;
	private transient Throw pending;

	{
		tag = "A5";
		name = "Hurl";
		castText = "Catch!";
		image = 199;
		tier = 3;
		mana = 5;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost() && hero.belongings.weapon() instanceof MeleeWeapon)
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public boolean confirmTarget(Hero hero) {
		if (pending == null || !validTarget(hero, pending.suggested)) return false;
		// Match throwable quickslots: deliver the second tap to the active
		// selector before GameScene.cancel() can discard the target.
		GameScene.handleCell(pending.suggested.pos);
		return true;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			if (!(hero.belongings.weapon() instanceof MeleeWeapon)){
				GLog.w( Messages.get(this, "no_weapon") );
				return;
			}
			if (confirmTarget(hero)) return;
			Throw selection = new Throw();
			selection.suggested = autoTarget(hero);
			GameScene.selectCell(selection);
			if (!selection.finished) {
				pending = selection;
				selection.showMarker();
			}
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	static boolean validTarget(Hero hero, Char enemy) {
		return Dungeon.level != null && enemy != null && enemy.isAlive()
				&& enemy.alignment == Char.Alignment.ENEMY && Actor.chars().contains(enemy)
				&& enemy.pos >= 0 && enemy.pos < Dungeon.level.length()
				&& Dungeon.level.heroFOV[enemy.pos] && Dungeon.level.distance(hero.pos, enemy.pos) <= RANGE
				&& new Ballistica(hero.pos, enemy.pos, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos == enemy.pos;
	}

	static Char autoTarget(Hero hero) {
		Char previous = xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton.lastTarget;
		if (validTarget(hero, previous)) return previous;
		Char nearest = null;
		for (Char enemy : Actor.chars()) {
			if (validTarget(hero, enemy) && (nearest == null
					|| Dungeon.level.trueDistance(hero.pos, enemy.pos) < Dungeon.level.trueDistance(hero.pos, nearest.pos))) nearest = enemy;
		}
		return nearest;
	}

	private class Throw extends CellSelector.Listener {
		Char suggested;
		boolean finished;
		com.watabou.noosa.Image marker;

		void showMarker() {
			if (suggested == null || suggested.sprite == null || suggested.sprite.parent == null) return;
			marker = new com.watabou.noosa.Image(xyz.gabriwar.warpedpixeldungeon.ui.Icons.get(
					xyz.gabriwar.warpedpixeldungeon.ui.Icons.TARGET)) {
				@Override public void update() {
					super.update();
					visible = validTarget(Dungeon.hero, suggested) && suggested.sprite.visible;
					point(suggested.sprite.center(this));
				}
			};
			marker.point(suggested.sprite.center(marker));
			suggested.sprite.parent.addToFront(marker);
		}

		void finish() {
			finished = true;
			if (pending == this) pending = null;
			if (marker != null) { marker.killAndErase(); marker.destroy(); marker = null; }
		}


		@Override
		public void onSelect( Integer target ){
			finish();
			if (target == null) return;
			final Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost()) return;
			KindOfWeapon held = hero.belongings.weapon();
			if (!(held instanceof MeleeWeapon)){
				GLog.w( Messages.get(Hurl.class, "no_weapon") );
				return;
			}
			final MeleeWeapon wep = (MeleeWeapon) held;
			if (target < 0 || target >= Dungeon.level.length()) return;

			Ballistica traj = new Ballistica( hero.pos, target, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET );
			final int far = traj.collisionPos;
			Char aimed = Actor.findChar( far );
			if (far == hero.pos || aimed == null || aimed.alignment != Char.Alignment.ENEMY
					|| !Dungeon.level.heroFOV[far] || Dungeon.level.distance( hero.pos, far ) > RANGE){
				GLog.w( Messages.get(Hurl.class, "no_target") );
				return;
			}

			xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton.target(aimed);


			hero.MP -= getManaCost();
			castTextYell();
			Invisibility.dispel();
			hero.busy();
			hero.sprite.zap( far );
			//the blade leaves her hand spinning
			ArcSpinFX.around( hero.sprite, 0xFFFFFF, 8, 0.35f, 0, 1300, 0.28f );
			SpatialSound.play( Assets.Sounds.MISS, hero, 1f, 1.1f );

			new Ricochet(hero, wep).fly(hero.pos, aimed);

		}

		@Override
		public String prompt(){
			return suggested == null ? Messages.get(Hurl.class, "prompt")
					: Messages.get(Hurl.class, "auto_prompt", suggested.name());
		}
	}

	static Char bounceTarget(int from, Set<Char> visited) {
		Char nearest = null;
		for (Char enemy : Actor.chars()) {
			if (visited.contains(enemy) || !enemy.isAlive() || enemy.alignment != Char.Alignment.ENEMY
					|| enemy.pos < 0 || enemy.pos >= Dungeon.level.length() || !Dungeon.level.heroFOV[enemy.pos]
					|| enemy.pos == from || Dungeon.level.distance(from, enemy.pos) > RANGE
					|| new Ballistica(from, enemy.pos, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos != enemy.pos) continue;
			if (nearest == null || Dungeon.level.trueDistance(from, enemy.pos)
					< Dungeon.level.trueDistance(from, nearest.pos)) nearest = enemy;
		}
		return nearest;
	}

	private class Ricochet {
		final Hero hero;
		final MeleeWeapon weapon;
		final float power;
		final Set<Char> visited = new HashSet<>();
		boolean landed;

		Ricochet(Hero hero, MeleeWeapon weapon) {
			this.hero = hero;
			this.weapon = weapon;
			//the combo built by earlier throws powers this whole throw, bounces included
			HurlCombo combo = level >= MAX_LEVEL ? hero.buff(HurlCombo.class) : null;
			power = DAMAGE * (combo == null ? 1f : combo.multiplier());
			if (combo != null && hero.sprite != null) hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 2 + combo.hits);
		}

		void fly(int from, Char target) {
			final int to = target.pos;
			visited.add(target);
			SkillFX.streak(from, to, weapon, () -> {
				boolean bounce = false;
				if (target.isAlive() && Char.hit(hero, target, false)) {
					landed = true;
					int damage = Math.round(weapon.damageRoll(hero) * power);
					damage = weapon.proc(hero, target, damage);
					damage = hero.heroSkills.allOnHit(target, damage, false);
					target.damage(damage, Hurl.this);
					SkillFX.flash(target);
					weapon.hitSound(1f, target);
					//each body it bites rings a little higher, and a bounce catches the light
					SpatialSound.play(Assets.Sounds.HIT_PARRY, target, 0.7f, 1.1f + 0.12f * (visited.size() - 1));
					if (target.sprite != null) new Flare(4, 10).color(0xFFFFFF, true).show(target.sprite, 0.3f);
					if (level >= MAX_LEVEL) Buff.affect(hero, HurlCombo.class).addHit();
					bounce = Random.Float() < 0.15f * level;
				} else if (target.isAlive()) {
					if (target.sprite != null) target.sprite.showStatus(CharSprite.NEUTRAL, target.defenseVerb());
					SpatialSound.play(Assets.Sounds.MISS, target);
				}
				Char next = bounce && hero.isAlive() ? bounceTarget(to, visited) : null;
				if (next != null) fly(to, next);
				else returnToHero(to);
			});
		}

		void returnToHero(int from) {
			// This is only a visual copy: the real weapon stays equipped throughout.
			SkillFX.streak(from, hero.pos, weapon, () -> {
				//a throw that lands nothing breaks the chain
				if (!landed) Buff.detach(hero, HurlCombo.class);
				SpatialSound.play(Assets.Sounds.HIT_PARRY, hero, 1f, 1.3f);
				//caught: a glint in her hand
				if (hero.sprite != null){
					new Flare(4, 8).color(0xFFFFFF, true).show(hero.sprite, 0.25f);
					hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 3);
				}
				hero.spendAndNext(TIME_TO_USE);
			});
		}
	}

	/** Hurl's own combo: only its thrown hits build it, and it fades when you stop throwing. */
	public static class HurlCombo extends Buff {

		private static final int MAX_HITS = 5;
		private static final float DURATION = 5f;

		{
			type = buffType.POSITIVE;
		}

		public int hits = 0;
		private float left = 0f;

		float multiplier(){ return 1f + 0.1f * hits; }

		void addHit(){
			hits = Math.min(MAX_HITS, hits + 1);
			left = DURATION;
			if (target != null && target.sprite != null){
				target.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Hurl.class, "combo", hits));
			}
		}

		@Override
		public boolean act() {
			left -= TICK;
			spend(TICK);
			if (left <= 0) detach();
			return true;
		}

		@Override
		public int icon() { return BuffIndicator.DUEL_COMBO; }

		@Override
		public float iconFadePercent() { return Math.max(0, (DURATION - left) / DURATION); }

		@Override
		public String iconTextDisplay() { return Integer.toString(hits); }

		@Override
		public String desc() {
			return Messages.get(this, "desc", hits, 10 * hits, dispTurns(left));
		}

		private static final String HITS = "hits";
		private static final String LEFT = "left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(HITS, hits);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			hits = bundle.getInt(HITS);
			left = bundle.getFloat(LEFT);
		}
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
