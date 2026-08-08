/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2024 Gabriel Batista
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

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfLightning extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_LIGHTNING;
    }

    private ArrayList<Char> affected = new ArrayList<>();
    ArrayList<Lightning.Arc> arcs = new ArrayList<>();

    private void arc( Char ch ) {
        affected.add( ch );

        int dist;
        if (Dungeon.level.water[ch.pos] && !ch.flying)
            dist = 2;
        else
            dist = 1;

        PathFinder.buildDistanceMap( ch.pos, BArray.not( Dungeon.level.solid, null ), dist );
        for (int i = 0; i < PathFinder.distance.length; i++) {
            if (PathFinder.distance[i] < Integer.MAX_VALUE) {
                Char n = Actor.findChar( i );
                if (n == Dungeon.hero && PathFinder.distance[i] > 1)
                    continue;
                else if (n != null && !affected.contains( n )) {
                    arcs.add(new Lightning.Arc(ch.sprite.center(), n.sprite.center()));
                    arc(n);
                }
            }
        }
    }

    @Override
    public void apply(Hero hero) {
        identify();
        affected.clear();
        arcs.clear();

        for (Mob mob : hero.getVisibleEnemies()) {
            if (mob != null) {
                arcs.add(new Lightning.Arc(hero.sprite.center(), mob.sprite.center()));
                arc(mob);
            }
        }

        ArrayList<Integer> cells = new ArrayList<>();
        for (int i = 10; i > 0; i--) {
            int c = Random.Int(Dungeon.level.length());
            if (hero.fieldOfView[c] && !cells.contains(c)) {
                cells.add(c);
            }
        }

        for (int p : cells) {
            arcs.add(new Lightning.Arc(hero.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(p)));
            CellEmitter.center( p ).burst( SparkParticle.FACTORY, 3 );
        }

        hero.sprite.parent.addToFront( new Lightning( arcs, null ) );
        Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
        Camera.main.shake(2, 0.5f);

        if (!affected.isEmpty()) {
            float multiplier = 0.4f + (0.6f / affected.size());
            for (Char ch : affected) {
                if (Dungeon.level.water[ch.pos]) multiplier = 1f;
                if (!(ch instanceof NPC) && !ch.isImmune(this.getClass())) {
                    ch.damage(Math.round(ch.damageRoll() * multiplier), this);
                }
            }
        }
    }

    @Override
    public int value() {
        return isKnown() ? 50 * quantity : super.value();
    }
}
