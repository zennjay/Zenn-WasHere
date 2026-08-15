package com.example.camera

enum class CameraMode(val title: String, val shortLabel: String) {
    PANORAMA("PANORAMA", "PANO"),
    PRO("PRO", "PRO"),
    SLOW_MOTION("SLOW MOTION", "SLO-MO"),
    CINEMATIC("CINEMATIC", "CINEMATIC"),
    VIDEO("VIDEO", "VIDEO"),
    PHOTO("PHOTO", "PHOTO"),
    PORTRAIT("PORTRAIT", "PORTRAIT"),
    NIGHT("NIGHT", "NIGHT");

    val isVideoMode: Boolean
        get() = this == VIDEO || this == CINEMATIC || this == SLOW_MOTION

    val supportsFlash: Boolean
        get() = this == PHOTO || this == PORTRAIT || this == NIGHT || this == PRO || isVideoMode

    val supportsHdr: Boolean
        get() = this == PHOTO || this == PORTRAIT || this == CINEMATIC
}
