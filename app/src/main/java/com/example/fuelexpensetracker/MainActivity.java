package com.example.fuelexpensetracker;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    DatabaseHelper databaseHelper;

    EditText etVehicleName, etQuantity, etPrice;
    Spinner spFuelType;

    TextView tvTotalExpenses, tvTotalLitres;
    TextView tvRefillCount, tvCalculatedCost;

    Button btnSave, btnClear;
    ListView listFuelRecords;

    ArrayList<String> fuelRecords;
    ArrayList<Integer> recordIds;

    ArrayAdapter<String> adapter;

    int editingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);

        etVehicleName = findViewById(R.id.etVehicleName);
        etQuantity = findViewById(R.id.etQuantity);
        etPrice = findViewById(R.id.etPrice);

        spFuelType = findViewById(R.id.spFuelType);

        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvTotalLitres = findViewById(R.id.tvTotalLitres);
        tvRefillCount = findViewById(R.id.tvRefillCount);
        tvCalculatedCost = findViewById(R.id.tvCalculatedCost);

        btnSave = findViewById(R.id.btnSave);
        btnClear = findViewById(R.id.btnClear);
        listFuelRecords = findViewById(R.id.listFuelRecords);

        setupFuelSpinner();
        setupCostCalculation();
        setupRecordList();

        btnSave.setOnClickListener(v -> saveFuelRecord());
        btnClear.setOnClickListener(v -> clearForm());

        refreshData();
    }
fghgdf
    private void setupFuelSpinner() {
        String[] fuelTypes = {"Petrol", "Diesel", "CNG"};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                fuelTypes
        );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spFuelType.setAdapter(spinnerAdapter);
    }

    private void setupCostCalculation() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                calculateCost();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etQuantity.addTextChangedListener(watcher);
        etPrice.addTextChangedListener(watcher);
    }

    private double getNumber(EditText field) {
        String value = field.getText().toString().trim();

        if (value.isEmpty()) {
            return 0;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void calculateCost() {
        double total = getNumber(etQuantity) * getNumber(etPrice);

        tvCalculatedCost.setText(String.format(
                Locale.getDefault(),
                "Total Cost: ₹%.2f",
                total
        ));
    }

    private void saveFuelRecord() {
        String vehicleName = etVehicleName.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();
        String priceText = etPrice.getText().toString().trim();

        if (vehicleName.isEmpty()) {
            etVehicleName.setError("Enter vehicle name");
            etVehicleName.requestFocus();
            return;
        }

        if (quantityText.isEmpty()) {
            etQuantity.setError("Enter quantity");
            etQuantity.requestFocus();
            return;
        }

        if (priceText.isEmpty()) {
            etPrice.setError("Enter price per litre");
            etPrice.requestFocus();
            return;
        }

        double quantity;
        double price;

        try {
            quantity = Double.parseDouble(quantityText);
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Enter valid numbers",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!Double.isFinite(quantity) || !Double.isFinite(price)
                || quantity <= 0 || price <= 0
                || !Double.isFinite(quantity * price)) {

            Toast.makeText(
                    this,
                    "Enter valid positive quantity and price",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String fuelType = spFuelType.getSelectedItem().toString();
        double totalCost = quantity * price;

        boolean success;

        if (editingId == -1) {
            long result = databaseHelper.addFuelRecord(
                    vehicleName,
                    fuelType,
                    quantity,
                    price,
                    totalCost
            );

            success = result != -1;
        } else {
            int result = databaseHelper.updateFuelRecord(
                    editingId,
                    vehicleName,
                    fuelType,
                    quantity,
                    price,
                    totalCost
            );

            success = result > 0;
        }

        if (success) {
            Toast.makeText(
                    this,
                    editingId == -1
                            ? "Fuel record saved"
                            : "Fuel record updated",
                    Toast.LENGTH_SHORT
            ).show();

            clearForm();
            refreshData();
        } else {
            Toast.makeText(
                    this,
                    "Unable to save record",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setupRecordList() {
        fuelRecords = new ArrayList<>();
        recordIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                fuelRecords
        );

        listFuelRecords.setAdapter(adapter);

        listFuelRecords.setOnItemClickListener(
                (parent, view, position, id) ->
                        loadRecordForEditing(recordIds.get(position))
        );

        listFuelRecords.setOnItemLongClickListener(
                (parent, view, position, id) -> {
                    int recordId = recordIds.get(position);

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Fuel Record")
                            .setMessage("Do you want to delete this record?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                int result =
                                        databaseHelper.deleteFuelRecord(recordId);

                                if (result > 0) {
                                    if (editingId == recordId) {
                                        clearForm();
                                    }

                                    refreshData();

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Record deleted",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();

                    return true;
                }
        );
    }

    private void loadRecordForEditing(int recordId) {
        Cursor cursor = databaseHelper.getFuelRecordById(recordId);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                editingId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id")
                );

                etVehicleName.setText(cursor.getString(
                        cursor.getColumnIndexOrThrow("vehicle_name")
                ));

                String fuelType = cursor.getString(
                        cursor.getColumnIndexOrThrow("fuel_type")
                );

                for (int i = 0; i < spFuelType.getCount(); i++) {
                    if (spFuelType.getItemAtPosition(i)
                            .toString().equals(fuelType)) {
                        spFuelType.setSelection(i);
                        break;
                    }
                }

                etQuantity.setText(String.valueOf(cursor.getDouble(
                        cursor.getColumnIndexOrThrow("quantity")
                )));

                etPrice.setText(String.valueOf(cursor.getDouble(
                        cursor.getColumnIndexOrThrow("price_per_litre")
                )));

                btnSave.setText("Update Fuel Record");
                calculateCost();
            }

            cursor.close();
        }
    }

    private void refreshData() {
        fuelRecords.clear();
        recordIds.clear();

        Cursor cursor = databaseHelper.getAllFuelRecords();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id")
                );

                String vehicleName = cursor.getString(
                        cursor.getColumnIndexOrThrow("vehicle_name")
                );

                String fuelType = cursor.getString(
                        cursor.getColumnIndexOrThrow("fuel_type")
                );

                double quantity = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("quantity")
                );

                double price = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("price_per_litre")
                );

                double totalCost = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("total_cost")
                );

                recordIds.add(id);

                fuelRecords.add(String.format(
                        Locale.getDefault(),
                        "%s\n%s\nFuel: %.2f litres\nPrice: ₹%.2f/litre\nTotal: ₹%.2f",
                        vehicleName,
                        fuelType,
                        quantity,
                        price,
                        totalCost
                ));
            }

            cursor.close();
        }

        adapter.notifyDataSetChanged();
        updateSummary();
    }

    private void updateSummary() {
        tvTotalExpenses.setText(String.format(
                Locale.getDefault(),
                "Total Expenses: ₹%.2f",
                databaseHelper.getTotalExpenses()
        ));

        tvTotalLitres.setText(String.format(
                Locale.getDefault(),
                "Total Fuel: %.2f litres",
                databaseHelper.getTotalLitres()
        ));

        tvRefillCount.setText(
                "Total Refills: " + databaseHelper.getRecordCount()
        );
    }

    private void clearForm() {
        editingId = -1;

        etVehicleName.setText("");
        etQuantity.setText("");
        etPrice.setText("");

        spFuelType.setSelection(0);

        btnSave.setText("Save Fuel Record");
        calculateCost();
    }
}