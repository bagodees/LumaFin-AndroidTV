package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.preference.constant.StreamingQuality
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile

internal fun DeviceProfile.withStreamingQuality(quality: StreamingQuality): DeviceProfile {
	val width = quality.maxWidth ?: return this
	val height = requireNotNull(quality.maxHeight)
	val limits = buildDeviceProfile {
		codecProfile {
			type = CodecType.VIDEO
			conditions {
				ProfileConditionValue.WIDTH lowerThanOrEquals width
				ProfileConditionValue.HEIGHT lowerThanOrEquals height
			}
		}
	}
	return copy(codecProfiles = limits.codecProfiles + codecProfiles)
}
