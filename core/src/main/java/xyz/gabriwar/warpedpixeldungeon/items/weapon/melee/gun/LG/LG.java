package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.LG;

import static xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LaserParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class LG extends Gun {

    {
        image = ItemSpriteSheet.LG_T5;
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
        return Math.max(0, (maxRound()-round)*3);
    }

    @Override
    public Bullet knockBullet(){
        return new LGBullet();
    }

    public class LGBullet extends Bullet {
        {
            hitSound = Assets.Sounds.BURNING;
            image = ItemSpriteSheet.NO_BULLET;
        }

        @Override
        protected void onThrow(int cell) {
            if (cell != curUser.pos) {
                Ballistica aim = new Ballistica(curUser.pos, cell, Ballistica.WONT_STOP);
                ArrayList<Char> chars = new ArrayList<>();
                int maxDist = 2*(LG.this.tier+1);
                int dist = Math.min(aim.dist, maxDist);
                int cells = aim.path.get(Math.min(aim.dist, dist));
                boolean terrainAffected = false;
                for (int c : aim.subPath(1, maxDist)) {

                    Char ch;
                    if ((ch = Actor.findChar( c )) != null) {
                        chars.add( ch );
                    }

                    if (Dungeon.level.flamable[c]) {
                        Dungeon.level.destroy( c );
                        GameScene.updateMap( c );
                        terrainAffected = true;

                    }

                    CellEmitter.center( c ).burst( LaserParticle.BURST, 3 );
                }
                if (terrainAffected) {
                    Dungeon.observe();
                }

                float multi;
                switch (weightMod) {
                    case NORMAL_WEIGHT: default:
                        multi = 2f;
                        break;
                    case LIGHT_WEIGHT:
                        multi = 1f;
                        break;
                    case HEAVY_WEIGHT:
                        multi = 3f;
                        break;
                }
                curUser.sprite.parent.add(new Beam.SuperNovaRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld( cells ), multi));

                for (Char ch : chars) {
                    for (int i=0; i<shotPerShoot(); i++) {
                        if (curUser.shoot(ch, this)) {
                            ch.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10+buffedLvl() );
                        }
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
            }

            Invisibility.dispel();
            onShoot();
        }

        @Override
        public void throwSound() {
            Sample.INSTANCE.play(Assets.Sounds.RAY, 1f);
        }
    }

}
