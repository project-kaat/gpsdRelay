package io.github.project_kaat.gpsdrelay

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.project_kaat.gpsdrelay.databinding.FragmentMainBinding


class mainTabFragment : Fragment(), View.OnClickListener {

    private val TAG = "mainTabFragment"

    private var _binding : FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var application : gpsdrelay

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.serverStartButton.setOnClickListener(this)
        application = this.activity?.application as gpsdrelay
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        buttonUpdate() //check if the service was launched without us knowing about it

        val prefs = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
        val listenerAddress = prefs.getString(getString(R.string.settings_key_ipa_src), getString(R.string.settings_ipa_src_default))
        val listenerPort = prefs.getString(getString(R.string.settings_key_ipp_src), getString(R.string.settings_ipp_src_default))

        binding.listenerAddressTextView.text = "${getString(R.string.main_current_ip_title)} : ${listenerAddress}:${listenerPort}"
    }

    override fun onClick(view: View?) { //toggle service state and update the button text to reflect the available action

        if (application.serverManager.isServiceRunning) {
            application.serverManager.stopService()
            buttonUpdate(false)
        }
        else {
            if (!application.serverManager.startService()) {
                Log.e(TAG, "gpsdRelay service failed to start")
                Toast.makeText(this.activity, "gpsdRelay service failed to start", Toast.LENGTH_LONG).show()
            }
            else {
                buttonUpdate(true)
            }
        }
    }


    private fun buttonUpdate(serviceRunning : Boolean) { //for when the state of the service is known
        if (serviceRunning) {
            binding.serverStartButton.text = getString(R.string.main_stop_button_text)
        }
        else {
            binding.serverStartButton.text = getString(R.string.main_start_button_text)
        }
    }

    private fun buttonUpdate() { //for when the state of the service is unknown
        buttonUpdate(application.serverManager.isServiceRunning)
    }


}