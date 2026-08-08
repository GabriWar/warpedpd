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

package xyz.gabriwar.warpedpixeldungeon.services.updates;

import com.watabou.noosa.Game;

//Google Play builds update through the store itself. Play policy forbids pointing
//players at downloads outside Play, so this service never checks GitHub and never
//shows an update prompt; the only thing it knows is where the listing lives.
public class PlayUpdates extends UpdateService {

	private static final String LISTING_URL =
			"https://play.google.com/store/apps/details?id=xyz.gabriwar.warpedpixeldungeon";

	@Override
	public boolean supportsUpdatePrompts() {
		return false;
	}

	@Override
	public boolean supportsBetaChannel() {
		return false;
	}

	@Override
	public void checkForUpdate(boolean useMetered, boolean includeBetas, UpdateResultCallback callback) {
		callback.onNoUpdateFound();
	}

	@Override
	public void initializeUpdate(AvailableUpdateData update) {
		//never called: no update is ever reported
	}

	@Override
	public boolean supportsReviews() {
		return false;
	}

	@Override
	public void initializeReview(ReviewResultCallback callback) {
		callback.onComplete();
	}

	@Override
	public void openReviewURI() {
		Game.platform.openURI( LISTING_URL );
	}
}
