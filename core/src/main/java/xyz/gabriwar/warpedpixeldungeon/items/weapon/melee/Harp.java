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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Harp extends MeleeWeapon {

	{
		image = ItemSpriteSheet.HARP;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.3f;

		tier = 5;
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  4*(tier+1) +    //24 base, down from 30
				lvl*(tier);     //+5 per level, down from +6
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//the harp's resonance strikes everyone else adjacent to the target
		//for a quarter of a regular damage roll
		int p = defender.pos;
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(p + n);
			if (ch != null && ch != attacker && ch != defender && ch.isAlive()) {
				ch.damage(augment.damageFactor(Random.NormalIntRange(min(), max())) / 4, this);
			}
		}

		return super.proc(attacker, defender, damage);
	}


	// ---- Duelist: Requiem. A chord that fills the room: every enemy in view takes ----
	// ---- a resonant hit and is slowed, every ally in view is mended a little. ----

	@Override
	protected int baseChargeUse(Hero hero, Char target){
		return 2;
	}

	private int requiemDamage(){
		return augment.damageFactor(Random.NormalIntRange(min(), max())) / 2;
	}

	private int requiemHeal(){
		return 2 + buffedLvl() / 2;
	}

	private int requiemSlow(){
		return 3 + buffedLvl() / 3;
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		ArrayList<Char> enemies = new ArrayList<>();
		ArrayList<Char> allies = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch == hero || !Dungeon.level.heroFOV[ch.pos] || !ch.isAlive()) continue;
			if (ch.alignment == Char.Alignment.ENEMY && !hero.isCharmedBy(ch)) enemies.add(ch);
			else if (ch.alignment == Char.Alignment.ALLY) allies.add(ch);
		}
		if (enemies.isEmpty() && allies.isEmpty() && hero.HP >= hero.HT){
			GLog.w(Messages.get(this, "ability_no_target"));
			return;
		}
		beforeAbilityUsed(hero, null);

		//the chord: notes off the harp, a swell of light, the sound
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.NOTE), 8);
		new Flare(6, 22).color(0xCCDDFF, true).show(hero.sprite, 0.9f).angularSpeed = 45;
		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 1f, 0.8f);
		Sample.INSTANCE.playDelayed(Assets.Sounds.CHARMS, 0.25f, 1f, 1.2f);

		int slow = requiemSlow();
		for (Char ch : enemies){
			int dmg = requiemDamage();
			ch.damage(dmg, this);
			if (ch.sprite != null) ch.sprite.emitter().burst(Speck.factory(Speck.BLUE_LIGHT), 3);
			if (ch.isAlive()){
				Buff.prolong(ch, Slow.class, slow);
			} else {
				onAbilityKill(hero, ch);
			}
		}
		int heal = requiemHeal();
		allies.add(hero);
		for (Char ch : allies){
			int healed = Math.min(heal, ch.HT - ch.HP);
			if (healed <= 0) continue;
			ch.HP += healed;
			if (ch.sprite != null){
				ch.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
				ch.sprite.showStatus(CharSprite.POSITIVE, "+" + healed);
			}
		}

		Invisibility.dispel();
		hero.spendAndNext(hero.attackDelay());
		afterAbilityUsed(hero);
	}

	@Override
	public String abilityInfo() {
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min())/2, augment.damageFactor(max())/2, requiemSlow(), requiemHeal());
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)/2, max(0)/2, 3, 2);
		}
	}

	@Override
	public String upgradeAbilityStat(int level){
		return augment.damageFactor(min(level))/2 + "-" + augment.damageFactor(max(level))/2 + ", " + (3 + level/3) + ", " + (2 + level/2);
	}
}
