import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.cs407.badgerstudy.User

@Dao
interface UserDao {

    // Inserts a user into the database
    @Insert
    suspend fun insertUser(user: User)

    // Retrieves the first user from the table (optional, still useful for specific cases)
    @Query("SELECT * FROM user_table LIMIT 1")
    suspend fun getFirstUser(): User?

    // Retrieves a user by username
    @Query("SELECT * FROM user_table WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    // Retrieves all users from the database
    @Query("SELECT * FROM user_table")
    suspend fun getAllUsers(): List<User>

    // Updates a user's password (can be used for password resets)
    @Query("UPDATE user_table SET password = :newPassword WHERE username = :username")
    suspend fun updatePassword(username: String, newPassword: String)

    // Deletes a user by username
    @Query("DELETE FROM user_table WHERE username = :username")
    suspend fun deleteUser(username: String)

    // Deletes all users (useful for testing or clearing the table)
    @Query("DELETE FROM user_table")
    suspend fun deleteAllUsers()
}
