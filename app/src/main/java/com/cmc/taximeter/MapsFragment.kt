package com.cmc.taximeter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsFragment : Fragment(R.layout.fragment_maps), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var destinationMarker: Marker? = null  // Marker pour le point d'arrivée

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Vérification des permissions
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        mMap.isMyLocationEnabled = true

        // Centrage initial sur Casablanca
        val casablanca = LatLng(33.5731, -7.5898)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(casablanca, 12f))

        // Écoute du clic sur la carte pour choisir le point d'arrivée
        mMap.setOnMapClickListener { latLng ->
            // Supprime l'ancien marker si existe
            destinationMarker?.remove()

            // Ajoute un marker sur le point cliqué
            destinationMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Point d'arrivée")
            )

            // Optionnel : recentre la caméra sur le point choisi
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }
}
