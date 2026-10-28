package com.example.fuelexpensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "FuelExpense.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "FuelRecords";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "vehicle_name TEXT NOT NULL, " +
                "fuel_type TEXT NOT NULL, " +
                "quantity REAL NOT NULL, " +
                "price_per_litre REAL NOT NULL, " +
                "total_cost REAL NOT NULL)";

        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("CREATE TABLE FuelRecords_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "vehicle_name TEXT NOT NULL, " +
                "fuel_type TEXT NOT NULL, " +
                "quantity REAL NOT NULL, " +
                "price_per_litre REAL NOT NULL, " +
                "total_cost REAL NOT NULL)");

        db.execSQL("INSERT INTO FuelRecords_new " +
                "(id, vehicle_name, fuel_type, quantity, price_per_litre, total_cost) " +
                "SELECT id, vehicle_name, fuel_type, quantity, price_per_litre, total_cost " +
                "FROM FuelRecords");

        db.execSQL("DROP TABLE FuelRecords");
        db.execSQL("ALTER TABLE FuelRecords_new RENAME TO FuelRecords");
    }

    public long addFuelRecord(String vehicleName, String fuelType,
                              double quantity, double pricePerLitre,
                              double totalCost) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("vehicle_name", vehicleName);
        values.put("fuel_type", fuelType);
        values.put("quantity", quantity);
        values.put("price_per_litre", pricePerLitre);
        values.put("total_cost", totalCost);

        long result = db.insert(TABLE_NAME, null, values);
        db.close();

        return result;
    }

    public int updateFuelRecord(int id, String vehicleName,
                                String fuelType, double quantity,
                                double pricePerLitre, double totalCost) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("vehicle_name", vehicleName);
        values.put("fuel_type", fuelType);
        values.put("quantity", quantity);
        values.put("price_per_litre", pricePerLitre);
        values.put("total_cost", totalCost);

        int result = db.update(
                TABLE_NAME,
                values,
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();
        return result;
    }

    public Cursor getAllFuelRecords() {
        SQLiteDatabase db = getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " ORDER BY id DESC",
                null
        );
    }

    public Cursor getFuelRecordById(int id) {
        SQLiteDatabase db = getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public int deleteFuelRecord(int id) {
        SQLiteDatabase db = getWritableDatabase();

        int result = db.delete(
                TABLE_NAME,
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();
        return result;
    }

    public double getTotalExpenses() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT IFNULL(SUM(total_cost), 0) FROM " + TABLE_NAME,
                null
        );

        double total = 0;

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        db.close();

        return total;
    }

    public double getTotalLitres() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT IFNULL(SUM(quantity), 0) FROM " + TABLE_NAME,
                null
        );

        double total = 0;

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        db.close();

        return total;
    }

    public int getRecordCount() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_NAME,
                null
        );

        int count = 0;

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return count;
    }
}