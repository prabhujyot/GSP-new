package `in`.allen.gsp

import `in`.allen.gsp.databinding.ActivityIntroBinding
import `in`.allen.gsp.ui.home.HomeActivity
import `in`.allen.gsp.utils.hideStatusBar
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.gohn.parallaxviewpager.ParallaxViewPager

class IntroActivity : AppCompatActivity() {

    private val TAG = IntroActivity::class.java.name
    private lateinit var binding: ActivityIntroBinding
    private lateinit var parallaxViewPager: ParallaxViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_intro)

        hideStatusBar()

        parallaxViewPager = findViewById(R.id.parallaxViewPager)
        parallaxViewPager.addMovementToView(R.id.title, 0.0f) // Stop On View
//        pager.addMovementToView(R.id.description, 0.2f) // Move Little
        parallaxViewPager.addMovementToView(R.id.image, 0.8f) // Move Much
        parallaxViewPager.adapter = IntroAdapter(layoutInflater)

        parallaxViewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {}

            override fun onPageSelected(position: Int) {
                if(position == 2) {
                    binding.btnNext.text = "Done"
                } else {
                    binding.btnNext.text = "Next"
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private class IntroAdapter(private val inflater: LayoutInflater): PagerAdapter() {

        var images = intArrayOf(R.drawable.group, R.drawable.gifts, R.drawable.coins)
        private var titles = arrayOf("A Platform That Gives Recognition\nto your Knowledge",
            "Play | Earn | Redeem",
            "Let's Play\n& Win Exciting Prizes",
        )

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getCount(): Int {
            return 3
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val v: View = inflater.inflate(R.layout.item_intro, null)

            val image = v.findViewById<ImageView>(R.id.image)
            val title = v.findViewById<TextView>(R.id.title)

            image.setImageResource(images[position % 3])
            title.text = titles[position % 3]

            container.addView(v)
            return v
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }

    fun btnActionIntro(view: View) {
        if(view.id == R.id.btnNext && ::parallaxViewPager.isInitialized) {
            var position = parallaxViewPager.currentItem
            if(position < 2) {
                parallaxViewPager.currentItem = ++ position
            } else {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
    }
}