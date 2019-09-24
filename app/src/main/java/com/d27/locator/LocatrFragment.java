package com.d27.locator;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.BinderThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.StreamGifDecoder;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class LocatrFragment extends SupportMapFragment {
    public static final String TAG = LocatrFragment.class.getSimpleName();
    private GoogleApiClient mApiClient;
    private Bitmap mMapImage;
    private GalleryItem mMapItem;
    private Location mCurrentLocation;
    private GoogleMap mMap;

    public static LocatrFragment newInstance() {

        Bundle args = new Bundle();

        LocatrFragment fragment = new LocatrFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                //연결상태 정보 확인 위한 callback 추가
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getActivity().invalidateOptionsMenu(); //calls onPrepareOptionsMenu()
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .build();
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
//                updateUI();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().invalidateOptionsMenu(); //calls onPrepareOptionsMenu()
        mApiClient.connect();
    }

    @Override
    public void onStop() {
        mApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_locatr, menu);

        MenuItem searchItem = menu.findItem(R.id.action_locate);
        searchItem.setEnabled(mApiClient.isConnected());
    }

    private void findImage() {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mApiClient, request, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Log.i(TAG, "onLocationChanged: " + location.getLatitude() + " / " + location.getLongitude());
                        new SearchTask().execute(location);
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch ((item.getItemId())) {
            case R.id.action_locate:
                findImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class SearchTask extends AsyncTask<Location, Void, Void> {
        private Bitmap mBitmap;
        private GalleryItem mGalleryItem;
        private Location mLocation;

        @Override
        protected Void doInBackground(Location... locations) {
            mLocation = locations[0];
            FlickrFetchr fetchr = new FlickrFetchr();
            List<GalleryItem> items = fetchr.searchPhotos(locations[0]);

            if (items.size() == 0) {
                return null;
            }
            for (int i = 0; i < items.size(); i++) {
                if (Math.abs(items.get(i).getLat()) > 0 && Math.abs(items.get(i).getLat()) > 0) {
                    mGalleryItem = items.get(i);
                    break;
                }
            }
            mGalleryItem = items.get(0);
            try {
                mBitmap = Glide.with(getActivity()).asBitmap().load(mGalleryItem.getUrl()).submit().get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mMapImage = mBitmap;
            mMapItem = mGalleryItem;
            mCurrentLocation = mLocation;

            try {
                updateUI();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateUI() throws ExecutionException {
//        if(mMap ==null || mMapImage ==null){
        if (mMap == null) {
            return;
        }
        LatLng itemPoint = new LatLng(mMapItem.getLat(), mMapItem.getLon());
        LatLng myPoint = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        ;
        BitmapDescriptor itemBitmap = null;

        itemBitmap = BitmapDescriptorFactory.fromBitmap(mMapImage);

        MarkerOptions itemMarker = new MarkerOptions()
                .position(itemPoint)
                .icon(itemBitmap);
        MarkerOptions myMarker = new MarkerOptions()
                .position(myPoint);
        mMap.clear();
        mMap.addMarker(itemMarker);
        mMap.addMarker(myMarker);


        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(itemPoint)
                .include(myPoint)
                .build();
        int margin = getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margin);
        mMap.animateCamera(update);
    }
}
