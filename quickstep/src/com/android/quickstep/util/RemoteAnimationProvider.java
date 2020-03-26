/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quickstep.util;

import android.animation.AnimatorSet;
import android.app.ActivityOptions;
import android.content.Context;
import android.os.Handler;

import com.android.launcher3.LauncherAnimationRunner;
import com.android.launcher3.WrappedLauncherAnimationRunner;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.RemoteAnimationAdapterCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.TransactionCompat;

public abstract class RemoteAnimationProvider {

    LauncherAnimationRunner mAnimationRunner;
    static final int Z_BOOST_BASE = 800570000;

    public abstract AnimatorSet createWindowAnimation(RemoteAnimationTargetCompat[] appTargets,
            RemoteAnimationTargetCompat[] wallpaperTargets);

    ActivityOptions toActivityOptions(Handler handler, long duration, Context context) {
        mAnimationRunner = new LauncherAnimationRunner(handler,
                false /* startAtFrontOfQueue */) {

            @Override
            public void onCreateAnimation(RemoteAnimationTargetCompat[] appTargets,
                    RemoteAnimationTargetCompat[] wallpaperTargets, AnimationResult result) {
                result.setAnimation(createWindowAnimation(appTargets, wallpaperTargets), context);
            }
        };
        final LauncherAnimationRunner wrapper = new WrappedLauncherAnimationRunner(
                mAnimationRunner, false /* startAtFrontOfQueue */);

        return ActivityOptionsCompat.makeRemoteAnimation(
                new RemoteAnimationAdapterCompat(wrapper, duration, 0));
    }

    /**
     * Prepares the given {@param targets} for a remote animation, and should be called with the
     * transaction from the first frame of animation.
     *
     * @param boostModeTargets The mode indicating which targets to boost in z-order above other
     *                         targets.
     */
    static void prepareTargetsForFirstFrame(RemoteAnimationTargetCompat[] targets,
            TransactionCompat t, int boostModeTargets) {
        for (RemoteAnimationTargetCompat target : targets) {
            t.setLayer(target.leash, getLayer(target, boostModeTargets));
            t.show(target.leash);
        }
    }

    public static int getLayer(RemoteAnimationTargetCompat target, int boostModeTarget) {
        return target.mode == boostModeTarget
                ? Z_BOOST_BASE + target.prefixOrderIndex
                : target.prefixOrderIndex;
    }

    /**
     * @return the target with the lowest opaque layer for a certain app animation, or null.
     */
    public static RemoteAnimationTargetCompat findLowestOpaqueLayerTarget(
            RemoteAnimationTargetCompat[] appTargets, int mode) {
        int lowestLayer = Integer.MAX_VALUE;
        int lowestLayerIndex = -1;
        for (int i = appTargets.length - 1; i >= 0; i--) {
            RemoteAnimationTargetCompat target = appTargets[i];
            if (target.mode == mode && !target.isTranslucent) {
                int layer = getLayer(target, mode);
                if (layer < lowestLayer) {
                    lowestLayer = layer;
                    lowestLayerIndex = i;
                }
            }
        }
        return lowestLayerIndex != -1
                ? appTargets[lowestLayerIndex]
                : null;
    }
}