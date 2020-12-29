package com.sdpdigital.glassblockbar.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

public class LightComposerViewModel : ViewModel() {
    public val bpmInfo: MutableLiveData<BpmInfo> = MutableLiveData(BpmInfo())
    public fun setBpm(bpm: Int, bpmStartTime: Long) {
        bpmInfo.postValue(bpmInfo.value?.copy(bpm = bpm, bpmStartTime = bpmStartTime))
    }
    public fun setBpmDelay(delay: Int) {
        bpmInfo.postValue(bpmInfo.value?.copy(signalDelay = delay))
    }
    public fun setTimeSignature(timeSig: TimeSignature) {
        bpmInfo.postValue(bpmInfo.value?.copy(timeSignature = timeSig))
    }
    public fun setBpmRange(range: BpmRange) {
        bpmInfo.postValue(bpmInfo.value?.copy(bpmRange = range))
    }
    public fun setBpmAndRange(bpm: Int, bpmStartTime: Long, range: BpmRange) {
        bpmInfo.postValue(bpmInfo.value?.copy(bpm = bpm, bpmStartTime = bpmStartTime, bpmRange = range))
    }
}

data class TimeSignature(
    val beatsPerMeasure: Int,
    val beatLength: Int)

data class BpmInfo(
    val bpm: Int = 0,
    val bpmStartTime: Long? = null,
    val timeSignature: TimeSignature = TimeSignatureEnum.FOUR_FOUR.value,
    val signalDelay: Int = 0,
    val bpmRange: BpmRange = BpmRange())

data class BpmRange(
    val bpmRangeStart: Int = 0,
    val bpmRangeEnd: Int = 255)

enum class TimeSignatureEnum(val value: TimeSignature) {
    FOUR_FOUR(TimeSignature(4, 4)),
    EIGHT_EIGHT(TimeSignature(8, 8)),
    SIXTEEN_SIXTEEN(TimeSignature(16, 16))
}