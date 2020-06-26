package com.mapzen.valhalla

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.MalformedURLException
import java.util.ArrayList
import java.util.Locale

open class ValhallaRouter : Router {

    private var language: String? = null
    private var type = Router.Type.DRIVING
    private val locations = ArrayList<JSON.Location>()
    private var maxDifficulty = 1
    private var callback: RouteCallback? = null
    private var units: Router.DistanceUnits = Router.DistanceUnits.KILOMETERS

    private var httpHandler: HttpHandler? = null

    override fun setHttpHandler(handler: HttpHandler): Router {
        httpHandler = handler
        return this
    }

    override fun setLanguage(language: Router.Language): Router {
        this.language = language.toString()
        return this
    }

    override fun setWalking(): Router {
        this.type = Router.Type.WALKING
        return this
    }

    override fun setDriving(): Router {
        this.type = Router.Type.DRIVING
        return this
    }

    override fun setBiking(): Router {
        this.type = Router.Type.BIKING
        return this
    }

    override fun setMultimodal(): Router {
        this.type = Router.Type.MULTIMODAL
        return this
    }

    override fun setLocation(point: DoubleArray): Router {
        this.locations.add(JSON.Location(point[0], point[1]))
        return this
    }

    override fun setLocation(point: DoubleArray, heading: Int): Router {
        this.locations.add(JSON.Location(point[0], point[1], heading))
        return this
    }

    override fun setLocation(point: DoubleArray,
            name: String?,
            street: String?,
            city: String?,
            state: String?): Router {

        this.locations.add(JSON.Location(point[0], point[1],
                name, street, city, state));
        return this
    }

    override fun setDistanceUnits(units: Router.DistanceUnits): Router {
        this.units = units
        return this
    }

    override fun setMaxHikingDifficulty(difficulty: Int): Router {
        this.maxDifficulty = difficulty
        return this
    }

    override fun clearLocations(): Router {
        this.locations.clear()
        return this
    }

    override fun setCallback(callback: RouteCallback): Router {
        this.callback = callback
        return this
    }

    override fun fetch(): Call<String>? {
        return httpHandler?.requestRoute(getJSONRequest(), object: Callback<String> {
            override fun onResponse(call: Call<String>?, response: Response<String>?) {
                if (response != null) {
                    if (response.isSuccessful && response.body() != null) {
                        response.body()?.let { callback?.success(Route(it)) }
                    } else {
                        callback?.failure(response.raw().code())
                    }
                }
            }

            override fun onFailure(call: Call<String>?, t: Throwable?) {
                t?.printStackTrace()
            }
        })
    }

    override fun getJSONRequest(): JSON {
        if (locations.size < 2) {
            throw  MalformedURLException()
        }
        var json = JSON()
        for (i in 0..(locations.size-1)){
            json.locations.add(locations[i])
        }

        if (language == null) {
            language = getDefaultLanguage()
        }

        json.costing = this.type.toString()
        json.directionsOptions.language = language
        json.directionsOptions.units = this.units.toString()
        json.costingOptions.maxHikingDifficulty = this.maxDifficulty.toString()
        return json
    }

    fun getDefaultLanguage(): String? {
        val locale = Locale.getDefault().language + "-" + Locale.getDefault().country
        return if (Router.Language.values().any { it.toString() == locale }) locale
            else Locale.getDefault().language
    }
}
