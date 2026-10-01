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

import java.util.ArrayList;
import com.watabou.utils.Bundle;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/** Throwdown; retains the old class and A1 tag for existing saves and hotbar assignments. */
public class Smash extends ActiveSkill1 {
    { name="Throwdown"; castText="Down you go!"; tier=1; image=18; mana=3; }
    @Override public boolean toggleable(){ return false; }
    @Override public boolean weaponScaled(){ return true; }
    @Override public void restoreInBundle(Bundle b){ super.restoreInBundle(b); active=false; }
    public int range(){ return 1 + 2 * level; }

    public static boolean validEnemy(Char enemy){
        return enemy != null && enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY
                && !Char.hasProp(enemy, Char.Property.BOSS);
    }

    public boolean canGrab(Hero hero, Char enemy){
        return validEnemy(enemy) && Dungeon.level.adjacent(hero.pos, enemy.pos)
                && Dungeon.level.heroFOV[enemy.pos] && !hero.isCharmedBy(enemy);
    }

    //An occupied destination is an impact point; the thrown enemy lands immediately before it.
    //The arc can cross characters and pits, but cannot cross walls. Chasms use the normal falling rules.
    public int landingCell(Char enemy, int destination){
        Level floor = Dungeon.level;
        if (destination < 0 || destination >= floor.length() || destination == enemy.pos
                || !floor.heroFOV[destination] || floor.distance(enemy.pos, destination) > range()) return -1;
        Char other = Actor.findChar(destination);
        if (other != null && (!validEnemy(other) || Dungeon.hero.isCharmedBy(other))) return -1;
        Ballistica path = new Ballistica(enemy.pos, destination, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);
        if (path.collisionPos != destination || path.dist < 1) return -1;
        int landing = other == null ? destination : path.path.get(path.dist - 1);
        // Traps are marked AVOID instead of PASSABLE so normal pathfinding avoids
        // them, but Throwdown may deliberately land an enemy there.
        if (!floor.passable[landing] && !floor.avoid[landing] && !floor.pit[landing]) return -1;
        Char occupant = Actor.findChar(landing);
        if (occupant != null && occupant != enemy) return -1;
        return landing;
    }

    @Override public ArrayList<String> actions(Hero hero){
        ArrayList<String> out = new ArrayList<>();
        if (level > 0 && hero.MP >= getManaCost()) out.add(AC_CAST);
        return out;
    }

    @Override public void execute(Hero hero, String action){
        if (!AC_CAST.equals(action) || level <= 0 || hero.MP < getManaCost()) return;
        final Level floor = Dungeon.level;
        GameScene.selectCell(new CellSelector.Listener(){
            @Override public String prompt(){ return Messages.get(Smash.class, "prompt"); }
            @Override public void onSelect(Integer cell){
                if (cell == null || Dungeon.level != floor) return;
                Char enemy = Actor.findChar(cell);
                if (!canGrab(hero, enemy)){
                    GLog.w(Messages.get(Smash.class, "no_target")); return;
                }
                //Heavy and anchored enemies can be struck, but never repositioned.
                if (Char.hasProp(enemy, Char.Property.LARGE) || Char.hasProp(enemy, Char.Property.IMMOVABLE)){
                    cast(hero, enemy, enemy.pos, null, floor); return;
                }
                //CellSelector resets after this click; install the second prompt next frame.
                com.watabou.noosa.Game.runOnRenderThread(() -> {
                    if (Dungeon.level != floor || !hero.ready || !canGrab(hero, enemy)) return;
                    GameScene.selectCell(new CellSelector.Listener(){
                    @Override public String prompt(){ return Messages.get(Smash.class, "landing", range()); }
                    @Override public void onSelect(Integer destination){
                        if (destination == null || Dungeon.level != floor) return;
                        if (!canGrab(hero, enemy) || hero.MP < getManaCost()) return;
                        int landing = landingCell(enemy, destination);
                        if (landing < 0){ GLog.w(Messages.get(Smash.class, "no_landing")); return; }
                        cast(hero, enemy, landing, Actor.findChar(destination), floor);
                    }
                    });
                });
            }
        });
    }

    private void cast(Hero hero, Char enemy, int landing, Char other, Level floor){
        if (Dungeon.level != floor || !canGrab(hero, enemy) || hero.MP < getManaCost()) return;
        hero.MP -= getManaCost();
        hero.heroSkills.lastUsed = this;
        hero.busy();
        Invisibility.dispel();
        int origin = enemy.pos;
        KindOfWeapon weapon = hero.belongings.weapon();
        final int damage = weapon == null ? RingOfForce.damageRoll(hero) : weapon.damageRoll(hero);
        Actor.add(new ThrowAction(hero, enemy, other, floor, origin, landing, damage));
        hero.spendAndNext(TIME_TO_USE);
    }

    /** Holds the scheduler across BOTH animations, rather than relying on a mob sprite flag. */
    public class ThrowAction extends Actor {
        private final Hero hero;
        private final Char enemy, other;
        private final Level floor;
        private final int origin, landing, damage;
        private boolean finished;

        public ThrowAction(Hero hero, Char enemy, Char other, Level floor, int origin, int landing, int damage){
            this.hero = hero;
            this.enemy = enemy;
            this.other = other;
            this.floor = floor;
            this.origin = origin;
            this.landing = landing;
            this.damage = damage;
            actPriority = VFX_PRIO + 10;
        }

        @Override public boolean act(){
            //Like Pushing, removal does not release the currently executing actor.
            Actor.remove(this);
            com.watabou.noosa.Game.runOnRenderThread(() -> {
                if (!validGrab()) { complete(); return; }
                if (hero.sprite != null && hero.sprite.parent != null){
                    hero.sprite.attack(origin, this::launch);
                } else launch();
            });
            return false;
        }

        private boolean validGrab(){
            return Dungeon.level == floor && Dungeon.hero == hero && hero.isAlive()
                    && canGrab(hero, enemy) && enemy.pos == origin
                    && (other == null || validEnemy(other));
        }

        private void launch(){
            if (finished) return;
            if (!validGrab() || !beginThrow(enemy, floor, origin, landing)){
                syncSprite(enemy, floor);
                complete(); return;
            }
            if (landing != origin && enemy.sprite != null && enemy.sprite.parent != null){
                enemy.sprite.place(origin);
                enemy.sprite.jump(origin, landing, this::land);
            } else land();
        }

        private void land(){
            if (finished) return;
            try {
                if (finishThrow(enemy, other, floor, origin, landing, damage)){
                    WarriorImpactFX.show(landing, true);
                    Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.9f);
                }
                if (Dungeon.level == floor){
                    Dungeon.observe();
                    GameScene.updateFog();
                }
            } finally {
                complete();
            }
        }

        private void complete(){
            if (finished) return;
            finished = true;
            //Only this action releases its scheduler slot. Mob/hero animation callbacks
            //cannot advance the turn while the throw owns it.
            next();
        }
    }

    public boolean beginThrow(Char enemy, Level floor, int origin, int landing){
        if (Dungeon.level != floor || !validEnemy(enemy) || enemy.pos != origin
                || landing < 0 || landing >= floor.length()
                || (!floor.passable[landing] && !floor.avoid[landing] && !floor.pit[landing])) return false;
        Char occupant = Actor.findChar(landing);
        if (occupant != null && occupant != enemy) return false;
        //The scheduler holds the turn while this tile is reserved. Terrain is deliberately
        //deferred: a chasm must not remove the mob and start its fall before the jump is drawn.
        if (landing != origin) enemy.beginAnimatedMove(landing);
        return Dungeon.level == floor && validEnemy(enemy) && enemy.pos == landing
                && Actor.chars().contains(enemy);
    }

    public boolean finishThrow(Char enemy, Char other, Level floor, int origin, int landing, int damage){
        try {
            if (Dungeon.level != floor || !validEnemy(enemy) || enemy.pos != landing) return false;
            syncSprite(enemy, floor);
            if (landing != origin) enemy.finishAnimatedMove(origin);
            if (Dungeon.level != floor || !validEnemy(enemy) || !Actor.chars().contains(enemy)
                    || enemy.pos != landing) return false;
            if (other != null && floor.adjacent(landing, other.pos)) collision(enemy, other, damage);
            else enemy.damage(damage, this);
            return true;
        } finally {
            //Even a cancelled throw must undo the jump's visual endpoint. Never move the actor
            //back to a cached tile: another character may already have entered it.
            syncSprite(enemy, floor);
        }
    }

    private static void syncSprite(Char enemy, Level floor){
        if (Dungeon.level == floor && enemy.sprite != null && enemy.pos >= 0 && enemy.pos < floor.length()){
            enemy.sprite.snapToPosition(enemy.pos);
            if (enemy.isAlive()) enemy.sprite.visible = floor.heroFOV[enemy.pos];
        }
    }

    public void collision(Char thrown, Char other, int damage){
        //Check both again at impact: bosses must never receive even collateral skill damage.
        if (!validEnemy(thrown) || !validEnemy(other)) return;
        thrown.damage(damage, this);
        other.damage(damage, this);
        if (thrown.isAlive()) Buff.prolong(thrown, Paralysis.class, 1f);
        if (other.isAlive()) Buff.prolong(other, Paralysis.class, 1f);
    }

    @Override public int getManaCost(){ return (int)Math.ceil(mana * (1 + .55 * level)); }
    @Override protected boolean upgrade(){ return true; }
}
