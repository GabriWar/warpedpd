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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT;

import static xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ConeAOE;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class FT extends Gun {

    {
        image = ItemSpriteSheet.FT_T5;
        tier = 5;
        max_round = 2;
        round = max_round;
        shootingAccuracy = 1.5f;
    }

    @Override
    public int baseBulletMax(int lvl) {
        return 3 * (tier() + 1) +
                lvl * (tier() + 1);
    }

    @Override
    public int bulletUse() {
        return Math.max(0, (maxRound()-round)*2);
    }

    @Override
    public Bullet knockBullet(){
        return new FTBullet();
    }

    public class FTBullet extends Bullet {
        {
            hitSound = Assets.Sounds.BURNING;
            image = ItemSpriteSheet.NO_BULLET;
        }

        @Override
        protected void onThrow(int cell) {
            if (cell != curUser.pos) {
                Ballistica aim = new Ballistica(curUser.pos, cell, Ballistica.WONT_STOP);
                int maxDist = FT.this.tier + 1;
                int dist = Math.min(aim.dist, maxDist);
                ConeAOE cone = new ConeAOE(aim,
                        dist,
                        30,
                        Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
                //cast to cells at the tip, rather than all cells, better performance.
                for (Ballistica ray : cone.outerRays){
                    ((MagicMissile)curUser.sprite.parent.recycle( MagicMissile.class )).reset(
                            MagicMissile.FIRE_CONE,
                            curUser.sprite,
                            ray.path.get(ray.dist),
                            null
                    );
                }
                ArrayList<Char> chars = new ArrayList<>();
                for (int cells : cone.cells){
                    //knock doors open
                    if (Dungeon.level.map[cells] == Terrain.DOOR){
                        Level.set(cells, Terrain.OPEN_DOOR);
                        GameScene.updateMap(cells);
                    }

                    //only ignite cells directly near caster if they are flammable
                    if (!(Dungeon.level.adjacent(curUser.pos, cells) && !Dungeon.level.flamable[cells])) {
                        GameScene.add(Blob.seed(cells, 2, Fire.class));
                    }

                    Char ch = Actor.findChar(cells);
                    if (ch != null && ch.alignment != hero.alignment){
                        chars.add(ch);
                    }
                }
                for (Char ch : chars) {
                    for (int i=0; i<shotPerShoot(); i++) {
                        curUser.shoot(ch, this);
                        //the rest of this shot must not re-trigger the ranged skills
                        curUser.extraShotAttack = true;
                    }
                    if (ch == hero && !ch.isAlive()) {
                        Dungeon.fail(getClass());
                        Badges.validateDeathFromFriendlyMagic();
                        GLog.n(Messages.get(this, "ondeath"));
                    }
                }
                curUser.extraShotAttack = false;

                //final zap at 2/3 distance, for timing of the actual effect
                MagicMissile.boltFromChar(curUser.sprite.parent,
                        MagicMissile.FIRE_CONE,
                        curUser.sprite,
                        cone.coreRay.path.get(dist * 2 / 3),
                        new Callback() {
                            @Override
                            public void call() {
                            }
                        });
            }

            Invisibility.dispel();
            onShoot();
        }

        @Override
        public void throwSound(Char thrower) {
            SpatialSound.play(Assets.Sounds.BURNING, thrower, 1f);
        }
    }
}
