package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class RestaurantListActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextInputEditText etCity;
    private Button btnSearch, btnLogout;
    private RecyclerView recyclerView;
    private RestaurantAdapter adapter;
    private List<String> restaurantList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_list);

        tvWelcome = findViewById(R.id.tvWelcome);
        etCity = findViewById(R.id.etCity);
        btnSearch = findViewById(R.id.btnSearch);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerView = findViewById(R.id.recyclerView);

        // Pega o nome do usuário passado pelo login
        String userName = getIntent().getStringExtra("user_name");
        if (userName != null && !userName.isEmpty()) {
            tvWelcome.setText("Olá, " + userName + "!");
        }

        // Configura RecyclerView
        restaurantList = new ArrayList<>();
        adapter = new RestaurantAdapter(restaurantList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = etCity.getText().toString().trim();
                if (city.isEmpty()) {
                    Toast.makeText(RestaurantListActivity.this,
                            getString(R.string.msg_fill_all_fields),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // Por enquanto lista fictícia — Google Places API vem depois
                loadMockRestaurants(city);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RestaurantListActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void loadMockRestaurants(String city) {
        restaurantList.clear();
        restaurantList.add("Restaurante Bom Sabor - " + city);
        restaurantList.add("Pizzaria Bella Napoli - " + city);
        restaurantList.add("Churrascaria do Sul - " + city);
        restaurantList.add("Sushi House - " + city);
        restaurantList.add("Cantina Italiana - " + city);
        adapter.notifyDataSetChanged();
    }
}