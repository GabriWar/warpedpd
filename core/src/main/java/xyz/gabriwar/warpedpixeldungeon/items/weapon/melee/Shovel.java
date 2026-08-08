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

import static xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
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
import com.watabou.noosa.audio.Sample;
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

    //Re-ARranged weapon: no Duelist ability was ever designed for it
    @Override
    public boolean hasDuelistAbility() {
        return false;
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
        Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 2, 1.1f);
        curUser.sprite.operate(curUser.pos);
    }
}
