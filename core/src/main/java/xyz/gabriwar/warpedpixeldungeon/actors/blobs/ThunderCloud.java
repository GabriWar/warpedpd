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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class ThunderCloud extends Blob {

    @Override
    protected void evolve() {
        super.evolve();

        int cell;

        Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
        for (int i = area.left; i < area.right; i++){
            for (int j = area.top; j < area.bottom; j++){
                cell = i + j*Dungeon.level.width();
                if (cur[cell] > 0) {
                    Dungeon.level.setCellToWater(true, cell);
                    if (fire != null){
                        fire.clear(cell);
                    }

                    if (Random.Float() < 0.10f) { //10% chance to make thunder
                        if (Dungeon.level.heroFOV[cell]) {
                            PointF cellPoint = DungeonTilemap.tileCenterToWorld(cell);

                            final float length = 6f;

                            float x = cellPoint.x;
                            float y = cellPoint.y-5f;

                            PointF from;
                            PointF to;
                            if (Random.Float() < 0.5f) {
                                from = new PointF(x-length, y-Random.Float(-length, length));
                                to = new PointF(x+length, y+Random.Float(-length, length));
                            } else {
                                from = new PointF(x-Random.Float(-length, length), y-length);
                                to = new PointF(x+Random.Float(-length, length), y+length);
                            }

                            ArrayList<Lightning.Arc> arcs = new ArrayList<>();
                            arcs.add(new Lightning.Arc(from, to));
                            Dungeon.hero.sprite.parent.add(new Lightning(arcs, null));
                            Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.7f, 1f);
                        }

                        Char ch = Actor.findChar(cell);
                        if (ch != null && !ch.isImmune(Electricity.class)) {
                            ch.damage(Random.NormalIntRange(5 + Dungeon.scalingDepth() / 4, 10 + Dungeon.scalingDepth() / 4), new Electricity()); //same with shocking dart
                        }
                    }

                    //fiery enemies take damage as if they are in toxic gas
                    Char ch = Actor.findChar(cell);
                    if (ch != null
                            && !ch.isImmune(getClass())
                            && Char.hasProp(ch, Char.Property.FIERY)){
                        ch.damage(1 + Dungeon.scalingDepth()/5, this);
                    }
                }
            }
        }
    }

    @Override
    public void use( BlobEmitter emitter ) {
        super.use( emitter );
        emitter.pour( Speck.factory( Speck.THUNDER_STORM ), 0.15f );
    }

    @Override
    public String tileDesc() {
        return Messages.get(this, "desc");
    }

}
