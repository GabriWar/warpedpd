package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles;

import xyz.gabriwar.warpedpixeldungeon.items.Item;

public abstract class DisposableMissileWeapon extends MissileWeapon {
    @Override
    public Item split(int amount) {
        return this;
    }
}
