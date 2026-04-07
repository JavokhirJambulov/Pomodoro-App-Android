package com.nemjava.pomodoro.screen

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.nemjava.pomodoro.R
import com.nemjava.pomodoro.commons.TimerAnimationCatalog
import com.nemjava.pomodoro.commons.TimerAnimationOption
import com.nemjava.pomodoro.databinding.ActivityAnimationSelectionBinding
import com.nemjava.pomodoro.databinding.ItemTimerAnimationBinding

class AnimationSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimationSelectionBinding
    private lateinit var adapter: TimerAnimationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_animation_selection)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val selectedKey = TimerAnimationCatalog.getSelectedOption(this).key
        adapter = TimerAnimationAdapter(
            options = TimerAnimationCatalog.getOptions(),
            selectedKey = selectedKey
        ) { option ->
            TimerAnimationCatalog.saveSelectedOption(this, option.key)
            adapter.updateSelectedKey(option.key)
            setResult(RESULT_OK)
        }

        binding.animationRecyclerView.layoutManager = GridLayoutManager(this, calculateSpanCount())
        binding.animationRecyclerView.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun calculateSpanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
    }
}

private class TimerAnimationAdapter(
    private val options: List<TimerAnimationOption>,
    selectedKey: String,
    private val onOptionSelected: (TimerAnimationOption) -> Unit
) : RecyclerView.Adapter<TimerAnimationAdapter.TimerAnimationViewHolder>() {

    private var selectedKey = selectedKey

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerAnimationViewHolder {
        val binding = ItemTimerAnimationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimerAnimationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimerAnimationViewHolder, position: Int) {
        holder.bind(
            option = options[position],
            isSelected = options[position].key == selectedKey,
            onOptionSelected = onOptionSelected
        )
    }

    override fun getItemCount(): Int = options.size

    override fun onViewRecycled(holder: TimerAnimationViewHolder) {
        super.onViewRecycled(holder)
        holder.clearAnimation()
    }

    fun updateSelectedKey(newSelectedKey: String) {
        if (selectedKey == newSelectedKey) return
        selectedKey = newSelectedKey
        notifyDataSetChanged()
    }

    class TimerAnimationViewHolder(
        private val binding: ItemTimerAnimationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundAnimationKey: String? = null

        fun bind(
            option: TimerAnimationOption,
            isSelected: Boolean,
            onOptionSelected: (TimerAnimationOption) -> Unit
        ) {
            boundAnimationKey = option.key
            binding.animationName.text = binding.root.context.getString(option.titleResId)
            binding.selectedBadge.isVisible = isSelected
            binding.animationCard.strokeColor = ContextCompat.getColor(
                binding.root.context,
                if (isSelected) R.color.screen_accent else R.color.current_plan_card_stroke
            )
            binding.animationCard.strokeWidth = if (isSelected) 4 else 1
            binding.animationCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (isSelected) R.color.button_color else R.color.current_plan_card_bg
                )
            )

            binding.animationPreview.cancelAnimation()
            binding.animationPreview.progress = 0f
            binding.animationPreview.isVisible = option.hasAnimation
            binding.animationOffPreview.isVisible = !option.hasAnimation
            if (option.hasAnimation) {
                LottieCompositionFactory.fromRawRes(binding.root.context, option.rawResId)
                    .addListener { composition ->
                        if (boundAnimationKey != option.key) return@addListener
                        binding.animationPreview.setComposition(composition)
                        binding.animationPreview.repeatCount = LottieDrawable.INFINITE
                        binding.animationPreview.playAnimation()
                    }
            }

            binding.animationCard.setOnClickListener {
                onOptionSelected(option)
            }
        }

        fun clearAnimation() {
            boundAnimationKey = null
            binding.animationPreview.cancelAnimation()
            binding.animationPreview.progress = 0f
            binding.animationPreview.isVisible = true
            binding.animationOffPreview.isVisible = false
        }
    }
}
