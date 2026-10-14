package androidx.leanback.widget;

import android.view.KeyEvent;

import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * Adapts Leanback 1.2's seek UI to independent forward/back intervals. Kept in the
 * widget package because Leanback exposes no public directional seek callback.
 * Recheck package-private ViewHolder access when upgrading Leanback.
 */
public final class DirectionalSeek {
    private DirectionalSeek() {}

    public static long previewInterval(long backward, long forward) {
        long a = Math.max(1, backward);
        long b = Math.max(1, forward);
        while (b != 0) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    public static long targetPosition(long position, long duration, long interval, boolean forward) {
        long boundedPosition = Math.max(0, Math.min(position, duration));
        long boundedInterval = Math.max(1, interval);
        return forward
                ? boundedPosition + Math.min(boundedInterval, duration - boundedPosition)
                : boundedPosition - Math.min(boundedInterval, boundedPosition);
    }

    public static void cancel(PlaybackTransportRowPresenter.ViewHolder vh) {
        if (vh.mInSeek) vh.stopSeek(true);
    }

    public static void install(PlaybackTransportRowPresenter.ViewHolder vh,
                               LongSupplier backwardInterval, LongSupplier forwardInterval) {
        vh.mProgressBar.setOnKeyListener((view, keyCode, event) -> {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return vh.mInSeek;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_MINUS:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_PLUS:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        boolean forward = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                                || keyCode == KeyEvent.KEYCODE_PLUS
                                || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
                        seek(vh, forward, forward ? forwardInterval.getAsLong() : backwardInterval.getAsLong());
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (!vh.mInSeek) return false;
                    if (event.getAction() == KeyEvent.ACTION_UP) vh.stopSeek(false);
                    return true;
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    if (!vh.mInSeek) return false;
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        vh.stopSeek(!vh.mProgressBar.isAccessibilityFocused());
                    }
                    return true;
                default:
                    return false;
            }
        });
        vh.mProgressBar.setAccessibilitySeekListener(new SeekBar.AccessibilitySeekListener() {
            @Override
            public boolean onAccessibilitySeekForward() {
                return seek(vh, true, forwardInterval.getAsLong());
            }

            @Override
            public boolean onAccessibilitySeekBackward() {
                return seek(vh, false, backwardInterval.getAsLong());
            }
        });
    }

    private static boolean seek(PlaybackTransportRowPresenter.ViewHolder vh, boolean forward, long interval) {
        if (!vh.startSeek()) return false;
        long target = targetPosition(vh.mCurrentTimeInMs, vh.mTotalTimeInMs, interval, forward);
        if (vh.mPositionsLength > 0) {
            int index = Arrays.binarySearch(vh.mPositions, 0, vh.mPositionsLength, target);
            if (index < 0) index = Math.min(-1 - index, vh.mPositionsLength - 1);
            vh.updateThumbsInSeek(index, forward);
        }
        vh.mProgressBar.setProgress((int) ((double) target / vh.mTotalTimeInMs * Integer.MAX_VALUE));
        vh.mSeekClient.onSeekPositionChanged(target);
        return true;
    }
}
