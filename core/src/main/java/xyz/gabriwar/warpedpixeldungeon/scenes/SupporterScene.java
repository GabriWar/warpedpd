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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.services.payments.PaymentService;
import xyz.gabriwar.warpedpixeldungeon.services.payments.Payments;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.ArrayList;

//the supporter page: what supporting gives back, and the ways to give. Store builds
//(Google Play) offer in-app donation tiers plus a secure purchase restore; every other
//build links Ko-fi. The perks are honored across both.
public class SupporterScene extends PixelScene {

	private static final int BTN_HEIGHT = 18;
	private static final int GAP = 2;

	@Override
	public void create() {
		super.create();

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		int elementWidth = PixelScene.landscape() ? 202 : 130;

		TitleBackground BG = new TitleBackground(w, h);
		add(BG);

		w -= insets.right + insets.left;
		h -= insets.top + insets.bottom;

		ExitButton btnExit = new ExitButton();
		btnExit.setPos(insets.left + w - btnExit.width(), insets.top);
		add(btnExit);

		IconTitle title = new IconTitle(Icons.GOLD.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f
		);
		align(title);
		add(title);

		PerksCard perks = new PerksCard();
		perks.setSize(elementWidth, 0);
		add(perks);

		//a store connection that failed at launch gets another go when the page opens
		Payments.reconnect();

		//ways to give: store tiers + restore when a provider is up, Ko-fi otherwise
		ArrayList<Component> waysToGive = new ArrayList<>();
		float btnsHeight = 0;
		if (Payments.supportsDonations()) {
			for (PaymentService.Tier tier : Payments.tiers()) {
				StyledButton b = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
						tier.name + " - _" + tier.price + "_/mo", 7) {
					@Override
					protected void onClick() {
						super.onClick();
						Payments.donate(tier.id, (success, message) -> {
							if (success) {
								if (icon() != null) {
									new Flare(6, 48).color(0xFFDD44, true).show(icon(), 2f);
								}
								GLog.p(Messages.get(SupporterScene.class, "donate_thanks"));
							} else if (message != null) {
								GLog.w(Messages.get(SupporterScene.class, "donate_fail"));
							}
						});
					}
				};
				b.icon(Icons.get(Icons.GOLD));
				b.textColor(Window.TITLE_COLOR);
				b.setSize(elementWidth, BTN_HEIGHT);
				add(b);
				waysToGive.add(b);
				btnsHeight += BTN_HEIGHT + GAP;
			}

			StyledButton restore = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
					Messages.get(this, "restore"), 7) {
				@Override
				protected void onClick() {
					super.onClick();
					Payments.restore((success, message) -> {
						if (success) {
							GLog.p(Messages.get(SupporterScene.class, "restore_done"));
						} else {
							GLog.w(Messages.get(SupporterScene.class, "restore_none"));
						}
					});
				}
			};
			restore.setSize(elementWidth, 14);
			add(restore);
			waysToGive.add(restore);
			btnsHeight += 14 + GAP;

			//Play (and Apple) require the renewal terms and the cancel path to be spelled
			//out on the screen that sells the subscription, not just in the store sheet
			RenderedTextBlock subTerms = PixelScene.renderTextBlock(
					Messages.get(this, "sub_terms", Payments.storeName()), 6);
			subTerms.maxWidth(elementWidth);
			add(subTerms);
			waysToGive.add(subTerms);
			btnsHeight += subTerms.height() + GAP;

			StyledButton manage = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
					Messages.get(this, "manage"), 7) {
				@Override
				protected void onClick() {
					super.onClick();
					WarpedPixelDungeon.platform.openURI(Payments.manageSubscriptionsLink());
				}
			};
			manage.setSize(elementWidth, 14);
			add(manage);
			waysToGive.add(manage);
			btnsHeight += 14 + GAP;

			//the tiers are auto-renewing subscriptions, so the purchase screen itself has
			//to link the policy and the terms - a store listing link isn't enough
			LegalLinks legal = new LegalLinks();
			legal.setSize(elementWidth, 11);
			add(legal);
			waysToGive.add(legal);
			btnsHeight += 11 + GAP;
		} else if (Payments.service != null) {
			//a store build whose billing isn't ready (not connected, or products not yet
			//live). Play policy forbids linking external payment pages from store builds,
			//so no Ko-fi here - just say the store is unavailable right now.
			RenderedTextBlock unavailable = PixelScene.renderTextBlock(
					Messages.get(this, "store_unavailable"), 6);
			unavailable.maxWidth(elementWidth);
			add(unavailable);
			waysToGive.add(unavailable);
			btnsHeight = unavailable.height() + GAP;
		} else {
			StyledButton kofi = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
					Messages.get(this, "supporter_link")) {
				@Override
				protected void onClick() {
					super.onClick();
					String link = Payments.KOFI_LINK
							+ "?utm_source=warpedpd&utm_medium=supporter_page&utm_campaign=ingame_link";
					WarpedPixelDungeon.platform.openURI(link);
				}
			};
			kofi.icon(Icons.get(Icons.GOLD));
			kofi.textColor(Window.TITLE_COLOR);
			kofi.setSize(elementWidth, BTN_HEIGHT);
			add(kofi);
			waysToGive.add(kofi);
			btnsHeight = BTN_HEIGHT + GAP;
		}

		float elementHeight = perks.height() + btnsHeight;

		float top = insets.top + 16 + (h - 16 - elementHeight) / 2f;
		float left = insets.left + (w - elementWidth) / 2f;

		perks.setPos(left, top);
		align(perks);

		float btnY = perks.bottom() + GAP;
		for (Component c : waysToGive) {
			c.setPos(left, btnY);
			align(c);
			btnY += c.height() + GAP;
		}
	}

	@Override
	protected void onBackPressed() {
		WarpedPixelDungeon.switchNoFade(TitleScene.class);
	}

	//the pitch and the perk list, on one card
	private static class PerksCard extends Component {

		NinePatch bg;
		RenderedTextBlock intro;
		Image[] perkIcons;
		RenderedTextBlock[] perkTexts;
		RenderedTextBlock outro;

		private static final Icons[] ICONS = {
				Icons.TALENT, Icons.CHANGES, Icons.WARNING, Icons.PREFS
		};
		private static final String[] KEYS = {
				"perk_vote", "perk_updates", "perk_bugs", "perk_design"
		};

		@Override
		protected void createChildren() {
			bg = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			add(bg);

			intro = PixelScene.renderTextBlock(Messages.get(SupporterScene.class, "intro"), 6);
			add(intro);

			perkIcons = new Image[ICONS.length];
			perkTexts = new RenderedTextBlock[ICONS.length];
			for (int i = 0; i < ICONS.length; i++) {
				perkIcons[i] = Icons.get(ICONS[i]);
				add(perkIcons[i]);
				perkTexts[i] = PixelScene.renderTextBlock(
						Messages.get(SupporterScene.class, KEYS[i]), 6);
				add(perkTexts[i]);
			}

			String sig = WPDSettings.supporter()
					? Messages.get(SupporterScene.class, "already_supporter")
					: Messages.get(SupporterScene.class, "outro");
			outro = PixelScene.renderTextBlock(sig + "\n\n- GabriWar", 6);
			add(outro);
		}

		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;

			float inLeft = x + bg.marginLeft();
			float textWidth = width - bg.marginHor();

			intro.maxWidth((int) textWidth);
			intro.setPos(inLeft, y + bg.marginTop() + 1);

			float pos = intro.bottom() + 4;
			for (int i = 0; i < perkIcons.length; i++) {
				perkTexts[i].maxWidth((int) (textWidth - 14));
				perkTexts[i].setPos(inLeft + 14, pos);
				perkIcons[i].x = inLeft + (12 - perkIcons[i].width()) / 2f;
				perkIcons[i].y = pos + (perkTexts[i].height() - perkIcons[i].height()) / 2f;
				PixelScene.align(perkIcons[i]);
				pos = perkTexts[i].bottom() + 3;
			}

			outro.maxWidth((int) textWidth);
			outro.setPos(inLeft, pos + 2);

			height = (outro.bottom() + bg.marginBottom() + 1) - y;
			bg.size(width, height);
		}
	}

	//policy + terms, side by side on one line. Apple requires both to be reachable from
	//the screen that sells the subscription, so this rides along with the tier buttons.
	private static class LegalLinks extends Component {

		private StyledButton privacy;
		private StyledButton terms;

		@Override
		protected void createChildren() {
			super.createChildren();

			privacy = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
					Messages.get(SupporterScene.class, "privacy"), 6) {
				@Override
				protected void onClick() {
					super.onClick();
					WarpedPixelDungeon.platform.openURI(Payments.PRIVACY_LINK);
				}
			};
			add(privacy);

			terms = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
					Messages.get(SupporterScene.class, "terms"), 6) {
				@Override
				protected void onClick() {
					super.onClick();
					WarpedPixelDungeon.platform.openURI(Payments.TERMS_LINK);
				}
			};
			add(terms);
		}

		@Override
		protected void layout() {
			super.layout();

			float half = (width - GAP) / 2f;
			privacy.setRect(x, y, half, height);
			PixelScene.align(privacy);
			terms.setRect(x + half + GAP, y, half, height);
			PixelScene.align(terms);
		}
	}
}
