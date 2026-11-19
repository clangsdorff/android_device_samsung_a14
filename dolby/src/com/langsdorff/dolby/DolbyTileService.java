/*
 * SPDX-FileCopyrightText: 2026 Langsdorff
 * SPDX-License-Identifier: Apache-2.0
 */

package com.langsdorff.dolby;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class DolbyTileService extends TileService {
    @Override
    public void onStartListening() {
        update();
    }

    @Override
    public void onClick() {
        DolbyController controller = DolbyController.get(this);
        controller.setEnabled(!controller.isEnabled());
        update();
    }

    private void update() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(DolbyController.get(this).isEnabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
