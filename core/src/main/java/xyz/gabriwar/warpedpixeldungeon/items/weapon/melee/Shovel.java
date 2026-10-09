/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2021 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import static xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Shovel extends MeleeWeapon {

    public static final String AC_DIG	= "DIG";

    {
        defaultAction = AC_DIG;

        image = ItemSpriteSheet.ADVENTURER_SHOVEL;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.1f;

        tier = 1;

        unique = true;
        bones = false;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_DIG);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_DIG)) {
            dig(hero.pos);
        }
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        return super.proc( attacker, defender, damage );
    }

    @Override
    public int max(int lvl) {
        return  5*(tier+1) +
                lvl*(tier+1);
    }

    public void dig(int pos) {
        ArrayList<Integer> tiles = new ArrayList<>();
        for (int i : PathFinder.NEIGHBOURS9) {
            int tile = pos + i;
            if (Dungeon.level.map[tile] == Terrain.GRASS) {
                tiles.add(tile);
            }
        }

        if (tiles.isEmpty()) {
            GLog.w(Messages.get(this, "no_grass"));
            return;
        }

        for (int tile : tiles) {
            Level.set(tile, Terrain.EMPTY);
            GameScene.updateMap(tile);
            CellEmitter.get(tile).burst( LeafParticle.LEVEL_SPECIFIC, 4 );
            if (Random.Float() < 0.1f) {
                Dungeon.level.drop(Generator.randomUsingDefaults(Generator.Category.SEED), pos).sprite.drop(tile);
            }
        }

        curUser.spend(Actor.TICK);
        curUser.busy();
        SpatialSound.play(Assets.Sounds.TRAMPLE, pos, 2, 1.1f);
        curUser.sprite.operate(curUser.pos);
    }

	// ---- Duelist ability: a shovelful of dirt in the face ----

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		dirtAbility(hero, target, this);
	}

	@Override
	public String abilityInfo() {
		int turns = levelKnown ? 4 + buffedLvl() : 4;
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", turns);
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(4 + level);
	}

	/** dirt flung 3 tiles: the target is blinded, and one standing on grass or soil is dug in as well */
	public static void dirtAbility(Hero hero, Integer target, MeleeWeapon wep){
		if (target == null) return;
		Ballistica toss = new Ballistica(hero.pos, target, Ballistica.PROJECTILE);
		int cell = toss.collisionPos;
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[cell]){
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, cell) > 3){
			GLog.w(Messages.get(wep, "ability_target_range"));
			return;
		}
		wep.beforeAbilityUsed(hero, enemy);
		int turns = 4 + wep.buffedLvl();
		Buff.prolong(enemy, Blindness.class, turns);
		int t = Dungeon.level.map[cell];
		if (t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS || Dungeon.level.water[cell]){
			Buff.prolong(enemy, Roots.class, 2f);
		}
		hero.sprite.zap(cell);
		for (int c : toss.subPath(1, toss.dist)){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 2);
		}
		CellEmitter.center(cell).burst(Speck.factory(Speck.DUST), 10);
		SkillFX.flash(enemy);
		SpatialSound.play(Assets.Sounds.TRAMPLE, cell, 1f, 1.2f);
		Invisibility.dispel();
		hero.spendAndNext(hero.attackDelay());
		wep.afterAbilityUsed(hero);
	}
}
