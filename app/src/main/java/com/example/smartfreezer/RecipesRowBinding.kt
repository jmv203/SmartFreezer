package com.example.smartfreezer

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import coil.load
import org.jsoup.Jsoup

class RecipesRowBinding {
    companion object{


        //Loading recipe image
        @BindingAdapter("loadImageFromUrl")
        @JvmStatic
        fun loadImageFromUrl(imageView : ImageView, imageUrl : String){
            imageView.load(imageUrl){
                crossfade(600)
                error(R.drawable.ic_error_placeholder)

            }
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

    }
}