package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val defaultPrice: Int
)

@Entity(tableName = "audit_snapshots")
data class AuditSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val timestamp: Long,
    val systemTotal: Int,
    val cashLeft: Int,
    val amountHandedOver1: String,
    val amountHandedOver2: String,
    val amountHandedOver3: String,
    val productsJson: String
)

@Dao
interface CashierDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("SELECT * FROM audit_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<AuditSnapshotEntity>>

    @Insert
    suspend fun insertSnapshot(snapshot: AuditSnapshotEntity)

    @Query("DELETE FROM audit_snapshots WHERE id = :id")
    suspend fun deleteSnapshot(id: Int)
}

@Database(entities = [ProductEntity::class, AuditSnapshotEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cashierDao(): CashierDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cashier_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
