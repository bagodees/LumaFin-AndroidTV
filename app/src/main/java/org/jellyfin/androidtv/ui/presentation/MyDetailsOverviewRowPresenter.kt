package org.jellyfin.androidtv.ui.presentation

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.ui.DetailRowView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemdetail.MyDetailsOverviewRow
import org.jellyfin.androidtv.ui.playback.PlaybackTrackSelection
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.showIfNotEmpty
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.koin.core.context.GlobalContext
import org.jellyfin.androidtv.util.InfoLayoutHelper
import org.jellyfin.androidtv.util.MarkdownRenderer
import org.jellyfin.sdk.model.api.BaseItemKind

class MyDetailsOverviewRowPresenter(
	private val markdownRenderer: MarkdownRenderer,
) : RowPresenter() {
	class ViewHolder(
		private val detailRowView: DetailRowView,
		private val markdownRenderer: MarkdownRenderer,
	) : RowPresenter.ViewHolder(detailRowView) {
		private val binding get() = detailRowView.binding

		fun setItem(row: MyDetailsOverviewRow) {
			val item = row.item
			val hasThumbnail = row.thumbnailImage != null

			if (item.type == BaseItemKind.EPISODE && item.seriesName != null) {
				setTitle(item.seriesName)
				binding.fdSubtitle.text = item.name
				binding.fdSubtitle.isVisible = true
			} else {
				setTitle(item.name)
				binding.fdSubtitle.isVisible = false
			}

			InfoLayoutHelper.addInfoRow(view.context, item, item.mediaSources?.getOrNull(row.selectedMediaSourceIndex), binding.fdMainInfoRow, hasThumbnail)

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
			if (hasThumbnail) binding.fdThumb.load(row.thumbnailImage, null, null, 1.0, 0)
			binding.guideMainStart.setGuidelineBegin(
				(view.resources.displayMetrics.density * if (hasThumbnail) 300 else 170).toInt()
			)
			binding.fdThumb.clipToOutline = true

			binding.mainImage.load(row.imageDrawable, null, null, 1.0, 0)

			bindTrackChips(row, hasThumbnail)

			setSummary(row.summary)

			if (row.item.type == BaseItemKind.PERSON) {
				binding.fdSummaryText.maxLines = 9
				binding.fdGenreRow.isVisible = false
			}

			binding.fdButtonRow.removeAllViews()
			for (button in row.actions) {
				val parent = button.parent
				if (parent is ViewGroup) parent.removeView(button)

				binding.fdButtonRow.addView(button)
			}
		}

		private fun bindTrackChips(row: MyDetailsOverviewRow, enabled: Boolean) {
			val item = row.item
			val streams = item.mediaSources?.getOrNull(row.selectedMediaSourceIndex)?.mediaStreams.orEmpty()
			val audioStreams = streams.filter { it.type == MediaStreamType.AUDIO }
			val subtitleStreams = streams.filter { it.type == MediaStreamType.SUBTITLE }
			val source = item.mediaSources?.getOrNull(row.selectedMediaSourceIndex)

			binding.fdTrackColumn.isVisible = enabled && (audioStreams.isNotEmpty() || subtitleStreams.isNotEmpty())
			binding.fdAudioChip.isVisible = audioStreams.isNotEmpty()
			binding.fdSubtitleChip.isVisible = subtitleStreams.isNotEmpty()
			if (!enabled || source == null) return

			val context = view.context
			val queueManager = GlobalContext.get().get<VideoQueueManager>()

			fun currentAudio(): MediaStream? {
				val chosen = PlaybackTrackSelection.get(item.id)?.audioIndex
				val lastLanguage = queueManager.getLastPlayedAudioLanguageIsoCode()
				return audioStreams.firstOrNull { it.index == chosen }
					?: audioStreams.firstOrNull { lastLanguage != null && it.language == lastLanguage }
					?: audioStreams.firstOrNull { it.index == source.defaultAudioStreamIndex }
					?: audioStreams.firstOrNull()
			}

			// Null means subtitles will be off, "Auto" means the server picks the default at playback time
			fun currentSubtitleLabel(): String {
				val chosen = PlaybackTrackSelection.get(item.id)?.subtitleIndex
				if (chosen != null) return subtitleStreams.firstOrNull { it.index == chosen }?.trackLabel() ?: context.getString(R.string.lbl_track_off)

				val lastLanguage = queueManager.getLastPlayedSubtitleLanguageIsoCode()
				val stream = when {
					lastLanguage != null -> if (lastLanguage.isEmpty()) null else subtitleStreams.firstOrNull { it.language == lastLanguage }
					source.defaultSubtitleStreamIndex != null -> subtitleStreams.firstOrNull { it.index == source.defaultSubtitleStreamIndex }
					else -> return context.getString(R.string.lbl_track_auto)
				}
				return stream?.trackLabel() ?: context.getString(R.string.lbl_track_off)
			}

			fun refreshLabels() {
				binding.fdAudioChip.text = context.getString(R.string.lbl_track_audio, currentAudio()?.trackLabel() ?: context.getString(R.string.lbl_track_off))
				binding.fdSubtitleChip.text = context.getString(R.string.lbl_track_subtitles, currentSubtitleLabel())
			}
			refreshLabels()

			binding.fdAudioChip.setOnClickListener { chip ->
				popupMenu(context, chip) {
					for (stream in audioStreams) item(stream.trackLabel()) {
						PlaybackTrackSelection.setAudio(item.id, stream.index)
						refreshLabels()
					}
				}.showIfNotEmpty()
			}

			binding.fdSubtitleChip.setOnClickListener { chip ->
				popupMenu(context, chip) {
					item(context.getString(R.string.lbl_track_off)) {
						PlaybackTrackSelection.setSubtitle(item.id, PlaybackTrackSelection.SUBTITLES_OFF)
						refreshLabels()
					}
					for (stream in subtitleStreams) item(stream.trackLabel()) {
						PlaybackTrackSelection.setSubtitle(item.id, stream.index)
						refreshLabels()
					}
				}.showIfNotEmpty()
			}
		}

		private fun MediaStream.trackLabel(): String =
			displayTitle?.takeIf { it.isNotBlank() }
				?: listOfNotNull(language, codec?.uppercase()).joinToString(" ").ifBlank { "#$index" }

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
}
