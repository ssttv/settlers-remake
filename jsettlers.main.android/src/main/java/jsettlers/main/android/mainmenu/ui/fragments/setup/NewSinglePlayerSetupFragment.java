/*
 * Copyright (c) 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package jsettlers.main.android.mainmenu.ui.fragments.setup;

import org.androidannotations.annotations.EFragment;

import jsettlers.common.menu.IMapDefinition;
import jsettlers.main.android.R;
import jsettlers.main.android.mainmenu.factories.PresenterFactory;
import jsettlers.main.android.mainmenu.presenters.setup.NewSinglePlayerSetupPresenter;
import jsettlers.main.android.mainmenu.views.NewSinglePlayerSetupView;

import android.support.v4.app.Fragment;

@EFragment(R.layout.fragment_new_single_player_setup)
public class NewSinglePlayerSetupFragment extends MapSetupFragment<NewSinglePlayerSetupPresenter> implements NewSinglePlayerSetupView {
	public static Fragment create(IMapDefinition mapDefinition) {
		return NewSinglePlayerSetupFragment_.builder().mapId(mapDefinition.getMapId()).build();
	}

	@Override
	protected NewSinglePlayerSetupPresenter createPresenter() {
		return PresenterFactory.createNewSinglePlayerSetupPresenter(getActivity(), this, mapId);
	}
}
