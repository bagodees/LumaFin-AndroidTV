package org.jellyfin.androidtv.ui.presentation

import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.util.safeSeriesArtwork
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.androidtv.util.hideEpisodeArtwork
import org.jellyfin.sdk.model.api.BaseItemDto
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.doOnLayout
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.ui.DetailRowView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemdetail.MyDetailsOverviewRow
import org.jellyfin.androidtv.ui.playback.PlaybackTrackSelection
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.util.trackLabels
import org.jellyfin.androidtv.util.audioTrackSummary
import org.jellyfin.androidtv.ui.itemdetail.DetailsMenu
import org.jellyfin.androidtv.ui.base.dialog.LumaFinMenuDialog
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.koin.core.context.GlobalContext
import org.jellyfin.androidtv.util.InfoLayoutHelper
import org.jellyfin.androidtv.util.MarkdownRenderer
import org.jellyfin.sdk.model.api.BaseItemKind
import androidx.lifecycle.findViewTreeLifecycleOwner
import org.jellyfin.androidtv.ui.playback.SubtitleLookupDialog
import org.jellyfin.androidtv.ui.playback.canGetMoreSubtitles
import org.jellyfin.androidtv.ui.playback.PlaybackChoiceRecorder
import org.jellyfin.androidtv.ui.playback.PlaybackChoiceScopeDialog
import org.jellyfin.androidtv.ui.playback.rememberedTracks
import android.content.DialogInterface
import org.jellyfin.androidtv.ui.itemdetail.showPersonOverviewLayout
import org.jellyfin.androidtv.ui.TextUnderButton
import org.jellyfin.androidtv.ui.itemdetail.applyDetailsTrackStyle
import org.jellyfin.androidtv.ui.itemdetail.setDetailsTrackValue
import org.jellyfin.androidtv.ui.itemdetail.showMediaOverviewLayout
import org.jellyfin.androidtv.ui.browsing.composable.inforow.BaseItemInfoRowView
import android.util.TypedValue
import org.jellyfin.androidtv.util.dp
import org.jellyfin.androidtv.ui.home.SideMenuFrame
import org.jellyfin.androidtv.ui.itemdetail.showLumaFinOverviewLayout
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.sdk.model.api.ImageType
import android.view.Gravity
import android.widget.LinearLayout

class MyDetailsOverviewRowPresenter(
	private val markdownRenderer: MarkdownRenderer,
) : RowPresenter() {
	class ViewHolder(
		private val detailRowView: DetailRowView,
		private val markdownRenderer: MarkdownRenderer,
	) : RowPresenter.ViewHolder(detailRowView) {
		private val binding get() = detailRowView.binding
		private var currentRow: MyDetailsOverviewRow? = null
		private var rowStyle = false
		private var lookupDialog: SubtitleLookupDialog? = null
		private var choicesDialog: DialogInterface? = null
		private var trackMenu: LumaFinMenuDialog? = null
		fun dismissLookup() {
			trackMenu?.dismiss(); trackMenu = null
			lookupDialog?.dismiss(); lookupDialog = null
			choicesDialog?.dismiss(); choicesDialog = null
		}

		private fun showBiography(row: MyDetailsOverviewRow, anchor: View) {
			val owner = view.findViewTreeLifecycleOwner() ?: return
			if (currentRow !== row) return
			choicesDialog?.dismiss()
			choicesDialog = LumaFinMenuDialog(view.context, owner, row.item.name.orEmpty(), fitContent = true).apply {
				body(markdownRenderer.toMarkdownSpanned(row.summary.orEmpty()))
				button(view.context.getString(android.R.string.ok), primary = true) { dismiss() }
				onDismiss { choicesDialog = null; if (anchor.isAttachedToWindow) anchor.requestFocus() }
				show()
			}
		}

		private fun DetailsMenu.choicesItem(row: MyDetailsOverviewRow, refresh: () -> Unit) {
			if (!PlaybackChoiceScopeDialog.available(row.item)) return
			item(PlaybackChoiceScopeDialog.menuLabel(view.context, row.item), navigates = true) {
				choicesDialog?.dismiss()
				val owner = view.findViewTreeLifecycleOwner() ?: return@item
				choicesDialog = PlaybackChoiceScopeDialog.showModern(view.context, owner, row.item, onChanged = refresh, onDismiss = { choicesDialog = null })
			}
		}

		fun setItem(row: MyDetailsOverviewRow) {
			if (currentRow !== row) dismissLookup()
			currentRow = row
			val item = row.item
			val hasThumbnail = row.thumbnailImage != null
			rowStyle = SideMenuFrame.isEnabled() && item.type in listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.SERIES, BaseItemKind.SEASON)
			// High contrast (#23): the secondary info values are normally dimmed.
			val highContrast = GlobalContext.get().get<org.jellyfin.androidtv.preference.UserPreferences>()[org.jellyfin.androidtv.preference.UserPreferences.highContrast]
			listOf(binding.infoValue1, binding.infoValue2, binding.infoValue3).forEach { it.alpha = if (highContrast) 1f else 0.6f }

			if (item.type == BaseItemKind.EPISODE && item.seriesName != null) {
				setTitle(item.seriesName)
				binding.fdSubtitle.text = item.name
				binding.fdSubtitle.isVisible = true
			} else {
				setTitle(item.name)
				binding.fdSubtitle.isVisible = false
			}

			InfoLayoutHelper.addInfoRow(view.context, item, item.mediaSources?.getOrNull(row.selectedMediaSourceIndex), binding.fdMainInfoRow, hasThumbnail)
			val splitDetails = item.type in listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.SERIES, BaseItemKind.SEASON) &&
				item.mediaSources?.getOrNull(row.selectedMediaSourceIndex) != null
			for (index in 0 until binding.fdMainInfoRow.childCount) {
				(binding.fdMainInfoRow.getChildAt(index) as? BaseItemInfoRowView)?.detailsLayout = splitDetails
			}
			binding.fdMainInfoRow.layoutParams.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
				if (splitDetails) 44f else 20f, view.resources.displayMetrics).toInt() + if (splitDetails) 4.dp(view.context) else 0

			val genres = item.genres?.joinToString(" / ")
			if (hasThumbnail) {
				// The thumbnail takes the place of the left info column, so fold those details into one line
				val credits = listOfNotNull(
					row.infoItem1?.takeIf { !it.label.isNullOrBlank() }?.let { "${it.label} ${it.value}" },
					row.infoItem3?.takeIf { !it.label.isNullOrBlank() }?.let { "${it.label} ${it.value}" },
				).joinToString("  •  ")
				binding.fdGenreRow.text = listOf(genres.orEmpty(), credits)
					.filter { it.isNotBlank() }
					.joinToString("\n")
			} else {
				binding.fdGenreRow.text = genres
			}

			// Keep the left column laid out (but invisible) so the summary keeps its height
			val infoVisibility = if (hasThumbnail) View.INVISIBLE else View.VISIBLE
			listOf(binding.infoTitle1, binding.infoValue1, binding.infoTitle2, binding.infoValue2, binding.infoTitle3, binding.infoValue3)
				.forEach { it.visibility = infoVisibility }

			binding.infoTitle1.text = row.infoItem1?.label
			binding.infoValue1.text = row.infoItem1?.value

			binding.infoTitle2.text = row.infoItem2?.label
			binding.infoValue2.text = row.infoItem2?.value

			binding.infoTitle3.text = row.infoItem3?.label
			binding.infoValue3.text = row.infoItem3?.value

			binding.fdThumb.isVisible = hasThumbnail
			if (hasThumbnail) setEpisodeArtwork(item, row)
			binding.guideMainStart.setGuidelineBegin(
				(view.resources.displayMetrics.density * if (hasThumbnail) 300 else 170).toInt()
			)
			binding.fdThumb.clipToOutline = true

			if (!hasThumbnail) binding.mainImage.load(row.imageDrawable, null, null, 1.0, 0)

			bindTrackChips(row, hasThumbnail || rowStyle)

			setSummary(row.summary)

			if (row.item.type == BaseItemKind.PERSON) {
				binding.fdSummaryText.maxLines = 9
				binding.fdGenreRow.isVisible = false
			}

			binding.fdButtonRow.removeAllViews()
			for (button in row.actions) {
				val parent = button.parent
				if (parent is ViewGroup) parent.removeView(button)

				if (rowStyle) button.applyDetailsRowStyle() else button.applyDetailsStyle()
				binding.fdButtonRow.addView(button)
			}
			if (rowStyle) {
				// The track pickers join the end of the button row
				for ((index, chip) in listOf(binding.fdAudioChip, binding.fdSubtitleChip).withIndex()) {
					(chip.parent as? ViewGroup)?.removeView(chip)
					binding.fdButtonRow.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
						gravity = Gravity.CENTER_VERTICAL
						if (index == 0) marginStart = 12.dp(view.context)
					})
				}
				val logo = item.itemImages[ImageType.LOGO] ?: item.parentImages[ImageType.LOGO]
				if (logo != null) {
					binding.mainImage.background = null
					binding.mainImage.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
					binding.mainImage.layoutParams = binding.mainImage.layoutParams.apply {
						width = ViewGroup.LayoutParams.MATCH_PARENT
						height = ViewGroup.LayoutParams.MATCH_PARENT
					}
					binding.mainImage.load(logo.getUrl(GlobalContext.get().get<ApiClient>(), maxHeight = 240), null, null, 1.0, 0)
				}
				detailRowView.showLumaFinOverviewLayout(hasLogo = logo != null)
			} else if (item.type in listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.SERIES, BaseItemKind.SEASON)) {
				detailRowView.showMediaOverviewLayout(hasThumbnail, row.imageDrawable?.contains("/Logo", ignoreCase = true) == true)
			}
			if (item.type == BaseItemKind.PERSON) {
				detailRowView.showPersonOverviewLayout()
				binding.fdMainInfoRow.isVisible = item.premiereDate != null
				binding.fdThumb.load(row.imageDrawable, null, ContextCompat.getDrawable(view.context, R.drawable.tile_port_person), 2.0 / 3.0, 0)
				if (row.summary.isNullOrBlank()) {
					binding.fdSummaryText.setText(R.string.person_biography_missing)
				} else {
					binding.fdSummaryText.setOnClickListener { anchor -> showBiography(row, anchor) }
					binding.fdButtonRow.addView(TextUnderButton.create(view.context, R.drawable.ic_info,
						label = view.context.getString(R.string.person_biography),
						onClickListener = { anchor -> showBiography(row, anchor) }).apply { applyDetailsStyle() })
				}
			}
            binding.fdButtonRow.doOnLayout { container ->
                if (rowStyle) {
                    // Pills differ in width, so fit by the average width after the track pickers take their space
                    val gap = 10.dp(view.context)
                    val pickers = listOf(binding.fdAudioChip, binding.fdSubtitleChip).filter { it.isVisible }
                        .sumOf { it.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); it.measuredWidth + gap } + if (binding.fdAudioChip.isVisible) 12.dp(view.context) else 0
                    val average = row.actions.map { button ->
                        button.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                        button.measuredWidth + gap
                    }.average()
                    // Posted so the visibility changes get their own layout pass instead of landing mid-layout
                    val capacity = ((container.width - pickers) / average).toInt().coerceIn(2, 7)
                    if (average > 0 && container.width > 0) container.post { if (currentRow === row) row.onActionRowLayout?.invoke(capacity) }
                    return@doOnLayout
                }
                val widest = row.actions.maxOfOrNull { button ->
                    val margins = button.layoutParams as? ViewGroup.MarginLayoutParams
                    button.measuredWidth + (margins?.leftMargin ?: 0) + (margins?.rightMargin ?: 0)
                } ?: 0
                if (widest > 0 && container.width > 0) row.onActionRowLayout?.invoke((container.width / widest).coerceIn(2, 7))
            }
		}

		fun setEpisodeArtwork(item: BaseItemDto, row: MyDetailsOverviewRow) {
			val hidden = hideEpisodeArtwork(item)
			val safeImage = if (hidden) safeSeriesArtwork(item) else null
			val safeUrl = safeImage?.getUrl(GlobalContext.get().get<ApiClient>())
			binding.fdThumb.load(if (hidden) safeUrl else row.thumbnailImage, null,
				if (hidden) ContextCompat.getDrawable(view.context, R.drawable.ic_tv) else null, 1.0, 0)
			binding.mainImage.load(if (hidden) null else row.imageDrawable, null, null, 1.0, 0)
		}

		private fun bindTrackChips(row: MyDetailsOverviewRow, enabled: Boolean, refreshedItem: BaseItemDto? = null) {
			val item = refreshedItem ?: row.item
			val streams = item.mediaSources?.getOrNull(row.selectedMediaSourceIndex)?.mediaStreams.orEmpty()
			val audioStreams = streams.filter { it.type == MediaStreamType.AUDIO }
			val subtitleStreams = streams.filter { it.type == MediaStreamType.SUBTITLE }
			val source = item.mediaSources?.getOrNull(row.selectedMediaSourceIndex)

			val lookupAllowed = canGetMoreSubtitles(item) && !source?.id.isNullOrBlank()
			binding.fdTrackColumn.isVisible = !rowStyle && enabled && (audioStreams.isNotEmpty() || subtitleStreams.isNotEmpty() || lookupAllowed)
			// In the row style the pickers live in the button row, so they hide themselves
			binding.fdAudioChip.isVisible = audioStreams.isNotEmpty() && (!rowStyle || source != null)
			binding.fdSubtitleChip.isVisible = (subtitleStreams.isNotEmpty() || lookupAllowed) && (!rowStyle || source != null)
			if (!enabled || source == null) return

			val context = view.context
			for (chip in listOf(binding.fdAudioChip, binding.fdSubtitleChip)) {
				chip.applyDetailsTrackStyle()
				if (rowStyle) {
					chip.minimumHeight = 36.dp(context)
					chip.maxLines = 1
					chip.setPadding(14.dp(context), 0, 10.dp(context), 0)
				}
			}
			val labels = audioStreams.trackLabels(context) + subtitleStreams.trackLabels(context)
			val queueManager = GlobalContext.get().get<VideoQueueManager>()
			// Same precedence as playback: details pick, remembered choice, session language, server default.
			fun remembered() = rememberedTracks(row.item, source)

			fun currentAudio(): MediaStream? {
				val chosen = PlaybackTrackSelection.get(row.playbackItemId)?.audioIndex
				val lastLanguage = queueManager.getLastPlayedAudioLanguageIsoCode()
				return audioStreams.firstOrNull { it.index == chosen }
					?: audioStreams.firstOrNull { it.index == remembered()?.audioIndex }
					?: audioStreams.firstOrNull { lastLanguage != null && it.language == lastLanguage }
					?: audioStreams.firstOrNull { it.index == source.defaultAudioStreamIndex }
					?: audioStreams.firstOrNull()
			}

			// Null means Auto: the server picks the default at playback time.
			fun currentSubtitleIndex(): Int? {
				val chosen = PlaybackTrackSelection.get(row.playbackItemId)?.subtitleIndex
				if (chosen != null) return subtitleStreams.firstOrNull { it.index == chosen }?.index ?: PlaybackTrackSelection.SUBTITLES_OFF
				val rememberedIndex = remembered()?.subtitleIndex
				if (rememberedIndex != null) return subtitleStreams.firstOrNull { it.index == rememberedIndex }?.index ?: PlaybackTrackSelection.SUBTITLES_OFF

				val lastLanguage = queueManager.getLastPlayedSubtitleLanguageIsoCode()
				val stream = when {
					lastLanguage != null -> if (lastLanguage.isEmpty()) null else subtitleStreams.firstOrNull { it.language == lastLanguage }
					source.defaultSubtitleStreamIndex != null -> subtitleStreams.firstOrNull { it.index == source.defaultSubtitleStreamIndex }
					else -> return null
				}
				return stream?.index ?: PlaybackTrackSelection.SUBTITLES_OFF
			}
			fun currentSubtitleLabel(): String {
				val index = currentSubtitleIndex() ?: return context.getString(R.string.lbl_track_auto)
				return labels[index] ?: context.getString(R.string.lbl_track_off)
			}

			fun refreshLabels() {
				binding.fdAudioChip.setDetailsTrackValue(context.getString(R.string.pref_audio),
					currentAudio()?.audioTrackSummary(context) ?: context.getString(R.string.lbl_track_off), inline = rowStyle)
				binding.fdSubtitleChip.setDetailsTrackValue(context.getString(R.string.pref_subtitles), currentSubtitleLabel(), inline = rowStyle)
			}
			refreshLabels()

			binding.fdAudioChip.setOnClickListener { chip ->
				trackMenu?.dismiss()
				trackMenu = DetailsMenu(chip, context.getString(R.string.pref_audio)).apply {
					for (stream in audioStreams) item(labels.getValue(stream.index), selected = stream.index == currentAudio()?.index) {
						PlaybackTrackSelection.setAudio(row.playbackItemId, stream.index)
						PlaybackChoiceRecorder.audio(row.item, audioStreams, stream.index)
						refreshLabels()
					}
					choicesItem(row, ::refreshLabels)
				}.showIfNotEmpty()
			}

			binding.fdSubtitleChip.setOnClickListener { chip ->
				trackMenu?.dismiss()
				trackMenu = DetailsMenu(chip, context.getString(R.string.pref_subtitles)).apply {
					item(context.getString(R.string.lbl_track_off), selected = currentSubtitleIndex() == PlaybackTrackSelection.SUBTITLES_OFF) {
						PlaybackTrackSelection.setSubtitle(row.playbackItemId, PlaybackTrackSelection.SUBTITLES_OFF)
						PlaybackChoiceRecorder.subtitle(row.item, subtitleStreams, PlaybackTrackSelection.SUBTITLES_OFF)
						refreshLabels()
					}
					for (stream in subtitleStreams) item(labels.getValue(stream.index), selected = stream.index == currentSubtitleIndex()) {
						PlaybackTrackSelection.setSubtitle(row.playbackItemId, stream.index)
						PlaybackChoiceRecorder.subtitle(row.item, subtitleStreams, stream.index)
						refreshLabels()
					}
					if (lookupAllowed) item(context.getString(R.string.subtitle_lookup_more), navigates = true) {
						val owner = view.findViewTreeLifecycleOwner() ?: return@item
						val selectedId = row.playbackItemId
						val selectedIndex = row.selectedMediaSourceIndex
						val sourceId = source.id ?: return@item
						dismissLookup()
						// Look up the actual selected version, rather than the grouped details title.
						val selectedItem = item.copy(id = selectedId)
						lookupDialog = SubtitleLookupDialog(context, owner, selectedItem, sourceId,
							stillCurrent = { currentRow === row && row.playbackItemId == selectedId && row.selectedMediaSourceIndex == selectedIndex && view.isAttachedToWindow },
							onRefreshed = { fresh ->
								val selectedSource = fresh.mediaSources?.firstOrNull { it.id == source.id }
								if (selectedSource != null) bindTrackChips(row, enabled, item.copy(mediaSources = item.mediaSources.orEmpty().map { if (it.id == source.id) selectedSource else it }))
							}, onDismiss = { lookupDialog = null; if (chip.isAttachedToWindow) chip.requestFocus() }).also { it.show() }
					}
					choicesItem(row, ::refreshLabels)
				}.showIfNotEmpty()
			}
		}


		fun setTitle(title: String?) {
			binding.fdTitle.text = title
		}

		fun setSummary(summary: String?) {
			binding.fdSummaryText.text = summary?.let { markdownRenderer.toMarkdownSpanned(it) }
		}

		fun setInfoValue3(text: String?) {
			binding.infoValue3.text = text
		}
	}

	var viewHolder: ViewHolder? = null
		private set

	init {
		syncActivatePolicy = SYNC_ACTIVATED_CUSTOM
	}

	override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
		val view = DetailRowView(parent.context)
		viewHolder = ViewHolder(view, markdownRenderer)
		return viewHolder!!
	}

	override fun onBindRowViewHolder(viewHolder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(viewHolder, item)
		if (item !is MyDetailsOverviewRow) return
		if (viewHolder !is ViewHolder) return

		viewHolder.setItem(item)
	}

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit

	override fun onUnbindRowViewHolder(viewHolder: RowPresenter.ViewHolder) {
		(viewHolder as? ViewHolder)?.dismissLookup()
		super.onUnbindRowViewHolder(viewHolder)
	}
}
