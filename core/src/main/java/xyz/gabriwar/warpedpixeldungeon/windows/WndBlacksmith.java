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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.items.BrokenSeal;
import xyz.gabriwar.warpedpixeldungeon.items.EquipableItem;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class WndBlacksmith extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 180;

	private static final int GAP  = 2;

	private final Hero hero;
	private final Blacksmith.Quest.HeroProgress hp;

	public WndBlacksmith( Blacksmith troll, Hero hero ) {
		super();

		this.hero = hero;
		this.hp = Blacksmith.Quest.progressFor( hero.id() );

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle titlebar = new IconTitle();
		titlebar.icon( troll.sprite() );
		titlebar.label( Messages.titleCase( troll.name() ) );
		titlebar.setRect( 0, 0, width, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( Messages.get(this, "prompt", hp.favor), 6 );
		message.maxWidth( width );
		message.setPos(0, titlebar.bottom() + GAP);
		add( message );

		ArrayList<RedButton> buttons = new ArrayList<>();

		int pickaxeCost = hp.freePickaxe ? 0 : 250;
		RedButton pickaxe = new RedButton(Messages.get(this, "pickaxe", pickaxeCost), 6){
			@Override
			protected void onClick() {
				GameScene.show(new WndOptions(
						troll.sprite(),
						Messages.titleCase( troll.name() ),
						Messages.get(WndBlacksmith.class, "pickaxe_verify") + (pickaxeCost == 0 ? "\n\n" + Messages.get(WndBlacksmith.class, "pickaxe_free") : ""),
						Messages.get(WndBlacksmith.class, "pickaxe_yes"),
						Messages.get(WndBlacksmith.class, "pickaxe_no")
				){
					@Override
					protected void onSelect(int index) {
						if (index == 0){
							if (hp.pickaxe.doPickUp( hero )) {
								NetManager.heroLog( hero, Messages.capitalize(Messages.get(hero, "you_now_have", hp.pickaxe.name()) ));
							} else {
								Dungeon.level.drop( hp.pickaxe, hero.pos ).sprite.drop();
							}
							hp.favor -= pickaxeCost;
							hp.pickaxe = null;
							WndBlacksmith.this.hide();

							if (!Blacksmith.Quest.rewardsAvailable()){
								Notes.remove( Notes.Landmark.TROLL );
							}
						}
					}
				});
			}
		};
		pickaxe.enable(hp.pickaxe != null && hp.favor >= pickaxeCost);
		buttons.add(pickaxe);

		int reforgecost = 500 + 1000*hp.reforges;
		RedButton reforge = new RedButton(Messages.get(this, "reforge", reforgecost), 6){
			@Override
			protected void onClick() {
				GameScene.show(new WndReforge(troll, hero, WndBlacksmith.this));
			}
		};
		reforge.enable(hp.favor >= reforgecost);
		buttons.add(reforge);

		int hardenCost = 500 + 1000*hp.hardens;
		RedButton harden = new RedButton(Messages.get(this, "harden", hardenCost), 6){
			@Override
			protected void onClick() {
				GameScene.selectItem(new HardenSelector());
			}
		};
		harden.enable(hp.favor >= hardenCost);
		buttons.add(harden);

		int upgradeCost = 1000 + 1000*hp.upgrades;
		RedButton upgrade = new RedButton(Messages.get(this, "upgrade", upgradeCost), 6){
			@Override
			protected void onClick() {
				GameScene.selectItem(new UpgradeSelector());
			}
		};
		upgrade.enable(hp.favor >= upgradeCost);
		buttons.add(upgrade);

		RedButton smith = new RedButton(Messages.get(this, "smith", 2000), 6){
			@Override
			protected void onClick() {
				GameScene.show(new WndOptions(
						troll.sprite(),
						Messages.titleCase( troll.name() ),
						Messages.get(WndBlacksmith.class, "smith_verify"),
						Messages.get(WndBlacksmith.class, "smith_yes"),
						Messages.get(WndBlacksmith.class, "smith_no")
				){
					@Override
					protected void onSelect(int index) {
						if (index == 0){
							hp.favor -= 2000;
							hp.smiths++;
							WndBlacksmith.this.hide();
							GameScene.show(new WndSmith(troll, hero));
						}
					}
				});
			}
		};
		smith.enable(hp.favor >= 2000);
		buttons.add(smith);

		RedButton cashOut = new RedButton(Messages.get(this, "cashout"), 6){
			@Override
			protected void onClick() {
				GameScene.show(new WndOptions(
						troll.sprite(),
						Messages.titleCase( troll.name() ),
						Messages.get(WndBlacksmith.class, "cashout_verify", hp.favor),
						Messages.get(WndBlacksmith.class, "cashout_yes"),
						Messages.get(WndBlacksmith.class, "cashout_no")
				){
					@Override
					protected void onSelect(int index) {
						if (index == 0){
							new Gold(hp.favor).doPickUp(hero, hero.pos);
							hp.favor = 0;
							WndBlacksmith.this.hide();
						}
					}
				});
			}
		};
		cashOut.enable(hp.favor > 0);
		buttons.add(cashOut);

		float pos = message.bottom() + 3*GAP;
		for (RedButton b : buttons){
			b.leftJustify = true;
			b.multiline = true;
			b.setSize(width, b.reqHeight());
			b.setRect(0, pos, width, b.reqHeight());
			b.enable(b.active); //so that it's visually reflected
			add(b);
			pos = b.bottom() + GAP;
		}

		resize(width, (int)pos);

	}

	protected static class WndReforge extends Window {

		private static final int WIDTH		= 120;

		private static final int BTN_SIZE	= 32;
		private static final float GAP		= 2;
		private static final float BTN_GAP	= 5;

		private final Hero hero;
		private final Blacksmith.Quest.HeroProgress hp;

		private ItemButton btnPressed;

		private ItemButton btnItem1;
		private ItemButton btnItem2;
		private RedButton btnReforge;

		public WndReforge( Blacksmith troll, final Hero hero, Window wndParent ) {
			super();

			this.hero = hero;
			this.hp = Blacksmith.Quest.progressFor( hero.id() );

			IconTitle titlebar = new IconTitle();
			titlebar.icon( troll.sprite() );
			titlebar.label( Messages.titleCase( troll.name() ) );
			titlebar.setRect( 0, 0, WIDTH, 0 );
			add( titlebar );

			RenderedTextBlock message = PixelScene.renderTextBlock( Messages.get(this, "message"), 6 );
			message.maxWidth( WIDTH);
			message.setPos(0, titlebar.bottom() + GAP);
			add( message );

			btnItem1 = new ItemButton() {
				@Override
				protected void onClick() {
					btnPressed = btnItem1;
					GameScene.selectItem( itemSelector );
				}
			};
			btnItem1.setRect( (WIDTH - BTN_GAP) / 2 - BTN_SIZE, message.top() + message.height() + BTN_GAP, BTN_SIZE, BTN_SIZE );
			add( btnItem1 );

			btnItem2 = new ItemButton() {
				@Override
				protected void onClick() {
					btnPressed = btnItem2;
					GameScene.selectItem( itemSelector );
				}
			};
			btnItem2.setRect( btnItem1.right() + BTN_GAP, btnItem1.top(), BTN_SIZE, BTN_SIZE );
			add( btnItem2 );

			btnReforge = new RedButton( Messages.get(this, "reforge") ) {
				@Override
				protected void onClick() {

					Item first, second;
					if (btnItem1.item().trueLevel() >= btnItem2.item().trueLevel()) {
						first = btnItem1.item();
						second = btnItem2.item();
					} else {
						first = btnItem2.item();
						second = btnItem1.item();
					}

					Sample.INSTANCE.play( Assets.Sounds.EVOKE );
					ScrollOfUpgrade.upgrade( hero );
					Item.evoke( hero );

					if (second.isEquipped( hero )) {
						((EquipableItem)second).doUnequip( hero, false );
					}
					second.detachAll( hero.belongings.backpack );

					if (second instanceof Armor){
						BrokenSeal seal = ((Armor) second).checkSeal();
						if (seal != null){
							Dungeon.level.drop( seal, hero.pos );
						}
					} else if (second instanceof MissileWeapon){
						Buff.affect(hero, MissileWeapon.UpgradedSetTracker.class)
								.levelThresholds.put(((MissileWeapon) second).setID, Integer.MAX_VALUE);
					}

					//preserves enchant/glyphs if present
					if (first instanceof Weapon && ((Weapon) first).hasGoodEnchant()){
						((Weapon) first).upgrade(true);
					} else if (first instanceof Armor && ((Armor) first).hasGoodGlyph()){
						((Armor) first).upgrade(true);
					} else {
						first.upgrade();
					}
					Badges.validateItemLevelAquired( first );
					Item.updateQuickslot();

					hp.favor -= 500 + 1000*hp.reforges;
					hp.reforges++;

					if (!Blacksmith.Quest.rewardsAvailable()){
						Notes.remove( Notes.Landmark.TROLL );
					}

					hide();
					if (wndParent != null){
						wndParent.hide();
					}
				}
			};
			btnReforge.enable( false );
			btnReforge.setRect( 0, btnItem1.bottom() + BTN_GAP, WIDTH, 20 );
			add( btnReforge );


			resize( WIDTH, (int)btnReforge.bottom() );
		}

		protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

			@Override
			public String textPrompt() {
				return Messages.get(WndReforge.class, "prompt");
			}

			@Override
			public Class<?extends Bag> preferredBag(){
				return Belongings.Backpack.class;
			}

			@Override
			public boolean itemSelectable(Item item) {
				return item.isIdentified() && !item.cursed && item.isUpgradable();
			}

			@Override
			public void onSelect( Item item ) {
				if (item != null && btnPressed.parent != null) {
					btnPressed.item(item);

					Item item1 = btnItem1.item();
					Item item2 = btnItem2.item();

					//need 2 items
					if (item1 == null || item2 == null) {
						btnReforge.enable(false);

					//both of the same type
					} else if (item1.getClass() != item2.getClass()) {
						btnReforge.enable(false);

					//and not the literal same item
					} else if (item1 == item2) {
						btnReforge.enable(false);

					} else {
						btnReforge.enable(true);
					}
				}
			}
		};

	}

	private class HardenSelector extends WndBag.ItemSelector {

		@Override
		public String textPrompt() {
			return Messages.get(this, "prompt");
		}

		@Override
		public Class<?extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item.isUpgradable()
					&& item.isIdentified() && !item.cursed
					&& ((item instanceof Weapon && !((Weapon) item).enchantHardened)
					|| (item instanceof Armor && !((Armor) item).glyphHardened));
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) {
				if (item instanceof Weapon){
					((Weapon) item).enchantHardened = true;
				} else if (item instanceof Armor){
					((Armor) item).glyphHardened = true;
				}

				hp.favor -= 500 + 1000*hp.hardens;
				hp.hardens++;

				WndBlacksmith.this.hide();

				Sample.INSTANCE.play(Assets.Sounds.EVOKE);
				Item.evoke( hero );

				if (!Blacksmith.Quest.rewardsAvailable()){
					Notes.remove( Notes.Landmark.TROLL );
				}
			}
		}
	}

	private class UpgradeSelector extends WndBag.ItemSelector {

		@Override
		public String textPrompt() {
			return Messages.get(this, "prompt");
		}

		@Override
		public Class<?extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item.isUpgradable()
					&& item.isIdentified()
					&& !item.cursed
					&& item.level() < 2;
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) {
				item.upgrade();
				int upgradeCost = 1000 + 1000*hp.upgrades;
				hp.favor -= upgradeCost;
				hp.upgrades++;

				WndBlacksmith.this.hide();

				Sample.INSTANCE.play(Assets.Sounds.EVOKE);
				ScrollOfUpgrade.upgrade( hero );
				Item.evoke( hero );

				Badges.validateItemLevelAquired( item );

				if (!Blacksmith.Quest.rewardsAvailable()){
					Notes.remove( Notes.Landmark.TROLL );
				}

				Catalog.countUse(item.getClass());
			}
		}
	}

	public static class WndSmith extends Window {

		private static final int WIDTH      = 128;
		private static final int BTN_SIZE	= 28;
		private static final int BTN_GAP	= 4;
		private static final int GAP		= 2;

		public WndSmith( Blacksmith troll, final Hero hero ){
			super();

			final Blacksmith.Quest.HeroProgress hp = Blacksmith.Quest.progressFor( hero.id() );

			IconTitle titlebar = new IconTitle();
			titlebar.icon(troll.sprite());
			titlebar.label(Messages.titleCase(troll.name()));

			RenderedTextBlock message = PixelScene.renderTextBlock( Messages.get(this, "prompt"), 6 );

			titlebar.setRect( 0, 0, WIDTH, 0 );
			add( titlebar );

			message.maxWidth(WIDTH);
			message.setPos(0, titlebar.bottom() + GAP);
			add( message );

			if (hp.smithRewards == null || hp.smithRewards.isEmpty()){
				Blacksmith.Quest.generateRewards(hp, false);
			}

			int count = 0;
			for (Item i : hp.smithRewards){
				count++;
				ItemButton btnReward = new ItemButton(){
					@Override
					protected void onClick() {
						GameScene.show(new RewardWindow(troll, hero, item()));
					}
				};
				btnReward.item( i );
				btnReward.setRect( count*(WIDTH - BTN_GAP) / hp.smithRewards.size() - BTN_SIZE,
						message.top() + message.height() + BTN_GAP,
						BTN_SIZE, BTN_SIZE );
				add( btnReward );

			}

			resize(WIDTH, (int)message.bottom() + 2*BTN_GAP + BTN_SIZE);

		}

		@Override
		public void onBackPressed() {
			//do nothing
		}

		private class RewardWindow extends WndInfoItem {

			public RewardWindow( Blacksmith troll, final Hero hero, Item item ) {
				super(item);

				final Blacksmith.Quest.HeroProgress hp = Blacksmith.Quest.progressFor( hero.id() );

				RedButton btnConfirm = new RedButton(Messages.get(WndSadGhost.class, "confirm")){
					@Override
					protected void onClick() {
						RewardWindow.this.hide();

						if (item instanceof Weapon && hp.smithEnchant != null){
							((Weapon) item).enchant(hp.smithEnchant);
						} else if (item instanceof Armor && hp.smithGlyph != null){
							((Armor) item).inscribe(hp.smithGlyph);
						}

						item.identify(false);
						Sample.INSTANCE.play(Assets.Sounds.EVOKE);
						Item.evoke( hero );
						if (item.doPickUp( hero )) {
							NetManager.heroLog( hero, Messages.capitalize(Messages.get(hero, "you_now_have", item.name())) );
						} else {
							Dungeon.level.drop( item, hero.pos ).sprite.drop();
						}
						WndSmith.this.hide();
						hp.smithRewards = null;

						if (!Blacksmith.Quest.rewardsAvailable()){
							Notes.remove( Notes.Landmark.TROLL );
						}
					}
				};
				btnConfirm.setRect(0, height+2, width/2-1, 16);
				add(btnConfirm);

				RedButton btnCancel = new RedButton(Messages.get(WndSadGhost.class, "cancel")){
					@Override
					protected void onClick() {
						RewardWindow.this.hide();
					}
				};
				btnCancel.setRect(btnConfirm.right()+2, height+2, btnConfirm.width(), 16);
				add(btnCancel);

				resize(width, (int)btnCancel.bottom());
			}
		}

	}

}
