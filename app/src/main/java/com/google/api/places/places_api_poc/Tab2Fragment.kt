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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.coordinatorlayout.widget.CoordinatorLayout

class Tab2Fragment : BaseTabFragment() {

    private lateinit var fragmentContainer: CoordinatorLayout
    private lateinit var textInputQuery: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val layout = inflater.inflate(R.layout.fragment_tab2, container, false)
        with(layout) {
            fragmentContainer = findViewById(R.id.layout_tab2_root)
            textInputQuery = findViewById(R.id.text_input_query)
        }
        return layout
    }

    override fun attachToUI() {
        textInputQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(inputString: Editable) {
                respondToTextChange(inputString)
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun respondToTextChange(inputString: Editable) {
        "🔤 $inputString".snack(fragmentContainer)
    }

}
