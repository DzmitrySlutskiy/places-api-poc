/*
 * Copyright 2018 Nazmul Idris. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.api.places.places_api_poc

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_tab1.*

class Tab1Fragment : BaseTabFragment() {

    // Inflate the layout.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tab1, container, false)
    }

    override fun attachToUI() {
        // Attach a behavior to the button.
        button_current_place_fragment.setOnClickListener { viewClicked ->
            getParentActivity().executeTaskOnPermissionGranted(
                object : PermissionDependentTask {
                    override fun getRequiredPermission() =
                            android.Manifest.permission.ACCESS_FINE_LOCATION

                    override fun onPermissionGranted() {
                        placesAPIViewModel.getCurrentPlace()
                        ThemedSnackbar.show(
                            fragment_container_tab1,
                            "❤️ This app will function well with this permission")
                    }

                    override fun onPermissionRevoked() {
                        ThemedSnackbar.show(
                            fragment_container_tab1,
                            "🛑 This app will not function without this permission")
                    }
                })
        }

        // Attach LiveData observers for current_place_text.
        placesAPIViewModel.currentPlaceData.observe(this, Observer { data ->
            Log.i(javaClass.name, "🎉observable reacting -> $data")
            current_place_text_fragment.text = data ?: "n/a"
        })
    }

}
