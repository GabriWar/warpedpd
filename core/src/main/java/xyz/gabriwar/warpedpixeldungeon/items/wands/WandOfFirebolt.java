/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.ColorMath;
import com.watabou.utils.Random;

public class WandOfFirebolt extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_FIREBOLT;

		collisionProperties = Ballistica.STOP_TARGET | Ballistica.STOP_SOLID;
	}

	@Override
	public int min(int lvl) {
		return 1 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 8 + lvl * lvl;
	}

	@Override
	public void onZap(Ballistica bolt) {

		for (int i = 1; i < bolt.dist; i++) {
			int c = bolt.path.get(i);
			if (Dungeon.level.flamable[c]) {
				GameScene.add(Blob.seed(c, 1, Fire.class));
			}
		}

		GameScene.add(Blob.seed(bolt.collisionPos, 1, Fire.class));

		Char ch = Actor.findChar(bolt.collisionPos);
		if (ch != null) {
			int dmg = damageRoll();
			ch.damage(dmg, this);
			Buff.affect(ch, Burning.class).reignite(ch);
			ch.sprite.emitter().burst(FlameParticle.FACTORY, 5);
			wandProc(ch, chargesPerCast());

			if (!curUser.isAlive()) {
				Dungeon.fail(this);
			}
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(
				curUser.sprite.parent,
				MagicMissile.FIRE,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		int level = Math.max(0, buffedLvl());
		float procChance = (level + 1f) / (level + 3f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {
			Buff.affect(defender, Burning.class).reignite(defender);
		}
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(ColorMath.random(0xFF7700, 0xFFCC00));
		particle.am = 0.6f;
		particle.setLifespan(0.7f);
		particle.acc.set(0, -40);
		particle.setSize(0.5f, 3f);
		particle.shuffleXY(1.5f);
	}
}
