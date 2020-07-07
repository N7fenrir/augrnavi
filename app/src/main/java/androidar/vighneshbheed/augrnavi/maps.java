package androidar.vighneshbheed.augrnavi;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.beyondar.android.world.World;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidar.vighneshbheed.augrnavi.ar.ArFragmentSupport;
import androidar.vighneshbheed.augrnavi.network.GeocodeResponse;
import androidar.vighneshbheed.augrnavi.network.RetrofitInterface;
import androidar.vighneshbheed.augrnavi.network.geocode.Location;
import androidar.vighneshbheed.augrnavi.onboarding.DefaultIntro;
import androidar.vighneshbheed.augrnavi.utils.UtilsCheck;
import androidar.vighneshbheed.augrnavi.utils.PermissionCheck;


import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class maps extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener
        , GoogleApiClient.ConnectionCallbacks, GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener {

    private final static String TAG = "maps";


    SharedPreferences getPrefs;
    boolean isFirstStart;

    private GoogleApiClient googleApiClient;
    private GoogleMap mMap;

    private Location location;
    private LocationManager locationManager;

    private Marker RevMarker;

    @BindView(R.id.fab_menu_btn)
    FloatingActionMenu fab_menu;
    @BindView(R.id.poi_browser_btn)
    com.github.clans.fab.FloatingActionButton poi_browser_btn;
//    @BindView(R.id.decode_box)
//    EditText decode_editText;
//    @BindView(R.id.decode_btn)
//    Button decode_button;
    @BindView(R.id.progressBar_maps)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

        Init_intro();
        PermissionCheck.initialPermissionCheckAll(this, this);

        progressBar.setVisibility(View.GONE);

        if (!UtilsCheck.isNetworkConnected(this)) {
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_content),
                    "Turn Internet On", Snackbar.LENGTH_SHORT);
            mySnackbar.show();
        }

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        poi_browser_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(maps.this, PoiBrowser.class);
                startActivity(intent);
            }
        });


//        decode_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                try {
//                    if (TextUtils.isEmpty(decode_editText.getText())) {
//                        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_content),
//                                "Search Field is Empty", Snackbar.LENGTH_SHORT);
//                        mySnackbar.show();
//                    } else {
//                        Geocode_Call(decode_editText.getText().toString());
//                    }
//                } catch (NullPointerException npe) {
//                    Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_content),
//                            "Search Field is Empty", Snackbar.LENGTH_SHORT);
//                    mySnackbar.show();
//                }
//            }
//        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    void Geocode_Call(String address) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        progressBar.setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.directions_base_url))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitInterface apiService =
                retrofit.create(RetrofitInterface.class);

        final Call<GeocodeResponse> call = apiService.getGecodeData(address,
                getResources().getString(R.string.google_maps_key));

        call.enqueue(new Callback<GeocodeResponse>() {
            @Override
            public void onResponse(Call<GeocodeResponse> call, Response<GeocodeResponse> response) {

                progressBar.setVisibility(View.GONE);

                List<androidar.vighneshbheed.augrnavi.network.geocode.Result> results = response.body().getResults();
                location = results.get(0).getGeometry().getLocation();
                Toast.makeText(maps.this, location.getLat() + "," + location.getLng(), Toast.LENGTH_SHORT).show();

                try {
                    mMap.clear();
                    LatLng loc = new LatLng(location.getLat(), location.getLng());
                    mMap.addMarker(new MarkerOptions()
                            .position(loc)
                            .title(results.get(0).getFormattedAddress())
                            .snippet(results.get(0).getGeometry().getLocationType()));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 14.0f));
                    mMap.getUiSettings().setMapToolbarEnabled(false);
                } catch (NullPointerException npe) {
                    Log.d(TAG, "onMapReady: Location is NULL");
                }
            }

            @Override
            public void onFailure(Call<GeocodeResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(maps.this, "Invalid Request", Toast.LENGTH_SHORT).show();
            }
        });

    }

    void Rev_Geocode_Call(LatLng latlng) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.directions_base_url))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        progressBar.setVisibility(View.VISIBLE);

        RetrofitInterface apiService =
                retrofit.create(RetrofitInterface.class);

        final Call<GeocodeResponse> call = apiService.getRevGecodeData(latlng.latitude + "," + latlng.longitude,
                getResources().getString(R.string.google_maps_key));

        call.enqueue(new Callback<GeocodeResponse>() {
            @Override
            public void onResponse(Call<GeocodeResponse> call, Response<GeocodeResponse> response) {

                progressBar.setVisibility(View.GONE);
                List<androidar.vighneshbheed.augrnavi.network.geocode.Result> results = response.body().getResults();
                String address = results.get(0).getFormattedAddress();
                Toast.makeText(maps.this, address, Toast.LENGTH_SHORT).show();

                RevMarker.setTitle(address);
                RevMarker.setSnippet(results.get(0).getGeometry().getLocationType());
            }

            @Override
            public void onFailure(Call<GeocodeResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(maps.this, "Invalid Request", Toast.LENGTH_SHORT).show();
            }
        });

    }

    void Init_intro() {

        getPrefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        isFirstStart = getPrefs.getBoolean("firstStart", true);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                Intent i = new Intent(maps.this, DefaultIntro.class);
                startActivity(i);

                SharedPreferences.Editor e = getPrefs.edit();

                e.putBoolean("firstStart", false);

                e.apply();
            }
        });

        if (isFirstStart) {
            t.start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 10: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }

        }
    }


    @Override
    public void onMapLongClick(LatLng latLng) {

        mMap.clear();

        RevMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        Toast.makeText(this, latLng.latitude + " " + latLng.longitude, Toast.LENGTH_SHORT).show();
        Rev_Geocode_Call(latLng);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng myPosition;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        android.location.Location location = locationManager.getLastKnownLocation(provider);


        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng latLng = new LatLng(latitude, longitude);
            myPosition = new LatLng(latitude, longitude);


            LatLng coordinate = new LatLng(latitude, longitude);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 12);
            mMap.animateCamera(yourLocation);
        }


        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        mMap.setOnMapLongClickListener(this);
        Log.d(TAG, "onMapReady: MAP IS READY");

    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick: Short Click " + latLng.toString());
    }
}
