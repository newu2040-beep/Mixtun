package com.example.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.FileCategory
import com.example.data.model.FileRecord
import com.example.data.repository.FileRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class PlaybackManager(
    private val context: Context,
    private val repository: FileRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var exoPlayer: ExoPlayer? = null

    private val _currentFile = MutableStateFlow<FileRecord?>(null)
    val currentFile: StateFlow<FileRecord?> = _currentFile.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()

    private var progressJob: Job? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer != null) return
        val player = ExoPlayer.Builder(context).build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                    persistCurrentPosition()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                    _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    persistCurrentPosition(completed = true)
                }
            }
        })
        exoPlayer = player
    }

    fun getPlayer(): ExoPlayer {
        initPlayer()
        return exoPlayer!!
    }

    fun play(file: FileRecord, resumePositionMs: Long? = null) {
        initPlayer()
        val player = exoPlayer ?: return
        _currentFile.value = file
        _isMiniPlayerVisible.value = false

        scope.launch {
            repository.updateLastOpened(file.id)
            val savedState = repository.getPlaybackStateDirect(file.id)
            val targetPosition = resumePositionMs ?: savedState?.positionMs ?: 0L

            val mediaItem = MediaItem.fromUri(file.uri)
            player.setMediaItem(mediaItem)
            player.prepare()

            val actualDuration = file.durationMs ?: savedState?.durationMs ?: 0L
            if (targetPosition > 0 && (actualDuration == 0L || targetPosition < actualDuration * 0.95)) {
                player.seekTo(targetPosition)
            }

            player.playbackParameters = player.playbackParameters.withSpeed(_playbackSpeed.value)
            player.play()
            _isPlaying.value = true
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
        persistCurrentPosition()
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(newPos)
        _currentPosition.value = newPos
    }

    fun setSpeed(speed: Float) {
        val player = exoPlayer ?: return
        _playbackSpeed.value = speed
        player.playbackParameters = player.playbackParameters.withSpeed(speed)
    }

    fun toggleMute() {
        val player = exoPlayer ?: return
        _isMuted.value = !_isMuted.value
        player.volume = if (_isMuted.value) 0f else 1f
    }

    fun showMiniPlayer() {
        if (_isPlaying.value && _currentFile.value != null) {
            _isMiniPlayerVisible.value = true
        }
    }

    fun dismissMiniPlayer() {
        _isMiniPlayerVisible.value = false
        exoPlayer?.pause()
        persistCurrentPosition()
    }

    fun stop() {
        persistCurrentPosition()
        exoPlayer?.stop()
        _isPlaying.value = false
        _isMiniPlayerVisible.value = false
    }

    fun release() {
        stopProgressTracker()
        persistCurrentPosition()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    if (dur > 0) _duration.value = dur
                }
                delay(500)
                // Periodically persist progress every 2 seconds
                if (_currentPosition.value % 2000 < 500) {
                    persistCurrentPosition()
                }
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun persistCurrentPosition(completed: Boolean = false) {
        val file = _currentFile.value ?: return
        val pos = exoPlayer?.currentPosition ?: _currentPosition.value
        val dur = exoPlayer?.duration ?: _duration.value
        if (dur > 0 && pos >= 0) {
            scope.launch(Dispatchers.IO) {
                repository.savePlaybackState(file.id, pos, dur, completed)
            }
        }
    }
}
