package org.jellyfin.androidtv.ui.playback.overlay;

import static java.lang.Math.round;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowView;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.UserSettingPreferences;
import org.jellyfin.androidtv.preference.constant.ClockBehavior;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.action.AndroidAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChannelBarChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChapterAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ClosedCaptionsAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.CustomAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.FastForwardAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.GuideAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PlayPauseAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PlaybackSpeedAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PreviousLiveTvChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.RecordAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.RewindAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SelectAudioAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SelectQualityAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SkipNextAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SkipPreviousAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ZoomAction;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.koin.java.KoinJavaComponent;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class CustomPlaybackTransportControlGlue extends PlaybackTransportControlGlue<VideoPlayerAdapter> {

    // Normal playback actions
    private PlayPauseAction playPauseAction;
    private RewindAction rewindAction;
    private FastForwardAction fastForwardAction;
    private SkipPreviousAction skipPreviousAction;
    private SkipNextAction skipNextAction;
    private SelectAudioAction selectAudioAction;
    private ClosedCaptionsAction closedCaptionsAction;
    private SelectQualityAction selectQualityAction;
    private PlaybackSpeedAction playbackSpeedAction;
    private ZoomAction zoomAction;
    private ChapterAction chapterAction;

    // TV actions
    private PreviousLiveTvChannelAction previousLiveTvChannelAction;
    private ChannelBarChannelAction channelBarChannelAction;
    private GuideAction guideAction;
    private RecordAction recordAction;

    private final PlaybackController playbackController;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;

    // Views from the (overridden) transport row layout
    private TextView mEndsText = null;
    private TextView mRemainingText = null;
    private TextView mFocusLabel = null;
    private ChapterTicksView mChapterTicks = null;
    private View mControlsDock = null;
    private View mSecondaryDock = null;
    private ViewTreeObserver.OnGlobalFocusChangeListener mFocusListener = null;

    private final Handler mHandler = new Handler();
    private Runnable mRefreshEndTime;
    private Runnable mRefreshViewVisibility;
    private final Runnable mRefreshRemaining = new Runnable() {
        @Override
        public void run() {
            updateRemainingTime();
            mHandler.postDelayed(this, 1000);
        }
    };

    private LinearLayout mButtonRef;

    CustomPlaybackTransportControlGlue(Context context, VideoPlayerAdapter playerAdapter, PlaybackController playbackController) {
        super(context, playerAdapter);
        this.playbackController = playbackController;

        mRefreshEndTime = () -> {
            setEndTime();
            if (!isPlaying()) {
                mHandler.postDelayed(mRefreshEndTime, 30000);
            }
        };

        mRefreshViewVisibility = () -> {
            if (mButtonRef != null && mEndsText != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
                mEndsText.setVisibility(mButtonRef.getVisibility());
            else
                mHandler.postDelayed(mRefreshViewVisibility, 100);
        };

        initActions(context);
    }

    @Override
    protected void onDetachedFromHost() {
        mHandler.removeCallbacks(mRefreshEndTime);
        mHandler.removeCallbacks(mRefreshViewVisibility);
        mHandler.removeCallbacks(mRefreshRemaining);

        closedCaptionsAction.removePopup();
        playbackSpeedAction.dismissPopup();
        selectAudioAction.dismissPopup();
        selectQualityAction.dismissPopup();
        zoomAction.dismissPopup();

        super.onDetachedFromHost();
    }

    @Override
    protected PlaybackRowPresenter onCreateRowPresenter() {
        final AbstractDetailsDescriptionPresenter detailsPresenter = new AbstractDetailsDescriptionPresenter() {
            @Override
            protected void onBindDescription(ViewHolder vh, Object item) {

            }
        };
        PlaybackTransportRowPresenter rowPresenter = new PlaybackTransportRowPresenter() {
            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                RowPresenter.ViewHolder vh = super.createRowViewHolder(parent);

                View root = vh.view;
                mEndsText = root.findViewById(R.id.ends_at);
                mRemainingText = root.findViewById(R.id.remaining_time);
                mFocusLabel = root.findViewById(R.id.focus_label);
                mChapterTicks = root.findViewById(R.id.chapter_ticks);
                mControlsDock = root.findViewById(androidx.leanback.R.id.controls_dock);
                mSecondaryDock = root.findViewById(androidx.leanback.R.id.secondary_controls_dock);
                mButtonRef = (LinearLayout) ((FrameLayout) mControlsDock).getChildAt(0);

                root.findViewById(androidx.leanback.R.id.playback_progress).setOnKeyListener((v, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
                        int index = primaryActionsAdapter.indexOf(playPauseAction);
                        if (mButtonRef != null && index >= 0 && index < mButtonRef.getChildCount()) {
                            mButtonRef.getChildAt(index).requestFocus();
                            return true;
                        }
                    }
                    return false;
                });

                ClockBehavior showClock = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getClockBehavior());
                if (showClock == ClockBehavior.ALWAYS || showClock == ClockBehavior.IN_VIDEO) {
                    setEndTime();
                } else {
                    mEndsText.setVisibility(View.GONE);
                    mEndsText = null;
                }

                return vh;
            }

            @Override
            protected void onProgressBarClicked(PlaybackTransportRowPresenter.ViewHolder vh) {
                CustomPlaybackTransportControlGlue controlglue = CustomPlaybackTransportControlGlue.this;
                controlglue.onActionClicked(controlglue.playPauseAction);
            }

            @Override
            protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
                super.onBindRowViewHolder(vh, item);
                vh.setOnKeyListener(CustomPlaybackTransportControlGlue.this);

                updateChapterTicks();
                mHandler.removeCallbacks(mRefreshRemaining);
                mHandler.post(mRefreshRemaining);

                mFocusListener = (oldFocus, newFocus) -> updateFocusLabel(newFocus);
                vh.view.getViewTreeObserver().addOnGlobalFocusChangeListener(mFocusListener);
            }

            @Override
            protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
                super.onUnbindRowViewHolder(vh);
                vh.setOnKeyListener(null);

                mHandler.removeCallbacks(mRefreshRemaining);
                if (mFocusListener != null) {
                    vh.view.getViewTreeObserver().removeOnGlobalFocusChangeListener(mFocusListener);
                    mFocusListener = null;
                }
            }
        };
        rowPresenter.setDescriptionPresenter(detailsPresenter);
        return rowPresenter;
    }

    private void initActions(Context context) {
        playPauseAction = new PlayPauseAction(context);
        UserSettingPreferences userSettingPreferences = KoinJavaComponent.get(UserSettingPreferences.class);
        rewindAction = new RewindAction(context);
        rewindAction.setLabels(new String[]{context.getString(R.string.lbl_skip_back, userSettingPreferences.get(UserSettingPreferences.Companion.getSkipBackLength()) / 1000)});
        fastForwardAction = new FastForwardAction(context);
        fastForwardAction.setLabels(new String[]{context.getString(R.string.lbl_skip_forward, userSettingPreferences.get(UserSettingPreferences.Companion.getSkipForwardLength()) / 1000)});
        skipPreviousAction = new SkipPreviousAction(context);
        skipNextAction = new SkipNextAction(context);
        selectAudioAction = new SelectAudioAction(context, this);
        selectAudioAction.setLabels(new String[]{context.getString(R.string.lbl_audio_track)});
        closedCaptionsAction = new ClosedCaptionsAction(context, this);
        closedCaptionsAction.setLabels(new String[]{context.getString(R.string.lbl_subtitle_track)});
        selectQualityAction = new SelectQualityAction(context, this, KoinJavaComponent.get(UserPreferences.class));
        selectQualityAction.setLabels(new String[]{context.getString(R.string.lbl_quality_profile)});
        playbackSpeedAction = new PlaybackSpeedAction(context, this, playbackController);
        playbackSpeedAction.setLabels(new String[]{context.getString(R.string.lbl_playback_speed)});
        zoomAction = new ZoomAction(context, this);
        zoomAction.setLabels(new String[]{context.getString(R.string.lbl_zoom)});
        chapterAction = new ChapterAction(context, this);
        chapterAction.setLabels(new String[]{context.getString(R.string.lbl_chapters)});

        previousLiveTvChannelAction = new PreviousLiveTvChannelAction(context, this);
        previousLiveTvChannelAction.setLabels(new String[]{context.getString(R.string.lbl_prev_item)});
        channelBarChannelAction = new ChannelBarChannelAction(context, this);
        channelBarChannelAction.setLabels(new String[]{context.getString(R.string.lbl_other_channels)});
        guideAction = new GuideAction(context, this);
        guideAction.setLabels(new String[]{context.getString(R.string.lbl_live_tv_guide)});
        recordAction = new RecordAction(context, this);
        recordAction.setLabels(new String[]{
                context.getString(R.string.lbl_record),
                context.getString(R.string.lbl_cancel_recording)
        });
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        this.primaryActionsAdapter = primaryActionsAdapter;
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        this.secondaryActionsAdapter = secondaryActionsAdapter;
    }

    void addMediaActions() {
        if (primaryActionsAdapter.size() > 0)
            primaryActionsAdapter.clear();
        if (secondaryActionsAdapter.size() > 0)
            secondaryActionsAdapter.clear();

        VideoPlayerAdapter playerAdapter = getPlayerAdapter();

        // Primary items: previous, skip back, play/pause, skip forward, next
        if (!playerAdapter.isLiveTv() && playerAdapter.hasPreviousItem()) {
            primaryActionsAdapter.add(skipPreviousAction);
        }

        if (playerAdapter.canSeek()) {
            primaryActionsAdapter.add(rewindAction);
        }

        primaryActionsAdapter.add(playPauseAction);

        if (playerAdapter.canSeek()) {
            primaryActionsAdapter.add(fastForwardAction);
        }

        if (!playerAdapter.isLiveTv() && playerAdapter.hasNextItem()) {
            primaryActionsAdapter.add(skipNextAction);
        }

        // Secondary items: everything else on the right of the row
        if (playerAdapter.isLiveTv()) {
            secondaryActionsAdapter.add(previousLiveTvChannelAction);
            secondaryActionsAdapter.add(channelBarChannelAction);
            secondaryActionsAdapter.add(guideAction);
            if (playerAdapter.canRecordLiveTv()) {
                secondaryActionsAdapter.add(recordAction);
                recordingStateChanged();
            }
        }

        if (playerAdapter.hasSubs()) {
            secondaryActionsAdapter.add(closedCaptionsAction);
        }

        if (playerAdapter.hasMultiAudio()) {
            secondaryActionsAdapter.add(selectAudioAction);
        }

        if (playerAdapter.hasChapters()) {
            secondaryActionsAdapter.add(chapterAction);
        }

        if (!playerAdapter.isLiveTv()) {
            secondaryActionsAdapter.add(playbackSpeedAction);
            secondaryActionsAdapter.add(selectQualityAction);
        }

        secondaryActionsAdapter.add(zoomAction);

        updateChapterTicks();
    }

    @Override
    public void onActionClicked(Action action) {
        if (action instanceof AndroidAction) {
            ((AndroidAction) action).onActionClicked(getPlayerAdapter());
        }
        notifyActionChanged(action);
    }

    public void onCustomActionClicked(Action action, View view) {
        // Handle custom action clicks which require a popup menu
        if (action instanceof CustomAction) {
            ((CustomAction) action).handleClickAction(playbackController, getPlayerAdapter(), getContext(), view);
        }

        if (action == playbackSpeedAction) {
            // Post a callback to calculate the new time, since Exoplayer updates this in an async fashion.
            // This is a hack, we should instead have onPlaybackParametersChanged call out to this
            // class to notify rather than poll. But communication is unidirectional at the moment:
            mHandler.postDelayed(mRefreshEndTime, 5000);  // 5 seconds
        }
    }

    private void updateChapterTicks() {
        if (mChapterTicks == null) return;
        VideoPlayerAdapter adapter = getPlayerAdapter();
        org.jellyfin.sdk.model.api.BaseItemDto item = adapter.getCurrentlyPlayingItem();
        java.util.List<org.jellyfin.sdk.model.api.ChapterInfo> chapters = item != null ? item.getChapters() : null;
        if (chapters == null || chapters.isEmpty()) {
            mChapterTicks.setChapters(new long[0], 0);
            return;
        }
        long[] positions = new long[chapters.size()];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = chapters.get(i).getStartPositionTicks() / 10000;
        }
        mChapterTicks.setChapters(positions, adapter.getDuration());
    }

    private static String formatTime(long ms) {
        long totalSeconds = Math.max(0, ms) / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long sec = totalSeconds % 60;
        return h > 0
                ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, sec)
                : String.format(java.util.Locale.US, "%d:%02d", m, sec);
    }

    private void updateRemainingTime() {
        if (mRemainingText == null) return;
        long duration = getPlayerAdapter().getDuration();
        if (duration < 1) {
            mRemainingText.setText("");
            return;
        }
        mRemainingText.setText("-" + formatTime(duration - getPlayerAdapter().getCurrentPosition()));
    }

    private void updateFocusLabel(View focused) {
        if (mFocusLabel == null) return;
        CharSequence label = null;
        View entry = focused;
        while (entry != null && entry.getParent() instanceof ViewGroup
                && ((ViewGroup) entry.getParent()).getParent() != mControlsDock
                && ((ViewGroup) entry.getParent()).getParent() != mSecondaryDock) {
            entry = (View) entry.getParent();
        }
        if (entry != null && entry.getParent() instanceof ViewGroup) {
            ViewGroup bar = (ViewGroup) entry.getParent();
            ArrayObjectAdapter adapter = null;
            if (bar.getParent() == mControlsDock) adapter = primaryActionsAdapter;
            else if (bar.getParent() == mSecondaryDock) adapter = secondaryActionsAdapter;

            int index = bar.indexOfChild(entry);
            if (adapter != null && index >= 0 && index < adapter.size() && adapter.get(index) instanceof Action) {
                Action action = (Action) adapter.get(index);
                label = action.getLabel1();
                if (label == null && action instanceof PlaybackControlsRow.MultiAction) {
                    PlaybackControlsRow.MultiAction multi = (PlaybackControlsRow.MultiAction) action;
                    try {
                        label = multi.getLabel(multi.getIndex());
                    } catch (RuntimeException ignored) {
                        // labels not set for this action
                    }
                }
            }
        }
        mFocusLabel.setText(label != null ? label : "");
    }

    private void setEndTime() {
        if (mEndsText == null || getPlayerAdapter().getDuration() < 1)
            return;
        long msLeft = getPlayerAdapter().getDuration() - getPlayerAdapter().getCurrentPosition();
        long realTimeLeft = round(msLeft / playbackController.getPlaybackSpeed());

        LocalDateTime endTime = LocalDateTime.now().plus(realTimeLeft, ChronoUnit.MILLIS);
        mEndsText.setText(getContext().getString(R.string.lbl_playback_control_ends, DateTimeExtensionsKt.getTimeFormatter(getContext()).format(endTime)));
    }

    private void notifyActionChanged(Action action) {
        ArrayObjectAdapter adapter = primaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = secondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
        }
    }

    void setInitialPlaybackDrawable() {
        playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
        notifyActionChanged(playPauseAction);
    }

    void invalidatePlaybackControls() {
        if (primaryActionsAdapter.size() > 0)
            primaryActionsAdapter.clear();
        if (secondaryActionsAdapter.size() > 0)
            secondaryActionsAdapter.clear();
        addMediaActions();
    }

    void recordingStateChanged() {
        if (getPlayerAdapter().isRecording()) {
            recordAction.setIndex(RecordAction.INDEX_RECORDING);
        } else {
            recordAction.setIndex(RecordAction.INDEX_INACTIVE);
        }
        notifyActionChanged(recordAction);
    }

    void updatePlayState() {
        playPauseAction.setIndex(isPlaying() ? PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE : PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
        notifyActionChanged(playPauseAction);
        setEndTime();
        if (!isPlaying()) {
            mHandler.removeCallbacks(mRefreshEndTime);
            mHandler.postDelayed(mRefreshEndTime, 30000);
        } else {
            mHandler.removeCallbacks(mRefreshEndTime);
        }

    }

    public void setInjectedViewsVisibility() {
        if (mButtonRef != null && mEndsText != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
            mEndsText.setVisibility(mButtonRef.getVisibility());
        mHandler.removeCallbacks(mRefreshViewVisibility);
        mHandler.postDelayed(mRefreshViewVisibility, 100);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            // The below actions are only handled on key up
            return super.onKey(v, keyCode, event);
        }

        VideoPlayerAdapter playerAdapter = getPlayerAdapter();

        if (playerAdapter.hasSubs() && keyCode == KeyEvent.KEYCODE_CAPTIONS) {
            closedCaptionsAction.handleClickAction(playbackController, getPlayerAdapter(), getContext(), v);
        }
        if (playerAdapter.hasMultiAudio() && keyCode == KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK) {
            selectAudioAction.handleClickAction(playbackController, getPlayerAdapter(), getContext(), v);
        }
        return super.onKey(v, keyCode, event);
    }
}
