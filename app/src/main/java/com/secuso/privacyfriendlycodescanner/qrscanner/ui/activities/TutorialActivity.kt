/*
 This file is part of Privacy Friendly App Example.

 Privacy Friendly App Example is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly App Example is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly App Example. If not, see <http://www.gnu.org/licenses/>.
 */
package com.secuso.privacyfriendlycodescanner.qrscanner.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.secuso.privacyfriendlycodescanner.qrscanner.BuildConfig
import com.secuso.privacyfriendlycodescanner.qrscanner.R
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.PrefManager
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.PreferenceKeys
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.PreferenceKeys.getDefaultSharedPreferencesName

/**
 * Class structure taken from tutorial at http://www.androidhive.info/2016/05/android-build-intro-slider-app/
 *
 * @author Karola Marky
 * @version 20161214
 */
class TutorialActivity : AppCompatActivity() {
    private var viewPager: ViewPager? = null
    private var myViewPagerAdapter: MyViewPagerAdapter? = null
    private var dotsLayout: LinearLayout? = null
    private lateinit var dots: Array<TextView?>
    private lateinit var layouts: IntArray
    private var btnSkip: Button? = null
    private var btnNext: Button? = null
    private var prefManager: PrefManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Checking for first time launch - before calling setContentView()
        prefManager = PrefManager(this)


        if (!prefManager!!.isFirstTimeLaunch && (intent == null || (ACTION_SHOW_ANYWAYS != intent.action && ACTION_SHOW_RELEASE_NOTES != intent.action))) {
            launchHomeScreen()
            return
        }

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        setContentView(R.layout.activity_tutorial)

        viewPager = findViewById<View>(R.id.viewPager) as ViewPager

        dotsLayout = findViewById<View>(R.id.dotsLayout) as LinearLayout

        btnSkip = findViewById<View>(R.id.btSkip) as Button

        btnNext = findViewById<View>(R.id.btNext) as Button


        // layouts of all welcome sliders
        // add few more layouts if you want
        if (prefManager!!.isFirstTimeLaunch || (intent != null && intent.action == ACTION_SHOW_ANYWAYS)) {
            layouts = DEFAULT_TUTORIAL_SLIDES
        } else {
            val appPreferences = getSharedPreferences(getDefaultSharedPreferencesName(this), MODE_PRIVATE)
            val lastVersionOpened = appPreferences.getInt(PreferenceKeys.APP_VERSION_LAST_OPENED, 0)
            val releaseNotes = mutableListOf<Int>()
            for (releaseNote in RELEASE_NOTES) {
                if (releaseNote.versionCode > lastVersionOpened) {
                    releaseNotes.addAll(releaseNote.slides.toList())
                }
            }

            if (releaseNotes.size == 0) {
                // No new patch notes, continue to app
                launchHomeScreen()
                return
            }
            layouts = releaseNotes.toIntArray()
        }

        // adding bottom dots
        addBottomDots(0)

        // making notification bar transparent
        changeStatusBarColor()

        myViewPagerAdapter = MyViewPagerAdapter()
        viewPager!!.adapter = myViewPagerAdapter
        viewPager!!.addOnPageChangeListener(viewPagerPageChangeListener)

        btnSkip!!.setOnClickListener { launchHomeScreen() }

        btnNext!!.setOnClickListener {
            // checking for last page
            // if last page home screen will be launched
            val current = getItem(+1)
            if (current < layouts.size) {
                // move to next screen
                viewPager!!.currentItem = current
            } else {
                launchHomeScreen()
            }
        }
        updateButtons(viewPager!!.currentItem)
    }

    private fun addBottomDots(currentPage: Int) {
        dots = arrayOfNulls(layouts.size)

        @ColorInt val colorActive = resources.getColor(R.color.dot_light_screen, theme)
        @ColorInt val colorInactive = resources.getColor(R.color.dot_dark_screen, theme)

        dotsLayout!!.removeAllViews()
        for (i in dots.indices) {
            dots[i] = TextView(this)
            dots[i]!!.text = Html.fromHtml("&#8226;")
            dots[i]!!.textSize = 35f
            dots[i]!!.setTextColor(colorInactive)
            dotsLayout!!.addView(dots[i])
        }

        if (dots.size > 0) dots[currentPage]!!.setTextColor(colorActive)
    }

    private fun getItem(i: Int): Int {
        return viewPager!!.currentItem + i
    }

    private fun launchHomeScreen() {
        prefManager!!.isFirstTimeLaunch = false
        val appPreferences = getSharedPreferences(getDefaultSharedPreferencesName(this), MODE_PRIVATE)
        appPreferences.edit().putInt(PreferenceKeys.APP_VERSION_LAST_OPENED, BuildConfig.VERSION_CODE).apply()
        val intent = Intent(this, ScannerActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    //  viewpager change listener
    var viewPagerPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
        override fun onPageSelected(position: Int) {
            addBottomDots(position)

            updateButtons(position)
        }

        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
        }

        override fun onPageScrollStateChanged(arg0: Int) {
        }
    }

    private fun updateButtons(position: Int) {
        // changing the next button text 'NEXT' / 'GOT IT'
        if (position == layouts.size - 1) {
            // last page. make button text to GOT IT
            btnNext!!.setText(R.string.okay)
            btnSkip!!.visibility = View.GONE
        } else {
            // still pages are left
            btnNext!!.setText(R.string.next)
            btnSkip!!.visibility = View.VISIBLE
        }
    }

    /**
     * Making notification bar transparent
     */
    private fun changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    /**
     * View pager adapter
     */
    inner class MyViewPagerAdapter : PagerAdapter() {
        private var layoutInflater: LayoutInflater? = null

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            layoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val view = layoutInflater!!.inflate(layouts[position], container, false)
            container.addView(view)

            return view
        }

        override fun getCount(): Int {
            return layouts.size
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view === obj
        }


        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val view = `object` as View
            container.removeView(view)
        }
    }

    private class ReleaseNotes(val versionCode: Int, val slides: List<Int>)

    companion object {
        private val TAG: String = TutorialActivity::class.java.simpleName

        @JvmField
        val ACTION_SHOW_ANYWAYS: String = TAG + ".ACTION_SHOW_ANYWAYS"

        @JvmField
        val ACTION_SHOW_RELEASE_NOTES: String = TAG + ".ACTION_SHOW_RELEASE_NOTES"

        private val DEFAULT_TUTORIAL_SLIDES = intArrayOf(
//            R.layout.tutorial_slide1,
//            R.layout.tutorial_slide2,
//            R.layout.tutorial_slide3,
            R.layout.url_tutorial_slide1,
            R.layout.url_tutorial_slide2,
            R.layout.url_tutorial_slide3,
            R.layout.url_tutorial_slide4,
            R.layout.url_tutorial_slide5,
            R.layout.url_tutorial_slide6,
            R.layout.url_tutorial_slide7,
            R.layout.url_tutorial_slide8,
            R.layout.url_tutorial_slide9,
        )
        private val RELEASE_NOTES: List<ReleaseNotes> = listOf()
    }
}
