package com.nemjava.pomodoro.commons

import android.content.Context
import com.nemjava.pomodoro.R
import androidx.core.content.edit

object TimerAnimationCatalog {
    private const val PREFS_NAME = "timer_animation_preferences"
    private const val PREF_SELECTED_ANIMATION = "selected_animation"
    private const val DEFAULT_ANIMATION_KEY = "cat"

    private val options = listOf(
        TimerAnimationOption("off", R.string.animation_off, 0),
        TimerAnimationOption("cat", R.string.animation_cat, R.raw.cat),
        TimerAnimationOption("running_cat", R.string.animation_running_cat, R.raw.running_cat),
        TimerAnimationOption("rainbow_cat", R.string.animation_rainbow_cat, R.raw.rainbow_cat),
        TimerAnimationOption("loving_cat", R.string.animation_loving_cat, R.raw.loving_cat),
        TimerAnimationOption("cat_sleeping", R.string.animation_cat_sleeping, R.raw.cat_sleeping),
        TimerAnimationOption("cat_crying", R.string.animation_cat_crying, R.raw.cat_crying),
        TimerAnimationOption("bear_dancing", R.string.animation_bear_dancing, R.raw.bear_dancing),
        TimerAnimationOption("bird_flying", R.string.animation_bird_flying, R.raw.bird_flying),
        TimerAnimationOption("boy_exercising", R.string.animation_boy_exercising, R.raw.boy_exercising),
        TimerAnimationOption("boy_meditating", R.string.animation_boy_meditating, R.raw.boy_meditating),
        TimerAnimationOption("girl_meditating", R.string.animation_girl_meditating, R.raw.girl_meditating),
        TimerAnimationOption("woman_designer", R.string.animation_woman_designer, R.raw.woman_designer),
        TimerAnimationOption("gibli_anime_girl", R.string.animation_gibli_anime_girl, R.raw.gibli_anime_girl),
        TimerAnimationOption("earth_rotating", R.string.animation_earth_rotating, R.raw.earth_rotating),
        TimerAnimationOption("planet_orbit", R.string.animation_planet_orbit, R.raw.planet_orbit),
        TimerAnimationOption("space_rocket", R.string.animation_space_rocket, R.raw.space_rocket)
    )

    fun getOptions(): List<TimerAnimationOption> = options

    fun getSelectedOption(context: Context): TimerAnimationOption {
        val selectedKey = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_SELECTED_ANIMATION, DEFAULT_ANIMATION_KEY)
        return options.firstOrNull { it.key == selectedKey } ?: options.first { it.key == DEFAULT_ANIMATION_KEY }
    }

    fun saveSelectedOption(context: Context, key: String) {
        if (options.none { it.key == key }) return
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(PREF_SELECTED_ANIMATION, key)
            }
    }
}
