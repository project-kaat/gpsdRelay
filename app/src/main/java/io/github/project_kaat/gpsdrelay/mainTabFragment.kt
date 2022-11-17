package io.github.project_kaat.gpsdrelay


import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.project_kaat.gpsdrelay.databinding.FragmentMainBinding


class mainTabFragment : Fragment(), View.OnClickListener {

    private var _binding : FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var isServiceRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.serverStartButton.setOnClickListener(this)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        buttonUpdate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
        val listenerAddress = prefs.getString(getString(R.string.settings_key_ipa_src), getString(R.string.settings_ipa_src_default))
        val listenerPort = prefs.getString(getString(R.string.settings_key_ipp_src), getString(R.string.settings_ipp_src_default))

        binding.listenerAddressTextView.text = "${getString(R.string.main_current_ip_title)} : ${listenerAddress}:${listenerPort}"
    }

    override fun onClick(view: View?) {

        if (isServiceRunning) {
            buttonStopService()
            isServiceRunning = false
        }
        else {
            isServiceRunning = buttonStartService()
        }

        buttonUpdate()
    }

    private fun buttonStartService(): Boolean {
        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireContext(), "Enable GPS first", Toast.LENGTH_LONG).show()
            return false
        }
        val startIntent = Intent(context, nmeaServerService::class.java)
        startIntent.action = getString(R.string.INTENT_ACTION_START_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(startIntent)
        } else {
            requireActivity().startService(startIntent)
        }

        return true

    }

    private fun buttonStopService() {
        val stopIntent = Intent(context, nmeaServerService::class.java)
        stopIntent.action = getString(R.string.INTENT_ACTION_STOP_SERVICE)
        requireActivity().startService(stopIntent)
    }

    private fun buttonUpdate() {
        if (isServiceRunning) {
            binding.serverStartButton.text = getString(R.string.main_stop_button_text)
        }
        else {
            binding.serverStartButton.text = getString(R.string.main_start_button_text)
        }

    }

}