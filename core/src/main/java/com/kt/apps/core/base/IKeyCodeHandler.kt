package com.kt.apps.core.base

interface IKeyCodeHandler {
    fun onDpadCenter()
    fun onDpadDown()
    fun onDpadUp()
    fun onDpadLeft()
    fun onDpadRight()
    fun onKeyCodeChannelUp()
    fun onKeyCodeChannelDown()
    fun onKeyCodeMediaPrevious()
    fun onKeyCodeMediaNext()
    fun onKeyCodeVolumeUp()
    fun onKeyCodeVolumeDown()
    fun onKeyCodePause()
    fun onKeyCodePlay()
    fun onKeyCodeMenu()
}

interface IMediaKeycodeHandler : IKeyCodeHandler {
    fun onKeyCodeForward()
    fun onKeyCodeRewind()
}

interface ITVGuideKeycodeHandler : IKeyCodeHandler {
    fun onKeyCodeProgram()
}