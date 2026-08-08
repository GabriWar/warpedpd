/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.spells;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.Trinity;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.AlchemistsToolkit;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.EtherealChains;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HolyTome;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HornOfPlenty;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.MasterThievesArmband;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SandalsOfNature;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.UnstableSpellbook;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMight;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Blindweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Fadeleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Sorrowmoss;
import xyz.gabriwar.warpedpixeldungeon.plants.Stormvine;
import xyz.gabriwar.warpedpixeldungeon.plants.Swiftthistle;
import xyz.gabriwar.warpedpixeldungeon.scenes.AlchemyScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.HeroIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SpiritForm extends ClericSpell {

	public static SpiritForm INSTANCE = new SpiritForm();

	@Override
	public int icon() {
		return HeroIcon.SPIRIT_FORM;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", ringLevel(), artifactLevel()) + "\n\n" + Messages.get(this, "charge_cost", (int)chargeUse(Dungeon.hero));
	}

	@Override
	public float chargeUse(Hero hero) {
		return 4;
	}

	@Override
	public boolean canCast(Hero hero) {
		return super.canCast(hero) && hero.hasTalent(Talent.SPIRIT_FORM);
	}

	@Override
	public void onCast(HolyTome tome, Hero hero) {

		GameScene.show(new Trinity.WndItemtypeSelect(tome, this));

	}

	public static int ringLevel(){
		return Dungeon.hero.pointsInTalent(Talent.SPIRIT_FORM);
	}

	public static int artifactLevel(){
		return 2 + 2*Dungeon.hero.pointsInTalent(Talent.SPIRIT_FORM);
	}

	public static class SpiritFormBuff extends FlavourBuff{

		{
			type = buffType.POSITIVE;
		}

		public static final float DURATION = 20f;

		private Bundlable effect;

		@Override
		public int icon() {
			return BuffIndicator.TRINITY_FORM;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0, 1, 0);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}

		public void setEffect(Bundlable effect){
			this.effect = effect;
			if (effect instanceof RingOfMight){
				((Ring) effect).level(ringLevel());
				Dungeon.hero.updateHT( false );
			}
		}

		@Override
		public void detach() {
			super.detach();
			if (effect instanceof RingOfMight){
				Dungeon.hero.updateHT( false );
			}
		}

		public Ring ring(){
			if (effect instanceof Ring){
				((Ring) effect).level(ringLevel());
				return (Ring) effect;
			}
			return null;
		}

		public Artifact artifact(){
			if (effect instanceof Artifact){
				if (((Artifact) effect).visiblyUpgraded() < artifactLevel()){
					((Artifact) effect).transferUpgrade(artifactLevel() - ((Artifact) effect).visiblyUpgraded());
				}
				return (Artifact) effect;
			}
			return null;
		}

		@Override
		public String desc() {
			if (ring() != null){
				return Messages.get(this, "desc", Messages.titleCase(ring().name()), dispTurns());
			} else if (artifact() != null){
				return Messages.get(this, "desc", Messages.titleCase(artifact().name()), dispTurns());
			}
			return super.desc();
		}

		private static final String EFFECT = "effect";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(EFFECT, effect);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			effect = bundle.get(EFFECT);
		}

	}

	public static void applyActiveArtifactEffect(ClassArmor armor, Artifact effect){
		if (effect instanceof AlchemistsToolkit){
			Talent.onArtifactUsed(Dungeon.hero);
			AlchemyScene.assignToolkit((AlchemistsToolkit) effect);
			Game.switchScene(AlchemyScene.class);

		} else if (effect instanceof DriedRose){
			ArrayList<Integer> spawnPoints = new ArrayList<>();
			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
				int p = Dungeon.hero.pos + PathFinder.NEIGHBOURS8[i];
				if (Actor.findChar(p) == null && !Dungeon.level.solid[p]) {
					spawnPoints.add(p);
				}
			}
			if (!spawnPoints.isEmpty()) {
				Wraith w = Wraith.spawnAt(Random.element(spawnPoints), Wraith.class);

				w.HP = w.HT = 20 + 8*artifactLevel();
				Buff.affect(w, Corruption.class);
			}
			Talent.onArtifactUsed(Dungeon.hero);
			Dungeon.hero.spendAndNext(1f);

		} else if (effect instanceof EtherealChains){
			GameScene.selectCell(((EtherealChains) effect).caster);
			if (Dungeon.quickslot.contains(armor)) {
				QuickSlotButton.useTargeting(Dungeon.quickslot.getSlot(armor));
			}

		} else if (effect instanceof HornOfPlenty){
			((HornOfPlenty) effect).doEatEffect(Dungeon.hero, 1);

		} else if (effect instanceof MasterThievesArmband){
			GameScene.selectCell(((MasterThievesArmband) effect).targeter);
			if (Dungeon.quickslot.contains(armor)) {
				QuickSlotButton.useTargeting(Dungeon.quickslot.getSlot(armor));
			}

		} else if (effect instanceof SandalsOfNature){
			((SandalsOfNature) effect).curSeedEffect = Random.oneOf(
					Blindweed.Seed.class, Fadeleaf.Seed.class, Firebloom.Seed.class,
					Icecap.Seed.class, Sorrowmoss.Seed.class, Stormvine.Seed.class
			);

			GameScene.selectCell(((SandalsOfNature) effect).cellSelector);
			if (Dungeon.quickslot.contains(armor)) {
				QuickSlotButton.useTargeting(Dungeon.quickslot.getSlot(armor));
			}

		} else if (effect instanceof TalismanOfForesight){
			GameScene.selectCell(((TalismanOfForesight) effect).scry);

		} else if (effect instanceof TimekeepersHourglass){
			Buff.affect(Dungeon.hero, Swiftthistle.TimeBubble.class).reset(artifactLevel());
			Dungeon.hero.spendAndNext(1f);

		} else if (effect instanceof UnstableSpellbook){
			((UnstableSpellbook) effect).doReadEffect(Dungeon.hero);

		} else if (effect instanceof SkeletonKey){
			GameScene.selectCell(((SkeletonKey) effect).targeter);
		}
	}

}
