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

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Client-side window for a host-routed interaction dialog (see {@link NetDialogs}).
 *
 * <p>The host can't show a window on a remote player's screen directly, so it
 * pushes a {@code SHOW_DIALOG} message; the client builds this window from the
 * payload and, when resolved, replies with the chosen option. Quest state lives
 * authoritatively on the host — this window only renders and reports the choice.
 */
public class WndNetDialog extends Window {

	private static final int WIDTH		= 120;
	private static final int BTN_SIZE	= 32;
	private static final int BTN_GAP	= 5;
	private static final int GAP		= 2;

	private final int dialogId;

	public WndNetDialog(int dialogId, String kind, JSONObject payload) {
		super();
		this.dialogId = dialogId;

		// Ghost/Imp/Wandmaker/Shopkeeper now reuse their real host windows (routed in
		// SpectatorReceiver.handleShowDialog), so only these kinds reach WndNetDialog.
		if (NetDialogs.KIND_PORTAL.equals(kind)) {
			buildPortal(payload);
		} else if (NetDialogs.KIND_SHOP_BUY.equals(kind)) {
			buildShopBuy(payload);
		} else if (NetDialogs.KIND_BLACKSMITH.equals(kind)) {
			buildBlacksmith(payload);
		} else {
			buildInfo(payload);
		}
	}

	// ------------------------------------------------------------------
	// FOR_SALE heap purchase confirm
	// ------------------------------------------------------------------

	private void buildShopBuy(JSONObject payload) {
		final int cell = payload.optInt("cell", -1);
		final String name = payload.optString("name", "item");
		final int price = payload.optInt("price", 0);
		final int gold = payload.optInt("gold", 0);
		final boolean canAfford = gold >= price;

		RenderedTextBlock titleBlock = PixelScene.renderTextBlock(name, 9);
		titleBlock.hardlight(TITLE_COLOR);
		titleBlock.setPos(GAP, GAP);
		titleBlock.maxWidth(WIDTH - 2 * GAP);
		add(titleBlock);

		String msg = "Price: " + price + " gold\nYour gold: " + gold;
		RenderedTextBlock msgBlock = PixelScene.renderTextBlock(msg, 6);
		msgBlock.maxWidth(WIDTH);
		msgBlock.setPos(0, titleBlock.bottom() + 2 * GAP);
		add(msgBlock);

		float pos = msgBlock.top() + msgBlock.height() + BTN_GAP;

		RedButton buyBtn = new RedButton(canAfford ? ("Buy") : ("Buy (not enough gold)")) {
			@Override
			protected void onClick() {
				if (!canAfford || cell < 0) return;
				NetManager.sendPlayerActionTyped("shop_buy", cell);
				hide();
			}
		};
		buyBtn.enable(canAfford);
		buyBtn.setRect(0, pos, WIDTH, 16);
		add(buyBtn);
		pos += 16 + GAP;

		RedButton cancel = new RedButton("Cancel") {
			@Override
			protected void onClick() { hide(); }
		};
		cancel.setRect(0, pos, WIDTH, 16);
		add(cancel);
		resize(WIDTH, (int) cancel.bottom());
	}

	private void buildPortal(JSONObject payload) {
		String stateStr = payload.optString("state", "LOCKED");
		int cost = payload.optInt("cost", 0);
		int gold = payload.optInt("gold", 0);
		int depth = payload.optInt("depth", 0);

		String title, message;
		switch (stateStr) {
			case "ACTIVE":
				title = "Portal Gate";
				message = "Channel the gate to travel to another active portal.\n\nEach trip costs 150 hunger.";
				break;
			case "DEAD":
				title = "Broken Portal";
				message = "The keeper is gone. The gate is dark and silent — its magic will not answer.";
				break;
			case "LOCKED":
			default:
				title = "Inactive Portal";
				message = "The shopkeeper eyes the gate behind them. \"Activate this gate for " + cost
						+ " gold and it'll carry you between every gate you've lit. Once paid, the magic stays with you.\"";
				break;
		}

		RenderedTextBlock titleBlock = PixelScene.renderTextBlock(title, 9);
		titleBlock.hardlight(TITLE_COLOR);
		titleBlock.setPos(GAP, GAP);
		titleBlock.maxWidth(WIDTH - 2 * GAP);
		add(titleBlock);

		RenderedTextBlock msg = PixelScene.renderTextBlock(message, 6);
		msg.maxWidth(WIDTH);
		msg.setPos(0, titleBlock.bottom() + 2 * GAP);
		add(msg);

		float pos = msg.top() + msg.height() + BTN_GAP;

		if ("ACTIVE".equals(stateStr)) {
			JSONArray dests = payload.optJSONArray("dests");
			int destCount = dests == null ? 0 : dests.length();
			if (destCount == 0) {
				RenderedTextBlock empty = PixelScene.renderTextBlock(
						"No other portals are active. Discover and unlock more.", 6);
				empty.maxWidth(WIDTH);
				empty.setPos(0, pos);
				add(empty);
				pos = empty.top() + empty.height() + BTN_GAP;
			} else {
				for (int i = 0; i < destCount; i++) {
					final int destDepth = dests.optInt(i);
					RedButton btn = new RedButton(depthLabel(destDepth)) {
						@Override
						protected void onClick() {
							NetManager.sendDialogChoice(dialogId, "travel:" + destDepth);
							hide();
						}
					};
					btn.setRect(0, pos, WIDTH, 16);
					add(btn);
					pos += 16 + GAP;
				}
			}
		} else if ("LOCKED".equals(stateStr)) {
			final boolean canAfford = gold >= cost;
			RedButton payBtn = new RedButton(canAfford
					? ("Pay " + cost + " gold")
					: ("Pay " + cost + " gold (not enough)")) {
				@Override
				protected void onClick() {
					if (!canAfford) return;
					NetManager.sendDialogChoice(dialogId, "pay");
					hide();
				}
			};
			payBtn.enable(canAfford);
			payBtn.setRect(0, pos, WIDTH, 16);
			add(payBtn);
			pos += 16 + GAP;
		}

		// Always-present Leave button
		RedButton leave = new RedButton("Leave") {
			@Override
			protected void onClick() {
				NetManager.sendDialogChoice(dialogId, "leave");
				hide();
			}
		};
		leave.setRect(0, pos, WIDTH, 16);
		add(leave);

		resize(WIDTH, (int) leave.bottom());
	}

	private static String depthLabel(int depth) {
		String region;
		switch ((depth - 1) / 5) {
			case 0: region = "Sewers"; break;
			case 1: region = "Prison"; break;
			case 2: region = "Caves";  break;
			case 3: region = "City";   break;
			case 4: region = "Halls";  break;
			default: region = "Floor"; break;
		}
		return region + " — Floor " + depth;
	}

	private void buildInfo(JSONObject payload) {
		String text = payload.optString("text", "");
		RenderedTextBlock message = PixelScene.renderTextBlock(text, 6);
		message.maxWidth(WIDTH);
		message.setPos(0, GAP);
		add(message);

		RedButton ok = new RedButton(Messages.get(WndNetDialog.class, "ok")) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		ok.setRect(0, message.top() + message.height() + BTN_GAP, WIDTH, 16);
		add(ok);

		resize(WIDTH, (int) ok.bottom());
	}

	// ------------------------------------------------------------------
	// Blacksmith — remote service menu (host applies on the acting hero)
	// ------------------------------------------------------------------

	private void buildBlacksmith(JSONObject payload) {
		final int favor = payload.optInt("favor", 0);
		int width = WIDTH;

		// Match the host's WndBlacksmith exactly: troll IconTitle + favor prompt.
		IconTitle titlebar = new IconTitle();
		titlebar.icon(new xyz.gabriwar.warpedpixeldungeon.sprites.BlacksmithSprite());
		titlebar.label(Messages.titleCase(Messages.get(
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith.class, "name")));
		titlebar.setRect(0, 0, width, 0);
		add(titlebar);

		RenderedTextBlock title = PixelScene.renderTextBlock(
				Messages.get(WndBlacksmith.class, "prompt", favor), 6);
		title.maxWidth(width);
		title.setPos(0, titlebar.bottom() + GAP);
		add(title);

		java.util.ArrayList<RedButton> buttons = new java.util.ArrayList<>();

		// pending smith pickup takes priority
		JSONArray smithPending = payload.optJSONArray("smithPending");
		if (smithPending != null && smithPending.length() > 0) {
			final JSONArray rewards = smithPending;
			for (int i = 0; i < rewards.length(); i++) {
				final int idx = i;
				final Item disp = displayItem(rewards.optJSONObject(i));
				RedButton b = new RedButton(disp != null ? disp.name() : ("#" + i), 6) {
					@Override
					protected void onClick() {
						NetManager.sendPlayerActionTyped("blacksmith_smith", idx);
						hide();
					}
				};
				buttons.add(b);
			}
		} else {
			boolean pickaxeAvail = payload.optBoolean("pickaxeAvail", false);
			final int pickaxeCost = payload.optInt("pickaxeCost", 250);
			if (pickaxeAvail) {
				RedButton b = new RedButton(Messages.get(WndBlacksmith.class, "pickaxe", pickaxeCost), 6) {
					@Override protected void onClick() { NetManager.sendPlayerActionTyped("blacksmith_pickaxe"); hide(); }
				};
				b.enable(favor >= pickaxeCost);
				buttons.add(b);
			}

			final int reforgeCost = payload.optInt("reforgeCost", 500);
			RedButton reforge = new RedButton(Messages.get(WndBlacksmith.class, "reforge", reforgeCost), 6) {
				@Override protected void onClick() { hide(); GameScene.show(new ReforgePicker()); }
			};
			reforge.enable(favor >= reforgeCost);
			buttons.add(reforge);

			final int hardenCost = payload.optInt("hardenCost", 500);
			RedButton harden = new RedButton(Messages.get(WndBlacksmith.class, "harden", hardenCost), 6) {
				@Override protected void onClick() { hide(); pickOne("blacksmith_harden", SVC_HARDEN); }
			};
			harden.enable(favor >= hardenCost);
			buttons.add(harden);

			final int upgradeCost = payload.optInt("upgradeCost", 1000);
			RedButton upgrade = new RedButton(Messages.get(WndBlacksmith.class, "upgrade", upgradeCost), 6) {
				@Override protected void onClick() { hide(); pickOne("blacksmith_upgrade", SVC_UPGRADE); }
			};
			upgrade.enable(favor >= upgradeCost);
			buttons.add(upgrade);

			final int smithCost = payload.optInt("smithCost", 2000);
			RedButton smith = new RedButton(Messages.get(WndBlacksmith.class, "smith", smithCost), 6) {
				@Override protected void onClick() { NetManager.sendPlayerActionTyped("blacksmith_smith_open"); hide(); }
			};
			smith.enable(favor >= smithCost);
			buttons.add(smith);

			RedButton cashout = new RedButton(Messages.get(WndBlacksmith.class, "cashout"), 6) {
				@Override protected void onClick() { NetManager.sendPlayerActionTyped("blacksmith_cashout"); hide(); }
			};
			cashout.enable(favor > 0);
			buttons.add(cashout);
		}

		float pos = title.bottom() + 3 * GAP;
		for (RedButton b : buttons) {
			b.leftJustify = true;
			b.multiline = true;
			b.setSize(width, b.reqHeight());
			b.setRect(0, pos, width, b.reqHeight());
			b.enable(b.active);
			add(b);
			pos = b.bottom() + GAP;
		}
		resize(width, (int) pos);
	}

	private static final int SVC_HARDEN = 1, SVC_UPGRADE = 2;

	// Pick one item from the client's OWN inventory and ship its name to the host.
	private void pickOne(final String action, final int svc) {
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override public String textPrompt() { return Messages.get(WndBlacksmith.class, "harden"); }
			@Override public Class<? extends xyz.gabriwar.warpedpixeldungeon.items.bags.Bag> preferredBag() {
				return xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings.Backpack.class;
			}
			@Override public boolean itemSelectable(Item item) { return blacksmithSelectable(item, svc); }
			@Override public void onSelect(Item item) {
				if (item != null) NetManager.sendInventoryAction(action, item.name(), -1);
			}
		});
	}

	private static boolean blacksmithSelectable(Item item, int svc) {
		if (item == null || !item.isIdentified() || item.cursed || !item.isUpgradable()) return false;
		if (svc == SVC_UPGRADE) return item.level() < 2;
		if (svc == SVC_HARDEN) {
			return (item instanceof xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon
						&& !((xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon) item).enchantHardened)
					|| (item instanceof xyz.gabriwar.warpedpixeldungeon.items.armor.Armor
						&& !((xyz.gabriwar.warpedpixeldungeon.items.armor.Armor) item).glyphHardened);
		}
		return true; // reforge: any identified upgradable
	}

	// Client reforge: pick two same-type items, ship "name1|name2".
	private class ReforgePicker extends Window {
		private ItemButton btn1, btn2;
		private RedButton confirm;
		private ItemButton pressed;
		ReforgePicker() {
			super();
			RenderedTextBlock msg = PixelScene.renderTextBlock(
					Messages.get(WndBlacksmith.WndReforge.class, "message"), 6);
			msg.maxWidth(WIDTH); msg.setPos(0, GAP); add(msg);

			btn1 = new ItemButton() { @Override protected void onClick() { pressed = btn1; pickItem(); } };
			btn1.setRect((WIDTH - BTN_GAP) / 2f - BTN_SIZE, msg.top() + msg.height() + BTN_GAP, BTN_SIZE, BTN_SIZE);
			add(btn1);
			btn2 = new ItemButton() { @Override protected void onClick() { pressed = btn2; pickItem(); } };
			btn2.setRect(btn1.right() + BTN_GAP, btn1.top(), BTN_SIZE, BTN_SIZE);
			add(btn2);

			confirm = new RedButton(Messages.get(WndBlacksmith.WndReforge.class, "reforge")) {
				@Override protected void onClick() {
					if (btn1.item() == null || btn2.item() == null) return;
					NetManager.sendInventoryAction("blacksmith_reforge",
							btn1.item().name() + "|" + btn2.item().name(), -1);
					hide();
				}
			};
			confirm.enable(false);
			confirm.setRect(0, btn1.bottom() + BTN_GAP, WIDTH, 18);
			add(confirm);
			resize(WIDTH, (int) confirm.bottom());
		}
		private void pickItem() {
			GameScene.selectItem(new WndBag.ItemSelector() {
				@Override public String textPrompt() { return Messages.get(WndBlacksmith.WndReforge.class, "prompt"); }
				@Override public Class<? extends xyz.gabriwar.warpedpixeldungeon.items.bags.Bag> preferredBag() {
					return xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings.Backpack.class;
				}
				@Override public boolean itemSelectable(Item item) { return blacksmithSelectable(item, 0); }
				@Override public void onSelect(Item item) {
					if (item != null && pressed != null) {
						pressed.item(item);
						Item i1 = btn1.item(), i2 = btn2.item();
						confirm.enable(i1 != null && i2 != null && i1 != i2 && i1.getClass() == i2.getClass());
					}
				}
			});
		}
	}

	/** Build a render-only Item from the host's display payload (img/name/desc). */
	public static Item displayItem(JSONObject data) {
		if (data == null) return null;
		final int img = data.optInt("img", 0);
		final String name = data.optString("name", "???");
		final String desc = data.optString("desc", "");
		final int lvl = data.optInt("lvl", 0);
		Item item = new Item() {
			// levelKnown so ItemSlot paints the +N badge (visiblyUpgraded gates on it).
			{ image = img; levelKnown = true; }
			@Override public String name() { return name; }
			@Override public String desc() { return desc; }
			@Override public boolean isIdentified() { return true; }
			@Override public int level() { return lvl; }
		};
		return item;
	}
}
