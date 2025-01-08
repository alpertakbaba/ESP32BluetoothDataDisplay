package com.example.esp32bluetoothdatadisplay;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "laps.db";
    // Versiyonu 2 yaptık çünkü tablo yapısına yeni bir sütun (datetime) ekliyoruz.
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_LAPS = "laps";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LAP_NUMBER = "lap_number";
    private static final String COLUMN_LAP_TIME = "lap_time";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_IS_FASTEST = "is_fastest";

    // Yeni ekleyeceğimiz sütun
    private static final String COLUMN_DATETIME = "datetime";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LAPS_TABLE = "CREATE TABLE " + TABLE_LAPS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LAP_NUMBER + " INTEGER,"
                + COLUMN_LAP_TIME + " TEXT,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_IS_FASTEST + " INTEGER,"
                + COLUMN_DATETIME + " TEXT"         // <-- yeni sütunumuz
                + ")";
        db.execSQL(CREATE_LAPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Tablo yapısı değiştiğinde (ör. versiyon 2'ye geçerken), eski tabloyu düşürüp yeniden oluşturuyoruz.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LAPS);
        onCreate(db);
    }

    // Lap verilerini ekleme
    public void addLap(Lap lap) {
        SQLiteDatabase db = this.getWritableDatabase();

        // INSERT ifadesinde datetime sütununu da ekliyoruz
        String INSERT_LAP = "INSERT INTO " + TABLE_LAPS + "("
                + COLUMN_LAP_NUMBER + ", "
                + COLUMN_LAP_TIME + ", "
                + COLUMN_DATE + ", "
                + COLUMN_IS_FASTEST + ", "
                + COLUMN_DATETIME + ") VALUES ("
                + lap.getLapNumber() + ", '"
                + lap.getLapTime() + "', '"
                + lap.getDate() + "', "
                + (lap.isFastest() ? 1 : 0) + ", '"
                + lap.getTime() + "')";
        db.execSQL(INSERT_LAP);
        db.close();
    }

    // Tüm lap verilerini çekme
    public List<Lap> getAllLaps() {
        List<Lap> lapList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_LAPS + " ORDER BY " + COLUMN_LAP_NUMBER + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                // Burada yeni Lap constructor'ını datetime parametresini de geçerek çağırıyoruz
                Lap lap = new Lap(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LAP_NUMBER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAP_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FASTEST)) == 1,
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATETIME))
                );
                lapList.add(lap);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return lapList;
    }

    // En hızlı turu güncelleme
    public void updateFastestLap(int lapNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        String UPDATE_FASTEST = "UPDATE " + TABLE_LAPS + " SET " + COLUMN_IS_FASTEST + " = 0";
        db.execSQL(UPDATE_FASTEST);

        UPDATE_FASTEST = "UPDATE " + TABLE_LAPS + " SET " + COLUMN_IS_FASTEST + " = 1 WHERE " + COLUMN_LAP_NUMBER + " = " + lapNumber;
        db.execSQL(UPDATE_FASTEST);
        db.close();
    }

    // Lap tablosunu sıfırlama
    public void clearAllLaps() {
        SQLiteDatabase db = this.getWritableDatabase();
        String CLEAR_TABLE = "DELETE FROM " + TABLE_LAPS;
        db.execSQL(CLEAR_TABLE);
        db.close();
    }
}
