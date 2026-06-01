/**
 * Audio management system for Intentia.
 *
 * Architecture:
 *   Main.java creates MusicManager on create(), disposes on dispose().
 *   MusicManager.update(delta) called from Main.render().
 *   Each Screen calls musicManager.play(MusicCommand.xxx("path")) in show().
 *
 * Components:
 *   MusicManager   — Central coordinator. Lives entire game. Handles fade,
 *                    crossfade, queue, track stack for restoration, error recovery.
 *   MusicCommand   — Immutable value object describing WHAT to play + HOW (fade, loop).
 *   MusicPriority  — Enum: MENU(0) < GAMEPLAY(1) < CINEMATIC(2) < DIALOGUE(3).
 *                    Higher priority interrupts lower; lower queues behind higher.
 *   ScreenMusicHandler — Interface screens can implement to declare music needs.
 *
 * Fade machine: IDLE → FADING_OUT → (startNewTrack + FADING_IN) → IDLE
 *                     → CROSSFADING → finishCrossfade → IDLE
 */
package io.yourPath.audio;
