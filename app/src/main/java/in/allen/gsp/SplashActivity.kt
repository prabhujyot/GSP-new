package `in`.allen.gsp

import `in`.allen.gsp.helpers.App
import `in`.allen.gsp.helpers.AppPreferences
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {

    private val tag = SplashActivity::class.java.name

    private lateinit var app: App
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        app = application as App

        var colorList = IntArray(2)
        colorList[0] = Color.rgb(5, 137, 229)
        colorList[1] = Color.rgb(52, 48, 182)
        imgFB.background = app.drawaleGradiantColor(
            R.drawable.left_corner_radius,
            colorList
        )
        colorList = IntArray(2)
        colorList[0] = Color.rgb(232, 232, 232)
        colorList[1] = Color.rgb(236, 236, 236)
        textFB.background = app.drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )
        textGG.background = app.drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )
        colorList = IntArray(2)
        colorList[0] = Color.rgb(239, 158, 17)
        colorList[1] = Color.rgb(248, 50, 41)
        imgGG.background = app.drawaleGradiantColor(
            R.drawable.left_corner_radius,
            colorList
        )
    }

    fun btnActionSplash(view: View) {
        if(view.id == R.id.btnFB) {

        } else if(view.id == R.id.btnGG) {

        }
        startActivity(Intent(this, HomeActivity::class.java))
    }


}