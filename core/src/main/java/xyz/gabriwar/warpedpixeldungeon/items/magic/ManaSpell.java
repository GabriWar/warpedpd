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

package xyz.gabriwar.warpedpixeldungeon.items.magic;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicImmune;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicSurge;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public abstract class ManaSpell extends Item {

	// Magic family types for the surge bonus system
	public enum MagicFamily {
		GENERAL, FIRE, COLD, LIT, ENERGY, STATUS
	}

	public int mpCost = 1;
	public int minLevel = 1;
	public MagicFamily magicType = MagicFamily.GENERAL;
	public boolean selfCast = false;
	public int spellNum = -1;

	public static Hero curUser;
	//the spell awaiting a target cell — set by castWithTarget, read by the caster
	//listener. curItem stays the Spellbook, so the spell must be tracked separately.
	public static ManaSpell curSpell;

	{
		stackable = false;
		unique = true;
		image = ItemSpriteSheet.JOURNAL_PAGE;
	}

	// Check if the hero's magic surge buff matches this spell's family for bonus
	protected boolean checkFam(Hero hero) {
		MagicSurge surge = hero.buff(MagicSurge.class);
		if (surge != null && magicType != MagicFamily.GENERAL) {
			return surge.buffFlavor() == magicType;
		}
		return false;
	}

	// Get the spell's effective level based on hero's magicLevel
	//the blast/storm tiers scale off a smaller divisor - that is what made them stronger than
	//the plain bolts. Unifying everything on /5 had collapsed the tiers into each other.
	protected int magicDivisor() {
		return 5;
	}

	protected int spellLevel() {
		return Math.max(1, Math.round(curUser.magicLevel / (float) magicDivisor()));
	}

	// Cast the spell — called by Spellbook, not directly as an item action
	public void cast(Hero hero, int cell) {
		curUser = hero;

		if (hero.buff(MagicImmune.class) != null) {
			GLog.w(Messages.get(this, "no_magic"));
			return;
		}

		if (hero.MP < mpCost) {
			GLog.w(Messages.get(ManaSpell.class, "no_mana"));
			return;
		}

		hero.MP -= mpCost;
		hero.busy();
		hero.sprite.operate(hero.pos);

		if (selfCast) {
			fx(hero.pos, new Callback() {
				@Override
				public void call() {
					onZap(hero.pos);
					hero.spendAndNext(1f);
				}
			});
		} else {
			fx(cell, new Callback() {
				@Override
				public void call() {
					onZap(cell);
					hero.spendAndNext(1f);
				}
			});
		}
	}

	// Request a target cell and then cast
	public void castWithTarget(Hero hero) {
		curUser = hero;

		if (hero.buff(MagicImmune.class) != null) {
			GLog.w(Messages.get(this, "no_magic"));
			return;
		}

		if (hero.MP < mpCost) {
			GLog.w(Messages.get(ManaSpell.class, "no_mana"));
			return;
		}

		if (selfCast) {
			cast(hero, hero.pos);
		} else {
			curSpell = this;
			GameScene.selectCell(caster);
		}
	}

	protected abstract void onZap(int cell);

	protected void fx(int cell, Callback callback) {
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
		callback.call();
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {
		GLog.p(Messages.get(this, "found"));
		return super.doPickUp(hero, pos);
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 10 * quantity;
	}

	protected static CellSelector.Listener caster = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer target) {
			if (target != null && curUser != null && curSpell != null) {
				curSpell.cast(curUser, target);
			}
			curSpell = null;
		}

		@Override
		public String prompt() {
			return Messages.get(ManaSpell.class, "prompt");
		}
	};
}
