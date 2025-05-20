package com.example.smartfreezer

import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import coil.load
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.smartfreezer.models.ExtendedIngredient
import com.example.smartfreezer.models.Nutrition
import org.jsoup.Jsoup
import java.util.Collections

class RecipesRowBinding {
    companion object{

        @BindingAdapter("loadImageFromUrl")
        @JvmStatic
        fun loadImageFromUrl(imageView: ImageView, imageUrl: String?) {
            imageUrl?.let {
                val lastSlashIndex = it.lastIndexOf("/")
                val dotIndex = it.lastIndexOf(".")

                if (lastSlashIndex != -1 && dotIndex != -1 && dotIndex > lastSlashIndex) {
                    val name = it.substring(lastSlashIndex + 1, dotIndex)
                    val extension = it.substring(dotIndex + 1)

                    val id = name.substringBefore("-")
                    val newUrl = "https://img.spoonacular.com/recipes/${id}-556x370.${extension}"

                    imageView.load(newUrl) {
                        crossfade(600)
                        error(R.drawable.ic_error_placeholder)
                    }
                } else {
                    // URL no válida, cargar como está
                    imageView.load(it) {
                        crossfade(600)
                        error(R.drawable.ic_error_placeholder)
                    }
                }
            }
        }


        @BindingAdapter("loadIngredientImage")
        @JvmStatic
        fun loadIngredientImage(imageView: ImageView, imageUrl: String?) {
            imageUrl?.let {
                imageView.load("https://spoonacular.com/cdn/ingredients_100x100/$it") {
                    error(R.drawable.ic_error_placeholder)
                }
            }
        }

        @BindingAdapter("loadIngredientImageBackground")
        @JvmStatic
        fun loadBackGround(imageView: ImageView, imageUrl: String?) {
            Glide.with(imageView).asBitmap()
                .load("https://spoonacular.com/cdn/ingredients_100x100/$imageUrl")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Bitmap?> {
                    override fun onLoadFailed(
                        @Nullable e: GlideException?,
                        model: Any,
                        target: Target<Bitmap?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any,
                        target: Target<Bitmap?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        val p: Palette = Palette.from(resource!!).generate()
                        val palette: Swatch = getDominantSwatch(p)
                        imageView.background = ContextCompat.getDrawable(
                            imageView.context,
                            R.drawable.background_ingredients
                        )
                        val drawable: GradientDrawable = imageView.background as GradientDrawable
                        drawable.setColor(palette.rgb)
                        drawable.cornerRadius = 20f
                        return true
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        }

        private fun getDominantSwatch(palette: Palette): Swatch {
            return Collections.max(
                palette.swatches
            ) { sw1, sw2 -> sw1.population.compareTo(sw2.population) }
        }

        // Parses Javascript into text
        @BindingAdapter("parseHtml")
        @JvmStatic
        fun parseHtml(textView : TextView, description : String?){
            if(description != null){
                val desc= Jsoup.parse(description).text()
                textView.text = desc
            }
        }

        @BindingAdapter("setAmount", "setUnit", requireAll = true)
        @JvmStatic
        fun loadMeasure(textView: TextView, amount: Double, unit: String) {
            var txt = ""
            txt = if (unit.isEmpty()) {
                "$amount items"
            } else {
                "$amount $unit"
            }
            textView.isSelected = true
            textView.text = txt
        }

        @BindingAdapter("setLikes")
        @JvmStatic
        fun setLikes(textView: TextView, likes: Int) {
            val likesTxt = "$likes likes"
            textView.text = likesTxt
        }

        @BindingAdapter("setMinutes")
        @JvmStatic
        fun setMinutes(textView: TextView, minutes: Int?) {
            minutes?.let {
                val minutesTxt = "$it min"
                textView.text = minutesTxt
            }
        }

        @BindingAdapter("setServings")
        @JvmStatic
        fun setServings(textView: TextView, serving: Int?) {
            serving?.let {
                val servings = "$it raciones"
                textView.text = servings
            }
        }

        @BindingAdapter("loadStepCount")
        @JvmStatic
        fun loadStepCount(textView: TextView, number: Int?) {
            number?.let {
                val txt = "Paso $it"
                textView.text = txt
            }
        }

        @BindingAdapter("loadStars")
        @JvmStatic
        fun loadStars(ratingBar: RatingBar, score: Double?) {
            score?.let {
                ratingBar.rating = ((it * 5) / 100).toFloat()
            }
        }

        @BindingAdapter("loadScoreValue")
        @JvmStatic
        fun loadScoreValue(textView: TextView, score: Double?) {
            score?.let {
                val value = ((it * 5) / 100)
                textView.text = String.format("%.1f", value)
            }
        }

        @BindingAdapter("loadCal")
        @JvmStatic
        fun loadCal(textView: TextView, nutrition: Nutrition?) {
            nutrition?.let {
                val txt = it.nutrients?.get(0)?.amount.toString() + " " + it.nutrients?.get(0)?.unit
                textView.text = txt
            }
        }

        @BindingAdapter("loadNoOfIngredients")
        @JvmStatic
        fun loadNoOfIngredients(textView: TextView, ingredients: List<ExtendedIngredient?>?) {
            val txt = ingredients?.size?.toString() + " ingredients"
            textView.text = txt
        }

        @BindingAdapter("drawableResource")
        @JvmStatic
        fun setDrawableResource(imageView: ImageView, resourceId: Int) {
            imageView.setImageResource(resourceId)
        }



    }
}