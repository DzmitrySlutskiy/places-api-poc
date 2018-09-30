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

package com.google.api.places.places_api_poc.model

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.places.*
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.OnCompleteListener
import com.google.api.places.places_api_poc.daggger.AutocompletePredictionsLiveData
import com.google.api.places.places_api_poc.daggger.LocationLiveData
import com.google.api.places.places_api_poc.daggger.ModalPlaceDetailsSheetLiveData
import com.google.api.places.places_api_poc.daggger.PlacesLiveData
import com.google.api.places.places_api_poc.misc.ExecutorWrapper
import com.google.api.places.places_api_poc.misc.getMyApplication
import com.google.api.places.places_api_poc.misc.isPermissionGranted
import com.google.api.places.places_api_poc.misc.log
import java.util.concurrent.ExecutorService
import javax.inject.Inject


class PlacesAPI(val app: Application) : AndroidViewModel(app), LifecycleObserver {

    // Places API Clients.
    @Inject
    lateinit var currentPlaceClient: PlaceDetectionClient
    @Inject
    lateinit var geoDataClient: GeoDataClient

    // Fused Location Provider Client.
    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Background Executor.
    @Inject
    lateinit var executorWrapper: ExecutorWrapper

    // Find Last Location.
    lateinit var getLastLocation: GetLastLocation

    // Find Current Place.
    lateinit var getCurrentPlace: GetCurrentPlace
    @Inject
    lateinit var getCurrentPlacesLiveData: PlacesLiveData

    // Fetch Place by ID.
    lateinit var getPlaceByID: GetPlaceByID

    // Fetch Autocomplete Predictions.
    lateinit var autocompletePredictions: AutocompletePredictions

    // Get Place Photos
    lateinit var getPlacePhotos: GetPlacePhotos

    // Get Photo.
    lateinit var getPhoto: GetPhoto

    // Modal "Place Details Sheet" Data.
    @Inject
    lateinit var modalPlaceDetailsSheetLiveData: ModalPlaceDetailsSheetLiveData

    // Autocomplete predictions Data.
    @Inject
    lateinit var autocompletePredictionsLiveData: AutocompletePredictionsLiveData

    // Get last location Data.
    @Inject
    lateinit var locationLiveData: LocationLiveData

    /**
     * When this object is constructed, setup the Dagger 2 subcomponent (@ActivityScope).
     * This can't be done in connect(), as the DriverActivity needs this to be setup as
     * soon as the ViewModel object is created (which eliminates any race conditions w/
     * Activity lifecycle, and lifecycle observer lifecycle (that's observing an Activity).
     */
    init {
        // Dagger 2 subcomponent creation.
        app.getMyApplication()
                .createActivityComponent()
                .inject(this)
    }

    //
    // Activity lifecycle.
    //

    // Lifecycle hooks.
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun connect() {
        "ON_CREATE ⇢ PlacesAPI.connect() ✅".log()

        "💥 connect() - got GetDataClient, PlaceDetectionClient, FusedLocationProviderClient".log()

        "ON_CREATE ⇢ Create Executor ✅".log()
        executorWrapper.create()

        "ON_CREATE ⇢ Create API wrappers ✅".log()
        getCurrentPlace = GetCurrentPlace(executorWrapper.executor,
                                          app,
                                          currentPlaceClient,
                                          getCurrentPlacesLiveData)
        getPlaceByID = GetPlaceByID(executorWrapper.executor,
                                    geoDataClient,
                                    modalPlaceDetailsSheetLiveData)
        autocompletePredictions = AutocompletePredictions(executorWrapper.executor,
                                                          geoDataClient,
                                                          autocompletePredictionsLiveData)
        getLastLocation = GetLastLocation(executorWrapper.executor,
                                          fusedLocationProviderClient,
                                          app,
                                          locationLiveData)
        getPhoto = GetPhoto(executorWrapper.executor,
                            geoDataClient,
                            modalPlaceDetailsSheetLiveData)
        getPlacePhotos = GetPlacePhotos(executorWrapper.executor,
                                        geoDataClient,
                                        getPhoto)
        "💥 connect() - complete!".log()

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun cleanup() {
        "ON_DESTROY ⇢ PlacesAPI cleanup ✅".log()
        executorWrapper.destroy()
        "🚿 cleanup() - complete!".log()
        app.getMyApplication().destroyActivityComponent()
    }

}

//
// Get Photo.
//

class GetPhoto(private val executor: ExecutorService,
               private val geoDataClient: GeoDataClient,
               private val modalPlaceDetailsSheetLiveData: ModalPlaceDetailsSheetLiveData) {

    fun execute(photoMetadata: PlacePhotoMetadata, attribution: CharSequence) {
        // Get a full-size bitmap for the photo.
        "PlacesAPI ⇢ GeoDataClient.getPhoto() ✅".log()
        geoDataClient.getPhoto(photoMetadata).let { requestTask ->
            requestTask.addOnCompleteListener(
                    executor,
                    OnCompleteListener { responseTask ->
                        if (responseTask.isSuccessful) {
                            processPhoto(responseTask.result.bitmap, attribution)
                        } else {
                            "⚠️ Task failed with exception ${responseTask.exception}".log()
                        }
                    }
            )
        }
    }

    private fun processPhoto(bitmap: Bitmap, attribution: CharSequence) {
        modalPlaceDetailsSheetLiveData.bitmap.postValue(
                BitmapWrapper(bitmap,
                              attribution.toString())
        )
    }

}

//
// Get Place Photos.
//

class GetPlacePhotos(private val executor: ExecutorService,
                     private val geoDataClient: GeoDataClient,
                     private val getPhoto: GetPhoto) {

    fun execute(placeId: String) {
        "PlacesAPI ⇢ GeoDataClient.getPlacePhotos() ✅".log()
        // Run this in background thread.
        geoDataClient.getPlacePhotos(placeId).let { requestTask ->
            requestTask.addOnCompleteListener(
                    executor,
                    OnCompleteListener { responseTask ->
                        if (responseTask.isSuccessful) {
                            processPhotosMetadata(responseTask.result)
                        } else {
                            "⚠️ Task failed with exception ${responseTask.exception}".log()
                        }
                    }
            )
        }

    }

    // This runs in a background thread.
    private fun processPhotosMetadata(photos: PlacePhotoMetadataResponse) {

        // Get the PlacePhotoMetadataBuffer (metadata for all of the photos).
        val photoMetadataBuffer = photos.photoMetadata

        val count = photoMetadataBuffer.count

        if (count > 0) {
            // Get the first photo in the list.
            val photoMetadata = photoMetadataBuffer.get(0)

            // Get the attribution text.
            val attribution = photoMetadata.attributions

            // Actually get the photo.
            getPhoto.execute(photoMetadata, attribution)
        }

    }

}

//
// Get Last Location.
//

class GetLastLocation(private val executor: ExecutorService,
                      private val currentLocationClient: FusedLocationProviderClient,
                      private val context: Application,
                      private val liveData: LocationLiveData) {

    /**
     * This function won't execute if FINE_ACCESS_LOCATION permission is not granted.
     */
    @SuppressLint("MissingPermission")
    fun execute() {
        if (isPermissionGranted(context,
                                ACCESS_FINE_LOCATION)) {
            "PlacesAPI ⇢ FusedLocationProviderClient.lastLocation() ✅".log()
            currentLocationClient.lastLocation.let { requestTask ->
                // Run this in background thread.
                requestTask.addOnCompleteListener(
                        executor,
                        OnCompleteListener { responseTask ->
                            if (responseTask.isSuccessful && responseTask.result != null) {
                                processCurrentLocation(responseTask.result)
                            } else {
                                "⚠️ Task failed with exception ${responseTask.exception}".log()
                            }
                        }
                )
            }
        }
    }

    // This runs in a background thread.
    private fun processCurrentLocation(value: Location) {
        liveData.postValue(value)
    }

}

//
// Place Autocomplete.
//

class AutocompletePredictions(private val executor: ExecutorService,
                              private val geoDataClient: GeoDataClient,
                              private val liveData: AutocompletePredictionsLiveData) {

    private val defaultFilter = AutocompleteFilter.Builder()
            .setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE)
            .build()

    fun execute(queryString: String,
                bounds: LatLngBounds,
                filter: AutocompleteFilter = defaultFilter) {
        "PlacesAPI ⇢ GeoDataClient.getAutocompletePredictions() ✅".log()
        geoDataClient.getAutocompletePredictions(queryString, bounds, filter)
                .let { requestTask ->
                    // Run this in background thread.
                    requestTask.addOnCompleteListener(
                            executor,
                            OnCompleteListener { responseTask ->
                                if (responseTask.isSuccessful) {
                                    processAutocompletePrediction(responseTask.result)
                                    responseTask.result.release()
                                } else {
                                    "⚠️ Task failed with exception ${responseTask.exception}".log()
                                }
                            }
                    )
                }
    }

    // This runs in a background thread.
    private fun processAutocompletePrediction(buffer: AutocompletePredictionBufferResponse) {
        val count = buffer.count

        if (count == 0) {
            "⚠️ No autocomplete predictions found".log()
            return
        }

        val outputList: MutableList<AutocompletePredictionData> = mutableListOf()

        for (index in 0 until count) {
            val item = buffer.get(index)
            outputList.add(AutocompletePredictionData(
                    placeId = item.placeId,
                    placeTypes = item.placeTypes,
                    fullText = item.getFullText(null),
                    primaryText = item.getPrimaryText(null),
                    secondaryText = item.getSecondaryText(null)
            ))
        }

        // Dump the list of AutocompletePrediction objects to logcat.
        outputList.joinToString("\n").log()

        // Update the LiveData, so observables can react to this change.
        liveData.postValue(outputList)

    }

}

//
// Place IDs and Details.
//

class GetPlaceByID(private val executor: ExecutorService,
                   private val geoDataClient: GeoDataClient,
                   private val modalPlaceDetailsSheetData: ModalPlaceDetailsSheetLiveData) {

    fun execute(placeId: String) {
        "PlacesAPI ⇢ GeoDataClient.getPlaceById() ✅".log()
        geoDataClient.getPlaceById(placeId).let { requestTask ->
            // Run this in background thread.
            requestTask.addOnCompleteListener(
                    executor,
                    OnCompleteListener { responseTask ->
                        if (responseTask.isSuccessful) {
                            processPlace(responseTask.result)
                            responseTask.result.release()
                        } else {
                            "⚠️ Task failed with exception ${responseTask.exception}".log()
                        }
                    }
            )
        }
    }

    // This runs in a background thread.
    private fun processPlace(placeBufferResponse: PlaceBufferResponse) {
        val place = placeBufferResponse.get(0)
        modalPlaceDetailsSheetData.postPlace(PlaceWrapper(
                place))
    }

}

//
// Current Place.
//

class GetCurrentPlace(private val executor: ExecutorService,
                      private val context: Context,
                      private val currentPlaceClient: PlaceDetectionClient,
                      private val liveData: PlacesLiveData) {

    /**
     * This function won't execute if FINE_ACCESS_LOCATION permission is not granted.
     */
    @SuppressLint("MissingPermission")
    fun execute() {
        if (isPermissionGranted(context,
                                ACCESS_FINE_LOCATION)) {
            // Permission is granted 🙌.
            "PlacesAPI ⇢ PlaceDetectionClient.getCurrentPlace() ✅".log()
            currentPlaceClient.getCurrentPlace(null).let { requestTask ->
                // Run this in background thread.
                requestTask.addOnCompleteListener(
                        executor,
                        OnCompleteListener { responseTask ->
                            if (responseTask.isSuccessful) {
                                processPlacelikelihoodBuffer(responseTask.result)
                                responseTask.result.release()
                            } else {
                                "⚠️ Task failed with exception ${responseTask.exception}".log()
                            }
                        })
            }
        }
    }

    /**
     * This runs in the background thread.
     * [PlaceLikelihoodBufferResponse docs](http://tinyurl.com/y9y9jl3d).
     */
    private fun processPlacelikelihoodBuffer(likeyPlaces: PlaceLikelihoodBufferResponse) {
        val outputList = mutableListOf<PlaceWrapper>()
        val count = likeyPlaces.count
        for (index in 0 until count) {
            outputList.add(PlaceWrapper(likeyPlaces.get(
                    index)))
        }

        // Dump the list of PlaceWrapper objects to logcat.
        outputList.joinToString("\n").log()

        // Update the LiveData, so observables can react to this change.
        liveData.postValue(outputList)
    }

}