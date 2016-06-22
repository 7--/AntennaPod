package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.util.Pair;
<<<<<<< HEAD
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
=======
>>>>>>> 92e8e52414f569be4d82a770afb0c50f4674e8a9
import android.view.SurfaceHolder;
import android.view.WindowManager;

<<<<<<< HEAD
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

=======
>>>>>>> 92e8e52414f569be4d82a770afb0c50f4674e8a9
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.playback.Playable;


/*
 * An inconvenience of an implementation like this is that some members and methods that once were
 * private are now protected, allowing for access from classes of the same package, namely
 * PlaybackService. A workaround would be to move this to a dedicated package.
 */
/**
 * Abstract class that allows for different implementations of the PlaybackServiceMediaPlayer for local
 * and remote (cast devices) playback.
 */
public abstract class PlaybackServiceMediaPlayer {
    public static final String TAG = "PlaybackSvcMediaPlayer";

    /**
     * Return value of some PSMP methods if the method call failed.
     */
    public static final int INVALID_TIME = -1;

    protected volatile PlayerStatus playerStatus;

    /**
     * A wifi-lock that is acquired if the media file is being streamed.
     */
    private WifiManager.WifiLock wifiLock;

    protected final PSMPCallback callback;
    protected final Context context;

    public PlaybackServiceMediaPlayer(@NonNull Context context,
                                      @NonNull PSMPCallback callback){
        this.context = context;
        this.callback = callback;

        playerStatus = PlayerStatus.STOPPED;
    }

    /**
     * Starts or prepares playback of the specified Playable object. If another Playable object is already being played, the currently playing
     * episode will be stopped and replaced with the new Playable object. If the Playable object is already being played, the method will
     * not do anything.
     * Whether playback starts immediately depends on the given parameters. See below for more details.
     * <p/>
     * States:
     * During execution of the method, the object will be in the INITIALIZING state. The end state depends on the given parameters.
     * <p/>
     * If 'prepareImmediately' is set to true, the method will go into PREPARING state and after that into PREPARED state. If
     * 'startWhenPrepared' is set to true, the method will additionally go into PLAYING state.
     * <p/>
     * If an unexpected error occurs while loading the Playable's metadata or while setting the MediaPlayers data source, the object
     * will enter the ERROR state.
     * <p/>
     * This method is executed on an internal executor service.
     *
     * @param playable           The Playable object that is supposed to be played. This parameter must not be null.
     * @param stream             The type of playback. If false, the Playable object MUST provide access to a locally available file via
     *                           getLocalMediaUrl. If true, the Playable object MUST provide access to a resource that can be streamed by
     *                           the Android MediaPlayer via getStreamUrl.
     * @param startWhenPrepared  Sets the 'startWhenPrepared' flag. This flag determines whether playback will start immediately after the
     *                           episode has been prepared for playback. Setting this flag to true does NOT mean that the episode will be prepared
     *                           for playback immediately (see 'prepareImmediately' parameter for more details)
     * @param prepareImmediately Set to true if the method should also prepare the episode for playback.
     */
<<<<<<< HEAD
    public void playMediaObject(@NonNull final Playable playable, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        Log.d(TAG, "playMediaObject(...)");
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();
                try {
                    playMediaObject(playable, false, stream, startWhenPrepared, prepareImmediately);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                } finally {
                    playerLock.unlock();
                }
            }
        });
    }

    /**
     * Internal implementation of playMediaObject. This method has an additional parameter that allows the caller to force a media player reset even if
     * the given playable parameter is the same object as the currently playing media.
     * <p/>
     * This method requires the playerLock and is executed on the caller's thread.
     *
     * @see #playMediaObject(de.danoeh.antennapod.core.util.playback.Playable, boolean, boolean, boolean)
     */
    private void playMediaObject(@NonNull final Playable playable, final boolean forceReset, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        if (!playerLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("method requires playerLock");
        }


        if (media != null) {
            if (!forceReset && media.getIdentifier().equals(playable.getIdentifier())
                    && playerStatus == PlayerStatus.PLAYING) {
                // episode is already playing -> ignore method call
                Log.d(TAG, "Method call to playMediaObject was ignored: media file already playing.");
                return;
            } else {
                // stop playback of this episode
                if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.PREPARED) {
                    mediaPlayer.stop();
                }
                // set temporarily to pause in order to update list with current position
                if (playerStatus == PlayerStatus.PLAYING) {
                    setPlayerStatus(PlayerStatus.PAUSED, media);
                }

                // smart mark as played
                if(media != null && media instanceof FeedMedia) {
                    FeedMedia oldMedia = (FeedMedia) media;
                    if(oldMedia.hasAlmostEnded()) {
                        Log.d(TAG, "smart mark as read");
                        FeedItem item = oldMedia.getItem();
                        DBWriter.markItemPlayed(item, FeedItem.PLAYED, false);
                        DBWriter.removeQueueItem(context, item, false);
                        DBWriter.addItemToPlaybackHistory(oldMedia);
                        if (item.getFeed().getPreferences().getCurrentAutoDelete()) {
                            Log.d(TAG, "Delete " + oldMedia.toString());
                            DBWriter.deleteFeedMediaOfItem(context, oldMedia.getId());
                        }
                    }
                }

                setPlayerStatus(PlayerStatus.INDETERMINATE, null);
            }
        }

        this.media = playable;
        this.stream = stream;
        this.mediaType = media.getMediaType();
        this.videoSize = null;
        createMediaPlayer();
        PlaybackServiceMediaPlayer.this.startWhenPrepared.set(startWhenPrepared);
        setPlayerStatus(PlayerStatus.INITIALIZING, media);
        try {
            media.loadMetadata();
            updateMediaSessionMetadata();
            if (stream) {
                mediaPlayer.setDataSource(media.getStreamUrl());
            } else {
                mediaPlayer.setDataSource(media.getLocalMediaUrl());
            }
            setPlayerStatus(PlayerStatus.INITIALIZED, media);

            if (mediaType == MediaType.VIDEO) {
                VideoPlayer vp = (VideoPlayer) mediaPlayer;
                //  vp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }

            if (prepareImmediately) {
                setPlayerStatus(PlayerStatus.PREPARING, media);
                mediaPlayer.prepare();
                onPrepared(startWhenPrepared);
            }

        } catch (Playable.PlayableException e) {
            e.printStackTrace();
            setPlayerStatus(PlayerStatus.ERROR, null);
        } catch (IOException e) {
            e.printStackTrace();
            setPlayerStatus(PlayerStatus.ERROR, null);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            setPlayerStatus(PlayerStatus.ERROR, null);
        }
    }

    private void updateMediaSessionMetadata() {
        executor.execute(() -> {
            final Playable p = this.media;
            if (p == null) {
                return;
            }
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, p.getFeedTitle());
            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, p.getEpisodeTitle());
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, p.getDuration());
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, p.getEpisodeTitle());
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, p.getFeedTitle());
            if (p.getImageUri() != null && UserPreferences.setLockscreenBackground()) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, p.getImageUri().toString());
                try {
                    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Bitmap art = Glide.with(context)
                            .load(p.getImageUri())
                            .asBitmap()
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .centerCrop()
                            .into(display.getWidth(), display.getHeight())
                            .get();
                    builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art);
                } catch (Throwable tr) {
                    Log.e(TAG, Log.getStackTraceString(tr));
                }
            }
            mediaSession.setMetadata(builder.build());
        });
    }

=======
    public abstract void playMediaObject(@NonNull Playable playable, boolean stream, boolean startWhenPrepared, boolean prepareImmediately);
>>>>>>> 92e8e52414f569be4d82a770afb0c50f4674e8a9

    /**
     * Resumes playback if the PSMP object is in PREPARED or PAUSED state. If the PSMP object is in an invalid state.
     * nothing will happen.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public abstract void resume();

    /**
     * Saves the current position and pauses playback. Note that, if audiofocus
     * is abandoned, the lockscreen controls will also disapear.
     * <p/>
     * This method is executed on an internal executor service.
     *
     * @param abandonFocus is true if the service should release audio focus
     * @param reinit       is true if service should reinit after pausing if the media
     *                     file is being streamed
     */
    public abstract void pause(boolean abandonFocus, boolean reinit);

    /**
     * Prepared media player for playback if the service is in the INITALIZED
     * state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public abstract void prepare();

    /**
     * Resets the media player and moves it into INITIALIZED state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public abstract void reinit();

    /**
     * Seeks to the specified position. If the PSMP object is in an invalid state, this method will do nothing.
     * Invalid time values (< 0) will be ignored.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public abstract void seekTo(int t);

    /**
     * Seek a specific position from the current position
     *
     * @param d offset from current position (positive or negative)
     */
    public abstract void seekDelta(int d);

    /**
     * Seek to the start of the specified chapter.
     */
    public void seekToChapter(@NonNull Chapter c) {
        seekTo((int) c.getStart());
    }

    /**
     * Returns the duration of the current media object or INVALID_TIME if the duration could not be retrieved.
     */
    public abstract int getDuration();

    /**
     * Returns the position of the current media object or INVALID_TIME if the position could not be retrieved.
     */
    public abstract int getPosition();

    public abstract boolean isStartWhenPrepared();

    public abstract void setStartWhenPrepared(boolean startWhenPrepared);

    /**
     * Returns true if the playback speed can be adjusted.
     */
    public abstract boolean canSetSpeed();

    /**
     * Sets the playback speed.
     * This method is executed on an internal executor service.
     */
    public abstract void setSpeed(float speed);

    /**
     * Returns the current playback speed. If the playback speed could not be retrieved, 1 is returned.
     */
    public abstract float getPlaybackSpeed();

    /**
     * Sets the playback volume.
     * This method is executed on an internal executor service.
     */
    public abstract void setVolume(float volumeLeft, float volumeRight);

    /**
     * Returns true if the mediaplayer can mix stereo down to mono
     */
    public abstract boolean canDownmix();

    public abstract void setDownmix(boolean enable);

    public abstract MediaType getCurrentMediaType();

    public abstract boolean isStreaming();

    /**
     * Releases internally used resources. This method should only be called when the object is not used anymore.
     */
    public abstract void shutdown();

    /**
     * Releases internally used resources. This method should only be called when the object is not used anymore.
     * This method is executed on an internal executor service.
     */
    public abstract void shutdownQuietly();

    public abstract void setVideoSurface(SurfaceHolder surface);

    public abstract void resetVideoSurface();

    /**
     * Return width and height of the currently playing video as a pair.
     *
     * @return Width and height as a Pair or null if the video size could not be determined. The method might still
     * return an invalid non-null value if the getVideoWidth() and getVideoHeight() methods of the media player return
     * invalid values.
     */
    public abstract Pair<Integer, Integer> getVideoSize();

    /**
     * Returns a PSMInfo object that contains information about the current state of the PSMP object.
     *
     * @return The PSMPInfo object.
     */
    public final synchronized PSMPInfo getPSMPInfo() {
        return new PSMPInfo(playerStatus, getPlayable());
    }

    /**
     * Returns the current status, if you need the media and the player status together, you should
     * use getPSMPInfo() to make sure they're properly synchronized. Otherwise a race condition
     * could result in nonsensical results (like a status of PLAYING, but a null playable)
     * @return the current player status
     */
    public PlayerStatus getPlayerStatus() {
        return playerStatus;
    }

    /**
     * Returns the current media, if you need the media and the player status together, you should
     * use getPSMPInfo() to make sure they're properly synchronized. Otherwise a race condition
     * could result in nonsensical results (like a status of PLAYING, but a null playable)
     * @return the current media. May be null
     */
    public abstract Playable getPlayable();

    protected abstract void setPlayable(Playable playable);

    public abstract void endPlayback(boolean wasSkipped, boolean switchingPlayers);

    /**
     * Moves the PSMP into STOPPED state. This call is only valid if the player is currently in
     * INDETERMINATE state, for example after a call to endPlayback.
     * This method will only take care of changing the PlayerStatus of this object! Other tasks like
     * abandoning audio focus have to be done with other methods.
     */
    public abstract void stop();

    /**
     * @return {@code true} if the WifiLock feature should be used, {@code false} otherwise.
     */
    protected abstract boolean shouldLockWifi();

    protected final synchronized void acquireWifiLockIfNecessary() {
        if (shouldLockWifi()) {
            if (wifiLock == null) {
                wifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
                wifiLock.setReferenceCounted(false);
            }
            wifiLock.acquire();
        }
    }

    protected final synchronized void releaseWifiLockIfNecessary() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    /**
     * Sets the player status of the PSMP object. PlayerStatus and media attributes have to be set at the same time
     * so that getPSMPInfo can't return an invalid state (e.g. status is PLAYING, but media is null).
     * <p/>
     * This method will notify the callback about the change of the player status (even if the new status is the same
     * as the old one).
     *
     * @param newStatus The new PlayerStatus. This must not be null.
     * @param newMedia  The new playable object of the PSMP object. This can be null.
     */
    protected synchronized final void setPlayerStatus(@NonNull PlayerStatus newStatus, Playable newMedia) {
        Log.d(TAG, this.getClass().getSimpleName() + ": Setting player status to " + newStatus);

        this.playerStatus = newStatus;
        setPlayable(newMedia);

        if (playerStatus != null) {
            Log.d(TAG, "playerStatus: " + playerStatus.toString());
        }

        callback.statusChanged(new PSMPInfo(playerStatus, getPlayable()));
    }

    protected void smartMarkAsPlayed(Playable media) {
        if(media != null && media instanceof FeedMedia) {
            FeedMedia oldMedia = (FeedMedia) media;
            if(oldMedia.hasAlmostEnded()) {
                Log.d(TAG, "smart mark as read");
                FeedItem item = oldMedia.getItem();
                if (item == null) {
                    return;
                }
                DBWriter.markItemPlayed(item, FeedItem.PLAYED, false);
                DBWriter.removeQueueItem(context, item, false);
                DBWriter.addItemToPlaybackHistory(oldMedia);
                if (item.getFeed().getPreferences().getCurrentAutoDelete()) {
                    Log.d(TAG, "Delete " + oldMedia.toString());
                    DBWriter.deleteFeedMediaOfItem(context, oldMedia.getId());
                }
            }
        }
    }

    public interface PSMPCallback {
        void statusChanged(PSMPInfo newInfo);

        void shouldStop();

        void playbackSpeedChanged(float s);

        void setSpeedAbilityChanged();

        void onBufferingUpdate(int percent);

        void onMediaChanged(boolean reloadUI);

        boolean onMediaPlayerInfo(int code, @StringRes int resourceId);

        boolean onMediaPlayerError(Object inObj, int what, int extra);

        boolean endPlayback(Playable media, boolean playNextEpisode, boolean wasSkipped, boolean switchingPlayers);
    }

    /**
     * Holds information about a PSMP object.
     */
    public static class PSMPInfo {
        public PlayerStatus playerStatus;
        public Playable playable;

        public PSMPInfo(PlayerStatus playerStatus, Playable playable) {
            this.playerStatus = playerStatus;
            this.playable = playable;
        }
    }
}
