package app.mapspert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels {
        AppViewModelFactory(CountryRepository.load(applicationContext), Stats(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MapspertTheme { MapspertApp(vm) } }
    }
}
