package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class RestaurantListActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private TextView tvWelcome;
    private TextInputEditText etCity;
    private Button btnSearch, btnLogout, btnLocation;
    private RecyclerView recyclerView;
    private RestaurantAdapter adapter;
    private List<String> restaurantList;
    private FirebaseAuth mAuth;
    private AppDatabase db;
    private DatabaseReference dbRef;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_list);

        mAuth = FirebaseAuth.getInstance();
        db = AppDatabase.getInstance(this);
        dbRef = FirebaseDatabase.getInstance().getReference("searches");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializa Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_places_api_key));
        }
        placesClient = Places.createClient(this);

        tvWelcome = findViewById(R.id.tvWelcome);
        etCity = findViewById(R.id.etCity);
        btnSearch = findViewById(R.id.btnSearch);
        btnLogout = findViewById(R.id.btnLogout);
        btnLocation = findViewById(R.id.btnLocation);
        recyclerView = findViewById(R.id.recyclerView);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            tvWelcome.setText("Olá, " + user.getDisplayName() + "!");
        }

        restaurantList = new ArrayList<>();
        adapter = new RestaurantAdapter(restaurantList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String city = etCity.getText().toString().trim();
            if (city.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }
            saveAndSearch(city);
        });

        btnLocation.setOnClickListener(v -> getLocation());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        Toast.makeText(this, "Obtendo localização...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String city = address.getLocality();
                        if (city == null) city = address.getSubAdminArea();
                        if (city == null) city = address.getAdminArea();
                        etCity.setText(city);
                        searchRestaurantsNearby(location.getLatitude(), location.getLongitude(), city);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Erro ao obter localização", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Localização não disponível. Ative o GPS.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchRestaurantsNearby(double lat, double lng, String city) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.RATING);
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);

        placesClient.findCurrentPlace(request).addOnSuccessListener(response -> {
            restaurantList.clear();
            for (var likelihood : response.getPlaceLikelihoods()) {
                Place place = likelihood.getPlace();
                if (place.getTypes() != null && place.getTypes().contains(Place.Type.RESTAURANT)) {
                    String name = place.getName() != null ? place.getName() : "Restaurante";
                    String address = place.getAddress() != null ? place.getAddress() : city;
                    restaurantList.add(name + " - " + address);
                }
            }
            if (restaurantList.isEmpty()) {
                saveAndSearch(city);
            } else {
                saveHistory(city);
                adapter.notifyDataSetChanged();
                Toast.makeText(this, restaurantList.size() + " restaurantes encontrados!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> saveAndSearch(city));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveHistory(String city) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String email = currentUser != null ? currentUser.getEmail() : "unknown";

        SearchHistory history = new SearchHistory(city, email, System.currentTimeMillis());
        Executors.newSingleThreadExecutor().execute(() ->
                db.searchHistoryDao().insert(history));

        String key = dbRef.push().getKey();
        if (key != null) {
            dbRef.child(key).child("city").setValue(city);
            dbRef.child(key).child("email").setValue(email);
            dbRef.child(key).child("timestamp").setValue(System.currentTimeMillis());
        }
    }

    private void saveAndSearch(String city) {
        saveHistory(city);
        restaurantList.clear();
        restaurantList.add("Restaurante Bom Sabor - " + city);
        restaurantList.add("Pizzaria Bella Napoli - " + city);
        restaurantList.add("Churrascaria do Sul - " + city);
        restaurantList.add("Sushi House - " + city);
        restaurantList.add("Cantina Italiana - " + city);
        adapter.notifyDataSetChanged();
    }
}