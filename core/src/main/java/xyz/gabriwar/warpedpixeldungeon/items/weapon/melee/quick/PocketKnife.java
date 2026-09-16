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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.quick;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.InventoryScroll;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class PocketKnife extends QuickWeapon {
    {
        tier = 1;
        image = ItemSpriteSheet.POCKET_KNIFE;
        hitSound = Assets.Sounds.HIT_SLASH;

        bones = false;
    }

    public static final String AC_FORGE	= "FORGE";
    boolean upgraded = false;

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (upgraded) actions.add(AC_FORGE);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_FORGE)) {
            GameScene.selectItem( itemSelector );
        }
    }

    @Override
    public int max(int lvl) {
        return  4*(tier+1) +    //base
                lvl*(tier+1);   //level scaling
    }

    @Override
    public Item upgrade() {
        this.upgraded = true;
        return super.upgrade();
    }

    @Override
    public String desc() {
        String desc = super.desc();
        if (upgraded) {
            desc += "\n\n" + Messages.get(this, "upgraded");
        }
        return desc;
    }

    protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

        @Override
        public String textPrompt() {
            return Messages.get(PocketKnife.class, "select_item");
        }

        @Override
        public Class<? extends Bag> preferredBag() {
            return Belongings.Backpack.class;
        }

        @Override
        public boolean itemSelectable(Item item) {
            return item instanceof MeleeWeapon && item.isUpgradable();
        }

        @Override
        public void onSelect( Item item ) {
            if (item == null) return;

            item.upgrade();
            Sample.INSTANCE.play( Assets.Sounds.EVOKE );
            CellEmitter.center( curUser.pos ).burst( Speck.factory( Speck.STAR ), 7 );
            curUser.sprite.operate( curUser.pos );
            PocketKnife.this.detach( curUser.belongings.backpack );
        }
    };
}
