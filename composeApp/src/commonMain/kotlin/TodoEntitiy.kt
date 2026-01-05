@Entity
data class TodoEntitiy (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
)